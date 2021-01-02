package ch.heigvd.iict.sym_labo4

import ch.heigvd.iict.sym_labo4.abstractactivies.BaseTemplateActivity
import android.bluetooth.BluetoothAdapter
import ch.heigvd.iict.sym_labo4.viewmodels.BleOperationsViewModel
import ch.heigvd.iict.sym_labo4.adapters.ResultsAdapter
import android.os.Bundle
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import androidx.lifecycle.ViewModelProvider
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import kotlinx.android.synthetic.main.activity_ble.view.*
import java.time.LocalDateTime
import java.util.*

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2019 - HEIG-VD, IICT
 */
class BleActivity : BaseTemplateActivity() {
    //system services
    private lateinit var bluetoothAdapter: BluetoothAdapter

    //view model
    private lateinit var bleViewModel: BleOperationsViewModel

    //gui elements
    private lateinit var operationPanel: View
    private lateinit var scanPanel: View
    private lateinit var scanResults: ListView
    private lateinit var emptyScanResults: TextView

    // AJOUT DES TEXTS D INFORMATIONS RECU VIA BLE
    private lateinit var dateBLE: TextView
    private lateinit var temperatureBLE: TextView
    private lateinit var buttonClickBLE: TextView

    // AJOUT DES BOUTONS ET INTERFACES POUR L'UTILISATEUR
    private lateinit var buttonTemperatureBLE: Button

    private lateinit var integerFieldBLE: EditText
    private lateinit var buttonIntegerBLE: Button

    private lateinit var dateFieldBLE: EditText
    private lateinit var buttonDateBLE: Button

    private lateinit var timeFieldBLE: EditText

    private lateinit var buttonDateUpdateBLE: Button

    //menu elements
    private var scanMenuBtn: MenuItem? = null
    private var disconnectMenuBtn: MenuItem? = null

    //adapters
    private lateinit var scanResultsAdapter: ResultsAdapter

    //states
    private var handler = Handler(Looper.getMainLooper())

