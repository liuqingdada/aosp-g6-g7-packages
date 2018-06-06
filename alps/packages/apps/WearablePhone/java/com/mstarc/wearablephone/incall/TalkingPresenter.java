/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.mstarc.wearablephone.incall;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.mstarc.wearablephone.incall.ContactInfoCache.ContactCacheEntry;
import com.mstarc.wearablephone.incall.ContactInfoCache.ContactInfoCacheCallback;
import com.mstarc.wearablephone.incall.InCallPresenter.InCallState;

import java.lang.ref.WeakReference;


/**
 * Presenter for the Incoming call widget.
 */
public class TalkingPresenter extends Presenter<TalkingPresenter.TalkingUi>
        implements CallList.CallUpdateListener,
        CallList.Listener,
        InCallPresenter.InCallStateListener,
        InCallPresenter.InCallDetailsListener,
        InCallPresenter.InCallEventListener, InCallPresenter.IncomingCallListener {

    private static final String TAG = TalkingPresenter.class.getSimpleName();
    private static final long CALL_TIME_UPDATE_INTERVAL_MS = 1000;

    private Call mPrimary;
    private ContactCacheEntry mPrimaryContactInfo;
    private Context mContext;
    private CallTimer mCallTimer;

    @Override
    public void onIncomingCall(InCallState oldState, InCallState newState, Call call) {
        onStateChange(oldState, newState, CallList.getInstance());
    }


    public static class ContactLookupCallback implements ContactInfoCacheCallback {
        private final WeakReference<TalkingPresenter> mOutgoingPresenter;
        private final boolean mIsPrimary;

        public ContactLookupCallback(TalkingPresenter outgoingPresenter, boolean isPrimary) {
            mOutgoingPresenter = new WeakReference<TalkingPresenter>(outgoingPresenter);
            mIsPrimary = isPrimary;
        }

        @Override
        public void onContactInfoComplete(String callId, ContactCacheEntry entry) {
            TalkingPresenter presenter = mOutgoingPresenter.get();
            if (presenter != null) {
                presenter.onContactInfoComplete(callId, entry, mIsPrimary);
            }
        }

        @Override
        public void onImageLoadComplete(String callId, ContactCacheEntry entry) {
            TalkingPresenter presenter = mOutgoingPresenter.get();
            if (presenter != null) {
                presenter.onImageLoadComplete(callId, entry);
            }
        }

    }

    public TalkingPresenter() {
        // create the call timer
        mCallTimer = new CallTimer(new Runnable() {
            @Override
            public void run() {
                updateCallTime();
            }
        });
    }

    public void init(Context context, Call call) {
        mContext = Preconditions.checkNotNull(context);

        // Call may be null if disconnect happened already.
        if (call != null) {
            mPrimary = call;
            startContactInfoSearch(call, true, call.getState() == Call.State.INCOMING);
            // Start/stop timers.
            if (mPrimary.getState() == Call.State.ACTIVE) {
                Log.d(this, "Starting the calltime timer");
                mCallTimer.start(CALL_TIME_UPDATE_INTERVAL_MS);
            } else {
                Log.d(this, "Canceling the calltime timer");
                mCallTimer.cancel();
                getUi().setPrimaryCallElapsedTime(false, 0);
            }
        }
    }

    public void updateCallTime() {
        final TalkingUi ui = getUi();

        if (ui == null || mPrimary == null || mPrimary.getState() != Call.State.ACTIVE) {
            if (ui != null) {
                ui.setPrimaryCallElapsedTime(false, 0);
            }
            mCallTimer.cancel();
        } else {
            final long callStart = mPrimary.getConnectTimeMillis();
            final long duration = System.currentTimeMillis() - callStart;
            ui.setPrimaryCallElapsedTime(true, duration);
        }
    }
    @Override
    public void onUiReady(TalkingUi ui) {
        super.onUiReady(ui);


        // Contact search may have completed before ui is ready.
        if (mPrimaryContactInfo != null) {
            updatePrimaryDisplayInfo();
        }

        // Register for call state changes last
        InCallPresenter.getInstance().addListener(this);
        InCallPresenter.getInstance().addDetailsListener(this);
        InCallPresenter.getInstance().addInCallEventListener(this);
    }

    @Override
    public void onUiUnready(TalkingUi ui) {
        super.onUiUnready(ui);
        // stop getting call state changes
        InCallPresenter.getInstance().removeListener(this);
        InCallPresenter.getInstance().removeIncomingCallListener(this);
        InCallPresenter.getInstance().removeDetailsListener(this);
        InCallPresenter.getInstance().removeInCallEventListener(this);

        mPrimary = null;
        mPrimaryContactInfo = null;
    }

    @Override
    public void onStateChange(InCallState oldState, InCallState newState, CallList callList) {
        Log.d(this, "onStateChange() " + newState);
        final TalkingUi ui = getUi();
        if (ui == null) {
            return;
        }

        Call primary = null;

        if (newState == InCallState.INCALL) {
            primary = callList.getActiveCall();
            if (primary == null) {
                primary = callList.getBackgroundCall();
            }
            Log.d(this, "talking call: " + primary);
            final boolean primaryChanged = !Call.areSame(mPrimary, primary);

            if(primaryChanged) {
                mPrimary = primary;
                if (mPrimary != null ) {
                    // primary call has changed
                    mPrimaryContactInfo = ContactInfoCache.buildCacheEntryFromCall(mContext, mPrimary,
                            mPrimary.getState() == Call.State.INCOMING);
                    updatePrimaryDisplayInfo();
                    maybeStartSearch(mPrimary, true);
                    mPrimary.setSessionModificationState(Call.SessionModificationState.NO_REQUEST);
                }else{
                    ui.setVisible(false);
                }
            }
            ui.setVisible(true);
        } else {
            ui.setVisible(false);
        }
        // Start/stop timers.
        if (mPrimary != null && mPrimary.getState() == Call.State.ACTIVE) {
            Log.d(this, "Starting the calltime timer");
            mCallTimer.start(CALL_TIME_UPDATE_INTERVAL_MS);
        } else {
            Log.d(this, "Canceling the calltime timer");
            mCallTimer.cancel();
            ui.setPrimaryCallElapsedTime(false, 0);
        }
    }
    public void endCallClicked() {
        if (mPrimary == null) {
            return;
        }
        Log.i(this, "Disconnecting call: " + mPrimary);
        Call call = mPrimary;
        mPrimary.setState(Call.State.DISCONNECTING);
        CallList.getInstance().onUpdate(mPrimary);
        TelecomAdapter.getInstance().disconnectCall(call.getId());
    }
    private void maybeStartSearch(Call call, boolean isPrimary) {
        // no need to start search for conference calls which show generic info.
        if (call != null && !call.isConferenceCall()) {
            startContactInfoSearch(call, isPrimary, call.getState() == Call.State.INCOMING);
        }
    }
    /**
     * Starts a query for more contact data for the save primary and secondary calls.
     */
    private void startContactInfoSearch(final Call call, final boolean isPrimary,
                                        boolean isIncoming) {
        final ContactInfoCache cache = ContactInfoCache.getInstance(mContext);

        cache.findInfo(call, isIncoming, new ContactLookupCallback(this, isPrimary));
    }

    private void onContactInfoComplete(String callId, ContactCacheEntry entry, boolean isPrimary) {
        updateContactEntry(entry);
        if (entry.name != null) {
            Log.d(TAG, "Contact found: " + entry);
        }
        if (entry.contactUri != null) {
            CallerInfoUtils.sendViewNotification(mContext, entry.contactUri);
        }
    }

    private void updateContactEntry(ContactCacheEntry entry) {
        mPrimaryContactInfo = entry;
        updatePrimaryDisplayInfo();
    }

    private void onImageLoadComplete(String callId, ContactCacheEntry entry) {
        if (getUi() == null) {
            return;
        }

        if (entry.photo != null) {
            if (mPrimary != null && callId.equals(mPrimary.getId())) {
                getUi().setPrimaryImage(entry.photo);
            }
        }
    }
    public final void processDtmf(char c) {
        Log.d(this, "Processing dtmf key " + c);
        // if it is a valid key, then update the display and send the dtmf tone.
        if (PhoneNumberUtils.is12Key(c) && mPrimary != null) {
            Log.d(this, "updating display and sending dtmf tone for '" + c + "'");

            // Append this key to the "digits" widget.
            getUi().appendDigitsToField(c);
            // Plays the tone through Telecomm.
            TelecomAdapter.getInstance().playDtmfTone(mPrimary.getId(), c);
        } else {
            Log.d(this, "ignoring dtmf request for '" + c + "'");
        }
    }
    public void stopDtmf() {
        if (mPrimary != null) {
            Log.d(this, "stopping remote tone");
            TelecomAdapter.getInstance().stopDtmfTone(mPrimary.getId());
        }
    }

    private void updatePrimaryDisplayInfo() {
        final TalkingUi ui = getUi();
        if (ui == null) {
            // TODO: May also occur if search result comes back after ui is destroyed. Look into
            // removing that case completely.
            Log.d(TAG, "updatePrimaryDisplayInfo called but ui is null!");
            return;
        }

        if (mPrimary == null) {
            // Clear the primary display info.
            ui.setPrimary(null, null, false, null, null, false);
            return;
        }
        if (mPrimaryContactInfo != null) {
            Log.d(TAG, "Update primary display info for " + mPrimaryContactInfo);

            String name = getNameForCall(mPrimaryContactInfo);
            String number = getNumberForCall(mPrimaryContactInfo);
            boolean nameIsNumber = name != null && name.equals(mPrimaryContactInfo.number);
            ui.setPrimary(
                    number,
                    name,
                    nameIsNumber,
                    mPrimaryContactInfo.label,
                    mPrimaryContactInfo.photo,
                    mPrimaryContactInfo.isSipCall);
        } else {
            // Clear the primary display info.
            ui.setPrimary(null, null, false, null, null, false);
        }

    }

    /**
     * Gets the name to display for the call.
     */
    private static String getNameForCall(ContactCacheEntry contactInfo) {
        if (TextUtils.isEmpty(contactInfo.name)) {
            return contactInfo.number;
        }
        return contactInfo.name;
    }

    /**
     * Gets the number to display for a call.
     */
    private static String getNumberForCall(ContactCacheEntry contactInfo) {
        // If the name is empty, we use the number for the name...so dont show a second
        // number in the number field
        if (TextUtils.isEmpty(contactInfo.name)) {
            return contactInfo.location;
        }
        return contactInfo.number;
    }

    @Override
    public void onCallListChange(CallList callList) {
        // no-op
    }

    @Override
    public void onDisconnect(Call call) {
        // no-op
    }

    @Override
    public void onIncomingCall(Call call) {
    }

    @Override
    public void onCallChanged(Call call) {
    }

    @Override
    public void onDetailsChanged(Call call, android.telecom.Call.Details details) {

    }

    @Override
    public void onFullScreenVideoStateChanged(boolean isFullScreenVideo) {

    }

    public interface TalkingUi extends Ui {

        void setVisible(boolean on);

        void setPrimary(String number, String name, boolean nameIsNumber, String label,
                        Drawable photo, boolean isSipCall);


        void setPrimaryImage(Drawable image);

        void setPrimaryCallElapsedTime(boolean show, long duration);

        void setPrimaryPhoneNumber(String phoneNumber);

        void setPrimaryLabel(String label);

        void appendDigitsToField(char digit);

    }
}
