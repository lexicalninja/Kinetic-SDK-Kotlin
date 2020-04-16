package com.lexicalninja.kinetic_sdk.smartcontrol

class PowerData {
    enum class ControlMode(private val code: Int) {
        ERG(0x00),
        Fluid(0x01),
        Resistance(0x02),
        Simulation(0x03);

        val byte: Byte
            get() = code.toByte()

        companion object {
            fun fromInt(i: Int): ControlMode {
                for (b in values()) {
                    if (b.code == i) {
                        return b
                    }
                }
                return ERG
            }
        }

    }

    var mode: ControlMode? = null
    var power = 0
    var speedKPH = 0.0
    var cadenceRPM = 0.0
    var targetResistance = 0
}