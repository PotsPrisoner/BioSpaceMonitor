package com.biospace.monitor.ble

import java.util.UUID

object WatchProtocol {

    val SERVICE_UUID     = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHAR_NOTIFY_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHAR_WRITE_UUID  = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val DESCRIPTOR_UUID  = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB")

    // Packet type byte[4]
    private const val TYPE_HISTORY  = 0x51
    private const val TYPE_BP_LIVE  = 0x92
    private const val TYPE_BP_HIST  = 0x52
    private const val TYPE_SPO2     = 0x73
    private const val TYPE_TEMP     = 0x87
    private const val TYPE_BATTERY  = 0x91
    private const val TYPE_RESP     = 0x9E
    private const val TYPE_STRESS   = 0x74
    private const val TYPE_SLEEP    = 0x9B

    // Subtypes for TYPE_HISTORY (byte[5])
    private const val SUB_HR        = 0x11  // heart rate history
    private const val SUB_STRESS    = 0x0B  // stress history
    private const val SUB_SPO2      = 0x12  // SpO2 history
    private const val SUB_STEPS     = 0x20  // steps/calories hourly
    private const val SUB_STEPS_TOT = 0x08  // steps total

    fun parse(raw: ByteArray): WatchReading? {
        if (raw.size < 5) return null
        val hdr  = raw[0].u8()
        val type = raw[4].u8()
        val sub  = if (raw.size > 5) raw[5].u8() else 0

        // FF 00 0C packets = BP records from watch
        if (hdr == 0xFF && raw.size >= 15) {
            return WatchReading.BloodPressure(
                year      = 2000 + raw[6].u8(),
                month     = raw[7].u8(),
                day       = raw[8].u8(),
                hour      = raw[9].u8(),
                minute    = raw[10].u8(),
                systolic  = raw[12].u8(),
                diastolic = raw[13].u8(),
                heartRate = raw[14].u8()
            )
        }

        if (hdr != 0xAB) return null

        return when (type) {

            TYPE_BATTERY -> if (raw.size >= 8)
                WatchReading.Battery(raw[7].u8()) else null

            TYPE_TEMP -> null // watch sends flag only, no real value

            TYPE_BATTERY -> if (raw.size >= 8)
                WatchReading.Battery(raw[7].u8()) else null

            // 0x51 history packets
            TYPE_HISTORY -> when (sub) {
                SUB_HR -> if (raw.size >= 12)
                    WatchReading.HeartRate(
                        year   = 2000 + raw[6].u8(), month  = raw[7].u8(),
                        day    = raw[8].u8(),         hour   = raw[9].u8(),
                        minute = raw[10].u8(),        bpm    = raw[11].u8()
                    ) else null

                SUB_SPO2 -> if (raw.size >= 12)
                    WatchReading.SpO2(raw[11].u8()) else null

                SUB_STRESS -> if (raw.size >= 12)
                    WatchReading.Stress(raw[11].u8()) else null

                SUB_STEPS -> if (raw.size >= 14)
                    WatchReading.StepsHourly(
                        year  = 2000 + raw[6].u8(), month = raw[7].u8(),
                        day   = raw[8].u8(),         hour  = raw[9].u8(),
                        steps = (raw[11].u8() shl 8) or raw[12].u8()
                    ) else null

                SUB_STEPS_TOT -> if (raw.size >= 8)
                    WatchReading.StepsSummary(
                        (raw[6].u8() shl 8) or raw[7].u8()
                    ) else null

                else -> null
            }

            // 0x52 BP history batches
            TYPE_BP_HIST -> if (raw.size >= 14)
                WatchReading.BloodPressure(
                    year      = 2000 + raw[6].u8(), month     = raw[7].u8(),
                    day       = raw[8].u8(),         hour      = raw[9].u8(),
                    minute    = raw[10].u8(),        systolic  = raw[11].u8(),
                    diastolic = raw[12].u8(),        heartRate = raw[13].u8()
                ) else null

            // 0x9E respiratory rate — byte[10] only, ignore byte[11]
            TYPE_RESP -> if (raw.size >= 11)
                WatchReading.RespiratoryRate(raw[10].u8()) else null

            // 0x74 stress
            TYPE_STRESS -> if (raw.size >= 7)
                WatchReading.Stress(raw[6].u8()) else null

            // 0x73 SpO2 hourly history — byte[10] is the value
            TYPE_SPO2 -> if (raw.size >= 11)
                WatchReading.SpO2(raw[10].u8()) else null

            TYPE_SLEEP -> if (raw.size >= 8)
                WatchReading.Sleep(raw[6].u8(), raw[7].u8()) else null

            else -> null
        }
    }

    private fun Byte.u8() = this.toInt() and 0xFF
}

sealed class WatchReading {
    data class Battery(val percent: Int) : WatchReading()
    data class Temperature(val celsius: Int) : WatchReading()
    data class StepsSummary(val steps: Int) : WatchReading()
    data class StepsHourly(val year: Int, val month: Int, val day: Int, val hour: Int, val steps: Int) : WatchReading()
    data class HeartRate(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val bpm: Int) : WatchReading()
    data class BloodPressure(val year: Int, val month: Int, val day: Int, val hour: Int, val minute: Int, val systolic: Int, val diastolic: Int, val heartRate: Int) : WatchReading()
    data class SpO2(val percent: Int) : WatchReading()
    data class RespiratoryRate(val rpm: Int) : WatchReading()
    data class Stress(val score: Int) : WatchReading()
    data class Sleep(val lightMinutes: Int, val deepMinutes: Int) : WatchReading()
}
