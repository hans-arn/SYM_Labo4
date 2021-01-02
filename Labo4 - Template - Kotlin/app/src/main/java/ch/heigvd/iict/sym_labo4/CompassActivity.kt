package ch.heigvd.iict.sym_labo4

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer
import kotlin.math.floor
import kotlin.math.round


/**
 * Project: Labo4
 * Created by fabien.dutoit on 21.11.2016
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2016 - HEIG-VD, IICT
 */
class CompassActivity : AppCompatActivity(),SensorEventListener {

    //opengl
    private lateinit var opglr: OpenGLRenderer
    private lateinit var m3DView: GLSurfaceView

    // Sensor manager and Sensor
    private lateinit var mSensorManager: SensorManager
    private lateinit var mAccelerometer: Sensor
    private lateinit var mMagnetometer: Sensor

    // store result for each sensors
    private var mSensorAcce: FloatArray? = FloatArray(3)
    private var mSensorMagn: FloatArray? = FloatArray(3)

    // swap matrix
    private var  matrix = FloatArray(16)


    override fun onResume() {
        super.onResume()
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        // initialize sensors
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)


        // on s'enregistre pour les capteurs accelerometre et magnetometre
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // we need fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // we initiate the view
        setContentView(R.layout.activity_compass)

        //we create the renderer
        opglr = OpenGLRenderer(applicationContext)

        // link to GUI
        m3DView = findViewById(R.id.compass_opengl)

        //init opengl surface view
        m3DView.setRenderer(opglr)
    }

        override fun onSensorChanged(event: SensorEvent) {
            // on teste la nature de l'événement et on enregistre les données
            val tmp1 : FloatArray = event.values
            var i = 0

            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                // lisse les valeurs des capteurs
                for(item in tmp1){
                    mSensorAcce?.set(i, round(item))
                    i++
                }
            }else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                // lisse les valeurs des capteurs
                for(item in tmp1){
                    mSensorMagn?.set(i, round(item))
                    i++
                }
            }

            SensorManager.getRotationMatrix(matrix, null, mSensorAcce, mSensorMagn)
            opglr.swapRotMatrix(matrix)
        }
    /*
        your activity need to register to accelerometer and magnetometer sensors' updates
        then you may want to call
        TODO
        opglr.swapRotMatrix()
        with the 4x4 rotation matrix, every time a new matrix is computed
        more information on rotation matrix can be found online:
        https://developer.android.com/reference/android/hardware/SensorManager.html#getRotationMatrix(float[],%20float[],%20float[],%20float[])
    */

}