package com.biospace.monitor.ble

import java.util.UUID

// ─── NUS UUIDs (confirmed from BpDoctor decompile) ──────────────────────────
object WatchProtocol {

    val NUS_SERVICE: UUID        = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_TX_WRITE: UUID       = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
    val NUS_RX_NOTIFY: UUID      = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    val CCCD: UUID               = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    val HEART_RATE_MEASUREMENT   = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

    // ─── Command type constants (list[1] in MsgPack array) ──────────────────
    // These were inferred from DataSyncMgr routing in the Wakeup SDK.
    // If readings don't parse correctly, check Logcat tag "WatchPacket" and
    // update these values to match byte[1] in the observed packets.
    const val CMD_CURRENT_BP            = 0x14  // real-time BP during measurement
    const val CMD_SINGLE_BP             = 0x15  // single completed BP from watch press
    const val CMD_MEASURE_BP            = 0x16  // BP with full timestamp
    const val CMD_CURRENT_HR            = 0x17  // real-time heart rate
    const val CMD_SINGLE_HR             = 0x18
    const val CMD_MEASURE_HR            = 0x19
    const val CMD_CURRENT_SPO2          = 0x1A  // real-time blood oxygen
    const val CMD_SINGLE_SPO2           = 0x1B
    const val CMD_MEASURE_SPO2          = 0x1C
    const val CMD_CURRENT_STEPS         = 0x21  // live step count (24-bit split)
    const val CMD_HOURLY_BUNDLE         = 0x22  // steps+kcal+HR+SpO2+BP in one packet
    const val CMD_ONE_KEY_BUNDLE        = 0x23  // HR+SpO2+BP triggered by watch button
    const val CMD_SLEEP                 = 0x30  // sleep segment
    const val CMD_STRESS                = 0x31  // stress/pressure score
    const val CMD_TEMPERATURE_SINGLE    = 0x40
    const val CMD_TEMPERATURE_MEASURE   = 0x41
    const val CMD_IMMUNITY_SINGLE       = 0x50
    const val CMD_IMMUNITY_MEASURE      = 0x51
    const val CMD_HOURLY_TEMP_IMM       = 0x52  // hourly temp+immunity bundle
    const val CMD_BATTERY               = 0x60
    const val CMD_SYNC_DONE             = 0xFF

    // ─── Sealed hierarchy of every readable metric ───────────────────────────
    sealed class WatchReading {
        // timestamp = epoch millis; 0 = use System.currentTimeMillis()
        data class BloodPressure(
            val timestampMs: Long,
            val systolic: Int,
            val diastolic: Int,
            val source: Source = Source.WATCH
        ) : WatchReading()

        data class HeartRate(
            val timestampMs: Long,
            val bpm: Int,
            val source: Source = Source.WATCH
        ) : WatchReading()

        data class SpO2(
            val timestampMs: Long,
            val percent: Int,
            val source: Source = Source.WATCH
        ) : WatchReading()

        data class Steps(
            val timestampMs: Long,
            val count: Int,
            val kcal: Int
        ) : WatchReading()

        data class Sleep(
            val timestampMs: Long,
            val type: Int,   // 0=awake 1=light 2=deep
            val durationMinutes: Int
        ) : WatchReading()

        data class Stress(
            val timestampMs: Long,
            val score: Int   // 0-100
        ) : WatchReading()

        data class Temperature(
            val timestampMs: Long,
            val celsius: Float
        ) : WatchReading()

        data class Immunity(
            val timestampMs: Long,
            val score: Int   // 0-100
        ) : WatchReading()

        data class Battery(val percent: Int) : WatchReading()

        data class HourlyBundle(
            val timestampMs: Long,
            val steps: Int,
            val kcal: Int,
            val heartRate: Int,
            val spO2: Int,
            val systolic: Int,
            val diastolic: Int
        ) : WatchReading()

        data class OneKeyBundle(
            val timestampMs: Long,
            val heartRate: Int,
            val spO2: Int,
            val systolic: Int,
            val diastolic: Int
        ) : WatchReading()

        object SyncDone : WatchReading()

        /** Raw packet that didn't match any known command — logged for debugging */
        data class Unknown(val cmdByte: Int, val raw: ByteArray) : WatchReading()
    }

    enum class Source { WATCH, MANUAL }

