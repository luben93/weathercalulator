package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*

@Entity//needs some uniqe check on places
data class Destination(
        var lat:Double,
        var lon:Double,
        var fromLat:Double,
        var fromLon:Double,
        var comuteStartIntervalStart: Long,
        var comuteStartIntervalStop: Long) {
    @PrimaryKey(autoGenerate = true)
    var id:Int?=null

    override fun toString(): String {
        return "Destination(lat=$lat, lon=$lon, fromLat=$fromLat, fromLon=$fromLon, comuteStartIntervalStart=$comuteStartIntervalStart, comuteStartIntervalStop=$comuteStartIntervalStop, id=$id)"
    }


}

@Dao
interface DestinationDao{
    @Query("SELECT * FROM destination ORDER BY comuteStartIntervalStart ASC")
    fun getAll(): List<Destination>

    //todo maybe get next starting instead
    @Query("SELECT * FROM destination WHERE comuteStartIntervalStart > :time  LIMIT 1")
    fun getNext(time: Long): Destination?


    @Query("SELECT *  FROM destination ORDER BY ABS(fromLat - :lat) + ABS(fromLon - :lng) ASC LIMIT 1")
    fun getClosetsOrigin(lat :Double,lng :Double):Destination?

    @Insert
    fun insert(dest: Destination):Long

    @Query("SELECT * FROM destination WHERE id = id")
    fun getById(id:Int): Destination
//    @Update
//    fun updateAll(vararg dest: Destination)

    @Delete
    fun delete(dest: Destination)

}