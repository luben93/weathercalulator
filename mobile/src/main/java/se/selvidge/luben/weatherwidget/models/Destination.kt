package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*

@Entity(primaryKeys = ["lat","lon"])
data class Destination(var lat:Double, var lon:Double, var comuteStartIntervalStart: Long,var comuteStartIntervalStop: Long) {

}

@Dao
interface DestinationDao{
    @Query("SELECT * FROM destination")
    fun getAll(): List<Destination>

    //todo maybe get next starting instead
    @Query("SELECT * FROM destination WHERE comuteStartIntervalStart < :time+1800000 AND comuteStartIntervalStop > :time LIMIT 1")
    fun getClosest(time: Long): Destination


    @Insert
    fun insertAll(vararg dest: Destination)

    @Update
    fun updateAll(vararg dest: Destination)

    @Delete
    fun delete(dest: Destination)

}