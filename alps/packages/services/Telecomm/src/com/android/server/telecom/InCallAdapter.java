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

package com.android.server.telecom;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.telecom.PhoneAccountHandle;

import com.android.internal.os.SomeArgs;
import com.android.internal.telecom.IInCallAdapter;

/**
 * Receives call commands and updates from in-call app and passes them through to CallsManager.
 * {@link InCallController} creates an instance of this class and passes it to the in-call app after
 * binding to it. This adapter can receive commands and updates until the in-call app is unbound.
 */
class InCallAdapter extends IInCallAdapter.Stub {
    private static final int MSG_ANSWER_CALL = 0;
    private static final int MSG_REJECT_CALL = 1;
    private static final int MSG_PLAY_DTMF_TONE = 2;
    private static final int MSG_STOP_DTMF_TONE = 3;
    private static final int MSG_POST_DIAL_CONTINUE = 4;
    private static final int MSG_DISCONNECT_CALL = 5;
    private static final int MSG_HOLD_CALL = 6;
    private static final int MSG_UNHOLD_CALL = 7;
    private static final int MSG_MUTE = 8;
    private static final int MSG_SET_AUDIO_ROUTE = 9;
    private static final int MSG_CONFERENCE = 10;
    private static final int MSG_SPLIT_FROM_CONFERENCE = 11;
    private static final int MSG_SWAP_WITH_BACKGROUND_CALL = 12;
    private static final int MSG_PHONE_ACCOUNT_SELECTED = 13;
    private static final int MSG_TURN_ON_PROXIMITY_SENSOR = 14;
    private static final int MSG_TURN_OFF_PROXIMITY_SENSOR = 15;
    private static final int MSG_MERGE_CONFERENCE = 16;
    private static final int MSG_SWAP_CONFERENCE = 17;
    /// M: Voice recording
    private static final int MSG_START_VOICE_RECORDING = 1000;
    private static final int MSG_STOP_VOICE_RECORDING = 1001;

    /// M: For update the ordered background call list;
    private static final int MSG_SET_SORTED_BACKGROUND_CALL_LIST = 1002;
    /// M: For update the ordered incoming call list;
    private static final int MSG_SET_SORTED_INCOMING_CALL_LIST = 1003;
    /// M: for ECT
    private static final int MSG_ECT = 1004;
    private static final int MSG_HANGUP_ALL = 1005;
    private static final int MSG_HANGUP_ALL_HOLD_CALLS = 1006;
    private static final int MSG_HANGUP_ACTIVE_AND_ANSWER_WAITING = 1007;
    /// M: For On/Off Device when connecting to Smart book
    private static final int MSG_POWER_SMARTBOOK = 1008;

    /// M: For VoLTE @{
    private static final int MSG_INVITE_CONFERENCE_PARTICIPANTS = 1009;
    /// @}

