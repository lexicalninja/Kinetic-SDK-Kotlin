package com.lexicalninja.kinetic_sdk

enum class FITCalibrateCoastState { UnknownState, Initializing, SpeedUp, StartCoasting, Coasting, SpeedUpDetected, Complete }
enum class FITCalibrateCoastResult { Success, TooFast, TooSlow, Middle, UnknownResult }
