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

package com.android.mms.service;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.service.carrier.CarrierMessagingService;
import android.service.carrier.ICarrierMessagingCallback;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.mms.service.exception.ApnException;
import com.android.mms.service.exception.MmsHttpException;
import com.android.mms.service.exception.MmsNetworkException;

/**
 * Base class for MMS requests. This has the common logic of sending/downloading MMS.
 */
public abstract class MmsRequest {
    private static final int RETRY_TIMES = 3;

    /**
     * Interface for certain functionalities from MmsService
     */
    public static interface RequestManager {
        /**
         * Enqueue an MMS request
         *
         * @param request the request to enqueue
         */
        public void addSimRequest(MmsRequest request);

        /*
         * @return Whether to auto persist received MMS
         */
        public boolean getAutoPersistingPref();

        /**
         * Read pdu (up to maxSize bytes) from supplied content uri
         * @param contentUri content uri from which to read
         * @param maxSize maximum number of bytes to read
         * @return read pdu (else null in case of error or too big)
         */
        public byte[] readPduFromContentUri(final Uri contentUri, final int maxSize);

        /**
         * Write pdu to supplied content uri
         * @param contentUri content uri to which bytes should be written
         * @param pdu pdu bytes to write
         * @return true in case of success (else false)
         */
        public boolean writePduToContentUri(final Uri contentUri, final byte[] pdu);
    }

    // The reference to the pending requests manager (i.e. the MmsService)
    protected RequestManager mRequestManager;
    // The SIM id
    protected int mSubId;
    // The creator app
    protected String mCreator;
    // MMS config
    protected MmsConfig.Overridden mMmsConfig;
    // MMS config overrides
    protected Bundle mMmsConfigOverrides;

    public MmsRequest(RequestManager requestManager, int subId, String creator,
            Bundle configOverrides) {
        mRequestManager = requestManager;
        mSubId = subId;
        mCreator = creator;
        mMmsConfigOverrides = configOverrides;
        mMmsConfig = null;
    }

    public int getSubId() {
        return mSubId;
    }

    private boolean ensureMmsConfigLoaded() {
        if (mMmsConfig == null) {
            // Not yet retrieved from mms config manager. Try getting it.
            final MmsConfig config = MmsConfigManager.getInstance().getMmsConfigBySubId(mSubId);
            if (config != null) {
                mMmsConfig = new MmsConfig.Overridden(config, mMmsConfigOverrides);
            }
        }
        return mMmsConfig != null;
    }

    private static boolean inAirplaneMode(final Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private static boolean isMobileDataEnabled(final Context context, final int subId) {
        final TelephonyManager telephonyManager =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.getDataEnabled(subId);
    }

    private static boolean isDataNetworkAvailable(final Context context, final int subId) {
        return !inAirplaneMode(context) && isMobileDataEnabled(context, subId);
    }

    /**
     * Execute the request
     *
     * @param context The context
     * @param networkManager The network manager to use
     */
    public void execute(Context context, MmsNetworkManager networkManager) {
        int result = SmsManager.MMS_ERROR_UNSPECIFIED;
        int httpStatusCode = 0;
        byte[] response = null;
        if (!ensureMmsConfigLoaded()) { // Check mms config
            Log.e(MmsService.TAG, "MmsRequest: mms config is not loaded yet");
            result = SmsManager.MMS_ERROR_CONFIGURATION_ERROR;
        } else if (!prepareForHttpRequest()) { // Prepare request, like reading pdu data from user
            Log.e(MmsService.TAG, "MmsRequest: failed to prepare for request");
            result = SmsManager.MMS_ERROR_IO_ERROR;
        } else if (!isDataNetworkAvailable(context, mSubId)) {
            Log.e(MmsService.TAG, "MmsRequest: in airplane mode or mobile data disabled");
            result = SmsManager.MMS_ERROR_NO_DATA_NETWORK;
        } else { // Execute
            long retryDelaySecs = 2;
            // Try multiple times of MMS HTTP request
            for (int i = 0; i < RETRY_TIMES; i++) {
                try {
                    networkManager.acquireNetwork();
                    final String apnName = networkManager.getApnName();
                    try {
                        ApnSettings apn = null;
                        try {
                            apn = ApnSettings.load(context, apnName, mSubId);
                        } catch (ApnException e) {
                            // If no APN could be found, fall back to trying without the APN name
                            if (apnName == null) {
                                // If the APN name was already null then don't need to retry
                                throw (e);
                            }
                            Log.i(MmsService.TAG, "MmsRequest: No match with APN name:"
                                    + apnName + ", try with no name");
                            apn = ApnSettings.load(context, null, mSubId);
                        }
                        Log.i(MmsService.TAG, "MmsRequest: using " + apn.toString());
                        response = doHttp(context, networkManager, apn);
                        result = Activity.RESULT_OK;
                        // Success
                        break;
                    } finally {
                        networkManager.releaseNetwork();
                    }
                } catch (ApnException e) {
                    Log.e(MmsService.TAG, "MmsRequest: APN failure", e);
                    result = SmsManager.MMS_ERROR_INVALID_APN;
                    break;
                } catch (MmsNetworkException e) {
                    Log.e(MmsService.TAG, "MmsRequest: MMS network acquiring failure", e);
                    result = SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS;
                    // Retry
                } catch (MmsHttpException e) {
                    Log.e(MmsService.TAG, "MmsRequest: HTTP or network I/O failure", e);
                    result = SmsManager.MMS_ERROR_HTTP_FAILURE;
                    httpStatusCode = e.getStatusCode();
                    // Retry
                } catch (Exception e) {
                    Log.e(MmsService.TAG, "MmsRequest: unexpected failure", e);
                    result = SmsManager.MMS_ERROR_UNSPECIFIED;
                    break;
                }
                try {
                    Thread.sleep(retryDelaySecs * 1000, 0/*nano*/);
                } catch (InterruptedException e) {}
                retryDelaySecs <<= 1;
            }
        }
        processResult(context, result, response, httpStatusCode);
    }

    /**
     * Process the result of the completed request, including updating the message status
     * in database and sending back the result via pending intents.
     *  @param context The context
     * @param result The result code of execution
     * @param response The response body
     * @param httpStatusCode The optional http status code in case of http failure
     */
    public void processResult(Context context, int result, byte[] response, int httpStatusCode) {
        final Uri messageUri = persistIfRequired(context, result, response);

        // Return MMS HTTP request result via PendingIntent
        final PendingIntent pendingIntent = getPendingIntent();
        if (pendingIntent != null) {
            boolean succeeded = true;
            // Extra information to send back with the pending intent
            Intent fillIn = new Intent();
            if (response != null) {
                succeeded = transferResponse(fillIn, response);
            }
            if (messageUri != null) {
                fillIn.putExtra("uri", messageUri.toString());
            }
            if (result == SmsManager.MMS_ERROR_HTTP_FAILURE && httpStatusCode != 0) {
                fillIn.putExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, httpStatusCode);
            }
            try {
                if (!succeeded) {
                    result = SmsManager.MMS_ERROR_IO_ERROR;
                }
                pendingIntent.send(context, result, fillIn);
            } catch (PendingIntent.CanceledException e) {
                Log.e(MmsService.TAG, "MmsRequest: sending pending intent canceled", e);
            }
        }

        revokeUriPermission(context);
    }

