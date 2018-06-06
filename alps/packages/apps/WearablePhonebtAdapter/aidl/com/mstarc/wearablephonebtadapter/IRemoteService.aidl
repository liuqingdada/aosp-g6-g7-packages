// IRemoteService.aidl
package com.mstarc.wearablephonebtadapter;
import com.mstarc.wearablephonebtadapter.IRemoteServiceCallback;

// Declare any non-default types here with import statements

interface IRemoteService {

    void registerCallback(IRemoteServiceCallback cb);

    void unregisterCallback(IRemoteServiceCallback cb);

    void doWatchCallByPhone();

    void doWatchReceiveCall();

    void doWatchRejectCall();

    boolean isAdvertiserConnect();

}
