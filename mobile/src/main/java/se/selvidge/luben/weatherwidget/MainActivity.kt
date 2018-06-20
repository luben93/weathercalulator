package se.selvidge.luben.weatherwidget

import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.design.widget.Snackbar
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import se.selvidge.luben.weatherwidget.MyService.Companion.halfHourInMs
import java.util.*

class MainActivity : AppCompatActivity() {
    companion object {
        val YOUR_AWESOME_ACTION = "YourAwesomeAction"
    }
//    public final
    var TAG = "MWIDGET"
    //TODO add options to select places for weather
     var myService:MyService? = null
    var myServiceConnecetion = object: ServiceConnection {
         override fun onServiceDisconnected(p0: ComponentName?) {
//             TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            myService = null
         }

         override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
//             TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
             val binder = p1 as MyService.LocalBinder
             myService = binder.service
         }
     }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        bindService( Intent(this,MyService::class.java),myServiceConnecetion, Context.BIND_AUTO_CREATE)

        strava.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()

            val intent = Intent(Intent.ACTION_RUN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.data = Uri.parse ("http://strava.com/nfc/record")
            intent.putExtra("rideType","Ride")
            startActivity(intent)



        }

        debug.setOnClickListener{ view -> myService?.doUpdate() }
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadCastReceiver, IntentFilter(YOUR_AWESOME_ACTION))
//        registerReceiver(this, IntentFilter())


    }

    val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
        Log.d(TAG,"did recive")
            when (intent?.action) {
                YOUR_AWESOME_ACTION -> haha()
                "haha" -> haha()
                else -> haha()
            }
        }
    }

    fun haha(){
        Log.d(TAG,"haha haha")
        mainTextView.text = myService?.data
    }
    fun UpdateView() {
        try {
            Log.d(TAG,"startign view update")
//            var aRealm = Realm.getDefaultInstance()

            var data = ""
//            aRealm.where<WeatherData>().between("time", Date(Date().time - halfHourInMs), Date(Date().time + halfHourInMs)).findAll().forEach {
//                Log.d(TAG, "looping Weathers $it")

//                data += it.getPrettyToString(this)
//            }
            mainTextView.text = data
            Log.d(TAG, "${Date(Date().time - halfHourInMs)}   ${java.util.Date(java.util.Date().time + se.selvidge.luben.weatherwidget.MyService.Companion.halfHourInMs)}")
        }catch (e: Exception){
            Log.w(TAG,"printintg data",e)
        }
    }
    override fun onResume() {
        super.onResume()
        Log.d(TAG,"app did resume")
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
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG,intent.toString())
    }

}
