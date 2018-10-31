package se.selvidge.luben.weatherwidget

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.FragmentManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.rollbar.android.Rollbar
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.locationManager
import se.selvidge.luben.weatherwidget.models.AppDatabase
import se.selvidge.luben.weatherwidget.models.Destination
import se.selvidge.luben.weatherwidget.models.RouteStep
import se.selvidge.luben.weatherwidget.models.WeatherDestination
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        val YOUR_AWESOME_ACTION = "YourAwesomeAction"
        val VIEW_MODEL_UPDATED = "VIEW_MODEL_UPDATED"
    }

    var list: ArrayList<WeatherDestination> = arrayListOf()//    var list = listOf(WeatherDestination(listOf(WeatherView(WeatherData(0.0,0.0,0,0.0,0,0),59.328446, 17.970361)), Destination(59.328446, 17.970361,0.0,0.0,0,0)))

    //    public final
    var TAG = "ACTIVITY"
    //TODO add options to select places for weather, kinda of done
//    var myService: MyService? = null
//    var myServiceConnecetion = object : ServiceConnection {
//        override fun onServiceDisconnected(p0: ComponentName?) {
//            myService = null
//        }
//
//        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
//            val binder = p1 as MyService.LocalBinder
//            myService = binder.service
//            myService?.doAsyncPushToView()
//        }
//    }
    lateinit var myFragmentManager: FragmentManager
    lateinit var adapter: CustomAdapter

//    @SuppressLint("MissingPermission")
//    @RequiresApi(Build.VERSION_CODES.HONEYCOMB)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Rollbar.init(this)

        try {


            setContentView(R.layout.activity_main)
            setSupportActionBar(toolbar)
            startService(Intent(this, MyService::class.java))

//        bindService(Intent(this, MyService::class.java), myServiceConnecetion, Context.BIND_AUTO_CREATE)
            myFragmentManager = fragmentManager


            val alarmMgr = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            var intentSync = Intent(applicationContext, alarmed::class.java)

            alarmMgr.setInexactRepeating(//todo only register once, this should work acoringly to interwebz
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10,
                    60000, PendingIntent.getBroadcast(applicationContext, 1, intentSync.apply { action = MyService.updateViewAction }, 0))//todo verify that this runs
            alarmMgr.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 100,//time since last
                    AlarmManager.INTERVAL_HOUR, PendingIntent.getBroadcast(applicationContext, 2, intentSync.apply { action = MyService.syncAction }, 0))//todo verify that this runs

            add.setOnClickListener { view ->
                Log.d(TAG, "gonna show picker")
                startActivity(Intent(this, PopoverComuteSelector::class.java))
//            dialogShower()

//        val fragmentTransaction = myFragmentManager.beginTransaction();
//        val fragment = PopoverComuteSelector()
////        fragment.initInternalFragments(myFragmentManager,this)
//        fragmentTransaction.add(R.id.fragment, fragment)
//        fragmentTransaction.commit()
            }


            clock.setOnClickListener { view ->
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()

//            val intent = Intent(Intent.ACTION_RUN)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            intent.data = Uri.parse ("http://strava.com/nfc/record")
//            intent.putExtra("rideType","Ride")
//            startActivity(intent)

                val intent = Intent(this, MyService::class.java)
                intent.action = MyService.syncAction
                startService(intent)
//    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
//            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(this,MyService::class.java).apply { action = MyService.syncAction })

            }


            recycle.setOnClickListener { view ->
                //            myService?.doAsyncPushToView()
                startService(Intent(this, MyService::class.java).apply { action = MyService.updateViewAction })
//            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(this,MyService::class.java).apply { action = MyService.updateViewAction })

            }
            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(broadCastReceiver, IntentFilter().apply {
                        addAction(YOUR_AWESOME_ACTION)
                        addAction(VIEW_MODEL_UPDATED)
                    })
//        registerReceiver(this, IntentFilter())


