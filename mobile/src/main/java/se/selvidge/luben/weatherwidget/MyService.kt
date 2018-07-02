package se.selvidge.luben.weatherwidget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.database.sqlite.SQLiteConstraintException
import android.location.Geocoder
import android.location.LocationManager
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
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


class MyService : Service() {
    //todone add alarm manager to update weather
    //todone listen after intents and react aproriatly, widget button etc
    //maybe use Machine learning to train weather -> clothes model

    companion object {
        fun getWeatherModel(context: Context) {
//            return data
            Log.d(TAG, "from static call")
            context.sendBroadcast(Intent(context, MyService::class.java))
        }

        var widget: NewAppWidget? = null
        val syncAction = "syncAction"
        val halfHourInMs = 1800000
        val TAG = "SERVICE"
        var now: Long = 0
            get() = Date().time

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
    //place lat,long
    //home 59.179741,18.127764        0
    //bandhagen 59.267410, 18.059313  1
    //v'sterled 59.328446, 17.970361  2
//    var locations = listOf(Pair(59.179741, 18.127764), Pair(59.267410, 18.059313), Pair(59.328446, 17.970361))
//    lateinit var fusedLocationClient:FusedLocationProviderClient

//    var weatherData:
//    var data = listOf(WeatherData(20.0,1.0,1,false,Address(Locale.ENGLISH)))

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    inner class AlarmReciver() : BroadcastReceiver() {

        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.d(TAG, "------------------------local-----------------------\nbrodcast inner class $p0, $p1")

            doUpdate()
        }

    }

//    override fun onDestroy() {
//
// super.onDestroy()//todo either unregister when done or create a background serivce
//        try {
//            unregisterReceiver(AlarmReciver())
//        } catch (e: Exception) {
//
//        }
//    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "on create")
        val context = this
        db = AppDatabase.getDatabase(context)


        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var intentSync = Intent(applicationContext, alarmed::class.java).apply {
            action = syncAction
        }
//        intentSync.setFlags(Intent.);
        var alarmIntent = PendingIntent.getBroadcast(applicationContext, 0, intentSync, 0)

        alarmMgr.setInexactRepeating(//todo only register once
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 100,
                AlarmManager.INTERVAL_HOUR, alarmIntent)//todo verify that this runs

        LocalBroadcastManager.getInstance(this).registerReceiver(AlarmReciver(), IntentFilter(syncAction))
//        registerReceiver(alarmed(), IntentFilter(syncAction))

//        locationManager =  this.getSystemService(Context.LOCATION_SERVICE) as LocationManager;


