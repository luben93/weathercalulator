package se.selvidge.luben.weatherwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.rollbar.android.Rollbar
import org.jetbrains.anko.doAsync

class alarmed: BroadcastReceiver() {
    override fun onReceive(p0: Context, p1: Intent) {
        Log.d("alarmed","$p0 $p1")
        try {
//            p0.startService(Intent(p0, MyService::class.java).apply { action = p1.action })
//            if(MyService.myself == null){
//                Log.d("alarmed","service is null trying to start")
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    p0.startForegroundService(Intent(p0, MyService::class.java))
//                }else{
//                    p0.startService(Intent(p0,MyService::class.java))
//                }
//            }
            val bt = BackgroundTasks(p0)
            doAsync {
                when (p1.action) {
                    MainActivity.syncAction -> bt.updateFromNetwork()
                    MainActivity.updateViewAction -> bt.updateViews()
                }
            }
        }catch (e:Throwable){
            //cant start service in background
            e.printStackTrace()
            Rollbar.instance().error(e)
        }
//        LocalBroadcastManager.getInstance(p0).sendBroadcast(Intent(p0,MyService::class.java).apply { action = p1.action })
    }

}