//        val autocompleteFragment = fragmentManager.findFragmentById(R.id.place_autocomplete_fragment) as PlaceAutocompleteFragment
//
//        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//            //todo add permission question
//            override fun onPlaceSelected(place: Place) {
//                Log.i(TAG, "Place: " + place.getName())
//                myService?.addComuteDestination(place,null, Pair(21600000L, 36000000L))
//            }
//
//            override fun onError(status: Status) {
//                // TODO: Handle the error.
//                Log.i(TAG, "An error occurred: $status")
//            }
//        })


            val rView = findViewById<RecyclerView>(R.id.rView)
            adapter = CustomAdapter(this@MainActivity, list)
            rView.layoutManager = GridLayoutManager(this@MainActivity, 1)
            rView.adapter = adapter
        }catch (e:Throwable){
            Rollbar.instance().error(e)
        }

}

    private fun dialogShower(){
        var display = windowManager.defaultDisplay
//            var size = Point();
//            display.getSize(size);
//
//
////            val doAlert = Dialog
        lateinit var destPlace: Place
        lateinit var originPlace:Place
        var timeStart:Long=0
//
        var alert = AlertDialog.Builder(this@MainActivity)
        val inflated = this.layoutInflater.inflate(R.layout.comute_selector,null)
//            val popWindow = PopupWindow(inflated, size.x - 50,size.y - 500, true );
//
        val dest = fragmentManager.findFragmentById(R.id.destination_autocomplete) as PlaceAutocompleteFragment
//        val currentLocation = MyService.getPlace(this, locationManager)
        val origin = fragmentManager.findFragmentById(R.id.origin_autocomplete) as PlaceAutocompleteFragment
//        origin.setText(Geocoder(this).getFromLocation(currentLocation.latitude,currentLocation.longitude,1).first().thoroughfare)
        val time = TimePickerDialog(this,{ view,hour,minute -> timeStart = (hour*60*60 + minute*60)*1000L},1,1,true)

        dest.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "dest: " + place.name)
                destPlace = place

            }
            override fun onError(status: Status) { Log.i(TAG, "An error occurred: $status") }
        })

        origin.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "origin: " + place.name)
                originPlace =place
            }
            override fun onError(status: Status) { Log.i(TAG, "An error occurred: $status") }
        })
        alert.setView(inflated)
        alert.setPositiveButton("Add"){dialogInterface, i ->
            //            popWindow.showAtLocation(inflated, Gravity.BOTTOM, 0,150);

            val db = AppDatabase.getDatabase(this@MainActivity)
//            myService?.addComuteDestination(destPlace!!, originPlace!!, Pair(timeStart, 36000000L))
            var destination = Destination(destPlace.latLng.latitude, destPlace.latLng.longitude, originPlace.latLng.latitude, originPlace.latLng.longitude, timeStart, 36000000L)
            val id = db.destinationDao().insert(destination)
            destination.id = id.toInt()//todo ugly hack will not scale, and rowid != primarykey
            db.routeStepDao().insertAll(RouteStep(originPlace.latLng.latitude, originPlace.latLng.longitude, 1, destination.id!!))//also ugly
            db.routeStepDao().insertAll(RouteStep(destPlace.latLng.latitude, destPlace.latLng.longitude, 1, destination.id!!))//ugly

        }
        alert.setNeutralButton("select time"){d,i->//removes underlying dialog :(
            time.show()
        }
        val dialog = alert.create()


//
//            val wds = WeekdaysDataSource(this, R.id.weekdays_stub)
//            wds.start(object : WeekdaysDataSource.Callback{
//                override fun onWeekdaysSelected(p0: Int, p1: ArrayList<WeekdaysDataItem>?) {
//                }
//
//                override fun onWeekdaysItemClicked(p0: Int, p1: WeekdaysDataItem?) {
//
//                }
//            })
//
        dialog.show()
    }

    override fun onPostResume() {
        super.onPostResume()
        Log.d(TAG, "app did resume")
        startService(Intent(this,MyService::class.java).apply { action = MyService.updateViewAction })

//        myService?.doAsyncPushToView()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(broadCastReceiver)
        } catch (re: RuntimeException) {

        }
//        unbindService(myServiceConnecetion)
    }

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            Log.d(TAG, "did recive")
            when (intent?.action) {
                VIEW_MODEL_UPDATED -> updateCards()
                YOUR_AWESOME_ACTION -> haha()
                "haha" -> haha()
                else -> haha()
            }
        }
    }


    fun haha() {
        Log.d(TAG, "haha haha")
//        mainTextView.text = myService?.data
    }

    fun UpdateView() {
        try {
            var data = "next up\n"
//            myService?.viewModel?.forEach {
//                data += it.getPrettyToString(this)
//            }
//            mainTextView.text = data
//            Log.d(TAG, "${Date(Date().time - halfHourInMs)}   ${java.util.Date(java.util.Date().time + MyService.Companion.halfHourInMs)}")
        } catch (e: Exception) {
            Log.w(TAG, "printintg data", e)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            updateCards()
        }catch (e:Throwable){
            Rollbar.instance().error(e)
        }
    }

    fun updateCards() {
        list.clear()
        doAsync {
            //            var list = listOf<WeatherDestination>()
            AppDatabase.getDatabase(this@MainActivity).destinationDao().getAll().forEach { dest ->
                MyService.getWeatherView(dest,this@MainActivity).let {
                    list.add(WeatherDestination(it, dest))
                    runOnUiThread {
                        //                        Log.d(TAG, "$list")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
//        val widgetUpdateIntent = Intent(this, NewAppWidget.UpdateService::class.java)
//        this.startService(widgetUpdateIntent)
//        NewAppWidget.updateAppWidget(this,)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                Log.d(TAG, "settings")
                true
            }
            else -> {
                Log.d(TAG, "cardsettings")
                true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, intent.toString())
    }

}
