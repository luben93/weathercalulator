package se.selvidge.luben.weatherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.RemoteViews

/**
 * Implementation of App Widget functionality.
 */
class NewAppWidget : AppWidgetProvider() {
    var TAG = "WIDGET"
    var data = "not fetched yet"
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        Log.d(TAG,"gonna do update")
        MyService.widget = this
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)
        Log.d(TAG,"didrecive something")
    }



//    companion object {
//        public var TAG = "CWIDGET"

//        private var counter:Int = 0

         fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
//            counter++
             Log.d(TAG,"didrecive something")

//            val widgetText = "${Calendar.getInstance().time.hours}:${Calendar.getInstance().time.minutes} \n ${MyService.getWeatherModel()}"
//            val widgetText = counter.toString() + data
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.new_app_widget)
//            views.setTextViewText(R.id.appwidget_text, widgetText)
//            MyService.getWeatherModel()

//            val intent = Intent(context, MyService::class.java)//todone send intent to service
            val intent = Intent(context, MainActivity::class.java)
//            val intent = Intent(context, this::class.java)
//            intent.putExtra("key","hehe")
//            intent.setAction(MainActivity::YOUR_AWESOME_ACTION.toString())

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            views.setOnClickPendingIntent(R.id.appwidget_text,pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)

             LocalBroadcastManager.getInstance(context).sendBroadcast(Intent(context,MyService::class.java).apply { action = MyService.updateViewAction })

         }


//    }



}

