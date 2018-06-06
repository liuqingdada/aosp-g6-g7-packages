/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.services.telephony.sip;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.telecom.AudioState;
import android.telecom.Connection;
import android.telecom.PhoneAccount;
import android.util.Log;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.sip.SipPhone;
import com.android.services.telephony.DisconnectCauseUtil;

import java.util.Objects;

final class SipConnection extends Connection {
    private static final String PREFIX = "[SipConnection] ";
    private static final boolean VERBOSE = true; /* STOP SHIP if true */

    private static final int MSG_PRECISE_CALL_STATE_CHANGED = 1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_PRECISE_CALL_STATE_CHANGED:
                    updateState(false);
                    break;
            }
        }
    };

    private com.android.internal.telephony.Connection mOriginalConnection;
    private Call.State mOriginalConnectionState = Call.State.IDLE;

    SipConnection() {
        if (VERBOSE) log("new SipConnection");
        setInitializing();
    }

    void initialize(com.android.internal.telephony.Connection connection) {
        if (VERBOSE) log("init SipConnection, connection: " + connection);
        mOriginalConnection = connection;
        if (getPhone() != null) {
            getPhone().registerForPreciseCallStateChanged(mHandler, MSG_PRECISE_CALL_STATE_CHANGED,
                    null);
        }
        updateAddress();
        setInitialized();
    }

    @Override
    public void onAudioStateChanged(AudioState state) {
        if (VERBOSE) log("onAudioStateChanged: " + state);
        if (getPhone() != null) {
            getPhone().setEchoSuppressionEnabled();
        }
    }

    @Override
    public void onStateChanged(int state) {
        if (VERBOSE) log("onStateChanged, state: " + Connection.stateToString(state));
    }

    @Override
    public void onPlayDtmfTone(char c) {
        if (VERBOSE) log("onPlayDtmfTone");
        if (getPhone() != null) {
            getPhone().startDtmf(c);
        }
    }

    @Override
    public void onStopDtmfTone() {
        if (VERBOSE) log("onStopDtmfTone");
        if (getPhone() != null) {
            getPhone().stopDtmf();
        }
    }

    @Override
    public void onDisconnect() {
        if (VERBOSE) log("onDisconnect");
        try {
            if (getCall() != null && !getCall().isMultiparty()) {
                getCall().hangup();
            } else if (mOriginalConnection != null) {
                mOriginalConnection.hangup();
            }
        } catch (CallStateException e) {
            log("onDisconnect, exception: " + e);
        }
    }

    @Override
    public void onSeparate() {
        if (VERBOSE) log("onSeparate");
        try {
            if (mOriginalConnection != null) {
                mOriginalConnection.separate();
            }
        } catch (CallStateException e) {
            log("onSeparate, exception: " + e);
        }
    }

    @Override
    public void onAbort() {
        if (VERBOSE) log("onAbort");
        onDisconnect();
    }

    @Override
    public void onHold() {
        if (VERBOSE) log("onHold");
        try {
            if (getPhone() != null && getState() == STATE_ACTIVE) {
                getPhone().switchHoldingAndActive();
            }
        } catch (CallStateException e) {
            log("onHold, exception: " + e);
        }
    }

    /// M: CC078: For DSDS/DSDA Two-action operation @{
    @Override
    public void onHold(String pendingCallAction) {
        if (VERBOSE) log("onHold");
        try {
            if (getPhone() != null && getState() == STATE_ACTIVE) {
                /// M: CC043: [ALPS01843977] Fake hold for 1A1W @{
                // Sync the behavior of SipConnection to TelephonyConnection.
                // Under 1A1W(within the same phone) scenario,
                // when CallsManager opts to hold the ACTIVE call,
                // fake the state to HOLD by setOnHold() without any operations on SipPhone.
                // Otherwise, i.e(just 1A), call legacy phone's switchHoldingAndActive()
                // For GsmPhone, CHLD=2 is sent to hold the ACTIVE call
                // For SipPhone, mForeground call is switched mBackground and then held.

                //getPhone().switchHoldingAndActive();

                SipPhone phone = getPhone();
                Call ringingCall = phone.getRingingCall();
                if (ringingCall.getState() != Call.State.WAITING) {
                    phone.switchHoldingAndActive();
                } else {
                    if ("answer".equals(pendingCallAction)){
                        // 1. To fake Telecomm FW state
                        setOnHold();
                        // 2. To fake Legacy Connection state
                        // in case 1W disconnects during switch, 1A remains ACTIVE and unsynced
                        mOriginalConnectionState = Call.State.HOLDING;
                    }
                }
                /// @}
            }
        } catch (CallStateException e) {
            log("onHold, exception: " + e);
        }
    }
    /// @}

    @Override
    public void onUnhold() {
        if (VERBOSE) log("onUnhold");
        try {
            if (getPhone() != null && getState() == STATE_HOLDING) {
                getPhone().switchHoldingAndActive();
            }
        } catch (CallStateException e) {
            log("onUnhold, exception: " + e);
        }
    }

    @Override
    public void onAnswer(int videoState) {
        if (VERBOSE) log("onAnswer");
        try {
            if (isValidRingingCall() && getPhone() != null) {
                getPhone().acceptCall(videoState);
            }
        } catch (CallStateException e) {
            log("onAnswer, exception: " + e);
        }
    }

    @Override
    public void onReject() {
        if (VERBOSE) log("onReject");
        try {
            if (isValidRingingCall() && getPhone() != null) {
                getPhone().rejectCall();
            }
        } catch (CallStateException e) {
            log("onReject, exception: " + e);
        }
    }

    @Override
    public void onPostDialContinue(boolean proceed) {
        if (VERBOSE) log("onPostDialContinue, proceed: " + proceed);
        // SIP doesn't have post dial support.
    }

    /// M: CC025: Interface for swap call @{
    public void onSwapWithBackgroundCall() {
        log("onSwapWithBackgroundCall");
        try {
            SipPhone phone = getPhone();
            if (phone != null) {
                phone.switchHoldingAndActive();
            } else {
                log("Attempting to swap a connection without backing phone.");
            }
        } catch (CallStateException e) {
            log("Exception occurred while trying to do swap.");
        }
    }
    /// @}

    /// M: CC026: Interface for hangup all connections @{
    @Override
    public void onHangupAll() {
        if (VERBOSE) log("onHangupAll");
        try {
            SipPhone phone = getPhone();
            if (phone != null) {
                phone.hangupAll();
            } else {
                log("Attempting to hangupAll a connection without backing phone.");
            }
        } catch (CallStateException e) {
            log("Call to phone.hangupAll() failed with exception: " + e);
        }
    }
    /// @}

    /// M: CC027: Proprietary scheme to build Connection Capabilities @{
    // Declare as default for SipConnectionService (in same package) to use
    /* private */ Call getCall() {
    /// @}
        if (mOriginalConnection != null) {
            return mOriginalConnection.getCall();
        }
        return null;
    }

    SipPhone getPhone() {
        Call call = getCall();
        if (call != null) {
            return (SipPhone) call.getPhone();
        }
        return null;
    }

    /// M: CC027: Proprietary scheme to build Connection Capabilities @{
    // Declare as default for SipConnectionService (in same package) to use
    /* private */ boolean isValidRingingCall() {
    /// @}
        Call call = getCall();
        return call != null && call.getState().isRinging() &&
                call.getEarliestConnection() == mOriginalConnection;
    }

    /// M: CC046: Force updateState for Connection once its ConnectionService is set @{
    @Override
    protected void fireOnCallState() {
        updateState(false);
    }
    /// @}

    private void updateState(boolean force) {
        if (mOriginalConnection == null) {
            return;
        }

        Call.State newState = mOriginalConnection.getState();
        if (VERBOSE) log("updateState, " + mOriginalConnectionState + " -> " + newState);
        if (force || mOriginalConnectionState != newState) {
            mOriginalConnectionState = newState;
            switch (newState) {
                case IDLE:
                    break;
                case ACTIVE:
                    setActive();
                    break;
                case HOLDING:
                    setOnHold();
                    break;
                case DIALING:
                case ALERTING:
                    setDialing();
                    // For SIP calls, we need to ask the framework to play the ringback for us.
                    setRingbackRequested(true);
                    break;
                case INCOMING:
                case WAITING:
                    setRinging();
                    break;
                case DISCONNECTED:
                    setDisconnected(DisconnectCauseUtil.toTelecomDisconnectCause(
                            mOriginalConnection.getDisconnectCause()));
                    close();
                    break;
                case DISCONNECTING:
                    break;
            }
        }
        /// M: CC047: [ALPS01778114] Update SIP call capabilities even if call state not change @{
        updateConnectionCapabilities(force);
        /// @}
    }

    private int buildConnectionCapabilities() {
        int capabilities = CAPABILITY_MUTE | CAPABILITY_SUPPORT_HOLD;
        /// M: CC027: Proprietary scheme to build Connection Capabilities @{
        if (getConnectionService() != null) {
            if (getConnectionService().canHold(this)) {
                capabilities |= CAPABILITY_HOLD;
            }
            if (getConnectionService().canUnHold(this)) {
                capabilities |= CAPABILITY_UNHOLD;
            }
            if (getConnectionService().canAdd(this)) {
                capabilities |= CAPABILITY_ADD_CALL;
            }
        }
        log("buildConnectionCapabilities: " + capabilitiesToString(capabilities));
        /// @}

        return capabilities;
    }

    void updateConnectionCapabilities(boolean force) {
        int newCallCapabilities = buildConnectionCapabilities();
        if (force || getConnectionCapabilities() != newCallCapabilities) {
            setConnectionCapabilities(newCallCapabilities);
        }
    }

    void onAddedToCallService() {
        if (VERBOSE) log("onAddedToCallService");
        updateState(true);
        updateConnectionCapabilities(true);
        setAudioModeIsVoip(true);
        if (mOriginalConnection != null) {
            setCallerDisplayName(mOriginalConnection.getCnapName(),
                    mOriginalConnection.getCnapNamePresentation());
        }
    }

    /**
     * Updates the handle on this connection based on the original connection.
     */
    private void updateAddress() {
        if (mOriginalConnection != null) {
            Uri address = getAddressFromNumber(mOriginalConnection.getAddress());
            int presentation = mOriginalConnection.getNumberPresentation();
            if (!Objects.equals(address, getAddress()) ||
                    presentation != getAddressPresentation()) {
                com.android.services.telephony.Log.v(this, "updateAddress, address changed");
                setAddress(address, presentation);
            }

            String name = mOriginalConnection.getCnapName();
            int namePresentation = mOriginalConnection.getCnapNamePresentation();
            if (!Objects.equals(name, getCallerDisplayName()) ||
                    namePresentation != getCallerDisplayNamePresentation()) {
                com.android.services.telephony.Log
                        .v(this, "updateAddress, caller display name changed");
                setCallerDisplayName(name, namePresentation);
            }
        }
    }

    /**
     * Determines the address for an incoming number.
     *
     * @param number The incoming number.
     * @return The Uri representing the number.
     */
    private static Uri getAddressFromNumber(String number) {
        // Address can be null for blocked calls.
        if (number == null) {
            number = "";
        }
        return Uri.fromParts(PhoneAccount.SCHEME_SIP, number, null);
    }

    private void close() {
        if (getPhone() != null) {
            getPhone().unregisterForPreciseCallStateChanged(mHandler);
        }
        mOriginalConnection = null;
        destroy();
    }

    private static void log(String msg) {
        Log.d(SipUtil.LOG_TAG, PREFIX + msg);
    }
}
