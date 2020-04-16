package com.lexicalninja.kinetic_sdk.smartcontrol

class ConfigData {
    enum class CalibrationState(private val code: Int) {
        NotPerformed(0),
        Initializing(1),
        SpeedUp(2),
        StartCoasting(3),
        Coasting(4),
        SpeedUpDetected(5),
        Complete(10);

        companion object {
            fun fromInt(i: Int): CalibrationState {
                for (b in values()) {
                    if (b.code == i) {
                        return b
                    }
                }
                return NotPerformed
            }
        }

    }

    var calibrationState: CalibrationState? = null
    var spindownTime = 0.0
}