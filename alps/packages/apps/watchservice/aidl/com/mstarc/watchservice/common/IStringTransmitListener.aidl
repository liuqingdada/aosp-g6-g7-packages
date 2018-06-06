// IByteTransmitListener.aidl
package com.mstarc.watchservice.common;

// Declare any non-default types here with import statements

interface IStringTransmitListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onReadData(String json);
}
