package com.lexicalninja.kinetic_sdk.inride

class ConfigData {
    enum class SensorUpdateRate {
        Millis1000, Millis500, Millis250
    }

    var proFlywheel:Boolean = false
    var currentSpindownTime:Double = 0.0
    var calibrationReady:Int = 0
    var calibrationStart:Int = 0
    var calibrationEnd:Int = 0
    var calibrationDebounce:Int = 0
    var updateRateDefault:Int = 0
    var updateRateCalibration:Int = 0
}