    /**
     * Returns true if sending / downloading using the carrier app has failed and completes the
     * action using platform API's, otherwise false.
     */
    protected boolean maybeFallbackToRegularDelivery(int carrierMessagingAppResult) {
        if (carrierMessagingAppResult
                == CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK
                || carrierMessagingAppResult
                        == CarrierMessagingService.DOWNLOAD_STATUS_RETRY_ON_CARRIER_NETWORK) {
            Log.d(MmsService.TAG, "Sending/downloading MMS by IP failed.");
            mRequestManager.addSimRequest(MmsRequest.this);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Converts from {@code carrierMessagingAppResult} to a platform result code.
     */
    protected static int toSmsManagerResult(int carrierMessagingAppResult) {
        switch (carrierMessagingAppResult) {
            case CarrierMessagingService.SEND_STATUS_OK:
                return Activity.RESULT_OK;
            case CarrierMessagingService.SEND_STATUS_RETRY_ON_CARRIER_NETWORK:
                return SmsManager.MMS_ERROR_RETRY;
            default:
                return SmsManager.MMS_ERROR_UNSPECIFIED;
        }
    }

    /**
     * Making the HTTP request to MMSC
     *
     * @param context The context
     * @param netMgr The current {@link MmsNetworkManager}
     * @param apn The APN setting
     * @return The HTTP response data
     * @throws MmsHttpException If any network error happens
     */
    protected abstract byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn)
            throws MmsHttpException;

    /**
     * @return The PendingIntent associate with the MMS sending invocation
     */
    protected abstract PendingIntent getPendingIntent();

    /**
     * @return The queue should be used by this request, 0 is sending and 1 is downloading
     */
    protected abstract int getQueueType();

    /**
     * Persist message into telephony if required (i.e. when auto-persisting is on or
     * the calling app is non-default sms app for sending)
     *
     * @param context The context
     * @param result The result code of execution
     * @param response The response body
     * @return The persisted URI of the message or null if we don't persist or fail
     */
    protected abstract Uri persistIfRequired(Context context, int result, byte[] response);

    /**
     * Prepare to make the HTTP request - will download message for sending
     * @return true if preparation succeeds (and request can proceed) else false
     */
    protected abstract boolean prepareForHttpRequest();

    /**
     * Transfer the received response to the caller
     *
     * @param fillIn the intent that will be returned to the caller
     * @param response the pdu to transfer
     * @return true if response transfer succeeds else false
     */
    protected abstract boolean transferResponse(Intent fillIn, byte[] response);

    /**
     * Revoke the content URI permission granted by the MMS app to the phone package.
     *
     * @param context The context
     */
    protected abstract void revokeUriPermission(Context context);

    /**
     * Base class for handling carrier app send / download result.
     */
    protected abstract class CarrierMmsActionCallback extends ICarrierMessagingCallback.Stub {
        @Override
        public void onSendSmsComplete(int result, int messageRef) {
            Log.e(MmsService.TAG, "Unexpected onSendSmsComplete call with result: " + result);
        }

        @Override
        public void onSendMultipartSmsComplete(int result, int[] messageRefs) {
            Log.e(MmsService.TAG, "Unexpected onSendMultipartSmsComplete call with result: "
                  + result);
        }

        @Override
        public void onFilterComplete(boolean keepMessage) {
            Log.e(MmsService.TAG, "Unexpected onFilterComplete call with result: " + keepMessage);
        }
    }
}
