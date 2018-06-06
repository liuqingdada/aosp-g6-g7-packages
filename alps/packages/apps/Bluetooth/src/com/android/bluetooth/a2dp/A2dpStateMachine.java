/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Bluetooth A2dp StateMachine
 *                      (Disconnected)
 *                           |    ^
 *                   CONNECT |    | DISCONNECTED
 *                           V    |
 *                         (Pending)
 *                           |    ^
 *                 CONNECTED |    | CONNECT
 *                           V    |
 *                        (Connected)
 */
package com.android.bluetooth.a2dp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetooth;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.Intent;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ParcelUuid;
import android.util.Log;
import com.android.bluetooth.Utils;
import com.android.bluetooth.btservice.AdapterService;
import com.android.bluetooth.btservice.ProfileService;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.os.SystemClock;

final class A2dpStateMachine extends StateMachine {
    private static final String TAG = "[BT][A2DP]A2dpStateMachine";
    private static final boolean DBG = true;

    private static final String ROLE_CHANGE_ACTION = "android.bluetooth.a2dp.role_change";
	private static final String ROLE_CHANGE_CANCEL_ACTION = "android.bluetooth.a2dp.role_change_canceled";
    private static final String REQUEST_ROLE = "request_role";
    private static final String DEVICE_ADDRESS = "device_address_to_connect";
    
    private static final String REQUEST_ROLE_SOURCE = "source";
    
    static final int CONNECT = 1;
    static final int DISCONNECT = 2;
    private static final int STACK_EVENT = 111;
	private static final int REQUEST_CONFIRM = 112;
    private static final int CONNECT_TIMEOUT = 201;
    
    // accept or reject the remote device connection request
    private static final int CONFIRM_REQUEST_ACCEPT = 1;
    private static final int CONFRIM_REQUEST_REJECT = 0;

    private Disconnected mDisconnected;
    private Pending mPending;
    private Connected mConnected;

    private A2dpService mService;
    private Context mContext;
    private BluetoothAdapter mAdapter;
    private final AudioManager mAudioManager;
    private IntentBroadcastHandler mIntentBroadcastHandler;
    private final WakeLock mWakeLock;

    private static final int MSG_CONNECTION_STATE_CHANGED = 0;

    // mCurrentDevice is the device connected before the state changes
    // mTargetDevice is the device to be connected
    // mIncomingDevice is the device connecting to us, valid only in Pending state
    //                when mIncomingDevice is not null, both mCurrentDevice
    //                  and mTargetDevice are null
    //                when either mCurrentDevice or mTargetDevice is not null,
    //                  mIncomingDevice is null
    // Stable states
    //   No connection, Disconnected state
    //                  both mCurrentDevice and mTargetDevice are null
    //   Connected, Connected state
    //              mCurrentDevice is not null, mTargetDevice is null
    // Interim states
    //   Connecting to a device, Pending
    //                           mCurrentDevice is null, mTargetDevice is not null
    //   Disconnecting device, Connecting to new device
    //     Pending
    //     Both mCurrentDevice and mTargetDevice are not null
    //   Disconnecting device Pending
    //                        mCurrentDevice is not null, mTargetDevice is null
    //   Incoming connections Pending
    //                        Both mCurrentDevice and mTargetDevice are null
    private BluetoothDevice mCurrentDevice = null;
    private BluetoothDevice mTargetDevice = null;
    private BluetoothDevice mIncomingDevice = null;
    private BluetoothDevice mPlayingA2dpDevice = null;

    private BluetoothDevice mRequestDevice = null;

    private long mBroadcastConnectionTime = 0;

    static {
        classInitNative();
    }

    private A2dpStateMachine(A2dpService svc, Context context) {
        super(TAG);
        mService = svc;
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();

        initNative();

        if (DBG)
        {
            setDbg(true);
        }

        mDisconnected = new Disconnected();
        mPending = new Pending();
        mConnected = new Connected();

        addState(mDisconnected);
        addState(mPending);
        addState(mConnected);

        setInitialState(mDisconnected);

        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluetoothA2dpService");

        mIntentBroadcastHandler = new IntentBroadcastHandler();

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.bluetooth.a2dp_source.connect_confirm");
        mContext.registerReceiver(mReceiver, filter);

    }

