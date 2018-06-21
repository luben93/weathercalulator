package se.selvidge.luben.weatherwidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.*
import android.database.sqlite.SQLiteConstraintException
import android.location.Geocoder
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.models.AppDatabase
import se.selvidge.luben.weatherwidget.models.WeatherData
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*






class MyService : Service() {
    //todo add alarm manager to update weather
    //todo listen after intents and react aproriatly, widget button etc
    //maybe use Machine learning to train weather -> clothes model

    companion object {
        fun getWeatherModel(): String {
//            return data
            return "handen 21 c\n" +
                    "östberga 20 c\n" +
                    "bromma 25 c"
        }
        var widget:NewAppWidget? = null
        val syncAction="syncAction"
        val halfHourInMs = 1800000;

//        var data = "not updated yet"
    }

    val activityIntent = Intent(MainActivity.YOUR_AWESOME_ACTION)
    val mBinder = LocalBinder()
    lateinit var db:AppDatabase
    //place lat,long
    //home 59.179741,18.127764        0
    //bandhagen 59.267410, 18.059313  1
    //v'sterled 59.328446, 17.970361  2
    var locations = listOf(Pair(59.179741, 18.127764), Pair(59.267410, 18.059313), Pair(59.328446, 17.970361))
    val TAG = "SWIDGET"
//    var weatherData:
//    var data = listOf(WeatherData(20.0,1.0,1,false,Address(Locale.ENGLISH)))

    override fun onBind(intent: Intent): IBinder {
//        TODO("Return the communication channel to the service.")
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        val context = this
//        Realm.init(context)
        db =  AppDatabase.getDatabase(context)

//        val config =  RealmConfiguration.Builder()
//                .deleteRealmIfMigrationNeeded()
//                .build()
//        Realm.setDefaultConfiguration(config)
//        realm = Realm.getDefaultInstance()

        val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MyService::class.java)
        var alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0)

        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_HOUR,
                AlarmManager.INTERVAL_HOUR, alarmIntent)//todo verify that this runs
        LocalBroadcastManager.getInstance(this).registerReceiver(br(), IntentFilter(syncAction))
        this.registerReceiver(object : BroadcastReceiver(){
            override fun onReceive(p0: Context?, p1: Intent?) {
               onCreate()
            }
        },IntentFilter(Intent.ACTION_BOOT_COMPLETED))


// Get a Realm instance for this thread


    }

    inner class br : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            Log.d(TAG,"brodcast $p0, $p1")
            doUpdate()
        }
    }

    inner class LocalBinder : Binder() {
        /**
         * @return the securedService instance
         */
        val service: MyService
            get() = this@MyService
    }

    fun doUpdate() {
//        Log.d(TAG, "pre drop old${realm.where<WeatherData>().count()}")
//        realm.beginTransaction()
//        realm.where<WeatherData>().lessThan("time",Date()).findAll().forEach(WeatherData::deleteFromRealm)
//        realm.commitTransaction()
//        Log.d(TAG, "post drop old${realm.where<WeatherData>().count()}")


                doAsync {
            try {
                data = ""
                locations.forEach { loc -> getJson(loc.first, loc.second, Date()) }
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


    private fun getJson(lat: Double, long: Double, time: Date) {

        val context = this
        var request = Request.Builder()
                .url("https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point/lon/${long}/lat/${lat}/data.json")
                .build()

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
                            var weatherData = WeatherData()
                            weatherData.lat = lat
                            weatherData.lon = long
                            weatherData.time = rowtime.time
                            for (row in datarow) {
                                Log.d(TAG, row.getString("name").toString())
                                when (row.getString("name")) {
                                    "t" -> weatherData.temp = row.getJSONArray("values")[0] as Double //temp
                                    "wd" -> weatherData.windDirection = row.getJSONArray("values")[0] as Int//wind dir
                                    "ws" -> weatherData.windSpeed = row.getJSONArray("values")[0] as Double//windspeed
                                    "pmax" -> weatherData.rain = (row.getJSONArray("values")[0] as Double)//rain cat
//                                    "pcat" -> weatherData.raining = (row.getJSONArray("values")[0] as Int != 0)//rain cat
                                    else -> Log.d(TAG, row.toString())
                                }
                            }


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
//                        asyncRealm.beginTransaction()
//                        asyncRealm.insertOrUpdate(realWeatherData.wind)
//                        asyncRealm.insertOrUpdate(realWeatherData)
//                        asyncRealm.commitTransaction()

//                        }
                    }
                } catch (pe: ParseException) {
                    pe.printStackTrace()
                } catch (e: Exception ){
                    e.printStackTrace()
                }
    }

    internal fun updateViews(){
//        val aRealm = Realm.getDefaultInstance()
//        Log.d(TAG,"gonna updateView from service, db size${aRealm.where<WeatherData>().count()} first key ${aRealm.where<WeatherData>().findFirst()} cur date${Date().time}")

        data = ""
        locations.forEach{ pair -> data += db.weatherDao().findByPlaceAndTime(pair.first,pair.second,Date().time).getPrettyToString (this)}
//        data += "\n"
//        locations.forEach{ pair -> data += db.weatherDao().findByPlaceAndTime(pair.first,pair.second,Date().time+ halfHourInMs*12).getPrettyToString (this)}


        //todo does not find any
//        aRealm.where<WeatherData>().between("time",Date(Date().time-halfHourInMs),Date(Date().time+halfHourInMs) ).
//                findAll().forEach {
//            Log.d(TAG,"${it.wind}")//todo does not fetch rain linked object
//            data += it.getPrettyToString(this) }

        val appWidgetManager = AppWidgetManager.getInstance(this
                .applicationContext)
        val widgetText = data

        val views = RemoteViews(this.applicationContext.packageName, R.layout.new_app_widget)
        views.setTextViewText(R.id.appwidget_text, widgetText)
        val thisWidget = ComponentName(this, NewAppWidget::class.java)
        appWidgetManager.updateAppWidget(thisWidget, views)


        data += "\n"
//        locations.forEach{ pair -> data += db.weatherDao().findByPlaceAndTime(pair.first,pair.second,Date().time+ (halfHourInMs)).getPrettyToString (this)}
        db.weatherDao().getAll().forEach { data.plus(it) }
        LocalBroadcastManager.getInstance(this).sendBroadcast(activityIntent)


    }


}

operator fun JSONArray.iterator(): Iterator<JSONObject>
        = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()