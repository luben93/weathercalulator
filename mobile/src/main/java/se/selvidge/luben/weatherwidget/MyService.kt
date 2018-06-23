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
import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.MyService.Companion.TAG
import se.selvidge.luben.weatherwidget.models.AppDatabase
import se.selvidge.luben.weatherwidget.models.Destination
import se.selvidge.luben.weatherwidget.models.WeatherData
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
            Log.d(TAG,"from static call")
            context.sendBroadcast(Intent(context, MyService::class.java))
        }
        var widget:NewAppWidget? = null
        val syncAction="syncAction"
        val halfHourInMs = 1800000
        val TAG = "SERVICE"
        var now:Long = 0
            get() = Date().time

//        var data = "not updated yet"
    }

    val activityIntent = Intent(MainActivity.YOUR_AWESOME_ACTION)
    val mBinder = LocalBinder()
    val weatherPointResolutionKm = 2
    val weatherPointResolutionSeconds = 1200
    lateinit var db:AppDatabase
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

    inner class AlarmReciver(): BroadcastReceiver() {

        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.d(TAG,"------------------------local-----------------------\nbrodcast inner class $p0, $p1")

            doUpdate()
        }

    }

    override fun onCreate() {
        super.onCreate()
        val context = this
        db =  AppDatabase.getDatabase(context)


        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        var intentSync = Intent(applicationContext, alarmed::class.java).apply {
            action = syncAction
        }
//        intentSync.setFlags(Intent.);
        var alarmIntent = PendingIntent.getBroadcast(applicationContext, 0, intentSync, 0)

        alarmMgr.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() +100,
                AlarmManager.INTERVAL_HALF_HOUR, alarmIntent)//todo verify that this runs

        LocalBroadcastManager.getInstance(this).registerReceiver(AlarmReciver(), IntentFilter(syncAction))
