package com.lexicalninja.kinetic_sdk.smartcontrol;

public class FirmwarePosition {
    private int mPosition;

    public FirmwarePosition(int position) {
        mPosition = position;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public int getPosition() {
        return mPosition;
    }
}