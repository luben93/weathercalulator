package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*
import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.text.SimpleDateFormat
import java.util.*

@Entity(primaryKeys = ["lon","lat","time"])
data class WeatherData(

    var temp:Double,
    var rain:Double,
    var lat:Double,
    var lon:Double,
    var windSpeed:Double,
    var windDirection:Int,
    var time:Long
    ){



    val wind:String
        get() = "W: $windSpeed m/s ${dir.values()[((windDirection)/45)]} "


//val wind:String
//    get() = "W: $windSpeed m/s ${dir.values()[((windDirection)/45)]} "

    //todo add human clothing model (values at when what clothes are suitable)
    //todo add get current weather from diffrence between DB rows, almost done
    override fun toString(): String {
        return "${SimpleDateFormat("HH:MM").format(Date(time))} $lat $lon T:$temp℃ $wind ${if(rain==0.0) "clear" else "$rain mm/h"}\n"
    }

    fun getPrettyToString(c: Context):String{
        return "${SimpleDateFormat("HH:MM").format(time)} ${getPlace(c).thoroughfare} T:${temp}℃ ${wind} ${if(rain==0.0) "clear" else "$rain mm/h"}\n"
    }

    fun getPlace(c: Context):Address{
        return Geocoder(c).getFromLocation(lat,lon,1).first()
    }

    enum class dir{
        N, NE, E, SE, S, SW, W, NW
    }
}


@Dao
interface WeatherDataDAO{
    @Query("SELECT * FROM weatherData")
    fun getAll(): List<WeatherData>


    @Query("SELECT * FROM weatherData WHERE lat = :lat AND lon = :lon AND " + "abs( time - :timestamp )< 3600000  ORDER BY time DESC LIMIT 2")
    fun findTwoByPlaceAndTime(lat:Double,lon:Double, timestamp: Long): List<WeatherData>

    @Query("SELECT * FROM weatherData WHERE lat = :lat AND lon = :lon AND " + "abs( time - :timestamp )< 1800000 LIMIT 1")
    fun findByPlaceAndTime(lat:Double,lon:Double, timestamp: Long): WeatherData

    @Insert
    fun insertAll(vararg weather: WeatherData)

    @Update
    fun updateAll(vararg weather: WeatherData)

    @Delete
    fun delete(weather: WeatherData)

}



