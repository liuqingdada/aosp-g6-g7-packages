// IRemoteServiceCallback.aidl
package com.mstarc.wearablephonebtadapter;

// Declare any non-default types here with import statements

interface IRemoteServiceCallback {

      void inCommingCall(String name, String number);

      void onEvent(int event);
}
