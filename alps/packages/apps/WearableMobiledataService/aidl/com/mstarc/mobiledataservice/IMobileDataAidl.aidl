// IMobileDataAidl.aidl
package com.mstarc.mobiledataservice;

// Declare any non-default types here with import statements

interface IMobileDataAidl {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void setMobileDataState(boolean open);

    boolean getMobileDataState();

    void reboot();
    void shutDown();
    void restoreFactory();
}
