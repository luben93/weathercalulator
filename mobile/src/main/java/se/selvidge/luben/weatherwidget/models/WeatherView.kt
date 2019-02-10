package se.selvidge.luben.weatherwidget.models

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

data class WeatherView(val weatherData: WeatherData,val lat: Double,val lon:Double){
    val wind:String
        get() = "W: ${"%.1f".format(Locale.ENGLISH, weatherData.windSpeed)}(${"%.1f".format(Locale.ENGLISH, weatherData.gustWindSpeed)}) m/s ${dir.values()[((weatherData.windDirection)/45)]} "


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

fun List<WeatherView>.foldAndAvg(context: Context): String = fold("") { acc, data -> acc + data.getPrettyToString(context) } + maxMinWW(
        map { ww -> ww.weatherData.temp }.max(),
        map { ww -> ww.weatherData.temp }.min(),
        map { ww -> ww.weatherData.windSpeed }.average(),
        map { ww -> ww.weatherData.gustWindSpeed }.max(),
        map { ww -> ww.weatherData.windDirection }.average().roundToInt(),
        map { ww -> ww.weatherData.rain }.max(),
        map { ww -> ww.weatherData.rain }.min(),
        map { ww -> ww.weatherData.humidity }.max(),
        map { ww -> ww.weatherData.humidity }.min())

fun maxMinWW(tp: Double?, tm: Double?, ws: Double,gust: Double?, wd: Int, rp: Double?, rm: Double?,hp:Int?,hm:Int?): String {
    return "max/min T: ${"%.1f".format(Locale.ENGLISH, tp)} / ${"%.1f".format(Locale.ENGLISH, tm)}℃ H: $hp-$hm% W: ${
    "%.1f".format(Locale.ENGLISH, ws)}(${"%.1f".format(Locale.ENGLISH, gust)}) m/s ${WeatherView.dir.values()[((wd) / 45)]} ${
    if (rp == 0.0 && rm == 0.0) "clear" else "rain: ${"%.1f".format(Locale.ENGLISH, rp)} - ${"%.1f".format(Locale.ENGLISH, rp)} mm/h"}\n"
}

data class WeatherDestination(val weatherDatas: List<WeatherView>,val destination: Destination)