    static A2dpStateMachine make(A2dpService svc, Context context) {
        Log.d(TAG, "make enter");
        A2dpStateMachine a2dpSm = new A2dpStateMachine(svc, context);
        a2dpSm.start();
        return a2dpSm;
    }

    public void doQuit() {
        log("doQuit");
        mContext.unregisterReceiver(mReceiver);
        quitNow();
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "[onReceive] action : " + action);
            if (action.equals("android.bluetooth.a2dp_source.connect_confirm")) {
                int n = intent.getIntExtra("confirmed", -1);
                String address = intent.getStringExtra("address");
                Log.d(TAG, "[onReceive] n : " + n + ", address : " + address);
                if (n != -1) {
//                    confirmRequestNative(getByteAddress(address), n);

                    mRequestDevice = null;

                    BluetoothDevice device = mAdapter.getRemoteDevice(address);
                    RequestConfirmInformation infor = new RequestConfirmInformation();
                    infor.device = device;
                    infor.acceptOrReject = n;
                    A2dpStateMachine.this.sendMessage(REQUEST_CONFIRM, infor);
                }
            }
        }
        
    };

    public void cleanup() {
        Log.d(TAG, "[cleanup] enter");
        cleanupNative();
    }

        private class Disconnected extends State {
        @Override
        public void enter() {
            log("Enter Disconnected: " + getCurrentMessage().what);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Disconnected process message: " + message.what);
//            log("Disconnected process message: stack " +  
//                Log.getStackTraceString(new Throwable()));
            if (mCurrentDevice != null || mTargetDevice != null  || mIncomingDevice != null) {
                loge("ERROR: current, target, or mIncomingDevice not null in Disconnected");
                return NOT_HANDLED;
            }

            boolean retValue = HANDLED;
            switch(message.what) {
                case CONNECT:
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    broadcastConnectionState(device, BluetoothProfile.STATE_CONNECTING,
                                   BluetoothProfile.STATE_DISCONNECTED);

                    if (!connectA2dpNative(getByteAddress(device)) ) {
                        broadcastConnectionState(device, BluetoothProfile.STATE_DISCONNECTED,
                                       BluetoothProfile.STATE_CONNECTING);
                        break;
                    }

                    synchronized (A2dpStateMachine.this) {
                        mTargetDevice = device;
                        transitionTo(mPending);
                    }
                    // TODO(BT) remove CONNECT_TIMEOUT when the stack
                    //          sends back events consistently
                    sendMessageDelayed(CONNECT_TIMEOUT, 30000);
                    break;
                case DISCONNECT:
                    // ignore
                    break;
                case STACK_EVENT:
                    StackEvent event = (StackEvent) message.obj;
                    switch (event.type) {
                        case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt, event.device);
                            break;
                        default:
                            loge("Unexpected stack event: " + event.type);
                            break;
                    }
                    break;

                case REQUEST_CONFIRM:
                    RequestConfirmInformation info = (RequestConfirmInformation)message.obj;
                    Log.d(TAG, "[Disconnected][processMessage] info.device : " + info.device 
                               + ", info.acceptOrReject : " + info.acceptOrReject);
                    if (!confirmRequestNative(getByteAddress(info.device), info.acceptOrReject)) {
                        Log.d(TAG, "[Disconnected][processMessage] confirmRequestNative Failed");
                        if (info.acceptOrReject == CONFIRM_REQUEST_ACCEPT) {
                            A2dpStateMachine.this.sendMessage(CONNECT, info.device);
                        } else if (info.acceptOrReject == CONFRIM_REQUEST_REJECT) {
                            broadcastConnectionState(info.device, BluetoothProfile.STATE_DISCONNECTED,
                                       BluetoothProfile.STATE_CONNECTING);
                        }
                        break;
                    }
                    Log.d(TAG, "[Disconnected][processMessage] confirmRequestNative success");
                    if (info.acceptOrReject == CONFIRM_REQUEST_ACCEPT) {
                        broadcastConnectionState(info.device, BluetoothProfile.STATE_CONNECTING,
                                  BluetoothProfile.STATE_DISCONNECTED);

                        synchronized (A2dpStateMachine.this) {
                            mTargetDevice = info.device;
                            transitionTo(mPending);
                        }
                    } else if (info.acceptOrReject == CONFRIM_REQUEST_REJECT) {
                        broadcastConnectionState(info.device, BluetoothProfile.STATE_DISCONNECTED,
                                  BluetoothProfile.STATE_CONNECTING);
                    }
                    break;

                default:
                    return NOT_HANDLED;
            }
            return retValue;
        }

        @Override
        public void exit() {
            log("Exit Disconnected: " + getCurrentMessage().what);
        }

        // in Disconnected state
        private void processConnectionEvent(int state, BluetoothDevice device) {
            Log.d(TAG, "[Disconnected][processConnectionEvent] state : " + state
                    + ", device : " + device);
            switch (state) {
            case CONNECTION_STATE_DISCONNECTED:
                logw("Ignore HF DISCONNECTED event, device: " + device);
				if (mRequestDevice != null && device.equals(mRequestDevice)) {
                    log("[Disconnected][processConnectionEvent] request device equal to disconnected device");
                    Intent intent = new Intent();
                    intent.setAction(ROLE_CHANGE_CANCEL_ACTION);
                    intent.putExtra(DEVICE_ADDRESS, device.getAddress());
                    intent.putExtra(REQUEST_ROLE, REQUEST_ROLE_SOURCE);
                    mContext.sendBroadcast(intent);
                }
                break;
            case CONNECTION_STATE_CONNECTING:
                if (okToConnect(device)){
                    logi("Incoming A2DP accepted");
                    broadcastConnectionState(device, BluetoothProfile.STATE_CONNECTING,
                                             BluetoothProfile.STATE_DISCONNECTED);
                    synchronized (A2dpStateMachine.this) {
                        mIncomingDevice = device;
                        transitionTo(mPending);
                    }
                } else {
                    //reject the connection and stay in Disconnected state itself
                    logi("Incoming A2DP rejected");
                    disconnectA2dpNative(getByteAddress(device));
                    // the other profile connection should be initiated
                    AdapterService adapterService = AdapterService.getAdapterService();
                    if (adapterService != null) {
                        adapterService.connectOtherProfile(device,
                                                           AdapterService.PROFILE_CONN_REJECTED);
                    }
                }
                break;
            case CONNECTION_STATE_CONNECTED:
                logw("A2DP Connected from Disconnected state");
                if (okToConnect(device)){
                    logi("Incoming A2DP accepted");
                    broadcastConnectionState(device, BluetoothProfile.STATE_CONNECTED,
                                             BluetoothProfile.STATE_DISCONNECTED);
                    synchronized (A2dpStateMachine.this) {
                        mCurrentDevice = device;
                        transitionTo(mConnected);
                    }
                } else {
                    //reject the connection and stay in Disconnected state itself
                    logi("Incoming A2DP rejected");
                    disconnectA2dpNative(getByteAddress(device));
                    // the other profile connection should be initiated
                    AdapterService adapterService = AdapterService.getAdapterService();
                    if (adapterService != null) {
                        adapterService.connectOtherProfile(device,
                                                           AdapterService.PROFILE_CONN_REJECTED);
                    }
                }
                break;
            case CONNECTION_STATE_DISCONNECTING:
                logw("Ignore HF DISCONNECTING event, device: " + device);
                break;
                
            case SOURCE_CONNECT_REQUEST:
                logi("SOURCE_CONNECT_REQUEST device : " + device);

                mRequestDevice = device;

                Intent intent = new Intent();
                intent.setAction(ROLE_CHANGE_ACTION);
                intent.putExtra(DEVICE_ADDRESS, device.getAddress());
                intent.putExtra(REQUEST_ROLE, REQUEST_ROLE_SOURCE);
                mContext.sendBroadcast(intent);
                break;
                
            default:
                loge("Incorrect state: " + state);
                break;
            }
        }
    }

    private class Pending extends State {
        @Override
        public void enter() {
            log("Enter Pending: " + getCurrentMessage().what);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Pending process message: " + message.what);

            boolean retValue = HANDLED;
            switch(message.what) {
                case CONNECT:
                    deferMessage(message);
                    break;
                case CONNECT_TIMEOUT:
                    onConnectionStateChanged(CONNECTION_STATE_DISCONNECTED,
                                             getByteAddress(mTargetDevice));
                    break;
                case DISCONNECT:
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    if (mCurrentDevice != null && mTargetDevice != null &&
                        mTargetDevice.equals(device) ) {
                        // cancel connection to the mTargetDevice
                        broadcastConnectionState(device, BluetoothProfile.STATE_DISCONNECTED,
                                       BluetoothProfile.STATE_CONNECTING);
                        synchronized (A2dpStateMachine.this) {
                            mTargetDevice = null;
                        }
                    } else {
                        deferMessage(message);
                    }
                    break;
                case STACK_EVENT:
                    StackEvent event = (StackEvent) message.obj;
                    switch (event.type) {
                        case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            removeMessages(CONNECT_TIMEOUT);
                            processConnectionEvent(event.valueInt, event.device);
                            break;
                        default:
                            loge("Unexpected stack event: " + event.type);
                            break;
                    }
                    break;

                case REQUEST_CONFIRM:
                    log("Ignore the request confirm in pending state");
					break;

                default:
                    return NOT_HANDLED;
            }
            return retValue;
        }

        // in Pending state
        private void processConnectionEvent(int state, BluetoothDevice device) {
            switch (state) {
                case CONNECTION_STATE_DISCONNECTED:
                    if ((mCurrentDevice != null) && mCurrentDevice.equals(device)) {
                        broadcastConnectionState(mCurrentDevice,
                                                 BluetoothProfile.STATE_DISCONNECTED,
                                                 BluetoothProfile.STATE_DISCONNECTING);
                        synchronized (A2dpStateMachine.this) {
                            mCurrentDevice = null;
                        }

                        if (mTargetDevice != null) {
                            if (!connectA2dpNative(getByteAddress(mTargetDevice))) {
                                broadcastConnectionState(mTargetDevice,
                                                         BluetoothProfile.STATE_DISCONNECTED,
                                                         BluetoothProfile.STATE_CONNECTING);
                                synchronized (A2dpStateMachine.this) {
                                    mTargetDevice = null;
                                    transitionTo(mDisconnected);
                                }
                            }
                        } else {
                            synchronized (A2dpStateMachine.this) {
                                mIncomingDevice = null;
                                transitionTo(mDisconnected);
                            }
                        }
                    } else if (mTargetDevice != null && mTargetDevice.equals(device)) {
                        // outgoing connection failed
                        broadcastConnectionState(mTargetDevice, BluetoothProfile.STATE_DISCONNECTED,
                                                 BluetoothProfile.STATE_CONNECTING);
                        synchronized (A2dpStateMachine.this) {
                            mTargetDevice = null;
                            transitionTo(mDisconnected);
                        }
                    } else if (mIncomingDevice != null && mIncomingDevice.equals(device)) {
                        broadcastConnectionState(mIncomingDevice,
                                                 BluetoothProfile.STATE_DISCONNECTED,
                                                 BluetoothProfile.STATE_CONNECTING);
                        synchronized (A2dpStateMachine.this) {
                            mIncomingDevice = null;
                            transitionTo(mDisconnected);
                        }
                    } else {
                        loge("Unknown device Disconnected: " + device);
                    }
                    break;
            case CONNECTION_STATE_CONNECTED:
                if ((mCurrentDevice != null) && mCurrentDevice.equals(device)) {
                    // disconnection failed
                    broadcastConnectionState(mCurrentDevice, BluetoothProfile.STATE_CONNECTED,
                                             BluetoothProfile.STATE_DISCONNECTING);
                    if (mTargetDevice != null) {
                        broadcastConnectionState(mTargetDevice, BluetoothProfile.STATE_DISCONNECTED,
                                                 BluetoothProfile.STATE_CONNECTING);
                    }
                    synchronized (A2dpStateMachine.this) {
                        mTargetDevice = null;
                        transitionTo(mConnected);
                    }
                } else if (mTargetDevice != null && mTargetDevice.equals(device)) {
                    broadcastConnectionState(mTargetDevice, BluetoothProfile.STATE_CONNECTED,
                                             BluetoothProfile.STATE_CONNECTING);
                    synchronized (A2dpStateMachine.this) {
                        mCurrentDevice = mTargetDevice;
                        mTargetDevice = null;
                        transitionTo(mConnected);
                    }
                } else if (mIncomingDevice != null && mIncomingDevice.equals(device)) {
                    broadcastConnectionState(mIncomingDevice, BluetoothProfile.STATE_CONNECTED,
                                             BluetoothProfile.STATE_CONNECTING);
                    synchronized (A2dpStateMachine.this) {
                        mCurrentDevice = mIncomingDevice;
                        mIncomingDevice = null;
                        transitionTo(mConnected);
                    }
                } else {
                    loge("Unknown device Connected: " + device);
                    // something is wrong here, but sync our state with stack
                    broadcastConnectionState(device, BluetoothProfile.STATE_CONNECTED,
                                             BluetoothProfile.STATE_DISCONNECTED);
                    synchronized (A2dpStateMachine.this) {
                        mCurrentDevice = device;
                        mTargetDevice = null;
                        mIncomingDevice = null;
                        transitionTo(mConnected);
                    }
                }
                break;
            case CONNECTION_STATE_CONNECTING:
                if ((mCurrentDevice != null) && mCurrentDevice.equals(device)) {
                    log("current device tries to connect back");
                    // TODO(BT) ignore or reject
                } else if (mTargetDevice != null && mTargetDevice.equals(device)) {
                    // The stack is connecting to target device or
                    // there is an incoming connection from the target device at the same time
                    // we already broadcasted the intent, doing nothing here
                    log("Stack and target device are connecting");
                }
                else if (mIncomingDevice != null && mIncomingDevice.equals(device)) {
                    loge("Another connecting event on the incoming device");
                } else {
                    // We get an incoming connecting request while Pending
                    // TODO(BT) is stack handing this case? let's ignore it for now
                    log("Incoming connection while pending, ignore");
                }
                break;
            case CONNECTION_STATE_DISCONNECTING:
                if ((mCurrentDevice != null) && mCurrentDevice.equals(device)) {
                    // we already broadcasted the intent, doing nothing here
                    if (DBG) {
                        log("stack is disconnecting mCurrentDevice");
                    }
                } else if (mTargetDevice != null && mTargetDevice.equals(device)) {
                    loge("TargetDevice is getting disconnected");
                } else if (mIncomingDevice != null && mIncomingDevice.equals(device)) {
                    loge("IncomingDevice is getting disconnected");
                } else {
                    loge("Disconnecting unknow device: " + device);
                }
                break;
                
            case SOURCE_CONNECT_REQUEST:
                logi("Source Connect Request While Pending, ignore it");
                break;
                
            default:
                loge("Incorrect state: " + state);
                break;
            }
        }

    }

    private class Connected extends State {
        @Override
        public void enter() {
            log("Enter Connected: " + getCurrentMessage().what);
            // Upon connected, the audio starts out as stopped
            broadcastAudioState(mCurrentDevice, BluetoothA2dp.STATE_NOT_PLAYING,
                                BluetoothA2dp.STATE_PLAYING);
        }

        @Override
        public void exit() {
            log("Exit Connected: " + getCurrentMessage().what);
        }

        @Override
        public boolean processMessage(Message message) {
            log("Connected process message: " + message.what);
            if (mCurrentDevice == null) {
                loge("ERROR: mCurrentDevice is null in Connected");
                return NOT_HANDLED;
            }

            boolean retValue = HANDLED;
            switch(message.what) {
                case CONNECT:
                {
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    if (mCurrentDevice.equals(device)) {
                        break;
                    }

                    broadcastConnectionState(device, BluetoothProfile.STATE_CONNECTING,
                                   BluetoothProfile.STATE_DISCONNECTED);
                    log("Connected process:  connect new device after disconnect currentdevice: ");
                    broadcastConnectionState(mCurrentDevice, BluetoothProfile.STATE_DISCONNECTING,
                                   BluetoothProfile.STATE_CONNECTED);
                    if (!disconnectA2dpNative(getByteAddress(mCurrentDevice))) {
                        broadcastConnectionState(device, BluetoothProfile.STATE_DISCONNECTED,
                                       BluetoothProfile.STATE_CONNECTING);
                        break;
                    }

                    synchronized (A2dpStateMachine.this) {
                        mTargetDevice = device;
                        transitionTo(mPending);
                    }
                }
                    break;
                case DISCONNECT:
                {
                    BluetoothDevice device = (BluetoothDevice) message.obj;
                    if (!mCurrentDevice.equals(device)) {
                        break;
                    }
                    broadcastConnectionState(device, BluetoothProfile.STATE_DISCONNECTING,
                                   BluetoothProfile.STATE_CONNECTED);
                    if (!disconnectA2dpNative(getByteAddress(device))) {
                        broadcastConnectionState(device, BluetoothProfile.STATE_CONNECTED,
                                       BluetoothProfile.STATE_DISCONNECTED);
                        break;
                    }
                    transitionTo(mPending);
                }
                    break;
                case STACK_EVENT:
                    StackEvent event = (StackEvent) message.obj;
                    switch (event.type) {
                        case EVENT_TYPE_CONNECTION_STATE_CHANGED:
                            processConnectionEvent(event.valueInt, event.device);
                            break;
                        case EVENT_TYPE_AUDIO_STATE_CHANGED:
                            processAudioStateEvent(event.valueInt, event.device);
                            break;
                        default:
                            loge("Unexpected stack event: " + event.type);
                            break;
                    }
                    break;

                case REQUEST_CONFIRM:
                    log("Ignore the request confirm in connected state");
					break;

                default:
                    return NOT_HANDLED;
            }
            return retValue;
        }

        // in Connected state
        private void processConnectionEvent(int state, BluetoothDevice device) {
            Log.d(TAG, "[Connected][processConnectionEvent] state : " + state
                    + ", device : " + device);
            switch (state) {
                case CONNECTION_STATE_DISCONNECTED:
                    if (mCurrentDevice.equals(device)) {
                        broadcastConnectionState(mCurrentDevice, BluetoothProfile.STATE_DISCONNECTED,
                                                 BluetoothProfile.STATE_CONNECTED);
                        synchronized (A2dpStateMachine.this) {
                            mCurrentDevice = null;
                            transitionTo(mDisconnected);
                        }
                    } else {
                        loge("Disconnected from unknown device: " + device);
                    }
                    break;
                    
                case SOURCE_CONNECT_REQUEST:
                    logi("Source Connect Request While Connected, ignore it");
                    break;
                    
              default:
                  loge("Connection State Device: " + device + " bad state: " + state);
                  break;
            }
        }
        private void processAudioStateEvent(int state, BluetoothDevice device) {
            if (!mCurrentDevice.equals(device)) {
                loge("Audio State Device:" + device + "is different from ConnectedDevice:" +
                                                           mCurrentDevice);
                return;
            }
            switch (state) {
                case AUDIO_STATE_STARTED:
                    if (mPlayingA2dpDevice == null) {
                       mPlayingA2dpDevice = device;
                        log("processAudioStateEvent: setAvrcpAudioState: PLAYING ");
                        mService.setAvrcpAudioState(BluetoothA2dp.STATE_PLAYING);
                       broadcastAudioState(device, BluetoothA2dp.STATE_PLAYING,
                                           BluetoothA2dp.STATE_NOT_PLAYING);
                    }
                    break;
                case AUDIO_STATE_REMOTE_SUSPEND:
                case AUDIO_STATE_STOPPED:
                    if(mPlayingA2dpDevice != null) {
                        mPlayingA2dpDevice = null;
                        log("processAudioStateEvent: setAvrcpAudioState: NOT_PLAYING ");
                        mService.setAvrcpAudioState(BluetoothA2dp.STATE_NOT_PLAYING);
                        broadcastAudioState(device, BluetoothA2dp.STATE_NOT_PLAYING,
                                            BluetoothA2dp.STATE_PLAYING);
                    }
                    break;
                default:
                  loge("Audio State Device: " + device + " bad state: " + state);
                  break;
            }
        }
    }

    int getConnectionState(BluetoothDevice device) {
        if (getCurrentState() == mDisconnected) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }

        synchronized (this) {
            IState currentState = getCurrentState();
            if (currentState == mPending) {
                if ((mTargetDevice != null) && mTargetDevice.equals(device)) {
                    return BluetoothProfile.STATE_CONNECTING;
                }
                if ((mCurrentDevice != null) && mCurrentDevice.equals(device)) {
                    return BluetoothProfile.STATE_DISCONNECTING;
                }
                if ((mIncomingDevice != null) && mIncomingDevice.equals(device)) {
                    return BluetoothProfile.STATE_CONNECTING; // incoming connection
                }
                return BluetoothProfile.STATE_DISCONNECTED;
            }

            if (currentState == mConnected) {
                if (mCurrentDevice != null && mCurrentDevice.equals(device)) {
                    return BluetoothProfile.STATE_CONNECTED;
                }
                return BluetoothProfile.STATE_DISCONNECTED;
            } else {
                loge("Bad currentState: " + currentState);
                return BluetoothProfile.STATE_DISCONNECTED;
            }
        }
    }

    List<BluetoothDevice> getConnectedDevices() {
        List<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
        synchronized(this) {
            try {
                log("getCurrentState in getConnectedDevices");
                if (getCurrentState() == mConnected) {
                    devices.add(mCurrentDevice);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                //detecting seldom CR ALPS01020702
                loge(TAG + " A2dpStateMachine crash, " + e.getMessage());
            }
        }
        return devices;
    }

    boolean isPlaying(BluetoothDevice device) {
        synchronized(this) {
            if (device.equals(mPlayingA2dpDevice)) {
                return true;
            }
        }
        return false;
    }

    boolean okToConnect(BluetoothDevice device) {
        AdapterService adapterService = AdapterService.getAdapterService();
        int priority = mService.getPriority(device);
        boolean ret = false;
        //check if this is an incoming connection in Quiet mode.
        if((adapterService == null) ||
           ((adapterService.isQuietModeEnabled() == true) &&
           (mTargetDevice == null))){
            ret = false;
        }
        // check priority and accept or reject the connection. if priority is undefined
        // it is likely that our SDP has not completed and peer is initiating the
        // connection. Allow this connection, provided the device is bonded
        else if((BluetoothProfile.PRIORITY_OFF < priority) ||
                ((BluetoothProfile.PRIORITY_UNDEFINED == priority) &&
                (device.getBondState() != BluetoothDevice.BOND_NONE))){
            ret= true;
        }
        return ret;
    }

    synchronized List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
        Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
        int connectionState;

        for (BluetoothDevice device : bondedDevices) {
            ParcelUuid[] featureUuids = device.getUuids();
            if (!BluetoothUuid.isUuidPresent(featureUuids, BluetoothUuid.AudioSink)) {
                continue;
            }
            connectionState = getConnectionState(device);
            for(int i = 0; i < states.length; i++) {
                if (connectionState == states[i]) {
                    deviceList.add(device);
                }
            }
        }
        return deviceList;
    }


    // This method does not check for error conditon (newState == prevState)
    private void broadcastConnectionState(BluetoothDevice device, int newState, int prevState) {

        int delay = mAudioManager.setBluetoothA2dpDeviceConnectionState(device, newState,
                BluetoothProfile.A2DP);
        log("broadcastConnectionState delay " + delay + ", StateMachine " + getCurrentState());
        mWakeLock.acquire();


        /// M: [ALPS01879322] time issue
        long time = SystemClock.uptimeMillis() + delay;
        if (mBroadcastConnectionTime > time) {
            time = mBroadcastConnectionTime;
        }
        else {
            mBroadcastConnectionTime = time;
        }

        mIntentBroadcastHandler.sendMessageAtTime(mIntentBroadcastHandler.obtainMessage(
                                                        MSG_CONNECTION_STATE_CHANGED,
                                                        prevState,
                                                        newState,
                                                        device),
                                                        time);
        //mIntentBroadcastHandler.sendMessageDelayed(mIntentBroadcastHandler.obtainMessage(
        //                                                MSG_CONNECTION_STATE_CHANGED,
        //                                                prevState,
        //                                                newState,
        //                                                device),
        //                                                delay);

        ///M: end
    }

    private void broadcastAudioState(BluetoothDevice device, int state, int prevState) {
        Intent intent = new Intent(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
        intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
        intent.putExtra(BluetoothProfile.EXTRA_STATE, state);
        intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
        mContext.sendBroadcast(intent, A2dpService.BLUETOOTH_PERM);

        log("A2DP Playing state : device: " + device + " State:" + prevState + "->" + state);
    }

    private byte[] getByteAddress(String address) {
        return Utils.getBytesFromAddress(address);
    }
    
    private byte[] getByteAddress(BluetoothDevice device) {
        return Utils.getBytesFromAddress(device.getAddress());
    }

    private void onConnectionStateChanged(int state, byte[] address) {
        log("onConnectionStateChanged , state " + state + " dev " + address + " this " + 
            this);
        StackEvent event = new StackEvent(EVENT_TYPE_CONNECTION_STATE_CHANGED);
        event.valueInt = state;
        event.device = getDevice(address);
        sendMessage(STACK_EVENT, event);
    }

    private void onAudioStateChanged(int state, byte[] address) {
        log("onAudioStateChanged , state " + state + " dev " + address + " this " + 
            A2dpStateMachine.this);

        StackEvent event = new StackEvent(EVENT_TYPE_AUDIO_STATE_CHANGED);
        event.valueInt = state;
        event.device = getDevice(address);
        sendMessage(STACK_EVENT, event);

    }
    private BluetoothDevice getDevice(byte[] address) {
        return mAdapter.getRemoteDevice(Utils.getAddressStringFromByte(address));
    }

    private class RequestConfirmInformation {
        int acceptOrReject;
		BluetoothDevice device;
    }

    private class StackEvent {
        int type = EVENT_TYPE_NONE;
        int valueInt = 0;
        BluetoothDevice device = null;

        private StackEvent(int type) {
            this.type = type;
        }
    }
    /** Handles A2DP connection state change intent broadcasts. */
    private class IntentBroadcastHandler extends Handler {

        private void onConnectionStateChanged(BluetoothDevice device, int prevState, int state) {
            Intent intent = new Intent(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            intent.putExtra(BluetoothProfile.EXTRA_PREVIOUS_STATE, prevState);
            intent.putExtra(BluetoothProfile.EXTRA_STATE, state);
            intent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            intent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT);
            mContext.sendBroadcast(intent, ProfileService.BLUETOOTH_PERM);
            log("[onConnectionStateChanged] Connection state " + device + ": " + prevState + "->" + state);
            mService.notifyProfileConnectionStateChanged(device, BluetoothProfile.A2DP, state, prevState);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECTION_STATE_CHANGED:
                    onConnectionStateChanged((BluetoothDevice) msg.obj, msg.arg1, msg.arg2);
                    mWakeLock.release();
                    break;
            }
        }
    }

    public void dump(StringBuilder sb) {
        ProfileService.println(sb, "mCurrentDevice: " + mCurrentDevice);
        ProfileService.println(sb, "mTargetDevice: " + mTargetDevice);
        ProfileService.println(sb, "mIncomingDevice: " + mIncomingDevice);
        ProfileService.println(sb, "mPlayingA2dpDevice: " + mPlayingA2dpDevice);
        ProfileService.println(sb, "StateMachine: " + this.toString());
    }

    // Event types for STACK_EVENT message
    final private static int EVENT_TYPE_NONE = 0;
    final private static int EVENT_TYPE_CONNECTION_STATE_CHANGED = 1;
    final private static int EVENT_TYPE_AUDIO_STATE_CHANGED = 2;

   // Do not modify without updating the HAL bt_av.h files.

    // match up with btav_connection_state_t enum of bt_av.h
    final static int CONNECTION_STATE_DISCONNECTED = 0;
    final static int CONNECTION_STATE_CONNECTING = 1;
    final static int CONNECTION_STATE_CONNECTED = 2;
    final static int CONNECTION_STATE_DISCONNECTING = 3;
    final static int SOURCE_CONNECT_REQUEST = 4;

    // match up with btav_audio_state_t enum of bt_av.h
    final static int AUDIO_STATE_REMOTE_SUSPEND = 0;
    final static int AUDIO_STATE_STOPPED = 1;
    final static int AUDIO_STATE_STARTED = 2;

    private native static void classInitNative();
    private native void initNative();
    private native void cleanupNative();
    private native boolean connectA2dpNative(byte[] address);
    private native boolean disconnectA2dpNative(byte[] address);
    private native boolean confirmRequestNative(byte[] address, int acceptOrReject);
}
