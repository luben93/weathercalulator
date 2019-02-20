package se.selvidge.luben.weatherwidget

import android.Manifest
import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.location.Location
import android.location.LocationManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.locationManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.models.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class BackgroundTasks(val context: Context) {

    private val db: AppDatabase = AppDatabase.getDatabase(context)
    private var client = OkHttpClient()
    private var viewModel = listOf<WeatherView>()
    private val viewModelUpdated = Intent(MainActivity.VIEW_MODEL_UPDATED)
    private val TAG = "backgroundTasks"
    private val halfHourInMs = 1800000
    private val hourInMs = halfHourInMs + halfHourInMs

    fun updateFromNetwork(){
        db.weatherDao().getPastDate(Date().time - 864000).forEach { db.weatherDao().delete(it) }//todo not sure if new data is actually updated
        db.destinationDao().getAll().forEach {
            db.routeStepDao().getAllFromDestination(it.id!!).forEach { loc -> getWeatherJson(loc, Date()) }
        }
        updateViews()
    }

    private fun getWeatherJson(step: RouteStep, time: Date) {

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
                        datarow.smhiValue("gust") as Double,
                        datarow.smhiValue("wd") as Int,
                        datarow.smhiValue("r") as Int,
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

    fun getPlace( ): Location? {
        val locationManager = context.locationManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            var currentLocation = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            for (provider in locationManager.getProviders(true)) {
                val newLoc = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                if (newLoc != null) {
                    if (newLoc.hasAccuracy() && newLoc.accuracy < currentLocation.accuracy) {
                        currentLocation = newLoc
                    }
                }
            }
            return currentLocation
        }

        return null
    }

    fun updateViews() {

        //widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val views = RemoteViews(context.packageName, R.layout.new_app_widget)
        val thisWidget = ComponentName(context, NewAppWidget::class.java)
        val currentLocation = getPlace( )

        viewModel = listOf()


        //main view model population loop
        Log.d(TAG, "gonna parse next route")
        val closest: Destination
        if (currentLocation != null) {
            closest = db.destinationDao().getClosetsOrigin(currentLocation.latitude, currentLocation.longitude)!!//todo either this or fix next wrap, closest seems to be working
        } else {
            closest = db.destinationDao().getNext(Date().time)!!
        }

        val midnight = Calendar.getInstance()
        midnight.timeZone = TimeZone.getDefault()// comment out for local system current timezone

        midnight.set(Calendar.HOUR_OF_DAY, 0)
        midnight.set(Calendar.MINUTE, 0)
        midnight.set(Calendar.SECOND, 0)
        midnight.set(Calendar.MILLISECOND, 0)

        val a = closest.comuteStartIntervalStart < 12 * hourInMs
        val b = Date().time - midnight.timeInMillis < 12 * hourInMs
        viewModel = getWeatherView(closest, a == b) //its not pretty but works for me

        Log.d(TAG, "close $closest \nnext NaNaNaNa batman")
        views.setTextViewText(R.id.appwidget_text,
                "${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:${Calendar.getInstance().get(Calendar.MINUTE)} " +
                        "  ${viewModel.foldAndAvg(context)}")
        appWidgetManager.updateAppWidget(thisWidget, views)

        //sending full data to app view
//        Log.d(TAG, "gonna send intent")
        LocalBroadcastManager.getInstance(context).sendBroadcast(viewModelUpdated)
    }

    fun getWeatherView(dest: Destination, launchOrNow: Boolean = false): List<WeatherView> {
        val EpochToZeroZero = Calendar.getInstance() // today
        EpochToZeroZero.timeZone = TimeZone.getDefault()// comment out for local system current timezone
        val timeSinceZeroZero: Long = (EpochToZeroZero.get(Calendar.HOUR_OF_DAY) * 60 * 60 + EpochToZeroZero.get(Calendar.MINUTE) * 60
                + EpochToZeroZero.get(Calendar.SECOND)) * 1000L + EpochToZeroZero.get(Calendar.MILLISECOND)

        EpochToZeroZero.set(Calendar.HOUR_OF_DAY, 0)
        EpochToZeroZero.set(Calendar.MINUTE, 0)
        EpochToZeroZero.set(Calendar.SECOND, 0)
        EpochToZeroZero.set(Calendar.MILLISECOND, 0)


        val launchTimeEpoch = dest.comuteStartIntervalStart + EpochToZeroZero.timeInMillis
        var t = launchTimeEpoch
        if (!launchOrNow) {
            t += if (dest.comuteStartIntervalStart < timeSinceZeroZero) 86400000 else 0
        } else {
            t = if (dest.comuteStartIntervalStart < timeSinceZeroZero) timeSinceZeroZero + EpochToZeroZero.timeInMillis else t //todo kl 19 man borde få morgondagens route så visas vad de skulle bli om man startar kl 19
        }
        var output = listOf<WeatherView>()
        db.routeStepDao().getAllFromDestination(dest.id!!).forEach {
            var time = t + (it.timeElapsed * 1000)
//                //todo add weekend support
            println(time)
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




//fun avgStdoWW(t:Double,to:Double,w:Double,wo:Double,wd:Int,r:Double,ro:Double):String{ //todo maybe use avg and stdOffset instead of max and min
//    return "T:${"%.1f".format(Locale.ENGLISH, t)} - ${"%.1f".format(Locale.ENGLISH, to)}℃ W: ${
//    "%.1f".format(Locale.ENGLISH, ws)} m/s ${WeatherView.dir.values()[((wd)/45)]} ${
//    if(rp==0.0 && rm == 0.0) "clear" else "${"%.1f".format(Locale.ENGLISH, rp)} - ${"%.1f".format(Locale.ENGLISH, rp)} mm/h"}\n"
//}

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
            first().gustWindSpeed.lerp(last().gustWindSpeed, factor),
            dir,
            first().humidity.lerp(last().humidity,factor),
            time)
}




fun Pair<WeatherData, Any?>.first() = first
fun Pair<Any?, WeatherData>.last() = second

fun Double.lerp(high: Double, factor: Double): Double = this + (high - this).times(factor)

fun Int.lerp(high: Int, factor: Double): Int = (this + (high - this).times(factor)).toInt()

fun Long.lerp(high: Long, factor: Double): Long = (this + (high - this).times(factor)).toLong()


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

fun RouteStepDao.updateOrInsert(input: RouteStep) = getFromLatLon(input.lat, input.lon)

fun Long.IsToday(): Boolean {
    val cal = Calendar.getInstance()
    cal.timeZone = TimeZone.getDefault()// comment out for local system current timezone

    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return this > cal.timeInMillis

}
