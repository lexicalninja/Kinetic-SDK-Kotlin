package com.lexicalninja.kinetic_sdk.inride

class PowerData {
    enum class SensorState {
        Normal, SpindownIdle, SpindownReady, SpindownActive
    }

    enum class SensorCalibrationResult {
        Unknown, Success, TooFast, TooSlow, Middle
    }

    enum class SensorCommandResult {
        None, Success, NotSupported, InvalidRequest, CalibrationResult, UnknownError
    }

    var timestamp = 0.0
    var state: SensorState? = null
    var power = 0
    var speedKPH = 0.0
    var rollerRPM = 0.0
    var cadenceRPM = 0.0
    var coasting = false
    var spindownTime = 0.0
    var calibrationResult: SensorCalibrationResult? = null
    var lastSpindownResultTime = 0.0
    var proFlywheel = false
    var commandResult: SensorCommandResult? = null
}