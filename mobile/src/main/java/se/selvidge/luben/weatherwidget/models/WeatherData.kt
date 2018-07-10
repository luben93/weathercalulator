package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.*

@Entity(primaryKeys = ["route_id","time"],foreignKeys = arrayOf(ForeignKey(entity = RouteStep::class,parentColumns = arrayOf("id"),childColumns = arrayOf("route_id"),onDelete = ForeignKey.CASCADE)))
data class WeatherData(

    var temp:Double,
    var rain:Double,
    @ColumnInfo(name="route_id")
    var routeId:Int,
    var windSpeed:Double,
    var windDirection:Int,
    var time:Long
    )

//todo add human clothing model (values at when what clothes are suitable)
//todo add get current weather from diffrence between DB rows, almost done se service

@Dao
interface WeatherDataDAO{
    @Query("SELECT * FROM weatherData")
    fun getAll(): List<WeatherData>

//todo still need som weather graph beetween a-b 1-2
//    @Query("SELECT * FROM weatherData WHERE lat = :lat AND lon = :lon AND " + "abs( time - :timestamp )< 3600000  ORDER BY time DESC LIMIT 2")
//    fun findTwoByPlaceAndTime(lat:Double,lon:Double, timestamp: Long): List<WeatherData>
//
//    @Query("SELECT * FROM weatherData WHERE lat = :lat AND lon = :lon AND " + "abs( time - :timestamp )< 1800000 LIMIT 1")
//    fun findByPlaceAndTime(lat:Double,lon:Double, timestamp: Long): WeatherData

    @Query("SELECT * FROM weatherdata WHERE route_id = :routeId AND " + " time > :timestamp ORDER BY time DESC LIMIT 1")
    fun getNextFromRoute(routeId: Int,timestamp:Long):WeatherData?

    @Query("SELECT * FROM weatherdata WHERE route_id = :routeId AND " + " time < :timestamp ORDER BY time ASC LIMIT 1")
    fun getPrevFromRoute(routeId: Int,timestamp:Long):WeatherData?

    @Insert
    fun insertAll(vararg weather: WeatherData)

    @Update
    fun updateAll(vararg weather: WeatherData)

    @Delete
    fun delete(weather: WeatherData)

}



