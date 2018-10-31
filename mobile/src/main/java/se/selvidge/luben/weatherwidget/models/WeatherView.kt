package se.selvidge.luben.weatherwidget.models

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.text.SimpleDateFormat
import java.util.*

data class WeatherView(val weatherData: WeatherData,val lat: Double,val lon:Double){
    val wind:String
        get() = "W: ${"%.1f".format(Locale.ENGLISH, weatherData.windSpeed)} m/s ${dir.values()[((weatherData.windDirection)/45)]} "


//val wind:String
//    get() = "W: $windSpeed m/s ${dir.values()[((windDirection)/45)]} "


    fun getPrettyToString(c: Context):String{
        val place = getPlace(c)
        var name = place.subLocality
        if(name == null){
            name = place.thoroughfare
        }
        val dataDate = SimpleDateFormat("dd").format(weatherData.time) 
        val todayDate = SimpleDateFormat("dd").format(System.currentTimeMillis()) 
        var weekday = ""
        if(todayDate != dataDate){
            weekday = SimpleDateFormat("EEE").format(weatherData.time) + " "
        }
        //todo if weather time is not today also show day
        return "${weekday}${SimpleDateFormat("HH:mm").format(weatherData.time)} P:${name} T:${"%.1f".format(Locale.ENGLISH, weatherData.temp)}℃ ${wind} H: ${weatherData.humidity}% ${if(weatherData.rain==0.0) "clear" else "${"%.1f".format(Locale.ENGLISH, weatherData.rain)} mm/h"}\n"
    }

    fun getPlace(c: Context): Address {
        return Geocoder(c).getFromLocation(lat,lon,1).first()
    }

    enum class dir{
        N, NE, E, SE, S, SW, W, NW
    }
}


data class WeatherDestination(val weatherDatas: List<WeatherView>,val destination: Destination)
