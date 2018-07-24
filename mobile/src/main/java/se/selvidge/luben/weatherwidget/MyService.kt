package se.selvidge.luben.weatherwidget

import android.annotation.SuppressLint
import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.*
import android.database.sqlite.SQLiteConstraintException
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import com.google.android.gms.location.places.Place
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.locationManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.models.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class MyService : IntentService("myService") {
    //todone add alarm manager to update weather
    //todone listen after intents and react aproriatly, widget button etc
    //maybe use Machine learning to train weather -> clothes model

    companion object {
        fun getWeatherModel(context: Context) {
//            return data
            Log.d(TAG, "from static call")
            context.sendBroadcast(Intent(context, MyService::class.java))
        }

        fun getWeatherView(dest: Destination,context: Context):List<WeatherView>{
            val me = MyService()
            me.db = AppDatabase.getDatabase(context)
            return me.getWeatherView(dest,!dest.comuteStartIntervalStart.IsToday())//todo needs to handle both wraparound and sameday

        }
        var widget: NewAppWidget? = null
        val syncAction = "NETWORK_SYNC"
        val updateViewAction = "VIEW_UPDATE"
        val halfHourInMs = 1800000
        val TAG = "SERVICE"
        var now: Long = 0
            get() = Date().time
        var myself: MyService? = null
//        var data = "not updated yet"
    }

    var data = ""
    var client = OkHttpClient()
    var viewModel = listOf<WeatherView>()
    val viewModelUpdated = Intent(MainActivity.VIEW_MODEL_UPDATED)
    val mBinder = LocalBinder()
    val weatherPointResolutionKm = 2
    val weatherPointResolutionSeconds = 1200
    lateinit var db: AppDatabase


    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

//    var reciverRegister = false
    var receiver = object : BroadcastReceiver() {
        override fun onReceive(broadcastContext: Context, broadcastIntent: Intent) {
//    override fun onHandleIntent(broadcastIntent: Intent) {
//        val broadcastContext = this
            Log.d(TAG,"broadcastContext = [${broadcastContext}], broadcastIntent = [${broadcastIntent}]")
        broadcastContext.startService(broadcastIntent)
//            doUpdate()
        }

    }

    override fun onDestroy() {
////
        super.onDestroy()//todo either unregister when done or create a background serivce, maybe solved
        try{
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
            unregisterReceiver(receiver)

//            LocalBroadcastManager.getInstance(this).unregisterReceiver(alarmed);
////            reciverRegister =false
        }catch(e:IllegalArgumentException){
            e.printStackTrace()
        }
    }

    override fun onHandleIntent(p0: Intent) {
        Log.d(TAG, "------------------------local-----------------------\nhandle intent, $p0")
        when (p0.action) {
            syncAction -> myself?.doUpdate()
            updateViewAction -> myself?.updateViews()
            Intent.ACTION_BOOT_COMPLETED -> Log.d(TAG,"did boot")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "on create")
        myself = this
        val context = this

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter().apply {
            addAction(syncAction)
            addAction(updateViewAction)
            addAction(Intent.ACTION_BOOT_COMPLETED)
        })

        db = AppDatabase.getDatabase(context)

    }

    fun removeDestination(id: Destination) {
        doAsync {
            db.destinationDao().delete(id)
            Log.d(TAG, "${db.destinationDao().getAll()} \n" +
                    " ${db.routeStepDao()} \n" +
                    " ${db.weatherDao()}")
        }
    }

    @SuppressLint("MissingPermission")//todo add permission question
    internal fun getRouteToDestination(dest: Destination) {
//        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val mode = "bicycling"
        var request = Request.Builder()//todo https://www.graphhopper.com/
                .url("https://maps.googleapis.com/maps/api/directions/json?origin=${dest.fromLat.toString() + "," + dest.fromLon}&destination=${dest.lat.toString() + "," + dest.lon}&key=${getString(R.string.google_direction_key)}&mode=$mode")
                .build()
        val response = client.newCall(request).execute()
        val out = response.body()?.string()
//        Log.d(TAG, out)
        val steps = JSONObject(out).getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        var timePassed = 0
        var timeElapsed = 0
//        var points = listOf(LatLng(currentLocation.latitude,currentLocation.longitude))
        for (step in steps) {
            val elapsed = step.getJSONObject("duration").getInt("value")
            timePassed += elapsed
            timeElapsed += elapsed

            if (timePassed > weatherPointResolutionSeconds) {

                timePassed = 0
                val end = step.getJSONObject("end_location")
//                Log.d(TAG,"$step , $end")
                db.routeStepDao().insertAll(RouteStep(end.getDouble("lat"), end.getDouble("lng"), timeElapsed, dest.id!!))
            }
        }

    }


    inner class LocalBinder : Binder() {
        /**
         * @return the securedService instance
         */
        val service: MyService
            get() = this@MyService
    }

    @SuppressLint("MissingPermission")
    fun addComuteDestination(dest: Place, currentLocation: Place, interval: Pair<Long, Long>) {
        doAsync {
            //            val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            Log.d(TAG, "$dest $currentLocation")
            var destination = Destination(dest.latLng.latitude, dest.latLng.longitude, currentLocation.latLng.latitude, currentLocation.latLng.longitude, interval.first, interval.second)
            val id = db.destinationDao().insert(destination)
            destination.id = id.toInt()//todo ugly hack will not scale, and rowid != primarykey
            Log.d(TAG, "inserting dest $destination $id ${db.destinationDao().getAll()}")
            db.routeStepDao().insertAll(RouteStep(currentLocation.latLng.latitude, currentLocation.latLng.longitude, 1, destination.id!!))//also ugly
            db.routeStepDao().insertAll(RouteStep(dest.latLng.latitude, dest.latLng.longitude, 1, destination.id!!))//ugly
            getRouteToDestination(destination)

        }
    }




    fun doUpdate() {
        Log.d(TAG, "gonna update")

        doAsync {
            try {
//                data = ""
                db.weatherDao().getPastDate(Date().time-864000).forEach { db.weatherDao().delete(it) }//todo not sure if new data is actually updated
                db.destinationDao().getAll().forEach {
                    db.routeStepDao().getAllFromDestination(it.id!!).forEach { loc -> getWeatherJson(loc, Date()) }
                }
                updateViews()

            } catch (e: Exception) {
                e.printStackTrace()
            }
//        return data
        }
    }


    private fun getWeatherJson(step: RouteStep, time: Date) {

        val context = this
//        Log.d(TAG, "starting weather fetch ${step.lat} ${step.lon}")
        var request = Request.Builder()//todo https://openweathermap.org/api
                .url("https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/${step.lon.format(6)}/lat/${step.lat.format(6)}/data.json")
                .build()
//        Log.d(TAG,"weather requset url ${request.url()}")
        val response = client.newCall(request).execute()
        val out = response.body()?.string()
//                Log.d(TAG, out)
        val json = JSONObject(out)

        try {
            val format = "yyyy-MM-dd'T'HH:mm:ss'Z'"
            for (keyval in json.getJSONArray("timeSeries")) {
                val rowtime = SimpleDateFormat(format).parse(keyval.getString("validTime"))

                val datarow = keyval.getJSONArray("parameters")
//                            Log.d(TAG, datarow.toString())
                var weatherData = WeatherData(
                        datarow.smhiValue("t") as Double,
                        datarow.smhiValue("pmax") as Double,
                        step.id!!,
                        datarow.smhiValue("ws") as Double,
                        datarow.smhiValue("wd") as Int,
                        rowtime.time

                )


//                        Log.d(TAG,weatherData.toString())
                val realWeatherData = weatherData
//                            data += realWeatherData
                try {
//                            Log.d(TAG,"inserting $realWeatherData")

                    db.weatherDao().insertAll(realWeatherData)
//                            Log.d(TAG,"did inserting ")

                } catch (e: SQLiteConstraintException) {
//                    db.weatherDao().delete(db.weatherDao().getExactFromRoute(step.id!!,time.time)!!)
//                    db.weatherDao().insertAll(realWeatherData)

                    db.weatherDao().updateAll(realWeatherData)
//                    Log.d(TAG,"sqlite constraint did update ",e)

                } catch (e: Exception) {
                    Log.w(TAG, "other execption $e \n do update ")

                    db.weatherDao().updateAll(realWeatherData)
                }

            }
        } catch (pe: ParseException) {
            pe.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun updateViews() {

        //widget
        val appWidgetManager = AppWidgetManager.getInstance(this
                .applicationContext)
        val views = RemoteViews(this.applicationContext.packageName, R.layout.new_app_widget)
        val thisWidget = ComponentName(this, NewAppWidget::class.java)
        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        viewModel = listOf()

//        val geo = Geocoder(this@MyService) //get time elpased since 00.00 today
//        val c = Calendar.getInstance() // today
//        c.timeZone = TimeZone.getTimeZone("UTC") // comment out for local system current timezone
//
//        c.set(Calendar.HOUR, 0)
//        c.set(Calendar.MINUTE, 0)
//        c.set(Calendar.SECOND, 0)
//        c.set(Calendar.MILLISECOND, 0)
////        Log.d(TAG,db.weatherDao().getAll().toString())
//        val millistamp = Date().time - c.timeInMillis
        //setup done


        //setting widget


        //main view model population loop
        Log.d(TAG, "gonna parse next route")
//        Log.d(TAG, db.destinationDao().getAll().toString())

//        val destination = db.destinationDao().getNext(millistamp)?:db.destinationDao().getNext(0)
//                destination.let { pair ->
        val closest = db.destinationDao().getClosetsOrigin(currentLocation.latitude,currentLocation.longitude)!!//todo either this or fix next wrap, closest seems to be working
//        val next = db.destinationDao().getNextWrapAround(millistamp)

//        db.destinationDao().getNextWrapAround(millistamp)?.let { pair ->
            //            Log.d(TAG, pair.toString())
//        val launchtime = closest!!.comuteStartIntervalStart + c.timeInMillis
//        val pair = Pair(closest,launchtime<Date().time)
//            viewModel = getWeatherView(pair.first!!, pair.second, c.timeInMillis)
        viewModel = getWeatherView(closest)//,!closest.comuteStartIntervalStart.IsToday())
//        }

        Log.d(TAG,"close $closest \nnext NaNaNaNa batman")
        views.setTextViewText(R.id.appwidget_text, viewModel.fold("") { acc, row ->
            val out = acc + row.getPrettyToString(this@MyService)
            out
        })
        appWidgetManager.updateAppWidget(thisWidget, views)

        //sending full data to app view
//        Log.d(TAG, "gonna send intent")
        LocalBroadcastManager.getInstance(this).sendBroadcast(viewModelUpdated)
//        return data
    }

    fun returnListOfDestinations(): List<Destination> {
        return db.destinationDao().getAll()
    }

    fun getWeatherView(dest: Destination,wrappedAround:Boolean=false,launchOrNow:Boolean=true): List<WeatherView> {
        val EpochToZeroZero = Calendar.getInstance() // today
        EpochToZeroZero.timeZone = TimeZone.getDefault()// comment out for local system current timezone
        val timeSinceZeroZero:Long = (EpochToZeroZero.get(Calendar.HOUR_OF_DAY) * 60 * 60 + EpochToZeroZero.get(Calendar.MINUTE) * 60
                +EpochToZeroZero.get(Calendar.SECOND)) * 1000L + EpochToZeroZero.get(Calendar.MILLISECOND)

        EpochToZeroZero.set(Calendar.HOUR_OF_DAY, 0)
        EpochToZeroZero.set(Calendar.MINUTE, 0)
        EpochToZeroZero.set(Calendar.SECOND, 0)
        EpochToZeroZero.set(Calendar.MILLISECOND, 0)

//        Log.d(TAG,"sinceEpochMs ${EpochToZeroZero.timeInMillis}")
//        Log.d(TAG,"timeZZ $timeSinceZeroZero")
        val launchTimeEpoch = dest.comuteStartIntervalStart + EpochToZeroZero.timeInMillis
//        val wrappedAround = dest.comuteStartIntervalStart<timeSinceZeroZero //todo needs to check if this is next and if origin if closest, even after start is greater current time should not wraparound
        val t = (if(launchOrNow) launchTimeEpoch else timeSinceZeroZero)
//        val pair = Pair(dest, wrappedAround)
        var output = listOf<WeatherView>()
        db.routeStepDao().getAllFromDestination(dest.id!!).forEach {
            //                val nowPlusStartInterval = pair.comuteStartIntervalStart + now + (it.timeElapsed * 1000)
            var time = t + (it.timeElapsed * 1000) + if(wrappedAround&&dest.comuteStartIntervalStart<timeSinceZeroZero) 86400000 else 0
//            if (wrappedAround) {//didWraparound //todo this is horribly broken,maybe not anymore
//                Log.d(TAG, "did wraparound ${dest.comuteStartIntervalStart}  ")
//                time = (  t + 86400000 + (it.timeElapsed * 1000))
//                //todo add weekend support
//            }
            println(time)
//                var now =  Date().time + (it.timeElapsed * 1000)
//                val time = pair.comuteStartIntervalStart + Date().time + (it.timeElapsed * 1000)
            db.weatherDao().getNextFromRoute(it.id!!, time)?.let { nextWeather ->
                Log.d(TAG, "pre create weatherview $it,$nextWeather  $time")

                db.weatherDao().getPrevFromRoute(it.id!!, time)?.let { prevWeather ->
//                    Log.d(TAG, "pre create weatherview $it,$nextWeather $prevWeather $time")
//                    Log.d(TAG,"${db.weatherDao().getAll()}")
                    val weather = Pair(prevWeather, nextWeather).toWeatherData(time)
                    weather.let { it1 ->
                        //                        Log.d(TAG, "weather  $it1")
                        output += WeatherView(it1, it.lat, it.lon)
                    }
                }
            }
        }
        return output
    }

}

fun Pair<WeatherData, WeatherData>.toWeatherData(now: Long): WeatherData {
    val factor = (now.toDouble() - first().time.toDouble()) / (last().time.toDouble() - first().time.toDouble())
    val time = first().time.lerp(last().time, factor)
    val dir = first().windDirection.lerp(last().windDirection, factor)

//    Log.d(TAG, "weather data first ${first.windDirection} last ${last().windDirection} out$dir")
    return WeatherData(
            first().temp.lerp(last().temp, factor),
            first().rain.lerp(last().rain, factor),
            first().routeId,
//            first().lon,
            first().windSpeed.lerp(last().windSpeed, factor),
            dir, time)
}

fun Pair<WeatherData, Any?>.first() = first
fun Pair<Any?, WeatherData>.last() = second

fun Double.lerp(high: Double, factor: Double): Double = this + (high - this).times(factor)


fun Int.lerp(high: Int, factor: Double): Int {
    val out = (this + (high - this).times(factor)).toInt()
//    Log.d(TAG, "lerp int: high $high factor $factor this $this out $out")
    return out
}

fun Long.lerp(high: Long, factor: Double): Long {
    val out = (this + (high - this).times(factor)).toLong()
//    Log.d(TAG, "lerp long: high $high factor $factor this $this out $out")
    return out
}

fun JSONArray.smhiValue(key: String): Number {
    for (JsonObject in this) {
        try {
            if (JsonObject.getString("name") == key) {
                return JsonObject.getJSONArray("values")[0] as Number //temp
            }
        } catch (j: JSONException) {
//            Log.w(TAG,"smhiVal fail",j)
        }
    }
    throw NoSuchFieldException("no key $key")
}

operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun Double.format(fracDigits: Int) = "%.${fracDigits}f".format(Locale.ENGLISH, this)

fun DestinationDao.getNextWrapAround(time: Long): Pair<Destination?, Boolean> {
    var out = getNext(time)
    val wraparpound = out == null
    if (wraparpound) {
        out = getNext(0)
    }
    return Pair(out, wraparpound)

}

fun RouteStepDao.updateOrInsert(input: RouteStep) {
    var back = getFromLatLon(input.lat, input.lon)
    if (back != null) {
        back
    }

}

fun Long.IsToday():Boolean{
    val cal = Calendar.getInstance()
    cal.timeZone = TimeZone.getDefault()// comment out for local system current timezone

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return this > cal.timeInMillis

}
