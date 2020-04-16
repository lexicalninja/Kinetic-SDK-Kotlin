package com.lexicalninja.kinetic_sdk.smartcontrol

import com.lexicalninja.kinetic_sdk.exceptions.InvalidDataException

class SmartControl {
    object DeviceInformation {
        const val UUID = "0000180a-0000-1000-8000-00805f9b34fb"

        object Characteristics {
            const val SYSTEM_ID_UUID = "00002a23-0000-1000-8000-00805f9b34fb"
            const val FIRMWARE_REVISION_STRING_UUID = "00002a26-0000-1000-8000-00805f9b34fb"
        }
    }

    object PowerService {
        const val UUID = "E9410200-B434-446B-B5CC-36592FC4C724"

        object Characteristics {
            const val POWER_UUID = "E9410201-B434-446B-B5CC-36592FC4C724"
            const val CONFIG_UUID = "E9410202-B434-446B-B5CC-36592FC4C724"
            const val CONTROL_POINT_UUID = "E9410203-B434-446B-B5CC-36592FC4C724"
        }
    }

    companion object {
        @Throws(InvalidDataException::class)
        fun ProcessPowerData(data: ByteArray, systemId: ByteArray): PowerData {
            return DataProcessor.ProcessPowerData(data, systemId)
        }

        @Throws(InvalidDataException::class)
        fun ProcessConfigurationData(data: ByteArray): ConfigData {
            return DataProcessor.ProcessConfigurationData(data)
        }

        @Throws(InvalidDataException::class)
        fun StartCalibrationCommandData(): ByteArray {
            return CommandFactory.StartCalibrationCommandData()
        }

        @Throws(InvalidDataException::class)
        fun StopCalibrationCommandData(): ByteArray {
            return CommandFactory.StopCalibrationCommandData()
        }

        @Throws(InvalidDataException::class)
        fun SetERGMode(target: Int): ByteArray {
            return CommandFactory.SetERGMode(target)
        }

        @Throws(InvalidDataException::class)
        fun SetFluidMode(level: Int): ByteArray {
            return CommandFactory.SetFluidMode(level)
        }

        @Throws(InvalidDataException::class)
        fun SetResistanceMode(resistance: Float): ByteArray {
            return CommandFactory.SetResistanceMode(resistance)
        }

        @Throws(InvalidDataException::class)
        fun SetSimulationMode(weight: Float, rollingCoeff: Float, windCoeff: Float, grade: Float, windSpeed: Float): ByteArray {
            return CommandFactory.SetSimulationMode(weight, rollingCoeff, windCoeff, grade, windSpeed)
        }

        fun FirmwareUpdateChunk(firmwareData: ByteArray, position: FirmwarePosition, systemId: ByteArray): ByteArray {
            return CommandFactory.FirmwareUpdateChunk(firmwareData, position, systemId)
        }
    }
}