package se.selvidge.luben.weatherwidget.models

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import android.content.Context


@Database(entities = arrayOf(WeatherData::class,Destination::class,RouteStep::class), version = 8)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDataDAO
    abstract fun destinationDao(): DestinationDao
    abstract fun routeStepDao(): RouteStepDao



    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `WeatherData` ADD COLUMN `gustWindSpeed` REAL NOT NULL DEFAULT 0")
            }
        }
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                synchronized(AppDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.applicationContext,
                                AppDatabase::class.java, "word_database")
                                .addMigrations(MIGRATION_7_8)
                                .fallbackToDestructiveMigration()
                                .build()

                    }
                }
            }
            return INSTANCE!!
        }
    }
}



