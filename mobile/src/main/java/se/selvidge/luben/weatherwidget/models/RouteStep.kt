package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*

@Entity(foreignKeys = arrayOf(ForeignKey(entity = Destination::class,parentColumns = ["id"], childColumns = ["destinationId"])))
data class RouteStep (

        var lat:Double,
        var lon:Double,
        var timeElapsed:Int,
        @ColumnInfo(name = "destinationId")
        var destinationId: Int
){
        @PrimaryKey(autoGenerate = true)
        var id:Int? =null
}

@Dao
interface RouteStepDao{
        @Query("SELECT * FROM routestep WHERE destinationId = :destinationId ORDER BY timeElapsed ASC")
        fun getAllFromDestination(destinationId: Int): List<RouteStep>

        @Query("SELECT * FROM routestep WHERE lat = :lat AND lon = :lon LIMIT 1")
        fun getFromLatLon(lat: Double,lon: Double):RouteStep?

        @Insert
        fun insertAll(vararg step: RouteStep)

        @Update
        fun updateAll(vararg step: RouteStep)

        @Delete
        fun delete(step: RouteStep)


}