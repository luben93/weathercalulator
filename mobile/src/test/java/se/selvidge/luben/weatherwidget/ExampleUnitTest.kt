package se.selvidge.luben.weatherwidget

import org.junit.Test

import org.junit.Assert.*
import org.junit.After
import android.arch.persistence.room.Room
import android.content.Context
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.doAsyncResult
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.runners.MockitoJUnitRunner
import org.powermock.api.mockito.PowerMockito
import org.powermock.api.mockito.PowerMockito.`when`
import org.powermock.api.mockito.PowerMockito.mockStatic
import org.powermock.core.classloader.annotations.PowerMockIgnore
import org.powermock.core.classloader.annotations.PrepareForTest
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.selvidge.luben.weatherwidget.models.*
import java.io.IOException
import java.util.*


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */


@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
        @PowerMockIgnore( "org.mockito.*", "org.robolectric.*", "android.*" )
@PrepareForTest(Calendar::class)
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    lateinit var mDb:AppDatabase
    lateinit var service:MyService
    lateinit var context: Context
    lateinit var EpochToZeroZero:Calendar
    var timeSinceZeroZero:Long =0
    @Before
    fun createDb() {


        context = RuntimeEnvironment.application

        mDb = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        service=MyService()
        service.db = mDb
        EpochToZeroZero = Calendar.getInstance() // today
        EpochToZeroZero.timeZone = TimeZone.getDefault()// comment out for local system current timezone

         timeSinceZeroZero = (EpochToZeroZero.get(Calendar.HOUR_OF_DAY) * 60 * 60 + EpochToZeroZero.get(Calendar.MINUTE) * 60
                +EpochToZeroZero.get(Calendar.SECOND)) * 1000L + EpochToZeroZero.get(Calendar.MILLISECOND)
        EpochToZeroZero.set(Calendar.HOUR_OF_DAY, 0)
        EpochToZeroZero.set(Calendar.MINUTE, 0)
        EpochToZeroZero.set(Calendar.SECOND, 0)
        EpochToZeroZero.set(Calendar.MILLISECOND, 0)


    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        mDb.close()
    }

    @Test
    fun testWeatherView(){
        var dest = Destination(59.3283, 17.9699, 59.1795, 18.1244, 0, 1)
        val launchTimeEpoch = dest.comuteStartIntervalStart + EpochToZeroZero.timeInMillis
        var back:WeatherView? = null

        Thread {
            mDb.destinationDao().insert(dest)
            dest = mDb.destinationDao().getAll().first()
            var routeStep = RouteStep(59.3283, 17.9699, 2, dest.id!!)
            mDb.routeStepDao().insertAll(routeStep)
            routeStep = mDb.routeStepDao().getAllFromDestination(dest.id!!).first()
            var weatherData = WeatherData(0.0, 0.0, routeStep.id!!, 0.0, 1, launchTimeEpoch-1700000)
            var weatherData2 = WeatherData(10.0, 0.0, routeStep.id!!, 2.0, 2, launchTimeEpoch+1700000)
            mDb.weatherDao().insertAll(weatherData,weatherData2)
            back= service.getWeatherView(dest).first()
        }.apply { start() }.join()

        assertEquals((WeatherView(WeatherData(5.0058823529411764, 0.0, 1,1.0011764705882353,  1, launchTimeEpoch+2000),59.3283,17.9699)),back)


    }

    @Test
    fun testWeatherViewNextLaunchWraparound(){

        if(EpochToZeroZero.get(Calendar.HOUR_OF_DAY)==23
                //&&EpochToZeroZero.get(Calendar.MINUTE)>55
                ){
            assertTrue("try again after 60 min",false)
        }

        val timediff =  timeSinceZeroZero - 3600000L
//        println(EpochToZeroZero.timeInMillis + timeSinceZeroZero)
//        println(timediff)


        var dest = Destination(59.3283, 17.9699, 59.1795, 18.1244,  timediff, timediff+timediff)
        val launchTimeEpoch = dest.comuteStartIntervalStart + EpochToZeroZero.timeInMillis
        var back:WeatherView? = null
//        println(launchTimeEpoch)

        Thread {
            mDb.destinationDao().insert(dest)
            dest = mDb.destinationDao().getAll().first()
            var routeStep = RouteStep(59.3283, 17.9699, 2, dest.id!!)
            mDb.routeStepDao().insertAll(routeStep)
            routeStep = mDb.routeStepDao().getAllFromDestination(dest.id!!).first()
            var weatherData = WeatherData(0.0, 0.0, routeStep.id!!, 0.0, 1, launchTimeEpoch-(timediff/2))
            var weatherData2 = WeatherData(10.0, 0.0, routeStep.id!!, 2.0, 2, launchTimeEpoch+(timediff/2))
            mDb.weatherDao().insertAll(weatherData,weatherData2)
//            println(mDb.weatherDao().getAll())
            back= service.getWeatherView(dest).first()
        }.apply { start() }.join()

        val view =WeatherView(WeatherData(5.006669054188066, 0.0, 1,1.0013338108376133,  1, launchTimeEpoch+2000),59.3283,17.9699)
        assertNotNull(back)
        assertEquals(view.weatherData.temp,back?.weatherData?.temp!!,0.01)
        assertEquals(view.weatherData.windSpeed,back?.weatherData?.windSpeed!!,0.01)
        assertEquals(view.weatherData.time,back?.weatherData?.time)


    }

    @Test
    fun testWeatherViewWrap(){
        var dest = Destination(59.3283, 17.9699, 59.1795, 18.1244, 0, 1)
        val launchTimeEpoch = dest.comuteStartIntervalStart + EpochToZeroZero.timeInMillis + 86400000

        var back:WeatherView? = null
        Thread {
            mDb.destinationDao().insert(dest)
            dest = mDb.destinationDao().getAll().first()
            var routeStep = RouteStep(59.3283, 17.9699, 2, dest.id!!)
            mDb.routeStepDao().insertAll(routeStep)
            routeStep = mDb.routeStepDao().getAllFromDestination(dest.id!!).first()
            var weatherData = WeatherData(0.0, 0.0, routeStep.id!!, 0.0, 1, launchTimeEpoch-1700000)
            var weatherData2 = WeatherData(10.0, 0.0, routeStep.id!!, 2.0, 2, launchTimeEpoch+1700000)
            mDb.weatherDao().insertAll(weatherData,weatherData2)
             back= service.getWeatherView(dest,true).first()
        }.apply { start() }.join()
        assertEquals((WeatherView(WeatherData(5.0058823529411764, 0.0, 1,1.0011764705882353,  1, launchTimeEpoch+2000),59.3283,17.9699)),back)

    }

    @Test
    fun testLerp(){

        var weatherData = WeatherData(18.0, 0.0, 1, 0.0, 1, 20)
        var weatherData2 = WeatherData(25.0, 0.0, 1, 2.0, 5, 50)

        val weather = Pair(weatherData, weatherData2).toWeatherData(35)
        assertEquals(WeatherData(21.50, 0.0, 1,1.0,  3, 35),weather)

    }
}
