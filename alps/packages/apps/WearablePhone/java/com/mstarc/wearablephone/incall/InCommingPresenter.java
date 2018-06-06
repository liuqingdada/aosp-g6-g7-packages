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
import android.telecom.VideoProfile;

import com.mstarc.wearablephone.util.InterceptCall;

import java.lang.ref.WeakReference;


/**
 * Presenter for the Incoming call widget.
 */
public class InCommingPresenter extends Presenter<InCommingPresenter.AnswerUi>
        implements CallList.CallUpdateListener, CallList.Listener {

    public InCommingPresenter(Context context){
        mContext = context;
    }

    private static final String TAG = InCommingPresenter.class.getSimpleName();

    private String mCallId;
    private Call mCall = null;
    private ContactInfoCache.ContactCacheEntry mContactInfo;
    private Context mContext;

    @Override
    public void onUiReady(AnswerUi ui) {
        super.onUiReady(ui);

        final CallList calls = CallList.getInstance();
        Call call;
        call = calls.getIncomingCall();

        if (call != null) {
            processIncomingCall(call);
        } else {
            getUi().showAnswerUi(false);
        }
        // Listen for incoming calls.
        calls.addListener(this);

    }

    @Override
    public void onUiUnready(AnswerUi ui) {
        super.onUiUnready(ui);

        CallList.getInstance().removeListener(this);

        // This is necessary because the activity can be destroyed while an incoming call exists.
        // This happens when back button is pressed while incoming call is still being shown.
        if (mCallId != null) {
            CallList.getInstance().removeCallUpdateListener(mCallId, this);
        }
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
        // TODO: Ui is being destroyed when the fragment detaches.  Need clean up step to stop
        // getting updates here.
        Log.d(this, "onIncomingCall: " + this);
        if (getUi() != null) {
            if (!call.getId().equals(mCallId)) {
                // A new call is coming in.
                processIncomingCall(call);
            }
        }
    }

    private void processIncomingCall(Call call) {
        mCallId = call.getId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);
        Log.d(TAG, "Showing incoming for call id: " + mCallId + " " + this);
        getUi().showAnswerUi(true);
        final ContactInfoCache cache = ContactInfoCache.getInstance(mContext);
        cache.findInfo(call, true, new InCommingPresenter.ContactLookupCallback(this, true));
    }

    @Override
    public void onCallChanged(Call call) {
        Log.d(this, "onCallStateChange() " + call + " " + this);
        if (call.getState() != Call.State.INCOMING) {
            // Stop listening for updates.
            CallList.getInstance().removeCallUpdateListener(mCallId, this);

            getUi().showAnswerUi(false);

            // mCallId will hold the state of the call. We don't clear the mCall variable here as
            // it may be useful for sending text messages after phone disconnects.
            mCallId = null;
        }
    }

    public void onAnswer(Context context) {
        if (mCallId == null) {
            return;
        }

        Log.d(this, "onAnswer " + mCallId);
        if (mCall.getSessionModificationState()
                == Call.SessionModificationState.RECEIVED_UPGRADE_TO_VIDEO_REQUEST) {
            InCallPresenter.getInstance().acceptUpgradeRequest(context);
        } else {
            TelecomAdapter.getInstance().answerCall(mCall.getId(), VideoProfile.VideoState.AUDIO_ONLY);
        }
    }

    /**
     * TODO: We are using reject and decline interchangeably. We should settle on
     * reject since it seems to be more prevalent.
     */
    public void onDecline() {
        Log.d(this, "onDecline " + mCallId);
        TelecomAdapter.getInstance().rejectCall(mCallId, false, null);
    }

    private void updateContactEntry(ContactInfoCache.ContactCacheEntry entry) {
        mContactInfo = entry;
        getUi().setName(mContactInfo.name != null ? mContactInfo.name : mContactInfo.number);
        getUi().setPhoto(mContactInfo.photo);
    }

    private void onImageLoadComplete(String callId, ContactInfoCache.ContactCacheEntry entry) {
        if (getUi() == null) {
            return;
        }
        if (entry.photo != null) {
            if (mCall != null && callId.equals(mCallId)) {
                updateContactEntry(entry);
            }
        }
    }

    private void onContactInfoComplete(String callId, ContactInfoCache.ContactCacheEntry entry, boolean isPrimary) {
        updateContactEntry(entry);
        if (entry.name != null) {
            Log.d(TAG, "Contact found: " + entry);
        }
        if (entry.contactUri != null) {
            CallerInfoUtils.sendViewNotification(mContext, entry.contactUri);
        }
    }

    public interface AnswerUi extends Ui {
        public void showAnswerUi(boolean show);

        public void setName(String name);

        public void setPhoto(Drawable photo);
    }

    public static class ContactLookupCallback implements ContactInfoCache.ContactInfoCacheCallback {
        private final WeakReference<InCommingPresenter> mInCommingPresenter;
        private final boolean mIsPrimary;

        public ContactLookupCallback(InCommingPresenter presenter, boolean isPrimary) {
            mInCommingPresenter = new WeakReference<InCommingPresenter>(presenter);
            mIsPrimary = isPrimary;
        }

        @Override
        public void onContactInfoComplete(String callId, ContactInfoCache.ContactCacheEntry entry) {
            InCommingPresenter presenter = mInCommingPresenter.get();
            if (presenter != null) {
                presenter.onContactInfoComplete(callId, entry, mIsPrimary);
            }
        }

        @Override
        public void onImageLoadComplete(String callId, ContactInfoCache.ContactCacheEntry entry) {
            InCommingPresenter presenter = mInCommingPresenter.get();
            if (presenter != null) {
                presenter.onImageLoadComplete(callId, entry);
            }
        }
    }
}