    private var isScanning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ble)

        //enable and start bluetooth - initialize bluetooth adapter
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        //link GUI
        operationPanel = findViewById(R.id.ble_operation)
        scanPanel = findViewById(R.id.ble_scan)
        scanResults = findViewById(R.id.ble_scanresults)
        emptyScanResults = findViewById(R.id.ble_scanresults_empty)

        // AJOUT DES TEXTS D INFORMATIONS RECU VIA BLE
        dateBLE = findViewById(R.id.date)
        temperatureBLE = findViewById(R.id.temp)
        buttonClickBLE = findViewById(R.id.buttonClick)

        // AJOUT DES BOUTONS ET INTERFACES POUR L'UTILISATEUR
        buttonTemperatureBLE = findViewById(R.id.buttonTemperature)

        integerFieldBLE = findViewById(R.id.integerField)
        buttonIntegerBLE = findViewById(R.id.buttonInteger)

        dateFieldBLE = findViewById(R.id.dateField)
        buttonDateBLE = findViewById(R.id.buttonDate)

        timeFieldBLE = findViewById(R.id.timeField)

        buttonDateUpdateBLE = findViewById(R.id.buttonDateUpdate)

        // For asking to read the temperature of the device
        buttonTemperatureBLE.setOnClickListener(){
            bleViewModel.readTemperature()
        }

        // For sending an integer to the device
        buttonIntegerBLE.setOnClickListener(){
            bleViewModel.sendInteger((integerFieldBLE.text).toString().toInt())
        }

        // For automatically updating the date
        buttonDateUpdateBLE.setOnClickListener(){
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val min = calendar.get(Calendar.MINUTE)
            val sec = calendar.get(Calendar.SECOND)
            val year = calendar.get(Calendar.YEAR)
            var month = calendar.get(Calendar.MONTH)+1
            if (month > 11) {
                month = 0
            }
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            bleViewModel.sendDate(hour,min,sec,year,month,day)
        }

        // For manually updating the date
        buttonDateBLE.setOnClickListener(){
            // Variables for the formats verifications
            var dateOK = false
            var timeOK = false

            var day: Int = 0
            var month: Int = 0
            var year: Int = 0

            var hour: Int = 0
            var min: Int = 0
            var sec: Int = 0

            // Verification for the Date format
            val regexDate = Regex("[0-3][0-9](\\.|\\/|-)[0-1][0-9](\\.|\\/|-)[0-9]{4}")
            if (regexDate.matchEntire(dateFieldBLE.text) != null){
                day = dateFieldBLE.text.substring(0,2).toInt()
                month = dateFieldBLE.text.substring(3,5).toInt()
                year = dateFieldBLE.text.substring(6,10).toInt()

                if (day<=31 && month <=12){
                    dateOK = true
                }
            }

            // Verification for the Time format
            val regexTime = Regex("[0-2][0-9](:)[0-5][0-9](:)[0-5][0-9]")
            if (regexTime.matchEntire(timeFieldBLE.text) != null){
                hour = timeFieldBLE.text.substring(0,2).toInt()
                min = timeFieldBLE.text.substring(3,5).toInt()
                sec = timeFieldBLE.text.substring(6,8).toInt()

                if (hour<=23 && min<=59 && sec<=59){
                    timeOK = true
                }
            }

            // If everything is OK we can call the function for updating the date and time
            if (dateOK && timeOK){
                bleViewModel.sendDate(hour,min,sec,year,month,day)
            }
            else {
                Toast.makeText(this, "Mauvais format !", Toast.LENGTH_LONG).show()
            }
        }

        //manage scanned item
        scanResultsAdapter = ResultsAdapter(this)
        scanResults.adapter = scanResultsAdapter
        scanResults.emptyView = emptyScanResults

        //connect to view model
        bleViewModel = ViewModelProvider(this).get(BleOperationsViewModel::class.java)

        updateGui()

        //events
        scanResults.setOnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
            runOnUiThread {
                //we stop scanning
                scanLeDevice(false)
                //we connect
                bleViewModel.connect(scanResultsAdapter.getItem(position).device)
            }
        }

        //ble events
        bleViewModel.isConnected.observe(this, { updateGui() })
        bleViewModel.temperature.observe(this, { updateGui() })
        bleViewModel.date.observe(this, {updateGui()})
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.ble_menu, menu)
        //we link the two menu items
        scanMenuBtn = menu.findItem(R.id.menu_ble_search)
        disconnectMenuBtn = menu.findItem(R.id.menu_ble_disconnect)
        //we update the gui
        updateGui()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_ble_search) {
            if (isScanning) scanLeDevice(false) else scanLeDevice(true)
            return true
        } else if (id == R.id.menu_ble_disconnect) {
            bleViewModel.disconnect()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        if (isScanning) scanLeDevice(false)
        if (isFinishing) bleViewModel.disconnect()
    }

    /*
     * Method used to update the GUI according to BLE status:
     * - connected: display operation panel (BLE control panel)
     * - not connected: display scan result list
     */
    private fun updateGui() {
        val isConnected = bleViewModel.isConnected.value
        if (isConnected != null && isConnected) {

            scanPanel.visibility = View.GONE
            operationPanel.visibility = View.VISIBLE

            temperatureBLE.text = bleViewModel.temperature.value + " Degrès Celsius"
            dateBLE.text = bleViewModel.date.value

            if (scanMenuBtn != null && disconnectMenuBtn != null) {
                scanMenuBtn!!.isVisible = false
                disconnectMenuBtn!!.isVisible = true
            }
        } else {
            operationPanel.visibility = View.GONE
            scanPanel.visibility = View.VISIBLE

            if (scanMenuBtn != null && disconnectMenuBtn != null) {
                disconnectMenuBtn!!.isVisible = false
                scanMenuBtn!!.isVisible = true
            }
        }
    }

    //this method need user granted localisation permission, our demo app is requesting it on MainActivity
    private fun scanLeDevice(enable: Boolean) {
        val bluetoothScanner = bluetoothAdapter.bluetoothLeScanner

        if (enable) {
            //config
            val builderScanSettings = ScanSettings.Builder()
            builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            builderScanSettings.setReportDelay(0)

            //we scan for any BLE device
            //we don't filter them based on advertised services...
            // Un filtre pour n'afficher que les devices proposant
            // le service "SYM" (UUID: "3c0a1000-281d-4b48-b2a7-f15579a1c38f")
            val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString("3c0a1000-281d-4b48-b2a7-f15579a1c38f")).build()
            val filters: List<ScanFilter> = listOf(filter)
            //reset display
            scanResultsAdapter.clear()
            bluetoothScanner.startScan(filters, builderScanSettings.build(), leScanCallback)
            Log.d(TAG, "Start scanning...")
            isScanning = true

            //we scan only for 15 seconds
            handler.postDelayed({ scanLeDevice(false) }, 15 * 1000L)
        } else {
            bluetoothScanner.stopScan(leScanCallback)
            isScanning = false
            Log.d(TAG, "Stop scanning (manual)")
        }
    }

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            runOnUiThread { scanResultsAdapter.addDevice(result) }
        }
    }

    companion object {
        private val TAG = BleActivity::class.java.simpleName
    }
}