package se.selvidge.luben.weatherwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

class alarmed: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.d("alarmed","$p0 $p1")
        LocalBroadcastManager.getInstance(p0!!).sendBroadcast(Intent(p0,MyService::class.java).apply { action = MyService.syncAction })
    }

}