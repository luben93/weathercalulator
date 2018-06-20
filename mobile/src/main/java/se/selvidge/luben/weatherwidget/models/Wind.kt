package se.selvidge.luben.weatherwidget.models

import android.util.Log
import io.realm.RealmModel
import io.realm.annotations.RealmClass

@RealmClass
open class Wind: RealmModel {

    var direction = 0
    var speed = 0.0
//    constructor() : this(0,0.0)

    fun prettyWind(): dir {
        Log.d("wind","$direction ${((direction)/45).toInt()}")
        return dir.values()[((direction)/45).toInt()]
    }

    override fun toString(): String {
        return "W: $speed m/s ${prettyWind()} "
    }

    enum class dir{
        N, NE, E, SE, S, SW, W, NW
    }


}