package se.selvidge.luben.weatherwidget

import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MyService : Service() {
    //todo add alarm manager to update weather
    //todo listen after intents and react aproriatly, widget button etc
    //maybe use Machine learning to train weather -> clothes model
    val activityIntent = Intent(MainActivity.YOUR_AWESOME_ACTION)

    val mBinder = LocalBinder()
    //place lat,long
    //home 59.179741,18.127764        0
    //bandhagen 59.267410, 18.059313  1
    //v'sterled 59.328446, 17.970361  2
    var locations = listOf(Pair(59.179741, 18.127764), Pair(59.267410, 18.059313), Pair(59.328446, 17.970361))
    val TAG = "SWIDGET"

//    var data = listOf(WeatherData(20.0,1.0,1,false,Address(Locale.ENGLISH)))

    override fun onBind(intent: Intent): IBinder {
//        TODO("Return the communication channel to the service.")
        return mBinder
    }

    inner class LocalBinder : Binder() {
        /**
         * @return the securedService instance
         */
        val service: MyService
            get() = this@MyService
    }

    fun doUpdate() {
        try {
            data = ""
            locations.forEach { loc -> getJson(loc.first, loc.second, Date()) }
//            getJson(locations[0].first, locations[0].second, Date())


//            fun doUpdate() {
//                try {
//                    data = ""
//                    locations.forEach { loc -> getJson(loc.first, loc.second, Date()) }
////            getJson(locations[0].first, locations[0].second, Date())


                    //widget view


//            val allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
//            for (widgetId in allWidgetIds){
//                    val remoteViews = RemoteViews(this
//                            .applicationContext.packageName,
//                            R.layout.new_app_widget)
//                remoteViews

//                    activityIntent.action = MainActivity::YOUR_AWESOME_ACTION.toString()
//                sendBroadcast(activityIntent)
//            startActivity(activityIntent)// todo always starts activity, never recives
//           }
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                }
//            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
//        return data
    }

    companion object {
        fun getWeatherModel(): String {
//            return data
            return "handen 21 c\n" +
                    "östberga 20 c\n" +
                    "bromma 25 c"
        }
        var widget:NewAppWidget? = null
//        var data = "not updated yet"
    }
        var data = ""
    var client = OkHttpClient()


    private fun getJson(lat: Double, long: Double, time: Date) {

        val context = this
        var request = Request.Builder()
                .url("https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/${long}/lat/${lat}/data.json")
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                Log.w(TAG, "http failed", e)
            }

//            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call, response: Response) {
                val out = response.body()?.string()
                Log.d(TAG, out)
                val json = JSONObject(out)


                try {
                    val format = "yyyy-MM-dd'T'HH:mm:ss'Z'"

//                    var Jarray = json.getJSONArray("timeSeries")
                    for(keyval in json.getJSONArray("timeSeries")){
//                    for (i in 0 until Jarray.length()) {
//                        val keyval = Jarray.getJSONObject(i)
//                        keyval.getString("validTime")
//                        var now = LocalDate.now()
//                        if(Math.abs(time.time-
//                        Log.d(NewAppWidget.TAG, "looping $i")
//                        Log.d(NewAppWidget.TAG, "${SimpleDateFormat(format).format(time)}")
                        val rowtime = SimpleDateFormat(format).parse(keyval.getString("validTime"))
//                        Log.d(NewAppWidget.TAG, "${rowtime}")
                        if (Math.abs(time.time - rowtime.time) < 1800000) {
                                                    Log.d(TAG, "${rowtime}")
                            val datarow = keyval.getJSONArray("parameters")
//                            Log.d(TAG, datarow.toString())

                            var weatherData= WeatherData(0.0,0.0,0,false,null)
                            for(row in datarow){
                                Log.d(TAG,row.getString("name").toString())
                                when (row.getString("name")){
                                    "t" -> weatherData?.temp = row.getJSONArray("values")[0] as Double //temp
                                    "wd" -> weatherData?.windDirection = row.getJSONArray("values")[0] as Int//wind dir
                                    "ws" -> weatherData?.windSpeed = row.getJSONArray("values")[0] as Double//windspeed
                                    "pcat" -> weatherData?.raining = (row.getJSONArray("values")[0] as Int != 0)//rain cat
                                    else -> Log.d(TAG,row.toString())
                                }
                            }

                        var geo = Geocoder(this@MyService)
                            weatherData?.place = geo.getFromLocation(lat,long,1).first()
                        Log.d(TAG,weatherData.toString())
                            val realWeatherData = weatherData
                            data += "${realWeatherData.place?.thoroughfare} T:${realWeatherData.temp}℃ W:${realWeatherData.windSpeed}m/s ${realWeatherData.windDirection} " +
                                    "${if(realWeatherData.raining) "raining" else "no rain"}\n"
//                            var temp = (datarow.get(11) as JSONObject).getJSONArray("values")[0] as Double
//                            var windSpeed = (datarow.get(14) as JSONObject).getJSONArray("values")[0] as Int
//                            var windDirection = (datarow.get(13) as JSONObject).getJSONArray("values")[0] as Int
                            Log.d(TAG,data)
                           updateViews()


                        }
                    }
                } catch (pe: ParseException) {
                    pe.printStackTrace()
                } catch (e: Exception ){
                    e.printStackTrace()
                }
//                    arr.forEach { rows:JSONObject -> {if(rows)}}

            }
        })
    }

    internal fun updateViews(){
        val appWidgetManager = AppWidgetManager.getInstance(this
                .applicationContext)
        LocalBroadcastManager.getInstance(this).sendBroadcast(activityIntent)
        val widgetText = "${Calendar.getInstance().time.hours}:${Calendar.getInstance().time.minutes} \n $data"
        val views = RemoteViews(this.applicationContext.packageName, R.layout.new_app_widget)
        views.setTextViewText(R.id.appwidget_text, widgetText)
        val thisWidget = ComponentName(this, NewAppWidget::class.java)
        appWidgetManager.updateAppWidget(thisWidget, views)
    }


}

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()