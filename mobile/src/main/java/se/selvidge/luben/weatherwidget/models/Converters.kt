package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromIntArray(value: IntArray?): String? {
        return value?.joinToString(" ")
    }

    @TypeConverter
    fun stringToIntArray(value: String?): IntArray? {
        return value?.split(" ")?.map{Integer.parseInt(it)}?.toIntArray()
    }

}