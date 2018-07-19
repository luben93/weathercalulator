package se.selvidge.luben.weatherwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class alarmed: BroadcastReceiver() {
    override fun onReceive(p0: Context, p1: Intent) {
        Log.d("alarmed","$p0 $p1")
        p0.startService(Intent(p0,MyService::class.java).apply { action = p1.action })
//        LocalBroadcastManager.getInstance(p0).sendBroadcast(Intent(p0,MyService::class.java).apply { action = p1.action })
    }

}