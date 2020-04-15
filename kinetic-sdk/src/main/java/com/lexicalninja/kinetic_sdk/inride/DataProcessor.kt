package com.lexicalninja.kinetic_sdk.inride

import com.lexicalninja.kinetic_sdk.exceptions.InvalidDataException
import com.lexicalninja.kinetic_sdk.inride.PowerData.SensorCalibrationResult
import kotlin.experimental.and

class DataProcessor {
    companion object {
        @Throws(InvalidDataException::class)
        fun ProcessConfigurationData(data: ByteArray): ConfigData {
            if (data.size != 20) {
                throw InvalidDataException("Invalid inRide Data")
            }
            val config =
                    ConfigData()
            var idx = 0
            config.calibrationReady = data[idx++].toInt() or data[idx++].toInt() shl 8
            config.calibrationStart = data[idx++].toInt() or data[idx++].toInt() shl 8
            config.calibrationEnd = data[idx++].toInt() or data[idx++].toInt() shl 8
            config.calibrationDebounce = data[idx++].toInt() or data[idx++].toInt() shl 8
            val currentSpindownTicks =
                    data[idx++].toInt() or (data[idx++].toInt() shl 8) or (data[idx++].toInt() shl 16) or (data[idx++].toInt() shl 24)
            config.currentSpindownTime =
                    ticksToSeconds(currentSpindownTicks.toLong())
            config.proFlywheel =
                    hasProFlywheel(config.currentSpindownTime)
            config.updateRateDefault = data[idx++].toInt() or data[idx++].toInt() shl 8
            config.updateRateCalibration = data[idx++].toInt() or data[idx++].toInt() shl 8
            return config
        }

        @Throws(InvalidDataException::class)
        fun ProcessPowerData(data: ByteArray, systemId: ByteArray): PowerData {
            if (data.size != 20) {
                throw InvalidDataException("Invalid inRide Data")
            }
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val powerData = PowerData()
            powerData.timestamp = System.currentTimeMillis() / 1000.0
            val powerBytes = translateBytes(data)
            when (powerBytes[0].toInt() and 0x30) { // stateBits
                0x10 -> {
                    powerData.state = PowerData.SensorState.SpindownIdle
                }
                0x20 -> {
                    powerData.state =
                            PowerData.SensorState.SpindownReady
                }
                0x30 -> {
                    powerData.state =
                            PowerData.SensorState.SpindownActive
                }
                else -> {
                    powerData.state = PowerData.SensorState.Normal
                }
            }
            when (powerBytes[0].toInt() and 0x0F) { //commandBits
                0x01 -> {
                    powerData.commandResult =
                            PowerData.SensorCommandResult.Success
                }
                0x02 -> {
                    powerData.commandResult =
                            PowerData.SensorCommandResult.NotSupported
                }
                0x03 -> {
                    powerData.commandResult =
                            PowerData.SensorCommandResult.InvalidRequest
                }
                0x0A -> {
                    powerData.commandResult =
                            PowerData.SensorCommandResult.CalibrationResult
                }
                0x0F -> {
                    powerData.commandResult =
                            PowerData.SensorCommandResult.UnknownError
                }
                else -> {
                    powerData.commandResult =
                            PowerData.SensorCommandResult.None
                }
            }
            var idx = 1
            val interval: Long = ((powerBytes[idx++].toInt() and 0xFF) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 8) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 16)).toLong()

