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
        const val YOUR_AWESOME_ACTION = "YourAwesomeAction"
        const val VIEW_MODEL_UPDATED = "VIEW_MODEL_UPDATED"
    }

    var list: ArrayList<WeatherDestination> = arrayListOf()

    var TAG = "ACTIVITY"
    //TODO add options to select places for weather, kinda of done

    lateinit var adapter: WeatherCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Rollbar.init(this)

        try {

            setContentView(R.layout.activity_main)
            setSupportActionBar(toolbar)
            startService(Intent(this, MyService::class.java))

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
            }


            clock.setOnClickListener { view ->
                val intent = Intent(this, MyService::class.java)
                intent.action = MyService.syncAction
                startService(intent)
            }

            recycle.setOnClickListener { view ->
                startService(Intent(this, MyService::class.java).apply { action = MyService.updateViewAction })

            }

            LocalBroadcastManager.getInstance(this)
                    .registerReceiver(broadCastReceiver, IntentFilter().apply {
                        addAction(YOUR_AWESOME_ACTION)
                        addAction(VIEW_MODEL_UPDATED)
                    })


            val rView = findViewById<RecyclerView>(R.id.rView)
            adapter = WeatherCardAdapter(this@MainActivity, list)
            rView.layoutManager = GridLayoutManager(this@MainActivity, 1)
            rView.adapter = adapter
        } catch (e: Throwable) {
            Rollbar.instance().error(e)
        }

    }

    override fun onPostResume() {
        super.onPostResume()
        Log.d(TAG, "app did resume")
        startService(Intent(this, MyService::class.java).apply { action = MyService.updateViewAction })

    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(broadCastReceiver)
        } catch (re: RuntimeException) {

        }
    }

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            Log.d(TAG, "did recive")
            when (intent?.action) {
                VIEW_MODEL_UPDATED -> updateCards()
                else -> Rollbar.instance().info("uncaught broadcast $intent")
            }
        }
    }


    override fun onResume() {
        super.onResume()
        try {
            updateCards()
        } catch (e: Throwable) {
            Rollbar.instance().error(e)
        }
    }

    fun updateCards() {
        list.clear()
        doAsync {
            //            var list = listOf<WeatherDestination>()
            AppDatabase.getDatabase(this@MainActivity).destinationDao().getAll().forEach { dest ->
                MyService.getWeatherView(dest, this@MainActivity).let {
                    list.add(WeatherDestination(it, dest))
                    runOnUiThread {
                        //                        Log.d(TAG, "$list")
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
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

}
