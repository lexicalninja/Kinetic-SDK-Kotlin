package com.lexicalninja.kinetic_sdk.inride

import com.lexicalninja.kinetic_sdk.exceptions.InvalidDataException
import com.lexicalninja.kinetic_sdk.inride.ConfigData.SensorUpdateRate

class CommandFactory {

    private object CalibrationParameters {
        // Ticks b/w sensor reading (32 kHz)
        const val CalibrationReady = 602
        const val CalibrationStart = 655
        const val CalibrationEnd = 950
        const val CalibrationDebounce = 327
        val UpdateIntervalDefault = SensorUpdateRate.Millis1000
        val UpdateIntervalFast = SensorUpdateRate.Millis250
    }

    private object SensorCommands {
        const val SetSpindownParams: Byte = 0x01
        const val SetName: Byte = 0x02
        const val StartCalibration: Byte = 0x03
        const val StopCalibration: Byte = 0x04
        const val SetSpindownTime: Byte = 0x05
    }

    companion object {
        @Throws(InvalidDataException::class)
        fun StartCalibrationCommandData(systemId: ByteArray): ByteArray {
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val key = CommandKeyForSystemId(systemId)
            val command = ByteArray(3)
            command[0] = key[0]
            command[1] = key[1]
            command[2] = SensorCommands.StartCalibration
            return command
        }

        @Throws(InvalidDataException::class)
        fun StopCalibrationCommandData(systemId: ByteArray): ByteArray {
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val key = CommandKeyForSystemId(systemId)
            val command = ByteArray(3)
            command[0] = key[0]
            command[1] = key[1]
            command[2] = SensorCommands.StopCalibration
            return command
        }

        @Throws(InvalidDataException::class)
        fun SetSpindownTimeCommandData(seconds: Double, systemId: ByteArray): ByteArray {
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val key = CommandKeyForSystemId(systemId)
            val command = ByteArray(7)
            command[0] = key[0]
            command[1] = key[1]
            command[2] = SensorCommands.SetSpindownTime
            val spindownTicks = (seconds * 32768).toInt()
            //        byte[] spindownBytes = BitConverter.GetBytes(spindownTicks);
            command[3] = (spindownTicks shr 0).toByte()
            command[4] = (spindownTicks shr 8).toByte()
            command[5] = (spindownTicks shr 16).toByte()
            command[6] = (spindownTicks shr 24).toByte()
            return command
        }

        @Throws(InvalidDataException::class)
        fun ConfigureSensorCommandData(updateRate: SensorUpdateRate?, systemId: ByteArray): ByteArray {
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val key = CommandKeyForSystemId(systemId)
            val command = ByteArray(15)
            command[0] = key[0]
            command[1] = key[1]
            command[2] = SensorCommands.SetSpindownParams
            command[3] = (CalibrationParameters.CalibrationReady shr 0).toByte()
            command[4] = (CalibrationParameters.CalibrationReady shr 8).toByte()
            command[5] = (CalibrationParameters.CalibrationStart shr 0).toByte()
            command[6] = (CalibrationParameters.CalibrationStart shr 8).toByte()
            command[7] = (CalibrationParameters.CalibrationEnd shr 0).toByte()
            command[8] = (CalibrationParameters.CalibrationEnd shr 8).toByte()
            command[9] = (CalibrationParameters.CalibrationDebounce shr 0).toByte()
            command[10] = (CalibrationParameters.CalibrationDebounce shr 8).toByte()
            var updateDefault = 32
            when (updateRate) {
                SensorUpdateRate.Millis1000 -> {
                    updateDefault = 32
                }
                SensorUpdateRate.Millis500 -> {
                    updateDefault = 16
                }
                SensorUpdateRate.Millis250 -> {
                    updateDefault = 8
                }
            }
            command[11] = (updateDefault shr 0).toByte()
            command[12] = (updateDefault shr 8).toByte()
            var updateFast = 8
            updateFast = when (CalibrationParameters.UpdateIntervalFast) {
                SensorUpdateRate.Millis1000 -> {
                    32
                }
                SensorUpdateRate.Millis500 -> {
                    16
                }
                SensorUpdateRate.Millis250 -> {
                    8
                }
            }
            command[13] = (updateFast shr 0).toByte()
            command[14] = (updateFast shr 8).toByte()
            return command
        }

        @Throws(InvalidDataException::class)
        fun SetPeripheralNameCommandData(name: String, systemId: ByteArray): ByteArray {
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val nameBytes = name.toByteArray()
            if (nameBytes.size < 3 || nameBytes.size > 8) {
                throw InvalidDataException("Peripheral name must be between 3 and 8 characters")
            }
            val key = CommandKeyForSystemId(systemId)
            val command = ByteArray(3 + nameBytes.size)
            command[0] = key[0]
            command[1] = key[1]
            command[2] = SensorCommands.SetName
            System.arraycopy(nameBytes, 0, command, 3, nameBytes.size)
            return command
        }

        @Throws(InvalidDataException::class)
        private fun CommandKeyForSystemId(systemId: ByteArray): ByteArray {
            if (systemId.size != 6) {
                throw InvalidDataException("Invalid System Id")
            }
            val sysidx1: Int = (systemId[3].toInt() and 0xFF) % 6
            val sysidx2: Int = (systemId[5].toInt() and 0xFF) % 6
            val commandKey = ByteArray(2)
            commandKey[0] = systemId[sysidx1]
            commandKey[1] = systemId[sysidx2]
            return commandKey
        }

    }
}