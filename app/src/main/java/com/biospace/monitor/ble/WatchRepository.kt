package com.biospace.monitor.ble

import android.content.Context
import android.util.Log
import com.biospace.monitor.ble.WatchProtocol.WatchReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val TAG = "WatchRepository"

class WatchRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val bleManager = WatchBleManager(context.applicationContext)

    // ─── Per-metric latest-value flows ──────────────────────────────────────
    private val _bloodPressure = MutableStateFlow<WatchReading.BloodPressure?>(null)
    val bloodPressure = _bloodPressure.asStateFlow()

    private val _heartRate = MutableStateFlow<WatchReading.HeartRate?>(null)
    val heartRate = _heartRate.asStateFlow()

    private val _spO2 = MutableStateFlow<WatchReading.SpO2?>(null)
    val spO2 = _spO2.asStateFlow()

    private val _steps = MutableStateFlow<WatchReading.Steps?>(null)
    val steps = _steps.asStateFlow()

    private val _sleep = MutableStateFlow<WatchReading.Sleep?>(null)
    val sleep = _sleep.asStateFlow()

    private val _stress = MutableStateFlow<WatchReading.Stress?>(null)
    val stress = _stress.asStateFlow()

    private val _temperature = MutableStateFlow<WatchReading.Temperature?>(null)
    val temperature = _temperature.asStateFlow()

    private val _immunity = MutableStateFlow<WatchReading.Immunity?>(null)
    val immunity = _immunity.asStateFlow()

    private val _hourlyBundle = MutableStateFlow<WatchReading.HourlyBundle?>(null)
    val hourlyBundle = _hourlyBundle.asStateFlow()

    private val _oneKeyBundle = MutableStateFlow<WatchReading.OneKeyBundle?>(null)
    val oneKeyBundle = _oneKeyBundle.asStateFlow()

    private val _battery = MutableStateFlow(-1)
    val battery = _battery.asStateFlow()

    // History ring buffers (most recent 200 of each)
    private val _bpHistory      = MutableStateFlow<List<WatchReading.BloodPressure>>(emptyList())
    val bpHistory = _bpHistory.asStateFlow()

    private val _hrHistory      = MutableStateFlow<List<WatchReading.HeartRate>>(emptyList())
    val hrHistory = _hrHistory.asStateFlow()

    private val _spo2History    = MutableStateFlow<List<WatchReading.SpO2>>(emptyList())
    val spo2History = _spo2History.asStateFlow()

    private val _sleepHistory   = MutableStateFlow<List<WatchReading.Sleep>>(emptyList())
    val sleepHistory = _sleepHistory.asStateFlow()

    private val _stressHistory  = MutableStateFlow<List<WatchReading.Stress>>(emptyList())
    val stressHistory = _stressHistory.asStateFlow()

    private val _tempHistory    = MutableStateFlow<List<WatchReading.Temperature>>(emptyList())
    val tempHistory = _tempHistory.asStateFlow()

    private val _immHistory     = MutableStateFlow<List<WatchReading.Immunity>>(emptyList())
    val immHistory = _immHistory.asStateFlow()

    private val _stepsHistory   = MutableStateFlow<List<WatchReading.Steps>>(emptyList())
    val stepsHistory = _stepsHistory.asStateFlow()

    // ─── Connection passthrough ──────────────────────────────────────────────
    val connectionState = bleManager.connectionState
    val lastKnownDevice = bleManager.lastKnownDevice

    // ─── Dispatch incoming readings ──────────────────────────────────────────
    init {
        scope.launch {
            bleManager.readings.filterNotNull().collect { reading ->
                dispatch(reading)
            }
        }
        scope.launch {
            bleManager.batteryLevel.collect { level ->
                if (level >= 0) _battery.value = level
            }
        }
    }

    private fun dispatch(reading: WatchReading) {
        Log.v(TAG, "dispatch: $reading")
        when (reading) {
            is WatchReading.BloodPressure -> {
                _bloodPressure.value = reading
                _bpHistory.value = (_bpHistory.value + reading).takeLast(200)
            }
            is WatchReading.HeartRate -> {
                _heartRate.value = reading
                _hrHistory.value = (_hrHistory.value + reading).takeLast(200)
            }
            is WatchReading.SpO2 -> {
                _spO2.value = reading
                _spo2History.value = (_spo2History.value + reading).takeLast(200)
            }
            is WatchReading.Steps -> {
                _steps.value = reading
                _stepsHistory.value = (_stepsHistory.value + reading).takeLast(200)
            }
            is WatchReading.Sleep -> {
                _sleep.value = reading
                _sleepHistory.value = (_sleepHistory.value + reading).takeLast(200)
            }
            is WatchReading.Stress -> {
                _stress.value = reading
                _stressHistory.value = (_stressHistory.value + reading).takeLast(200)
            }
            is WatchReading.Temperature -> {
                _temperature.value = reading
                _tempHistory.value = (_tempHistory.value + reading).takeLast(200)
            }
            is WatchReading.Immunity -> {
                _immunity.value = reading
                _immHistory.value = (_immHistory.value + reading).takeLast(200)
            }
            is WatchReading.HourlyBundle -> {
                _hourlyBundle.value = reading
                // Fan-out hourly bundle into individual metric flows too
                if (reading.heartRate > 0) {
                    val hr = WatchReading.HeartRate(reading.timestampMs, reading.heartRate)
                    _heartRate.value = hr
                    _hrHistory.value = (_hrHistory.value + hr).takeLast(200)
                }
                if (reading.spO2 > 0) {
                    val sp = WatchReading.SpO2(reading.timestampMs, reading.spO2)
                    _spO2.value = sp
                    _spo2History.value = (_spo2History.value + sp).takeLast(200)
                }
                if (reading.systolic > 0 && reading.diastolic > 0) {
                    val bp = WatchReading.BloodPressure(reading.timestampMs, reading.systolic, reading.diastolic)
                    _bloodPressure.value = bp
                    _bpHistory.value = (_bpHistory.value + bp).takeLast(200)
                }
                if (reading.steps > 0) {
                    val st = WatchReading.Steps(reading.timestampMs, reading.steps, reading.kcal)
                    _steps.value = st
                    _stepsHistory.value = (_stepsHistory.value + st).takeLast(200)
                }
            }
            is WatchReading.OneKeyBundle -> {
                _oneKeyBundle.value = reading
                if (reading.heartRate > 0) _heartRate.value =
                    WatchReading.HeartRate(reading.timestampMs, reading.heartRate)
                if (reading.spO2 > 0) _spO2.value =
                    WatchReading.SpO2(reading.timestampMs, reading.spO2)
                if (reading.systolic > 0 && reading.diastolic > 0) _bloodPressure.value =
                    WatchReading.BloodPressure(reading.timestampMs, reading.systolic, reading.diastolic)
            }
            is WatchReading.Battery -> _battery.value = reading.percent
            is WatchReading.SyncDone -> Log.i(TAG, "Sync complete")
            is WatchReading.Unknown -> { /* already logged in BleManager */ }
        }
    }

    // ─── Actions ─────────────────────────────────────────────────────────────
    fun connect() = bleManager.startScan()
    fun connectTo(mac: String) = bleManager.connectTo(mac)
    fun disconnect() = bleManager.disconnect()
    fun close() = bleManager.close()

    companion object {
        @Volatile private var INSTANCE: WatchRepository? = null
        fun getInstance(context: Context): WatchRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WatchRepository(context).also { INSTANCE = it }
            }
    }
}
