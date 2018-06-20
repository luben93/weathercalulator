package se.selvidge.luben.weatherwidget.models

import android.content.Context
import android.location.Address
import android.location.Geocoder
import io.realm.RealmModel
import io.realm.annotations.PrimaryKey
import io.realm.annotations.RealmClass
import java.text.SimpleDateFormat
import java.util.*

@RealmClass
open class WeatherData: RealmModel{
        var temp=0.0
        var wind:Wind? = null
    var raining=false
        var lat=0.0
        var lon=0.0

    @PrimaryKey
    var _key=""
    var key:String
        get() = "$lat:$lon:${time.time}"//does not work
        set(value){
            _key="$lat:$lon:${time.time}"
        }

    //    private var _place = Address(Locale.ENGLISH)
    var place: Address
        get() {
            var a = Address(Locale.ENGLISH)
            a.longitude = lon
            a.latitude = lat
            return a
        }
        set(value) {
             lon= value.longitude
            lat = value.latitude
        }
    var time=Date()

    //todo add human clothing model (values at when what clothes are suitable)
//    var place:Address
//    get() {return Address()
//    }
    override fun toString(): String {
        return "${SimpleDateFormat("HH:MM").format(time)} ${place.thoroughfare} T:${temp}℃ ${wind} ${if(raining) "rain" else "clear"} k$key\n"
    }

    fun getPrettyToString(c: Context):String{
        return "${SimpleDateFormat("HH:MM").format(time)} ${getPlace(c).thoroughfare} T:${temp}℃ ${wind} ${if(raining) "rain" else "clear"}\n"
    }
    fun getPlace(c: Context):Address{
        return Geocoder(c).getFromLocation(lat,lon,1).first()
    }
}