    // ─── MsgPack-aware packet parser ─────────────────────────────────────────
    // BpDoctor's Wakeup SDK encodes NUS payloads as MsgPack fixarray of fixints.
    // Each list element maps 1:1 to list.get(N) in the DataSyncMgr handlers.
    // Layout: [header0, cmdType, arg2, arg3, arg4, arg5, data0, data1, ...]
    //                    ^[1]                              ^[6]
    fun parse(raw: ByteArray, nowMs: Long = System.currentTimeMillis()): WatchReading {
        val bytes = decodeMsgPackArray(raw) ?: return WatchReading.Unknown(-1, raw)

        if (bytes.size < 2) return WatchReading.Unknown(-1, raw)
        val cmd = bytes[1]

        return when (cmd) {
            // ── Blood Pressure ───────────────────────────────────────────────
            CMD_CURRENT_BP, CMD_SINGLE_BP -> {
                if (bytes.size < 9) return WatchReading.Unknown(cmd, raw)
                val sys = bytes[6]; val dia = bytes[7]
                if (sys <= 0 || dia <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.BloodPressure(nowMs, sys, dia)
            }
            CMD_MEASURE_BP -> {
                if (bytes.size < 13) return WatchReading.Unknown(cmd, raw)
                val ts = timestampFromBytes(bytes, 6)
                val sys = bytes[11]; val dia = bytes[12]
                if (sys <= 0 || dia <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.BloodPressure(ts, sys, dia)
            }
            // ── Heart Rate ───────────────────────────────────────────────────
            CMD_CURRENT_HR, CMD_SINGLE_HR -> {
                if (bytes.size < 8) return WatchReading.Unknown(cmd, raw)
                val bpm = bytes[6]
                if (bpm <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.HeartRate(nowMs, bpm)
            }
            CMD_MEASURE_HR -> {
                if (bytes.size < 12) return WatchReading.Unknown(cmd, raw)
                val ts = timestampFromBytes(bytes, 6)
                val bpm = bytes[11]
                if (bpm <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.HeartRate(ts, bpm)
            }
            // ── SpO2 ─────────────────────────────────────────────────────────
            CMD_CURRENT_SPO2, CMD_SINGLE_SPO2 -> {
                if (bytes.size < 8) return WatchReading.Unknown(cmd, raw)
                val pct = bytes[6]
                if (pct <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.SpO2(nowMs, pct)
            }
            CMD_MEASURE_SPO2 -> {
                if (bytes.size < 12) return WatchReading.Unknown(cmd, raw)
                val ts = timestampFromBytes(bytes, 6)
                val pct = bytes[11]
                if (pct <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.SpO2(ts, pct)
            }
            // ── Steps (live — 24-bit split across 3 bytes) ───────────────────
            CMD_CURRENT_STEPS -> {
                if (bytes.size < 17) return WatchReading.Unknown(cmd, raw)
                val steps = bytes[8] + (bytes[7] shl 8) + (bytes[6] shl 16)
                val kcal  = bytes[11] + (bytes[10] shl 8) + (bytes[9] shl 16)
                WatchReading.Steps(nowMs, steps, kcal)
            }
            // ── Hourly bundle (steps+kcal+HR+SpO2+BP) ────────────────────────
            CMD_HOURLY_BUNDLE -> {
                if (bytes.size < 20) return WatchReading.Unknown(cmd, raw)
                val ts    = timestampFromBytes(bytes, 6)
                val steps = bytes[12] + (bytes[11] shl 8) + (bytes[10] shl 16)
                val kcal  = bytes[15] + (bytes[14] shl 8) + (bytes[13] shl 16)
                val hr    = bytes[16]
                val spo2  = bytes[17]
                val sys   = bytes[18]
                val dia   = bytes[19]
                WatchReading.HourlyBundle(ts, steps, kcal, hr, spo2, sys, dia)
            }
            // ── One-key bundle (HR+SpO2+BP from watch button) ─────────────────
            CMD_ONE_KEY_BUNDLE -> {
                if (bytes.size < 11) return WatchReading.Unknown(cmd, raw)
                val hr   = bytes[6]; val spo2 = bytes[7]
                val sys  = bytes[8]; val dia  = bytes[9]
                WatchReading.OneKeyBundle(nowMs, hr, spo2, sys, dia)
            }
            // ── Sleep ────────────────────────────────────────────────────────
            CMD_SLEEP -> {
                if (bytes.size < 14) return WatchReading.Unknown(cmd, raw)
                val ts       = timestampFromBytes(bytes, 6)
                val sleepType = bytes[11]
                // duration stored as 2-byte big-endian at indices 12-13
                val durMins  = (bytes[12] shl 8) + bytes[13]
                WatchReading.Sleep(ts, sleepType, durMins)
            }
            // ── Stress ───────────────────────────────────────────────────────
            CMD_STRESS -> {
                if (bytes.size < 12) return WatchReading.Unknown(cmd, raw)
                val ts    = timestampFromBytes(bytes, 6)
                val score = bytes[11]
                if (score <= 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.Stress(ts, score)
            }
            // ── Temperature ──────────────────────────────────────────────────
            CMD_TEMPERATURE_SINGLE -> {
                if (bytes.size < 9) return WatchReading.Unknown(cmd, raw)
                val tempC = "${bytes[6]}.${bytes[7]}".toFloatOrNull() ?: return WatchReading.Unknown(cmd, raw)
                WatchReading.Temperature(nowMs, tempC)
            }
            CMD_TEMPERATURE_MEASURE -> {
                if (bytes.size < 13) return WatchReading.Unknown(cmd, raw)
                val ts    = timestampFromBytes(bytes, 6)
                val tempC = "${bytes[11]}.${bytes[12]}".toFloatOrNull() ?: return WatchReading.Unknown(cmd, raw)
                WatchReading.Temperature(ts, tempC)
            }
            // ── Immunity ─────────────────────────────────────────────────────
            CMD_IMMUNITY_SINGLE -> {
                if (bytes.size < 8) return WatchReading.Unknown(cmd, raw)
                val score = minOf(bytes[6], 100)
                if (score == 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.Immunity(nowMs, score)
            }
            CMD_IMMUNITY_MEASURE -> {
                if (bytes.size < 12) return WatchReading.Unknown(cmd, raw)
                val ts    = timestampFromBytes(bytes, 6)
                val score = minOf(bytes[11], 100)
                if (score == 0) return WatchReading.Unknown(cmd, raw)
                WatchReading.Immunity(ts, score)
            }
            // ── Battery ──────────────────────────────────────────────────────
            CMD_BATTERY -> {
                if (bytes.size < 7) return WatchReading.Unknown(cmd, raw)
                WatchReading.Battery(bytes[6])
            }
            CMD_SYNC_DONE -> WatchReading.SyncDone
            else -> WatchReading.Unknown(cmd, raw)
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /** Convert MsgPack-encoded NUS bytes to List<Int>.
     *  Handles fixarray (0x9N) of fixint values.
     *  Falls back to treating raw bytes as the integer list. */
    private fun decodeMsgPackArray(raw: ByteArray): List<Int>? {
        if (raw.isEmpty()) return null
        val first = raw[0].toInt() and 0xFF
        return when {
            // MsgPack fixarray: 0x90-0x9F → count in low nibble
            first in 0x90..0x9F -> {
                val count = first and 0x0F
                if (raw.size < count + 1) return null
                List(count) { i -> raw[i + 1].toInt() and 0xFF }
            }
            // MsgPack array16: 0xDC → next 2 bytes = count
            first == 0xDC -> {
                if (raw.size < 3) return null
                val count = ((raw[1].toInt() and 0xFF) shl 8) or (raw[2].toInt() and 0xFF)
                if (raw.size < count + 3) return null
                List(count) { i -> raw[i + 3].toInt() and 0xFF }
            }
            // Not MsgPack — treat raw bytes directly as the integer array
            else -> List(raw.size) { i -> raw[i].toInt() and 0xFF }
        }
    }

    /** bytes[offset]..bytes[offset+4] = year-2000, month, day, hour, minute */
    private fun timestampFromBytes(bytes: List<Int>, offset: Int): Long {
        val year   = bytes[offset] + 2000
        val month  = bytes[offset + 1]
        val day    = bytes[offset + 2]
        val hour   = bytes[offset + 3]
        val minute = bytes[offset + 4]
        return java.util.Calendar.getInstance().apply {
            set(year, month - 1, day, hour, minute, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