//        registerReceiver(alarmed(), IntentFilter(syncAction))



        this.registerReceiver(object : BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?) {
                Log.d(TAG,"did boot")
               onCreate()
            }
        },IntentFilter(Intent.ACTION_BOOT_COMPLETED))//todo verify this
    }

    @SuppressLint("MissingPermission")//todo add permission question
    internal fun getRouteToDestination(dest: Destination):List<LatLng>{
        var locationManager =  this.getSystemService(Context.LOCATION_SERVICE) as LocationManager;
        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val mode="bicycling"
        var request = Request.Builder()
                .url("https://maps.googleapis.com/maps/api/directions/json?origin=${currentLocation.latitude.toString()+","+currentLocation.longitude}&destination=${dest.lat.toString()+","+dest.lon}&key=${getString(R.string.google_direction_key)}&mode=$mode")
                .build()
        val response= client.newCall(request).execute()
        val out = response.body()?.string()
        Log.d(TAG, out)
        val steps = JSONObject(out).getJSONArray("routes").getJSONObject(0).
                getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        var timePassed = 0;
        var points = listOf(LatLng(currentLocation.latitude,currentLocation.longitude))
        for (step in steps){
            timePassed += step.getJSONObject("duration").getInt("value")
            if(timePassed>weatherPointResolutionSeconds){

                timePassed=0
                val end = step.getJSONObject("end_location")
                Log.d(TAG,"$step , $end")
                points + LatLng(end.getDouble("lat"),end.getDouble("lng"))
            }
        }
        return points
    }


    inner class LocalBinder : Binder() {
        /**
         * @return the securedService instance
         */
        val service: MyService
            get() = this@MyService
    }

    fun addComuteDestination(place: Place,interval : Pair<Long,Long>) {
        doAsync {
            db.destinationDao().insertAll(Destination(place.latLng.latitude, place.latLng.longitude, interval.first, interval.second))
        }
    }

    fun doAsyncPushToView(){
        doAsync {
            updateViews()
        }
    }

    fun doUpdate() {
        Log.d(TAG,"gonna update")

                doAsync {
            try {
                val geo = Geocoder(this@MyService)
//                data = ""
                db.destinationDao().getAll().forEach {
                    val locations = getRouteToDestination(it)
//                    data+= "\ndest: ${geo.getFromLocation(it.lat, it.lon, 1).first().thoroughfare}\n"
                    locations.forEach { loc -> getWeatherJson(loc.latitude, loc.longitude, Date()) }
                }
//            getJson(locations[0].first, locations[0].second, Date())
                updateViews()
//todo empty old data

            } catch (e: Exception) {
                e.printStackTrace()
            }
//        return data
        }
    }



    var data = ""
    var client = OkHttpClient()


    private fun getWeatherJson(lat: Double, long: Double, time: Date) {

        val context = this
        Log.d(TAG,"starting weather fetch $lat $long")
        var request = Request.Builder()
                .url("https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/${long.format(6)}/lat/${lat.format(6)}/data.json")
                .build()
        Log.d(TAG,"weather requset url ${request.url()}")
              val response= client.newCall(request).execute()
                val out = response.body()?.string()
                Log.d(TAG, out)
                val json = JSONObject(out)

//        val asyncRealm = Realm.getDefaultInstance()
                try {
                    val format = "yyyy-MM-dd'T'HH:mm:ss'Z'"
                    val lastRowTime = Date()
                    for(keyval in json.getJSONArray("timeSeries")){
                        val rowtime = SimpleDateFormat(format).parse(keyval.getString("validTime"))
//                        if(rowtime.time-lastRowTime.time>halfHourInMs+halfHourInMs){
//                            return
//                        }
//                        Log.d(NewAppWidget.TAG, "${rowtime}")
//                        if (Math.abs(time.time - rowtime.time) < 1800000) {
//                                                    Log.d(TAG, "${rowtime}")
                            val datarow = keyval.getJSONArray("parameters")
//                            Log.d(TAG, datarow.toString())
                            var geo = Geocoder(this@MyService)
//                            weatherData?.place =
                            var weatherData = WeatherData(
                                    datarow.smhiValue("t") as Double,
                                    datarow.smhiValue("pmax") as Double,
                                    lat,
                                    long,
                                    datarow.smhiValue("ws") as Double,
                                    datarow.smhiValue("wd") as Int,
                                    rowtime.time

                                    )
//                            weatherData.lat = lat
//                            weatherData.lon = long
//                            weatherData.time = rowtime.time
//                            for (row in datarow) {
//                                Log.d(TAG, row.getString("name").toString())
//                                when (row.getString("name")) {
//                                    "t" -> weatherData.temp = row.getJSONArray("values")[0] as Double //temp
//                                    "wd" -> weatherData.windDirection = row.getJSONArray("values")[0] as Int//wind dir
//                                    "ws" -> weatherData.windSpeed = row.getJSONArray("values")[0] as Double//windspeed
//                                    "pmax" -> weatherData.rain = (row.getJSONArray("values")[0] as Double)//rain cat
////                                    "pcat" -> weatherData.raining = (row.getJSONArray("values")[0] as Int != 0)//rain cat
//                                    else -> Log.d(TAG, row.toString())
//                                }
//                            }


                        Log.d(TAG,weatherData.toString())
                            val realWeatherData = weatherData
//                            data += realWeatherData
                        try {
                            Log.d(TAG,"inserting $realWeatherData")

                            db.weatherDao().insertAll(realWeatherData)
                            Log.d(TAG,"did inserting ")

                        }catch (e:SQLiteConstraintException ){
                            Log.d(TAG,"sqlite constraint do update ",e)

                            db.weatherDao().updateAll(realWeatherData)
                        }catch (e: Exception){
                            Log.d(TAG,"other execption $e \n do update ")

                            db.weatherDao().updateAll(realWeatherData)
                        }

//                            data += "${realWeatherData.place?.thoroughfare} T:${realWeatherData.temp}℃ ${realWeatherData.wind} " +
//                                    "${if(realWeatherData.raining) "rain" else "clear"}\n"
                            Log.d(TAG,"inserting $realWeatherData")

//                        }
                    }
                } catch (pe: ParseException) {
                    pe.printStackTrace()
                } catch (e: Exception ){
                    e.printStackTrace()
                }
    }

    private fun updateViews(){
            data = ""
        val c = Calendar.getInstance() // today
//        c.timeZone = TimeZone.getTimeZone("UTC") // comment out for local system current timezone
        c.set(Calendar.HOUR, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        Log.d(TAG,db.weatherDao().getAll().toString())
        val millistamp = c.timeInMillis
            db.destinationDao().getClosest(Date().time-millistamp).let { pair -> //todo iterate over points on the route instead of only destination
                Log.d(TAG, pair.toString())
                data += db.weatherDao().findTwoByPlaceAndTime(pair.lat, pair.lon, Date().time)
                        .toWeatherData(Date()).getPrettyToString(this) }
//        data += "\n"
//        locations.forEach{ pair -> data += db.weatherDao().findByPlaceAndTime(pair.first,pair.second,Date().time+ halfHourInMs*12).getPrettyToString (this)}

            val appWidgetManager = AppWidgetManager.getInstance(this
                    .applicationContext)
            val widgetText = data

            val views = RemoteViews(this.applicationContext.packageName, R.layout.new_app_widget)
            views.setTextViewText(R.id.appwidget_text, widgetText)
            val thisWidget = ComponentName(this, NewAppWidget::class.java)
            appWidgetManager.updateAppWidget(thisWidget, views)


            data += "\n"
            db.destinationDao().getAll().forEach { pair -> data += db.weatherDao().findByPlaceAndTime(pair.lat, pair.lon, Date().time).getPrettyToString(this) }
            db.destinationDao().getAll().forEach { pair ->
                data += db.weatherDao().findByPlaceAndTime(pair.lat, pair.lon, Calendar.getInstance().apply { set(Calendar.HOUR, 2) }.timeInMillis).getPrettyToString(this)
            }
            db.weatherDao().getAll().forEach { data.plus(it) }
            LocalBroadcastManager.getInstance(this).sendBroadcast(activityIntent)

//        return data
    }


}

