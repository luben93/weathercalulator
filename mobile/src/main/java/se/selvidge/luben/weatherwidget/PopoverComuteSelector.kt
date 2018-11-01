package se.selvidge.luben.weatherwidget

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.location.Geocoder
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.TextView
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.Places
import com.google.android.gms.location.places.internal.PlaceEntity
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.model.LatLng
import com.touchboarder.weekdaysbuttons.WeekdaysDataItem
import com.touchboarder.weekdaysbuttons.WeekdaysDataSource
import kotlinx.android.synthetic.main.comute_selector.*
import okhttp3.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.locationManager
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.models.AppDatabase
import se.selvidge.luben.weatherwidget.models.Destination
import se.selvidge.luben.weatherwidget.models.RouteStep
import java.io.IOException
import java.sql.SQLException
import java.util.*

class PopoverComuteSelector : AppCompatActivity() {
    //todo ability to init this with existing destination to edit
    companion object {
        const val TAG = "ComuteSelectorAct"
    }

    val weatherPointResolutionSeconds = 1200


    lateinit var destPlaceLatLng: LatLng
    lateinit var originPlaceLatLng: LatLng
    val cal = Calendar.getInstance()


    var timeStart: Long = (cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 + cal.get(Calendar.MINUTE) * 60) * 1000L


    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)
        Log.d(TAG, "gonna show picker")


        setContentView(R.layout.comute_selector)

        val dest = fragmentManager.findFragmentById(R.id.destination_autocomplete) as PlaceAutocompleteFragment
        val currentLocation = MyService.getPlace(this, locationManager)
        val origin = fragmentManager.findFragmentById(R.id.origin_autocomplete) as PlaceAutocompleteFragment
        if (currentLocation != null) {
            val location = Geocoder(this).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).first()
            origin.setText(location.thoroughfare)
            originPlaceLatLng = LatLng(location.latitude, location.longitude)
        }
        val time = TimePickerDialog(this, { view, hour, minute ->
            time_picker.startAt(hour, minute)
            timeStart = (hour * 60 * 60 + minute * 60) * 1000L
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)



        origin.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "origin: " + place.name)
                originPlaceLatLng = place.latLng
            }

            override fun onError(status: Status) {
                Log.i(TAG, "origin An error occurred: $status")
            }
        })

        dest.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "origin: " + place.name)
                destPlaceLatLng = place.latLng
            }

            override fun onError(status: Status) {
                Log.i(TAG, "origin An error occurred: $status")
            }
        })

        selector_submit.setOnClickListener {
            //todo verify this
            Log.d(TAG, "submit to service $destPlaceLatLng $originPlaceLatLng $timeStart")

            //todo stop this activiy if succesful, and navigate back
            doAsync {
                //todo add spinner
                try {
                    addDestination()                   //todo fails
                } catch (e: Exception) {
                    Snackbar.make(it, "failed ${e.localizedMessage}", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    e.printStackTrace()
                }
            }
        }

        //todo add view setting for interval


        time_picker.startAt(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        time_picker.setOnClickListener {
            time.show()
        }

        val wds = WeekdaysDataSource(this, R.id.weekdays_stub)
        wds.start(object : WeekdaysDataSource.Callback {
            override fun onWeekdaysSelected(p0: Int, p1: ArrayList<WeekdaysDataItem>?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onWeekdaysItemClicked(p0: Int, p1: WeekdaysDataItem?) {

            }
        })


    }

    internal fun addDestination() {


        val db = AppDatabase.getDatabase(this)

        Log.d(TAG, "gonna insert $destPlaceLatLng $originPlaceLatLng")
        var destination = Destination(destPlaceLatLng.latitude, destPlaceLatLng.longitude, originPlaceLatLng.latitude, originPlaceLatLng.longitude, timeStart, 0)
        val id = db.destinationDao().insert(destination)
        Log.d(TAG, "dest $destination  insert id $id")
        destination.id = id.toInt()//todo ugly hack will not scale, and rowid != primarykey
        Log.d(TAG, "inserting dest $destination $id ${db.destinationDao().getAll()}")
        db.routeStepDao().insertAll(RouteStep(originPlaceLatLng.latitude, originPlaceLatLng.longitude, 1, destination.id!!))//also ugly
//        db.routeStepDao().insertAll(RouteStep(destPlaceLatLng.latitude, destPlaceLatLng.longitude, 1, destination.id!!))//ugly
        getRouteToDestination(destination)

        done()

    }

    internal fun done() {
        runOnUiThread {
            this@PopoverComuteSelector.finish()
        }
    }

    internal fun getRouteToDestination(dest: Destination) {
//        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val client = OkHttpClient()

        val mode = "bicycling"
        var request = Request.Builder()//todo https://www.graphhopper.com/
                .url("https://maps.googleapis.com/maps/api/directions/json?origin=${dest.fromLat.toString() + "," + dest.fromLon}&destination=${dest.lat.toString() + "," + dest.lon}&key=${getString(R.string.google_direction_key)}&mode=$mode")
                .build()

        val db = AppDatabase.getDatabase(this@PopoverComuteSelector)


        val response = client.newCall(request).execute()
        val out = response.body()?.string()
//        Log.d(TAG, out)
        val steps = JSONObject(out).getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        var timePassed = 0
        var timeElapsed = 0
//        var points = listOf(LatLng(currentLocation.latitude,currentLocation.longitude))
//                try {
        for (step in steps) {//todo should use kilometers passed instead of time between steps
            val elapsed = step.getJSONObject("duration").getInt("value")
            timePassed += elapsed
            timeElapsed += elapsed

            if (timePassed > weatherPointResolutionSeconds) {

                timePassed = 0
                val end = step.getJSONObject("end_location")
                Log.d(TAG, "step $step , end $end , elapsed $timeElapsed \n id $dest")
                db.routeStepDao().insertAll(RouteStep(end.getDouble("lat"), end.getDouble("lng"), timeElapsed, dest.id!!))
            }
        }

//                }catch (e:Exception ){
//                    Snackbar.make(selector_submit, "failed ${e.localizedMessage}", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show()
//                    e.printStackTrace()
//                }
    }
}


fun TextView.startAt(hour: Int, minute: Int) {
    text = "launch time: ${hour}:${minute}"
}