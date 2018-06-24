package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context


@Database(entities = arrayOf(WeatherData::class,Destination::class,RouteStep::class), version = 4)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDataDAO
    abstract fun destinationDao(): DestinationDao
    abstract fun routeStepDao(): RouteStepDao


    companion object {
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                                AppDatabase::class.java, "word_database")
                                .fallbackToDestructiveMigration()
                                .build()

                    }
                }
            }
            return INSTANCE!!
        }
    }
}



