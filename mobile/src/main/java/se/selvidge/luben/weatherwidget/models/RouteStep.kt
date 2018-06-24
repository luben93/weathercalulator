package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*

@Entity(foreignKeys = arrayOf(ForeignKey(entity = Destination::class,parentColumns = ["id"], childColumns = ["destinationId"])))
data class RouteStep (

        var lat:Double,
        var lon:Double,
        @ColumnInfo(name = "destinationId")
        var destinationId: Int
){
        @PrimaryKey(autoGenerate = true)
        var id:Int? =null
}

@Dao
interface RouteStepDao{
        @Query("SELECT * FROM routestep WHERE destinationId = :destinationId")
        fun getAllFromDestination(destinationId: Int): List<RouteStep>

        @Insert
        fun insertAll(vararg step: RouteStep)

        @Update
        fun updateAll(vararg step: RouteStep)

        @Delete
        fun delete(step: RouteStep)
}