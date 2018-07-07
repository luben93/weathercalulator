package se.selvidge.luben.weatherwidget

import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.places.Place
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment
import com.google.android.gms.location.places.ui.PlaceSelectionListener
import com.touchboarder.weekdaysbuttons.WeekdaysDataItem
import com.touchboarder.weekdaysbuttons.WeekdaysDataSource
import kotlinx.android.synthetic.main.comute_selector.*
import org.jetbrains.anko.locationManager
import java.util.*

class popoverComuteSelector: AppCompatActivity() {
    companion object {
        val TAG = "ComuteSelectorAct"
    }
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG,"gonna show picker")

//        var display = getWindowManager().getDefaultDisplay();
//        var size = Point();
//        display.getSize(size);


//            val doAlert = Dialog
        var destPlace: Place?=null
        var originPlace: Place?=null
        var timeStart:Long=0

//            var alert = AlertDialog.Builder(this@MainActivity)
//        val inflated = this.layoutInflater.inflate(R.layout.comute_selector,this)
        setContentView(R.layout.comute_selector)
//        val popWindow = PopupWindow(inflated, size.x - 50,size.y - 500, true );

        val dest = fragmentManager.findFragmentById(R.id.destination_autocomplete) as PlaceAutocompleteFragment
        val currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        val origin = fragmentManager.findFragmentById(R.id.origin_autocomplete) as PlaceAutocompleteFragment
        origin.setText(Geocoder(this).getFromLocation(currentLocation.latitude,currentLocation.longitude,1).first().thoroughfare)
        val time = TimePickerDialog(this,{ view,hour,minute -> timeStart = (hour*60*60 + minute*60)*1000L},1,1,true)

        dest.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "dest: " + place.getName())
                destPlace = place

            }
            override fun onError(status: Status) { Log.i(TAG, "An error occurred: $status") }
        })

        origin.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                Log.i(TAG, "origin: " + place.getName())
                originPlace =place
            }
            override fun onError(status: Status) { Log.i(TAG, "An error occurred: $status") }
        })
//            alert.setView(inflated)
//            alert.setPositiveButton("Add"){dialogInterface, i ->
//        popWindow.showAtLocation(inflated, Gravity.BOTTOM, 0,150);

//                     myService?.addComuteDestination(destPlace!!, originPlace!!, Pair(timeStart, 36000000L))

        //todo add view setting for interval
//            }
//            val dialog = alert.create()

        time_picker.setOnClickListener{
            time.show()
        }

        val wds = WeekdaysDataSource(this, R.id.weekdays_stub)
        wds.start(object : WeekdaysDataSource.Callback{
            override fun onWeekdaysSelected(p0: Int, p1: ArrayList<WeekdaysDataItem>?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onWeekdaysItemClicked(p0: Int, p1: WeekdaysDataItem?) {

            }
        })

//            dialog.show()

    }
}