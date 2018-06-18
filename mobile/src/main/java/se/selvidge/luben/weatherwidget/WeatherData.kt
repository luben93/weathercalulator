package se.selvidge.luben.weatherwidget

import android.location.Address
import android.util.Log

data class WeatherData(
        var temp:Double,
        var wind: Wind,
        var raining:Boolean,
        var place: Address?
//todo move to pretty string here
//todo add human clothing model (values at when what clothes are suitable)


)


class Wind(var direction: Int,var speed: Double) {
    fun prettyWind():dir{
        Log.d("wind","$direction ${((direction)/45).toInt()}")
        return dir.values()[((direction)/45).toInt()]
    }

    override fun toString(): String {
        return "W: $speed m/s ${prettyWind()} $direction "
    }
}

enum class dir{
    N, NE, E, SE, S, SW, W, NW
}