            val ticks: Long = ((powerBytes[idx++].toInt() and 0xFF) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 8) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 16) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 24)).toLong()

            val revs: Long = (powerBytes[idx++].toInt() and 0xFF).toLong()

            val ticksPrevious: Long = ((powerBytes[idx++].toInt() and 0xFF) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 8) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 16) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 24)).toLong()

            val revsPrevious = powerBytes[idx++].toLong()

            val cadenceRaw: Long = ((powerBytes[idx++].toInt() and 0xFF) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 8)).toLong()

            powerData.cadenceRPM = adjustCadence(cadenceRaw, powerData.timestamp)

            val spindownTicks: Long = ((powerBytes[idx++].toInt() and 0xFF) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 8) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 16) or
                    ((powerBytes[idx++].toInt() and 0xFF) shl 24)).toLong()

            powerData.lastSpindownResultTime = ticksToSeconds(spindownTicks)

            powerData.speedKPH = speedForTicks(ticks, revs)

            powerData.rollerRPM = 0.0
            if (ticks > 0) {
                val seconds = ticksToSeconds(ticks)
                val rollerRPS = revs / seconds
                powerData.rollerRPM = rollerRPS * 60
            }
            val speedKPHPrev = speedForTicks(ticksPrevious, revsPrevious)
            powerData.proFlywheel = false
            powerData.spindownTime = (SpindownTimes.Max + SpindownTimes.Min) * 0.5
            if (powerData.lastSpindownResultTime >= SpindownTimes.Min && powerData.lastSpindownResultTime <= SpindownTimes.Max) {
                powerData.spindownTime = powerData.lastSpindownResultTime
            } else if (powerData.lastSpindownResultTime >= SpindownTimes.MinPro && powerData.lastSpindownResultTime <= SpindownTimes.MaxPro) {
                powerData.spindownTime = powerData.lastSpindownResultTime
                powerData.proFlywheel = true
            }
            val aa = alpha(interval, ticks, revs, powerData.speedKPH, ticksPrevious, revsPrevious, speedKPHPrev, powerData.proFlywheel)
            powerData.coasting = aa.coasting
            if (aa.coasting) {
                powerData.power = 0
            } else {
                powerData.power = powerForSpeed(powerData.speedKPH, powerData.spindownTime, aa.alpha, revs)
            }
            powerData.calibrationResult =
                    resultForSpindown(powerData.lastSpindownResultTime)
            return powerData
        }

        private fun resultForSpindown(time: Double): SensorCalibrationResult {
            val calibrationResult: SensorCalibrationResult = if (time >= SpindownTimes.Min && time <= SpindownTimes.Max) {
                SensorCalibrationResult.Success
            } else if (time >= SpindownTimes.MinPro && time <= SpindownTimes.MaxPro) {
                SensorCalibrationResult.Success
            } else if (time < SpindownTimes.Min) {
                SensorCalibrationResult.TooFast
            } else if (time > SpindownTimes.MaxPro) {
                SensorCalibrationResult.TooSlow
            } else {
                SensorCalibrationResult.Middle
            }
            return calibrationResult
        }

        val CADENCE_BUFFER_SIZE_MAX = 10
        val CADENCE_BUFFER_SIZE_DEFAULT = 3
        val CADENCE_BUFFER_WEIGHT_DEFAULT = 2
        private val cadenceBufferSize = CADENCE_BUFFER_SIZE_DEFAULT
        private val cadenceBufferWeight = CADENCE_BUFFER_WEIGHT_DEFAULT
        private var cadenceBufferCount = 0
        private val cadenceBuffer = arrayOfNulls<CadenceMark>(CADENCE_BUFFER_SIZE_MAX)

        private fun adjustCadence(crankRPM: Long, timestamp: Double): Double {
            if (crankRPM == 0L) {
                return 0.0
            }
            if (cadenceBufferCount > 0 && timestamp - cadenceBuffer[0]!!.timestamp > 2) {
                cadenceBufferCount = 0
            }
            val adjustedRPM = 0.8652 * crankRPM.toDouble() + 5.2617

            // shift cadence values down ...
            for (i in cadenceBufferCount downTo 1) {
                cadenceBuffer[i] =
                        cadenceBuffer[i - 1]
            }
            cadenceBuffer[0] = CadenceMark()
            cadenceBuffer[0]!!.cadenceRPM = adjustedRPM
            cadenceBuffer[0]!!.timestamp = timestamp
            cadenceBufferCount = Math.min(cadenceBufferSize, cadenceBufferCount + 1)
            var rollingRPM = adjustedRPM * cadenceBufferWeight
            for (i in 1 until cadenceBufferSize) {
                rollingRPM += cadenceBuffer[i]!!.cadenceRPM
            }
            rollingRPM /= (cadenceBufferSize + cadenceBufferWeight - 1).toDouble()
            return rollingRPM
        }

        private fun alpha(interval: Long, ticks: Long, revs: Long, speedKPH: Double, ticksPrevious: Long,
                          revsPrevious: Long, speedKPHPrevious: Double, proFlywheel: Boolean): AA {
            if (ticks > 0 && ticksPrevious > 0) {
                val tpr = ticks / revs.toDouble()
                val ptpr = ticksPrevious / revsPrevious.toDouble()
                val dtpr = tpr - ptpr
                if (dtpr > 0) {
                    // slowing down...
                    val deltaSpeed = speedKPHPrevious - speedKPH
                    val alpha = deltaSpeed * dtpr
                    // alpha is positive...
                    // TODO: this alpha value needs to be adjusted after a deep dissection of the speed curve for deceleration on the flywheel
                    if (alpha > 200 && !proFlywheel) {
                        return AA(true, alpha)
                    } else if (alpha > 20 && proFlywheel) {
                        return AA(true, alpha)
                    }
                    return AA(false, alpha)
                } else {
                    AA(false, 0.0)
                    // speeding up! (or staying the same)
                }
            }
            return AA(false, 0.0)
        }

        private fun powerForSpeed(kph: Double, spindown: Double, aa: Double, revolutions: Long): Int {
            val mph = kph * 0.621371
            val rawPower = 5.244820 * mph + 0.019168 * (mph * mph * mph)
            var dragOffset = 0.0
            dragOffset = if (spindown > 0 && rawPower > 0) {
                val proFlywheel = hasProFlywheel(spindown)

                val spindownTimeMS = spindown * 1000

                val dragOffsetSlope = if (proFlywheel) PowerConstants.DragSlopeOffsetPro
                else PowerConstants.DragSlopeOffset

                val dragOffsetPowerSlope = if (proFlywheel) PowerConstants.PowerSlopeOffsetPro
                else PowerConstants.PowerSlopeOffset

                val yIntercept = if (proFlywheel) PowerConstants.YInterceptPro
                else PowerConstants.YIntercept

                dragOffsetPowerSlope * spindownTimeMS * rawPower * 0.00001 + dragOffsetSlope * spindownTimeMS + yIntercept
            } else {
                0.0
            }
            // double alphaOffset = 0.05 * PowerConstants.MomentOfIntertia * aa;
            var power = rawPower + dragOffset
            if (power < 0) {
                power = 0.0
            }
            return power.toInt()
        }

        private fun ticksToSeconds(ticks: Long): Double {
            return ticks / 32768.0
        }

        private fun hasProFlywheel(spindown: Double): Boolean {
            return spindown >= SpindownTimes.MinPro && spindown <= SpindownTimes.MaxPro
        }

        private fun speedForTicks(ticks: Long, revs: Long): Double {
            return if (ticks == 0L || revs == 0L) {
                0.0
            } else 19974.826517 * revs.toDouble() / ticks.toDouble()
        }

        private fun translateBytes(data: ByteArray): ByteArray {
            val translated = data.clone()
            val rotate: Int = (data[0].toInt() and 0xC0) shr 6
            val indicesArray = arrayOf(
                    byteArrayOf(14, 15, 12, 16, 11, 5, 17, 3, 2, 1, 19, 13, 6, 4, 8, 9, 10, 18, 7),
                    byteArrayOf(12, 14, 8, 11, 16, 4, 7, 13, 18, 1, 3, 19, 6, 15, 9, 5, 10, 17, 2),
                    byteArrayOf(11, 5, 1, 9, 4, 18, 7, 15, 6, 2, 10, 12, 16, 3, 14, 13, 19, 17, 8),
                    byteArrayOf(13, 5, 18, 1, 3, 12, 15, 10, 14, 19, 16, 8, 6, 11, 2, 9, 4, 17, 7)
            )
            var xorIdx1 = rotate + 1
            xorIdx1 %= 4
            var xorIdx2 = xorIdx1 + 1
            xorIdx2 %= 4
            for (index in 1..19) {
                translated[index] = (translated[index].toInt() xor
                        (indicesArray[xorIdx1][index - 1] + indicesArray[xorIdx2][index - 1]))
                        .toByte()
            }
            val reordered = translated.clone()
            for (index in 0..18) {
                reordered[index + 1] = translated[indicesArray[rotate][index].toInt()]
            }
            return reordered
        }

        private object SpindownTimes {
            const val Min = 1.5
            const val MinPro = 4.7
            const val Max = 2.0
            const val MaxPro = 6.5
        }

        private class AA(var coasting: Boolean, var alpha: Double)

        private class CadenceMark {
            var timestamp = 0.0
            var cadenceRPM = 0.0
        }

        // second conversion 0.9313225746
        private object PowerConstants {
            private const val MomentOfIntertia = 4.0

            // drop 20 watts @ 300 power
            const val PowerSlopeOffset =
                    4.55 // 9.0516435765      // 8.43     <-- adjusted for spindown timing fix
            const val PowerSlopeOffsetPro =
                    2.62 // 2.44     <-- adjusted for spindown timing fix
            const val DragSlopeOffset =
                    -0.1425 // -0.1398  <-- adjusted for spindown timing fix
            const val DragSlopeOffsetPro =
                    -0.021 // -0.0201  <-- adjusted for spindown timing fix
            const val YIntercept = 236.20
            const val YInterceptPro = 104.97
        }
    }
}