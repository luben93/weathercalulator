package se.selvidge.luben.weatherwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.support.annotation.RequiresApi
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import android.widget.TextView
import okhttp3.*
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.R.id.appwidget_text
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal
import java.time.temporal.TemporalUnit
import java.util.*

/**
 * Implementation of App Widget functionality.
 */
class NewAppWidget : AppWidgetProvider() {
    public var TAG = "WWIDGET"
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

        private var counter:Int = 0

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            counter++

            val widgetText = counter.toString() + MyService.getWeatherModel()
//            val widgetText = counter.toString() + data
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.new_app_widget)
            views.setTextViewText(R.id.appwidget_text, widgetText)


//            val intent = Intent(context, MyService::class.java)//todo send intent to service
            val intent = Intent(context, MainActivity::class.java)
//            val intent = Intent(context, this::class.java)
//            intent.putExtra("key","hehe")
//            intent.setAction(MainActivity::YOUR_AWESOME_ACTION.toString())

            val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
            views.setOnClickPendingIntent(R.id.appwidget_text,pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }


//    }



}

