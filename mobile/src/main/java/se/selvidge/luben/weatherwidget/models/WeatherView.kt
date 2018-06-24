package se.selvidge.luben.weatherwidget.models

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.text.SimpleDateFormat

data class WeatherView(val weatherData: WeatherData,val lat: Double,val lon:Double){
    val wind:String
        get() = "W: ${weatherData.windSpeed} m/s ${dir.values()[((weatherData.windDirection)/45)]} "


//val wind:String
//    get() = "W: $windSpeed m/s ${dir.values()[((windDirection)/45)]} "


    override fun toString(): String {
        return "${SimpleDateFormat("HH:MM").format(weatherData.time)} P:$lat,$lon T:${weatherData.temp}℃ $wind ${if(weatherData.rain==0.0) "clear" else "${weatherData.rain} mm/h"}\n"
    }

    fun getPrettyToString(c: Context):String{
        return "${SimpleDateFormat("HH:MM").format(weatherData.time)} P:${getPlace(c).thoroughfare} T:${weatherData.temp}℃ ${wind} ${if(weatherData.rain==0.0) "clear" else "${weatherData.rain} mm/h"}\n"
    }

    fun getPlace(c: Context): Address {
        return Geocoder(c).getFromLocation(lat,lon,1).first()
    }

    enum class dir{
        N, NE, E, SE, S, SW, W, NW
    }
}