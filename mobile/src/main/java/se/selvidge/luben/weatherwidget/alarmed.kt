package se.selvidge.luben.weatherwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rollbar.android.Rollbar
import org.jetbrains.anko.doAsync

class alarmed: BroadcastReceiver() {
    override fun onReceive(p0: Context, p1: Intent) {
        Log.d("alarmed","$p0 $p1")
        try {
//            p0.startService(Intent(p0, MyService::class.java).apply { action = p1.action })
            when (p1.action) {
                MyService.syncAction -> MyService.myself?.doUpdate()
                MyService.updateViewAction -> doAsync {   MyService.myself?.updateViews() }
            }
        }catch (e:Throwable){
            //cant start service in background
            Rollbar.instance().error(e)
            e.printStackTrace()
        }
//        LocalBroadcastManager.getInstance(p0).sendBroadcast(Intent(p0,MyService::class.java).apply { action = p1.action })
    }

}