    private final class InCallAdapterHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Call call;
            switch (msg.what) {
                case MSG_ANSWER_CALL: {
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        call = mCallIdMapper.getCall(args.arg1);
                        int videoState = (int) args.arg2;
                        if (call != null) {
                            mCallsManager.answerCall(call, videoState);
                        } else {
                            Log.w(this, "answerCall, unknown call id: %s", msg.obj);
                        }
                    } finally {
                        args.recycle();
                    }
                    break;
                }
                case MSG_REJECT_CALL: {
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        call = mCallIdMapper.getCall(args.arg1);
                        boolean rejectWithMessage = args.argi1 == 1;
                        String textMessage = (String) args.arg2;
                        if (call != null) {
                            mCallsManager.rejectCall(call, rejectWithMessage, textMessage);
                        } else {
                            Log.w(this, "setRingback, unknown call id: %s", args.arg1);
                        }
                    } finally {
                        args.recycle();
                    }
                    break;
                }
                case MSG_PLAY_DTMF_TONE:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        mCallsManager.playDtmfTone(call, (char) msg.arg1);
                    } else {
                        Log.w(this, "playDtmfTone, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_STOP_DTMF_TONE:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        mCallsManager.stopDtmfTone(call);
                    } else {
                        Log.w(this, "stopDtmfTone, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_POST_DIAL_CONTINUE:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        mCallsManager.postDialContinue(call, msg.arg1 == 1);
                    } else {
                        Log.w(this, "postDialContinue, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_DISCONNECT_CALL:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        mCallsManager.disconnectCall(call);
                    } else {
                        Log.w(this, "disconnectCall, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_HOLD_CALL:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        mCallsManager.holdCall(call);
                    } else {
                        Log.w(this, "holdCall, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_UNHOLD_CALL:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        mCallsManager.unholdCall(call);
                    } else {
                        Log.w(this, "unholdCall, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_PHONE_ACCOUNT_SELECTED: {
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        call = mCallIdMapper.getCall(args.arg1);
                        if (call != null) {
                            mCallsManager.phoneAccountSelected(call,
                                    (PhoneAccountHandle) args.arg2, args.argi1 == 1);
                        } else {
                            Log.w(this, "phoneAccountSelected, unknown call id: %s", args.arg1);
                        }
                    } finally {
                        args.recycle();
                    }
                    break;
                }
                case MSG_MUTE:
                    mCallsManager.mute(msg.arg1 == 1);
                    break;
                case MSG_SET_AUDIO_ROUTE:
                    mCallsManager.setAudioRoute(msg.arg1);
                    break;
                case MSG_CONFERENCE: {
                    SomeArgs args = (SomeArgs) msg.obj;
                    try {
                        call = mCallIdMapper.getCall(args.arg1);
                        Call otherCall = mCallIdMapper.getCall(args.arg2);
                        if (call != null && otherCall != null) {
                            mCallsManager.conference(call, otherCall);
                        } else {
                            Log.w(this, "conference, unknown call id: %s", msg.obj);
                        }
                    } finally {
                        args.recycle();
                    }
                    break;
                }
                case MSG_SPLIT_FROM_CONFERENCE:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        call.splitFromConference();
                    } else {
                        Log.w(this, "splitFromConference, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_TURN_ON_PROXIMITY_SENSOR:
                    mCallsManager.turnOnProximitySensor();
                    break;
                case MSG_TURN_OFF_PROXIMITY_SENSOR:
                    mCallsManager.turnOffProximitySensor((boolean) msg.obj);
                    break;
                case MSG_MERGE_CONFERENCE:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        call.mergeConference();
                    } else {
                        Log.w(this, "mergeConference, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_SWAP_CONFERENCE:
                    call = mCallIdMapper.getCall(msg.obj);
                    if (call != null) {
                        call.swapConference();
                    } else {
                        Log.w(this, "swapConference, unknown call id: %s", msg.obj);
                    }
                    break;
                case MSG_START_VOICE_RECORDING:
                    mCallsManager.startVoiceRecording();
                    break;
                case MSG_STOP_VOICE_RECORDING:
                    mCallsManager.stopVoiceRecording();
                    break;
                case MSG_SET_SORTED_BACKGROUND_CALL_LIST: {
                    @SuppressWarnings("unchecked")
                    final List<String> list = (List<String>) msg.obj;
                    final List<Call> callList = getCallListById(list);
                    mCallsManager.setSortedBackgroundCallList(callList);
                    break;
                }
                case MSG_SET_SORTED_INCOMING_CALL_LIST: {
                    @SuppressWarnings("unchecked")
                    final List<String> list = (List<String>) msg.obj;
                    final List<Call> callList = getCallListById(list);

                    mCallsManager.setSortedIncomingCallList(callList);
                    break;
                }
                case MSG_ECT:
                    call = mCallIdMapper.getCall(msg.obj);
                    mCallsManager.explicitCallTransfer(call);
                    break;
                case MSG_HANGUP_ALL:
                    mCallsManager.hangupAll();
                    break;
                case MSG_HANGUP_ALL_HOLD_CALLS:
                    mCallsManager.hangupAllHoldCalls();
                    break;
                case MSG_HANGUP_ACTIVE_AND_ANSWER_WAITING:
                    mCallsManager.hangupActiveAndAnswerWaiting();
                    break;
                case MSG_POWER_SMARTBOOK:
                    mCallsManager.updatePowerForSmartBook((Boolean)msg.obj);
                    break;
                /// M: For VoLTE @{
                case MSG_INVITE_CONFERENCE_PARTICIPANTS:
                    SomeArgs args = (SomeArgs) msg.obj;
                    String conferenceCallId = (String)args.arg1;
                    List<String> numbers = (List<String>) args.arg2;
                    call = mCallIdMapper.getCall(conferenceCallId);
                    if (mCallIdMapper.isValidConferenceId(conferenceCallId) && call != null) {
                        call.inviteConferenceParticipants(numbers);
                    } else {
                        Log.w(this, "inviteConferenceParticipants, unknown conference id: %s",
                                conferenceCallId);
                    }
                    break;
                /// @}
                default:
                    break;
            }
        }
    }

    private List<Call> getCallListById(List<String> list) {
        final List<Call> callList = new ArrayList<Call>(list.size());
        for (String id : list) {
            callList.add(mCallIdMapper.getCall(id));
        }

        return callList;
    }

    private final CallsManager mCallsManager;
    private final Handler mHandler = new InCallAdapterHandler();
    private final CallIdMapper mCallIdMapper;

    /** Persists the specified parameters. */
    public InCallAdapter(CallsManager callsManager, CallIdMapper callIdMapper) {
        ThreadUtil.checkOnMainThread();
        mCallsManager = callsManager;
        mCallIdMapper = callIdMapper;
    }

    @Override
    public void answerCall(String callId, int videoState) {
        Log.d(this, "answerCall(%s,%d)", callId, videoState);
        if (mCallIdMapper.isValidCallId(callId)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = videoState;
            mHandler.obtainMessage(MSG_ANSWER_CALL, args).sendToTarget();
        }
    }

    @Override
    public void rejectCall(String callId, boolean rejectWithMessage, String textMessage) {
        Log.d(this, "rejectCall(%s,%b,%s)", callId, rejectWithMessage, textMessage);
        if (mCallIdMapper.isValidCallId(callId)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.argi1 = rejectWithMessage ? 1 : 0;
            args.arg2 = textMessage;
            mHandler.obtainMessage(MSG_REJECT_CALL, args).sendToTarget();
        }
    }

    @Override
    public void playDtmfTone(String callId, char digit) {
        Log.d(this, "playDtmfTone(%s,%c)", callId, digit);
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_PLAY_DTMF_TONE, (int) digit, 0, callId).sendToTarget();
        }
    }

    @Override
    public void stopDtmfTone(String callId) {
        Log.d(this, "stopDtmfTone(%s)", callId);
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_STOP_DTMF_TONE, callId).sendToTarget();
        }
    }

    @Override
    public void postDialContinue(String callId, boolean proceed) {
        Log.d(this, "postDialContinue(%s)", callId);
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_POST_DIAL_CONTINUE, proceed ? 1 : 0, 0, callId).sendToTarget();
        }
    }

    @Override
    public void disconnectCall(String callId) {
        Log.v(this, "disconnectCall: %s", callId);
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_DISCONNECT_CALL, callId).sendToTarget();
        }
    }

    @Override
    public void holdCall(String callId) {
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_HOLD_CALL, callId).sendToTarget();
        }
    }

    @Override
    public void unholdCall(String callId) {
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_UNHOLD_CALL, callId).sendToTarget();
        }
    }

    @Override
    public void phoneAccountSelected(String callId, PhoneAccountHandle accountHandle,
            boolean setDefault) {
        if (mCallIdMapper.isValidCallId(callId)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = accountHandle;
            args.argi1 = setDefault? 1 : 0;
            mHandler.obtainMessage(MSG_PHONE_ACCOUNT_SELECTED, args).sendToTarget();
        }
    }

    @Override
    public void mute(boolean shouldMute) {
        mHandler.obtainMessage(MSG_MUTE, shouldMute ? 1 : 0, 0).sendToTarget();
    }

    @Override
    public void setAudioRoute(int route) {
        mHandler.obtainMessage(MSG_SET_AUDIO_ROUTE, route, 0).sendToTarget();
    }

    @Override
    public void conference(String callId, String otherCallId) {
        if (mCallIdMapper.isValidCallId(callId) &&
                mCallIdMapper.isValidCallId(otherCallId)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = callId;
            args.arg2 = otherCallId;
            mHandler.obtainMessage(MSG_CONFERENCE, args).sendToTarget();
        }
    }

    @Override
    public void splitFromConference(String callId) {
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_SPLIT_FROM_CONFERENCE, callId).sendToTarget();
        }
    }

    @Override
    public void mergeConference(String callId) {
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_MERGE_CONFERENCE, callId).sendToTarget();
        }
    }

    @Override
    public void swapConference(String callId) {
        if (mCallIdMapper.isValidCallId(callId)) {
            mHandler.obtainMessage(MSG_SWAP_CONFERENCE, callId).sendToTarget();
        }
    }

    @Override
    public void turnOnProximitySensor() {
        mHandler.obtainMessage(MSG_TURN_ON_PROXIMITY_SENSOR).sendToTarget();
    }

    @Override
    public void turnOffProximitySensor(boolean screenOnImmediately) {
        mHandler.obtainMessage(MSG_TURN_OFF_PROXIMITY_SENSOR, screenOnImmediately).sendToTarget();
    }

    /**
     * M: Start voice recording
     */
    @Override
    public void startVoiceRecording() {
        mHandler.obtainMessage(MSG_START_VOICE_RECORDING).sendToTarget();
    }

    /**
     * M: Stop voice recording
     */
    @Override
    public void stopVoiceRecording() {
        mHandler.obtainMessage(MSG_STOP_VOICE_RECORDING).sendToTarget();
    }

    @Override
    public void updatePowerForSmartBook(boolean onOff) {
        mHandler.obtainMessage(MSG_POWER_SMARTBOOK, onOff).sendToTarget();
    }

    @Override
    public void setSortedBackgroudCallList(List<String> list) {
        mHandler.obtainMessage(MSG_SET_SORTED_BACKGROUND_CALL_LIST,
                list).sendToTarget();
    }

    @Override
    public void setSortedIncomingCallList(List<String> list) {
        mHandler.obtainMessage(MSG_SET_SORTED_INCOMING_CALL_LIST,
                list).sendToTarget();
    }

    @Override
    public void explicitCallTransfer(String callId) {
        mHandler.obtainMessage(MSG_ECT, callId).sendToTarget();
    }

    @Override
    public void hangupAll() {
        Log.v(this, "hangupAll()");
        mHandler.obtainMessage(MSG_HANGUP_ALL).sendToTarget();
    }

    @Override
    public void hangupAllHoldCalls() {
        Log.v(this, "hangupAllHoldCalls()");
        mHandler.obtainMessage(MSG_HANGUP_ALL_HOLD_CALLS).sendToTarget();
    }

    @Override
    public void hangupActiveAndAnswerWaiting() {
        Log.v(this, "hangupActiveAndAnswerWaiting()");
        mHandler.obtainMessage(MSG_HANGUP_ACTIVE_AND_ANSWER_WAITING).sendToTarget();
    }

    /// M: For VoLTE @{
    @Override
    public void inviteConferenceParticipants(String conferenceCallId, List<String> numbers) {
        Log.v(this, "inviteConferenceParticipants()..." + conferenceCallId + " / " + numbers);
        if (numbers == null || numbers.size() <= 0) {
            Log.w(this, "inviteConferenceParticipants()... unexpected parameters, abandon!");
            return;
        }
        if (mCallIdMapper.isValidConferenceId(conferenceCallId)) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = conferenceCallId;
            args.arg2 = numbers;
            mHandler.obtainMessage(MSG_INVITE_CONFERENCE_PARTICIPANTS, args).sendToTarget();
        }
    }
    /// @}
}
