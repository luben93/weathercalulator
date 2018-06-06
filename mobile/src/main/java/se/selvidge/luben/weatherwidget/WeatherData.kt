package se.selvidge.luben.weatherwidget

import android.location.Address

data class WeatherData(
        var temp:Double,
        var windSpeed: Double,
        var windDirection: Int,
        var raining:Boolean,
        var place: Address?
//todo move to pretty string here
//todo add human clothing model (values at when what clothes are suitable)
)