        this.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.d(TAG, "did boot")
                if (p0 != null) {
                    postCreate(p0)
                }
            }
        }, IntentFilter(Intent.ACTION_BOOT_COMPLETED))//todo does not work like expected
    }

    fun postCreate(context: Context){
        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var intentSync = Intent(applicationContext, alarmed::class.java).apply {
            action = syncAction
        }
//        intentSync.setFlags(Intent.);
        var alarmIntent = PendingIntent.getBroadcast(applicationContext, 0, intentSync, 0)

        alarmMgr.setInexactRepeating(//todo only register once
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 100,
                AlarmManager.INTERVAL_HOUR, alarmIntent)//todo verify that this runs

        LocalBroadcastManager.getInstance(this).registerReceiver(AlarmReciver(), IntentFilter(syncAction))
    }

    @SuppressLint("MissingPermission")//todo add permission question
    internal fun getRouteToDestination(dest: Destination) {
        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val mode = "bicycling"
        var request = Request.Builder()//todo https://www.graphhopper.com/
                .url("https://maps.googleapis.com/maps/api/directions/json?origin=${currentLocation.latitude.toString() + "," + currentLocation.longitude}&destination=${dest.lat.toString() + "," + dest.lon}&key=${getString(R.string.google_direction_key)}&mode=$mode")
                .build()
        val response = client.newCall(request).execute()
        val out = response.body()?.string()
        Log.d(TAG, out)
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
    fun addComuteDestination(place: Place, interval: Pair<Long, Long>) {
        doAsync {
            val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            var destination = Destination(place.latLng.latitude, place.latLng.longitude, currentLocation.latitude, currentLocation.longitude, interval.first, interval.second)
            val id = db.destinationDao().insert(destination)
            destination.id = id.toInt()//todo ugly hack will not scale, and rowid != primarykey
            Log.d(TAG,"inserting dest $destination $id ${db.destinationDao().getAll()}")
            db.routeStepDao().insertAll(RouteStep(currentLocation.latitude, currentLocation.longitude, 1, destination.id!!))
            getRouteToDestination(destination)

        }
    }

    fun doAsyncPushToView() {
        doAsync {
            updateViews()
        }
    }

    fun doUpdate() {
        Log.d(TAG, "gonna update")

        doAsync {
            try {
                val geo = Geocoder(this@MyService)
//                data = ""
                db.destinationDao().getAll().forEach {
                    db.routeStepDao().getAllFromDestination(it.id!!).forEach { loc -> getWeatherJson(loc, Date()) }
                }
                updateViews()
//todo empty old data

            } catch (e: Exception) {
                e.printStackTrace()
            }
//        return data
        }
    }


    private fun getWeatherJson(step: RouteStep, time: Date) {

        val context = this
        Log.d(TAG,"starting weather fetch ${step.lat} ${step.lon}")
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
//                            Log.d(TAG,"sqlite constraint do update ",e)

                    db.weatherDao().updateAll(realWeatherData)
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

    private fun updateViews() {

        //widget
        val appWidgetManager = AppWidgetManager.getInstance(this
                .applicationContext)
        val views = RemoteViews(this.applicationContext.packageName, R.layout.new_app_widget)
        val thisWidget = ComponentName(this, NewAppWidget::class.java)

        viewModel = listOf()

        val geo = Geocoder(this@MyService) //get time elpased since 00.00 today
        val c = Calendar.getInstance() // today
        c.timeZone = TimeZone.getTimeZone("UTC") // comment out for local system current timezone

        c.set(Calendar.HOUR, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
//        Log.d(TAG,db.weatherDao().getAll().toString())
        val millistamp = Date().time - c.timeInMillis
        //setup done


        //setting widget


        //main view model population loop
        Log.d(TAG, "gonna parse next route")
//        Log.d(TAG, db.destinationDao().getAll().toString())

//        val destination = db.destinationDao().getNext(millistamp)?:db.destinationDao().getNext(0)
//                destination.let { pair ->
            db.destinationDao().getNextWrapAround(millistamp)?.let { pair ->
            //            Log.d(TAG, pair.toString())
                viewModel = getWeatherView(pair.first!!,pair.second,c.timeInMillis)

        }

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

    fun returnListOfDestinations():List<Destination>{
        return db.destinationDao().getAll()
    }

    fun getWeatherView(dest: Destination,wrappedAround:Boolean = false,timeOfDay:Long=Date().time):List<WeatherView>{
        val pair = Pair(dest,wrappedAround)
        var output = listOf<WeatherView>()
        db.routeStepDao().getAllFromDestination(pair.first.id!!).forEach {

            //                val nowPlusStartInterval = pair.comuteStartIntervalStart + now + (it.timeElapsed * 1000)
            var time =  Date().time + (it.timeElapsed * 1000)//todo replace all Date().time with now val and use timezones
            if(pair.second){//didWraparound
                Log.d(TAG,"did wraparound ${pair.first.comuteStartIntervalStart} ${timeOfDay} ")
                time = (pair.first.comuteStartIntervalStart + timeOfDay  + 36000000 + (it.timeElapsed * 1000)) //- 86400000 //todo -10h not working,
                //todo add weekend support
            }
//                var now =  Date().time + (it.timeElapsed * 1000)
//                val time = pair.comuteStartIntervalStart + Date().time + (it.timeElapsed * 1000)
            db.weatherDao().getNextFromRoute(it.id!!, time)?.let { nextWeather ->
                db.weatherDao().getPrevFromRoute(it.id!!, time)?.let { prevWeather ->
                                        Log.d(TAG,"pre create weatherview $it,$nextWeather $prevWeather")
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

fun DestinationDao.getNextWrapAround(time: Long): Pair<Destination?,Boolean >{
    var out = getNext(time)
    val wraparpound = out == null
    if (wraparpound) {
        out = getNext(0)
    }
    return Pair(out,wraparpound)

}

fun RouteStepDao.updateOrInsert(input: RouteStep) {
    var back = getFromLatLon(input.lat, input.lon)
    if (back != null) {
        back
    }

}