fun List<WeatherData>.toWeatherData(now:Date):WeatherData{

    val factor = (now.time.toDouble()-first().time.toDouble())/(last().time.toDouble() - first().time.toDouble())
    return WeatherData(
            first().temp.avrageFactor(last().temp,factor),
            first().rain.avrageFactor(last().rain,factor),
            first().lat,
            first().lon,
            first().windSpeed.avrageFactor(last().windSpeed,factor),
            first().windDirection.avrageFactor(last().windDirection,factor),
            first().time.avrageFactor(last().time,factor))
}

fun Double.avrageFactor(high:Double,factor: Double):Double=this + (high - this).times(factor)


fun Int.avrageFactor(high:Int,factor: Double):Int{
    return (this + (high - this).times(factor)).toInt()
}

fun Long.avrageFactor(high: Long,factor: Double):Long{
    return (this + (high - this).times(factor)).toLong()
}

fun JSONArray.smhiValue(key:String): Number {
    for (JsonObject in this){
        try {
            if (JsonObject.getString("name") == key) {
                return JsonObject.getJSONArray("values")[0] as Number //temp
            }
//            val found=  JsonObject.takeIf { o ->
//                Log.d(TAG,o.toString())
//                o.getJSONObject("name").getString(key).isNullOrEmpty().not() }
//            Log.d(TAG,found.toString())
//            return found?.getJSONArray("values")?.get(0) as Number

        }catch (j:JSONException){
            Log.w(TAG,"smhiVal fail",j)
        }
    }
    throw NoSuchFieldException("no key $key")
}

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

fun Double.format(fracDigits: Int) = "%.${fracDigits}f".format(Locale.ENGLISH,this)
