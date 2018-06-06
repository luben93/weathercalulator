package se.selvidge.luben.weatherwidget

import android.location.Address

data class WeatherData(
        var temp:Double,
        var windSpeed: Double,
        var windDirection: Int,
        var raining:Boolean,
        var place: Address?

)

