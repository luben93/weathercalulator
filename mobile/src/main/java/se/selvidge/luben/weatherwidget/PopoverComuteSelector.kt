package se.selvidge.luben.weatherwidget

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.google.android.gms.maps.model.LatLng
import com.rollbar.android.Rollbar
import com.touchboarder.weekdaysbuttons.WeekdaysDataItem
import com.touchboarder.weekdaysbuttons.WeekdaysDataSource
import kotlinx.android.synthetic.main.comute_selector.*
import okhttp3.*
import org.jetbrains.anko.doAsync
import org.json.JSONObject
import se.selvidge.luben.weatherwidget.models.AppDatabase
import se.selvidge.luben.weatherwidget.models.Destination
import se.selvidge.luben.weatherwidget.models.RouteStep
import java.util.*

class PopoverComuteSelector : AppCompatActivity() {
    //todo ability to init this with existing destination to edit
    companion object {
        const val TAG = "ComuteSelectorAct"
    }

    val weatherPointResolutionSeconds = 1200


    lateinit var destPlaceLatLng: LatLng
    lateinit var originPlaceLatLng: LatLng
    var weekdays:ArrayList<WeekdaysDataItem>? = null
    val cal = Calendar.getInstance()
    val db = AppDatabase.getDatabase(this)


    private var timeStart: Long = (cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 + cal.get(Calendar.MINUTE) * 60) * 1000L



    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.comute_selector)
        val wds = WeekdaysDataSource(this, R.id.weekdays_stub)

        val dest = fragmentManager.findFragmentById(R.id.destination_autocomplete) as PlaceAutocompleteFragment
        val origin = fragmentManager.findFragmentById(R.id.origin_autocomplete) as PlaceAutocompleteFragment
        var hourMinute = Pair(cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE))
        var destFromDb:Destination? = null
        if(intent.extras != null) {

            doAsync {
                destFromDb = db.destinationDao().getById(intent.extras.getInt("destinationId"))
                //todo set dest and origin from db
                val from = Geocoder(this@PopoverComuteSelector).getFromLocation(destFromDb!!.fromLat, destFromDb!!.fromLon, 1).first()
                origin.setText(from.thoroughfare)
                originPlaceLatLng = LatLng(from.latitude, from.longitude)

                val to = Geocoder(this@PopoverComuteSelector).getFromLocation(destFromDb!!.lat, destFromDb!!.lon, 1).first()
                dest.setText(to.thoroughfare)
                destPlaceLatLng = LatLng(to.latitude, to.longitude)
                destFromDb?.weekdays?.forEach {  wds.setSelectedDays(it) }




                hourMinute = Pair((destFromDb!!.comuteStartIntervalStart / 3600000).toInt(), (destFromDb!!.comuteStartIntervalStart / 600000 % 60).toInt())

                selector_buttons.addView(Button(this@PopoverComuteSelector).apply {
                    text = "delete "
                    setOnClickListener {
                        doAsync {
                            db.destinationDao().delete(destFromDb!!)
                            done()
                        }
                    }
                })
            }
        }else {

            val currentLocation = BackgroundTasks(this).getPlace()
            if (currentLocation != null) {
                val location = Geocoder(this).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).first()
                origin.setText(location.thoroughfare)
                originPlaceLatLng = LatLng(location.latitude, location.longitude)
            }

        }

        val time = TimePickerDialog(this, { view, hour, minute ->
            time_picker.startAt(hour, minute)
            timeStart = (hour * 60 * 60 + minute * 60) * 1000L
        }, hourMinute.first,hourMinute.second , true)

        time_picker.startAt(hourMinute.first,hourMinute.second)
        time_picker.setOnClickListener {
            time.show()
        }

        wds.start(object : WeekdaysDataSource.Callback {
            override fun onWeekdaysSelected(p0: Int, p1: ArrayList<WeekdaysDataItem>?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                weekdays = p1

            }

            override fun onWeekdaysItemClicked(p0: Int, p1: WeekdaysDataItem?) {

            }
        })

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
            doAsync {
                try {
                    if(destFromDb != null ){
                        db.destinationDao().delete(destFromDb!!)
                    }
                    addDestination()                   //todo this should update or insert (or remove and insert)
                } catch (e: Exception) {
                    Snackbar.make(it, "failed ${e.localizedMessage}", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    e.printStackTrace()
                    Rollbar.instance().error(e)

                }
            }
        }

    }

    internal fun addDestination() {



            Log.d(TAG, "gonna insert $destPlaceLatLng $originPlaceLatLng")
        var destination = Destination(destPlaceLatLng.latitude, destPlaceLatLng.longitude, originPlaceLatLng.latitude, originPlaceLatLng.longitude, (this.weekdays?.map { it.calendarDayId }
                ?: listOf(1,2,3,4,5)).toIntArray(), timeStart, 0)
        val id = db.destinationDao().insert(destination)
            Log.d(TAG, "dest $destination  insert id $id")
        destination.id = id.toInt()//todo ugly hack will not scale, and rowid != primarykey
            Log.d(TAG, "inserting dest $destination $id ${db.destinationDao().getAll()}")
        db.routeStepDao().insertAll(RouteStep(originPlaceLatLng.latitude, originPlaceLatLng.longitude, 1, destination.id!!))//also ugly
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

