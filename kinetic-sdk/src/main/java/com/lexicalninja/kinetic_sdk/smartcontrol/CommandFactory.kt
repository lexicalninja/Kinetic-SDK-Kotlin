package com.lexicalninja.kinetic_sdk.smartcontrol

import com.lexicalninja.kinetic_sdk.exceptions.InvalidDataException
import java.util.concurrent.ThreadLocalRandom

internal object CommandFactory {
    @Throws(InvalidDataException::class) fun StartCalibrationCommandData(): ByteArray {
        val command = ByteArray(3)
        command[0] = ControlType.SpindownCalibration
        command[1] = 0x01
        command[2] = nonce()
        return hashCommand(command)
    }

    @Throws(InvalidDataException::class) fun StopCalibrationCommandData(): ByteArray {
        val command = ByteArray(3)
        command[0] = ControlType.SpindownCalibration
        command[1] = 0x00
        command[2] = nonce()
        return hashCommand(command)
    }

    @Throws(InvalidDataException::class) fun SetERGMode(target: Int): ByteArray {
        val command = ByteArray(5)
        command[0] = ControlType.SetPerformance
        command[1] = PowerData.ControlMode.ERG.byte
        command[2] = (target shr 8).toByte()
        command[3] = (target shr 0).toByte()
        command[4] = nonce()
        return hashCommand(command)
    }

    @Throws(InvalidDataException::class) fun SetFluidMode(level: Int): ByteArray {
        val command = ByteArray(4)
        command[0] = ControlType.SetPerformance
        command[1] = PowerData.ControlMode.Fluid.byte
        command[2] = Math.max(0, Math.min(9, level)).toByte()
        command[3] = nonce()
        return hashCommand(command)
    }

    @Throws(InvalidDataException::class) fun SetResistanceMode(resistance: Float): ByteArray {
        var resistance = resistance
        resistance = Math.max(0f, Math.min(1f, resistance))
        val normalized = Math.round(65535 * resistance)
        val command = ByteArray(5)
        command[0] = ControlType.SetPerformance
        command[1] = PowerData.ControlMode.Resistance.byte
        command[2] = (normalized shr 8).toByte()
        command[3] = (normalized shr 0).toByte()
        command[4] = nonce()
        return hashCommand(command)
    }

    @Throws(InvalidDataException::class) fun SetSimulationMode(weight: Float, rollingCoeff: Float, windCoeff: Float, grade: Float, windSpeed: Float): ByteArray {
        val command = ByteArray(13)
        command[0] = ControlType.SetPerformance
        command[1] = PowerData.ControlMode.Simulation.byte

        // weight is in KGs ... multiply by 100 to get 2 points of precision
        val weight100 = Math.round(Math.min(655.36, weight.toDouble()) * 100).toInt()
        command[2] = (weight100 shr 8).toByte()
        command[3] = (weight100 shr 0).toByte()

        // Rolling coeff is < 1. multiply by 10,000 to get 5 points of precision
        // coeff cannot be larger than 6.5536 otherwise it rolls over ...
        val rr10000 = Math.round(Math.min(6.5536, rollingCoeff.toDouble()) * 10000).toInt()
        command[4] = (rr10000 shr 8).toByte()
        command[5] = (rr10000 shr 0).toByte()

        // Wind coeff is typically < 1. multiply by 10,000 to get 5 points of precision
        // coeff cannot be larger than 6.5536 otherwise it rolls over ...
        val wr10000 = Math.round(Math.min(6.5536, windCoeff.toDouble()) * 10000).toInt()
        command[6] = (wr10000 shr 8).toByte()
        command[7] = (wr10000 shr 0).toByte()

        // Grade is between -45.0 and 45.0
        // Mulitply by 100 to get 2 points of precision
        val grade100 = Math.round(Math.max(-45f, Math.min(45f, grade)) * 100)
        command[8] = (grade100 shr 8).toByte()
        command[9] = (grade100 shr 0).toByte()

        // windspeed is in meters / second. convert to CM / second
        val windSpeedCM = Math.round(windSpeed * 100)
        command[10] = (windSpeedCM shr 8).toByte()
        command[11] = (windSpeedCM shr 0).toByte()
        command[12] = nonce()
        return hashCommand(command)
    }

    fun FirmwareUpdateChunk(firmware: ByteArray, position: FirmwarePosition, systemId: ByteArray?): ByteArray {
        var pos = position.position
        val payloadSize = Math.min(17, firmware.size - pos)
        val writeData = ByteArray(payloadSize + 3)
        writeData[0] = ControlType.Firmware

        // high bit indicates the start of the firmware update, bit 6 is reserved, and the low 6 bits are a packet sequence number
        writeData[1] = (if (pos == 0) 0x80 else pos / 17 and 0x3F).toByte()
        run {
            var index = 0
            while (index < payloadSize) {
                writeData[index + 2] = firmware[pos]
                index++
                pos++
            }
        }
        writeData[payloadSize + 2] = nonce()
        var hashSeed = 0x42
        if (systemId != null) {
            hashSeed = DataProcessor.hash8WithSeed(0, systemId)
        }
        var hash: Int = DataProcessor.hash8WithSeed(hashSeed, writeData[payloadSize + 2].toInt() and 0xFF)
        for (index in 0 until payloadSize + 2) {
            val temp = writeData[index]
            writeData[index] = (writeData[index].toInt() xor hash).toByte()
            hash = DataProcessor.hash8WithSeed(hash, temp.toInt() and 0xFF)
        }
        position.position = pos
        return writeData
    }

    private fun nonce(): Byte {
        return (ThreadLocalRandom.current().nextInt(0, 256) and 0xFF).toByte()
    }

    private fun hashCommand(command: ByteArray): ByteArray {
        var hash: Int = DataProcessor.hash8WithSeed(0x42, command[command.size - 1].toInt() and 0xFF)
        for (index in 0 until command.size - 1) {
            val temp = command[index]
            command[index] = (command[index].toInt() xor hash).toByte()
            hash = DataProcessor.hash8WithSeed(hash, temp.toInt() and 0xFF)
        }
        return command
    }

    internal object ControlType {
        const val SetPerformance: Byte = 0x00
        const val Firmware: Byte = 0x01
        internal const val MotorSpeed: Byte = 0x02
        internal const val SpindownCalibration: Byte = 0x03
        internal const val AntiRattle: Byte = 0x04
    }
}