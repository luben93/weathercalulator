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
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.Places
import com.google.android.gms.location.places.internal.PlaceEntity
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
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
import java.util.*

class PopoverComuteSelector : AppCompatActivity() {
    companion object {
        val TAG = "ComuteSelectorAct"
    }

    val weatherPointResolutionSeconds = 1200

    var myService: MyService? = null
    var myServiceConnecetion = object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {
            myService = null
        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as MyService.LocalBinder
            myService = binder.service
            Log.d("POPOVER", myService.toString())
        }
    }

    lateinit var destPlace: Place
    lateinit var originPlace: Place
    val cal = Calendar.getInstance()


    var timeStart: Long = (cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 + cal.get(Calendar.MINUTE) * 60) * 1000L

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {


        super.onCreate(savedInstanceState)
        Log.d(TAG, "gonna show picker")

//        var display = getWindowManager().getDefaultDisplay();
//        var size = Point();
//        display.getSize(size);

//        val inflated = inflater.inflate(R.layout.comute_selector, container, false);

//            val doAlert = Dialog

//        bindService(Intent(this, MyService::class.java), myServiceConnecetion, Context.BIND_AUTO_CREATE)//todo this does not work anymore, do inserts yourself

//            var alert = AlertDialog.Builder(this@MainActivity)
//        val inflated = this.layoutInflater.inflate(R.layout.comute_selector,this)
        setContentView(R.layout.comute_selector)
//        val popWindow = PopupWindow(inflated, size.x - 50,size.y - 500, true );

        val dest = fragmentManager.findFragmentById(R.id.destination_autocomplete) as PlaceAutocompleteFragment
        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val origin = fragmentManager.findFragmentById(R.id.origin_autocomplete) as PlaceAutocompleteFragment
        val location = Geocoder(this).getFromLocation(currentLocation.latitude, currentLocation.longitude, 1).first()
        origin.setText(location.thoroughfare)

        //todo show time as button text and use current time as base
        val time = TimePickerDialog(this, { view, hour, minute -> timeStart = (hour * 60 * 60 + minute * 60) * 1000L }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)

        dest.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "dest: " + place.name)
                destPlace = place

            }

            override fun onError(status: Status) {
                Log.i(TAG, "An error occurred: $status")
            }
        })

        origin.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "origin: " + place.name)
                originPlace = place
            }

            override fun onError(status: Status) {
                Log.i(TAG, "An error occurred: $status")
            }
        })

        selector_submit.setOnClickListener {
            //todo verify this
            Log.d(TAG, "submit to service $destPlace $originPlace $timeStart")
//            var success:Boolean = false
//                destPlace?.let { it1 -> originPlace?.let { it2 ->
//                    success = addDestination()//(it1, it2, Pair(timeStart, 36000000L)) == true }
//    }//todo move myservice.addComuteDest to separate function
            //todo stop this activiy if succesful, and navigate back
            doAsync {
               try{
                   addDestination()
                   this@PopoverComuteSelector.finish()
                } catch (e:Exception) {
                    Snackbar.make(it, "failed ${e.localizedMessage}", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                }
            }
        }
//            alert.setView(inflated)
//            alert.setPositiveButton("Add"){dialogInterface, i ->
//        popWindow.showAtLocation(inflated, Gravity.BOTTOM, 0,150);

//                     myService?.addComuteDestination(destPlace!!, originPlace!!, Pair(timeStart, 36000000L))

        //todo add view setting for interval
//            }
//            val dialog = alert.create()

        time_picker.text = "launch time: ${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE)}"
        time_picker.setOnClickListener {
            time.show()
        }

        val wds = WeekdaysDataSource(this, R.id.weekdays_stub)
        wds.start(object : WeekdaysDataSource.Callback {
            override fun onWeekdaysSelected(p0: Int, p1: ArrayList<WeekdaysDataItem>?) {
//                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            //
            override fun onWeekdaysItemClicked(p0: Int, p1: WeekdaysDataItem?) {
//
            }
        })


//            dialog.show()
//        return inflated

    }

//   lateinit var destPlace: Place
//   lateinit var originPlace: Place
//    var timeStart:Long=0

//   lateinit   var dest: PlaceAutocompleteFragment
//
//
//    lateinit var origin: PlaceAutocompleteFragment
//
//    @SuppressLint("MissingPermission")
//    fun initInternalFragments(fragmentManager: FragmentManager, context: Context){
//         dest = fragmentManager.findFragmentById(R.id.destination_autocomplete) as PlaceAutocompleteFragment
//         val currentLocation = context.locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
//         origin = fragmentManager.findFragmentById(R.id.origin_autocomplete) as PlaceAutocompleteFragment
//        origin.setText(Geocoder(context).getFromLocation(currentLocation.latitude,currentLocation.longitude,1).first().thoroughfare)
//        dest.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//            override fun onPlaceSelected(place: Place) {
//                Log.i(TAG, "dest: " + place.getName())
////                destPlace = place
//
//            }
//            override fun onError(status: Status) { Log.i(TAG, "An error occurred: $status") }
//        })
//
//        origin.setOnPlaceSelectedListener(object : PlaceSelectionListener {
//            override fun onPlaceSelected(place: Place) {
//                Log.i(TAG, "origin: " + place.getName())
////                originPlace =place
//            }
//            override fun onError(status: Status) { Log.i(TAG, "An error occurred: $status") }
//        })
//
//    }

    internal fun addDestination() {


            val db = AppDatabase.getDatabase(this)
            db.beginTransaction()
            Log.d(MyService.TAG, "gonna insert $destPlace $originPlace")
            var destination = Destination(destPlace.latLng.latitude, destPlace.latLng.longitude, originPlace.latLng.latitude, originPlace.latLng.longitude, timeStart, 0)
            val id = db.destinationDao().insert(destination)
            destination.id = id.toInt()//todo ugly hack will not scale, and rowid != primarykey
            Log.d(MyService.TAG, "inserting dest $destination $id ${db.destinationDao().getAll()}")
            db.routeStepDao().insertAll(RouteStep(originPlace.latLng.latitude, originPlace.latLng.longitude, 1, destination.id!!))//also ugly
            db.routeStepDao().insertAll(RouteStep(destPlace.latLng.latitude, destPlace.latLng.longitude, 1, destination.id!!))//ugly
            db.endTransaction()
        doAsync {
            getRouteToDestination(destination)
        }

    }

    internal fun getRouteToDestination(dest: Destination) {
//        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val client = OkHttpClient()
        val db = AppDatabase.getDatabase(this)

        val mode = "bicycling"
        var request = Request.Builder()//todo https://www.graphhopper.com/
                .url("https://maps.googleapis.com/maps/api/directions/json?origin=${dest.fromLat.toString() + "," + dest.fromLon}&destination=${dest.lat.toString() + "," + dest.lon}&key=${getString(R.string.google_direction_key)}&mode=$mode")
                .build()
        client.newCall(request).enqueue(object:Callback{
            override fun onFailure(call: Call?, e: IOException) {
                Snackbar.make(selector_submit, "failed ${e.localizedMessage}", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()            }

            override fun onResponse(call: Call?, response: Response) {


//        val response = client.newCall(request).execute()
        val out = response.body()?.string()
//        Log.d(TAG, out)
        val steps = JSONObject(out).getJSONArray("routes").getJSONObject(0).getJSONArray("legs").getJSONObject(0).getJSONArray("steps")
        var timePassed = 0
        var timeElapsed = 0
//        var points = listOf(LatLng(currentLocation.latitude,currentLocation.longitude))
        for (step in steps) {
            val elapsed = step.getJSONObject("duration").getInt("value")
            timePassed += elapsed
            timeElapsed += elapsed

            if (timePassed > weatherPointResolutionSeconds) {

                timePassed = 0
                val end = step.getJSONObject("end_location")
//                Log.d(TAG,"$step , $end")
                db.routeStepDao().insertAll(RouteStep(end.getDouble("lat"), end.getDouble("lng"), timeElapsed, dest.id!!))
            }
        }
            }

        })
    }
}
