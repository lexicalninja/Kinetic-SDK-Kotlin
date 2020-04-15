package com.lexicalninja.kinetic_sdk.inride

import com.lexicalninja.kinetic_sdk.exceptions.InvalidDataException
import com.lexicalninja.kinetic_sdk.inride.ConfigData.SensorUpdateRate

object InRide {

    object DeviceInformation {
        const val UUID = "0000180a-0000-1000-8000-00805f9b34fb"

        object Characteristics {
            const val SYSTEM_ID_UUID = "00002a23-0000-1000-8000-00805f9b34fb"
        }
    }

    object PowerService {
        const val UUID = "E9410100-B434-446B-B5CC-36592FC4C724"

        object Characteristics {
            const val POWER_UUID = "E9410101-B434-446B-B5CC-36592FC4C724"
            const val CONFIG_UUID = "E9410104-B434-446B-B5CC-36592FC4C724"
            const val CONTROL_POINT_UUID = "E9410102-B434-446B-B5CC-36592FC4C724"
        }
    }

    @Throws(InvalidDataException::class) fun ProcessConfigurationData(data: ByteArray): ConfigData {
        return DataProcessor.ProcessConfigurationData(data)
    }

    @Throws(InvalidDataException::class) fun ProcessPowerData(data: ByteArray, systemId: ByteArray): PowerData {
        return DataProcessor.ProcessPowerData(data, systemId)
    }

    @Throws(InvalidDataException::class) fun StartCalibrationCommandData(systemId: ByteArray): ByteArray {
        return CommandFactory.StartCalibrationCommandData(systemId)
    }

    @Throws(InvalidDataException::class) fun StopCalibrationCommandData(systemId: ByteArray): ByteArray {
        return CommandFactory.StopCalibrationCommandData(systemId)
    }

    @Throws(InvalidDataException::class) fun SetSpindownTimeCommandData(seconds: Double, systemId: ByteArray): ByteArray {
        return CommandFactory.SetSpindownTimeCommandData(seconds, systemId)
    }

    @Throws(InvalidDataException::class) fun ConfigureSensorCommandData(updateRate: SensorUpdateRate, systemId: ByteArray): ByteArray {
        return CommandFactory.ConfigureSensorCommandData(updateRate, systemId)
    }

    @Throws(InvalidDataException::class) fun SetPeripheralNameCommandData(name: String, systemId: ByteArray): ByteArray {
        return CommandFactory.SetPeripheralNameCommandData(name, systemId)
    }
}