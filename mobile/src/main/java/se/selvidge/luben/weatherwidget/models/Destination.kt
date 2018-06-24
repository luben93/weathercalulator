package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*

@Entity
data class Destination(
        var lat:Double,
        var lon:Double,
        var fromLat:Double,
        var fromLon:Double,
        var comuteStartIntervalStart: Long,
        var comuteStartIntervalStop: Long) {
    @PrimaryKey(autoGenerate = true)
    var id:Int?=null
}

@Dao
interface DestinationDao{
    @Query("SELECT * FROM destination")
    fun getAll(): List<Destination>

    //todo maybe get next starting instead
    @Query("SELECT * FROM destination WHERE comuteStartIntervalStart > :time-1800000  LIMIT 1")
    fun getNext(time: Long): Destination


    @Insert
    fun insertAll(vararg dest: Destination)

    @Update
    fun updateAll(vararg dest: Destination)

    @Delete
    fun delete(dest: Destination)

}