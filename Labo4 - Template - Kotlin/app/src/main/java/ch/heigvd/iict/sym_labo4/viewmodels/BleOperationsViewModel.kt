package ch.heigvd.iict.sym_labo4.viewmodels

import android.app.Application
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.data.Data
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.nio.ByteBuffer
import java.util.*

/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 06.11.2020
 * (C) 2019 - HEIG-VD, IICT
 */
class BleOperationsViewModel(application: Application) : AndroidViewModel(application) {

    private var ble = SYMBleManager(application.applicationContext)
    private var mConnection: BluetoothGatt? = null

    //live data - observer
    val isConnected = MutableLiveData(false)

    val date = MutableLiveData("Jour / Mois / Année | Heure : Minute : Seconde")
    val temperature = MutableLiveData("TEMPERATURE")

    //UUIDs for services and characteristics
    private val timeServiceUUID = "00001805-0000-1000-8000-00805f9b34fb"
    private val symServiceUUID = "3c0a1000-281d-4b48-b2a7-f15579a1c38f"

    private val currentTimeCharUUID = "00002A2B-0000-1000-8000-00805f9b34fb"
    private val integerCharUUID = "3c0a1001-281d-4b48-b2a7-f15579a1c38f"
    private val temperatureCharUUID = "3c0a1002-281d-4b48-b2a7-f15579a1c38f"
    private val buttonClickCharUUID = "3c0a1003-281d-4b48-b2a7-f15579a1c38f"

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
        ble.disconnect()
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "User request connection to: $device")
        if (!isConnected.value!!) {
            ble.connect(device)
                    .retry(1, 100)
                    .useAutoConnect(false)
                    .enqueue()
        }
    }

    fun disconnect() {
        Log.d(TAG, "User request disconnection")
        ble.disconnect()
        mConnection?.disconnect()
    }

    /* TODO
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */

    fun readTemperature(): Boolean {
        return if (!isConnected.value!! || temperatureChar == null)
            false
        else
            ble.readTemperature()
    }

    private val bleConnectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnecting")
            isConnected.value = false
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnected")
            isConnected.value = true
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceDisconnecting")
            isConnected.value = false
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceReady")
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.d(TAG, "onDeviceFailedToConnect")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            if(reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                Log.d(TAG, "onDeviceDisconnected - not supported")
                Toast.makeText(getApplication(), "Device not supported - implement method isRequiredServiceSupported()", Toast.LENGTH_LONG).show()
            }
            else
                Log.d(TAG, "onDeviceDisconnected")
            isConnected.value = false
        }

    }

    private inner class SYMBleManager(applicationContext: Context) : BleManager(applicationContext) {
        /**
         * BluetoothGatt callbacks object.
         */
        private var mGattCallback: BleManagerGattCallback? = null

        public override fun getGattCallback(): BleManagerGattCallback {
            //we initiate the mGattCallback on first call, singleton
            if (mGattCallback == null) {
                mGattCallback = object : BleManagerGattCallback() {

                    public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                        mConnection = gatt //trick to force disconnection

                        Log.d(TAG, "isRequiredServiceSupported - TODO")

                        /*
                        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
                          bien tous les services et les caractéristiques attendues, on vérifiera aussi que les
                          caractéristiques présentent bien les opérations attendues
                        - On en profitera aussi pour garder les références vers les différents services et
                          caractéristiques (déclarés en lignes 39 à 44)
                        */
                        var AllCheck = false

                        // Vérification des services et des caractéristiques
                        for (i in gatt.services){
                            if (i.uuid == UUID.fromString(timeServiceUUID)){
                                timeService = i
                                for (j in i.characteristics) {
                                    if (j.uuid == UUID.fromString(currentTimeCharUUID)){
                                        currentTimeChar = j
                                    }
                                }
                            }
                            if (i.uuid == UUID.fromString(symServiceUUID)){
                                symService = i
                                for (j in i.characteristics) {
                                    if (j.uuid == UUID.fromString(integerCharUUID)){
                                        integerChar = j
                                    }
                                    if (j.uuid == UUID.fromString(buttonClickCharUUID)){
                                        buttonClickChar = j
                                    }
                                    if (j.uuid == UUID.fromString(temperatureCharUUID)){
                                        temperatureChar = j
                                    }
                                }
                            }
                        }

                        // Vérification des opérations
                        if (timeService != null && symService != null && currentTimeChar != null && integerChar != null && buttonClickChar != null && temperatureChar != null) {
                            AllCheck = true
                        }

                        return AllCheck
                    }

                    override fun initialize() {
                        /*  TODO
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */
                        // Réception des notifications

                        // Local
                        mConnection?.setCharacteristicNotification(currentTimeChar, true)
                        mConnection?.setCharacteristicNotification(buttonClickChar, true)

                        // Remote
                        val clientConfigurationUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

                        var descCT = currentTimeChar?.getDescriptor(clientConfigurationUUID)
                        if (descCT != null) {
                            descCT.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        }
                        mConnection?.writeDescriptor(descCT)

                        var descBC = buttonClickChar?.getDescriptor(clientConfigurationUUID)
                        if (descBC != null) {
                            descBC.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        }
                        mConnection?.writeDescriptor(descBC)

                        // CALLBACKS
                        // Les callbacks passent par la fonction onCharacteristicNotified

                    }

                    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                        super.onCharacteristicRead(gatt, characteristic)
                        if (characteristic.uuid.toString() == temperatureCharUUID) {
                            temperature.postValue(characteristic.getIntValue(Data.FORMAT_UINT16,0).div(10).toString())
                        }
                    }

                    override fun onCharacteristicNotified(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                        super.onCharacteristicNotified(gatt, characteristic)

                        readTemperature()

                        if (characteristic.uuid == currentTimeChar?.uuid) {
                            var year = characteristic.getIntValue(Data.FORMAT_UINT16, 0)
                            var month = characteristic.getIntValue(Data.FORMAT_UINT8, 2)
                            var day = characteristic.getIntValue(Data.FORMAT_UINT8, 3)

                            var hour = characteristic.getIntValue(Data.FORMAT_UINT8, 4);
                            var min = characteristic.getIntValue(Data.FORMAT_UINT8, 5);
                            var sec = characteristic.getIntValue(Data.FORMAT_UINT8, 6);

                            date.postValue("$day/$month/$year | $hour:$min:$sec")
                        }
                        if (characteristic.uuid == buttonClickChar?.uuid) {
                            Log.d("ON NOTIFICATION button", String(characteristic.value))
                        }
                    }

                    override fun onDeviceDisconnected() {
                        //we reset services and characteristics
                        timeService = null
                        currentTimeChar = null
                        symService = null
                        integerChar = null
                        temperatureChar = null
                        buttonClickChar = null
                    }
                }
            }
            return mGattCallback!!
        }


        fun readTemperature(): Boolean {
            /*
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            if (temperatureChar != null) {
                mConnection?.readCharacteristic(temperatureChar)
                return true
            } else {
                return false
            }
        }
    }

    companion object {
        private val TAG = BleOperationsViewModel::class.java.simpleName
    }

    init {
        ble.setConnectionObserver(bleConnectionObserver)
    }

}