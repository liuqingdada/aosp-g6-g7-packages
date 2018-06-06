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
import android.app.ActivityManagerNative;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.service.carrier.CarrierMessagingService;
import android.service.carrier.ICarrierMessagingCallback;
import android.service.carrier.ICarrierMessagingService;
import android.telephony.CarrierMessagingServiceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.mms.service.exception.MmsHttpException;

import com.google.android.mms.MmsException;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;
import com.google.android.mms.pdu.PduPersister;
import com.google.android.mms.pdu.RetrieveConf;
import com.google.android.mms.util.SqliteWrapper;

import java.util.List;

/**
 * Request to download an MMS
 */
public class DownloadRequest extends MmsRequest {
    private static final String LOCATION_SELECTION =
            Telephony.Mms.MESSAGE_TYPE + "=? AND " + Telephony.Mms.CONTENT_LOCATION + " =?";

    private final String mLocationUrl;
    private final PendingIntent mDownloadedIntent;
    private final Uri mContentUri;

    public DownloadRequest(RequestManager manager, int subId, String locationUrl,
            Uri contentUri, PendingIntent downloadedIntent, String creator,
            Bundle configOverrides) {
        super(manager, subId, creator, configOverrides);
        mLocationUrl = locationUrl;
        mDownloadedIntent = downloadedIntent;
        mContentUri = contentUri;
    }

    @Override
    protected byte[] doHttp(Context context, MmsNetworkManager netMgr, ApnSettings apn)
            throws MmsHttpException {
        final MmsHttpClient mmsHttpClient = netMgr.getOrCreateHttpClient();
        if (mmsHttpClient == null) {
            Log.e(MmsService.TAG, "MMS network is not ready!");
            throw new MmsHttpException(0/*statusCode*/, "MMS network is not ready");
        }
        return mmsHttpClient.execute(
                mLocationUrl,
                null/*pud*/,
                MmsHttpClient.METHOD_GET,
                apn.isProxySet(),
                apn.getProxyAddress(),
                apn.getProxyPort(),
                mMmsConfig);
    }

    @Override
    protected PendingIntent getPendingIntent() {
        return mDownloadedIntent;
    }

    @Override
    protected int getQueueType() {
        return MmsService.QUEUE_INDEX_DOWNLOAD;
    }

    @Override
    protected Uri persistIfRequired(Context context, int result, byte[] response) {
        // Let any mms apps running as secondary user know that a new mms has been downloaded.
        notifyOfDownload(context);

        if (!mRequestManager.getAutoPersistingPref() && !"com.android.mms".equals(mCreator)) {
            return null;
        }
        Log.d(MmsService.TAG, "DownloadRequest.persistIfRequired");
        if (response == null || response.length < 1) {
            Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: empty response");
            return null;
        }
        final long identity = Binder.clearCallingIdentity();
        try {
            final GenericPdu pdu =
                    (new PduParser(response, mMmsConfig.getSupportMmsContentDisposition())).parse();
            if (pdu == null || !(pdu instanceof RetrieveConf)) {
                Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: invalid parsed PDU");
                return null;
            }
            final RetrieveConf retrieveConf = (RetrieveConf) pdu;
            final int status = retrieveConf.getRetrieveStatus();
            if (status != PduHeaders.RETRIEVE_STATUS_OK) {
                Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: retrieve failed "
                        + status);
                // Update the retrieve status of the NotificationInd
                final ContentValues values = new ContentValues(1);
                values.put(Telephony.Mms.RETRIEVE_STATUS, status);
                SqliteWrapper.update(
                        context,
                        context.getContentResolver(),
                        Telephony.Mms.CONTENT_URI,
                        values,
                        LOCATION_SELECTION,
                        new String[] {
                                Integer.toString(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND),
                                mLocationUrl
                        });
                return null;
            }
            // Store the downloaded message
            final PduPersister persister = PduPersister.getPduPersister(context);
            final Uri messageUri = persister.persist(
                    pdu,
                    Telephony.Mms.Inbox.CONTENT_URI,
                    true/*createThreadId*/,
                    true/*groupMmsEnabled*/,
                    null/*preOpenedFiles*/);
            if (messageUri == null) {
                Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: can not persist message");
                return null;
            }
            // Update some of the properties of the message
            final ContentValues values = new ContentValues();
            values.put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L);
            values.put(Telephony.Mms.READ, 0);
            values.put(Telephony.Mms.SEEN, 0);
            if (!TextUtils.isEmpty(mCreator)) {
                values.put(Telephony.Mms.CREATOR, mCreator);
            }
            values.put(Telephony.Mms.SUBSCRIPTION_ID, mSubId);
            if (SqliteWrapper.update(
                    context,
                    context.getContentResolver(),
                    messageUri,
                    values,
                    null/*where*/,
                    null/*selectionArg*/) != 1) {
                Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: can not update message");
            }
            // Delete the corresponding NotificationInd
            SqliteWrapper.delete(context,
                    context.getContentResolver(),
                    Telephony.Mms.CONTENT_URI,
                    LOCATION_SELECTION,
                    new String[]{
                            Integer.toString(PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND),
                            mLocationUrl
                    });

            return messageUri;
        } catch (MmsException e) {
            Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: can not persist message", e);
        } catch (SQLiteException e) {
            Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: can not update message", e);
        } catch (RuntimeException e) {
            Log.e(MmsService.TAG, "DownloadRequest.persistIfRequired: can not parse response", e);
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
        return null;
    }

    private void notifyOfDownload(Context context) {
        final Intent intent = new Intent(Telephony.Sms.Intents.MMS_DOWNLOADED_ACTION);
        intent.addFlags(Intent.FLAG_RECEIVER_NO_ABORT);

        // Get a list of currently started users.
        int[] users = null;
        try {
            users = ActivityManagerNative.getDefault().getRunningUserIds();
        } catch (RemoteException re) {
        }
        if (users == null) {
            users = new int[] {UserHandle.ALL.getIdentifier()};
        }
        final UserManager userManager =
                (UserManager) context.getSystemService(Context.USER_SERVICE);

        // Deliver the broadcast only to those running users that are permitted
        // by user policy.
        for (int i = users.length - 1; i >= 0; i--) {
            UserHandle targetUser = new UserHandle(users[i]);
            if (users[i] != UserHandle.USER_OWNER) {
                // Is the user not allowed to use SMS?
                if (userManager.hasUserRestriction(UserManager.DISALLOW_SMS, targetUser)) {
                    continue;
                }
                // Skip unknown users and managed profiles as well
                UserInfo info = userManager.getUserInfo(users[i]);
                if (info == null || info.isManagedProfile()) {
                    continue;
                }
            }
            context.sendOrderedBroadcastAsUser(intent, targetUser,
                    android.Manifest.permission.RECEIVE_MMS,
                    AppOpsManager.OP_RECEIVE_MMS,
                    null,
                    null, Activity.RESULT_OK, null, null);
        }
    }

    /**
     * Transfer the received response to the caller (for download requests write to content uri)
     *
     * @param fillIn the intent that will be returned to the caller
     * @param response the pdu to transfer
     */
    @Override
    protected boolean transferResponse(Intent fillIn, final byte[] response) {
        return mRequestManager.writePduToContentUri(mContentUri, response);
    }

    @Override
    protected boolean prepareForHttpRequest() {
        return true;
    }

    /**
     * Try downloading via the carrier app.
     *
     * @param context The context
     * @param carrierMessagingServicePackage The carrier messaging service handling the download
     */
    public void tryDownloadingByCarrierApp(Context context, String carrierMessagingServicePackage) {
        final CarrierDownloadManager carrierDownloadManger = new CarrierDownloadManager();
        final CarrierDownloadCompleteCallback downloadCallback =
                new CarrierDownloadCompleteCallback(context, carrierDownloadManger);
        carrierDownloadManger.downloadMms(context, carrierMessagingServicePackage,
                downloadCallback);
    }

    @Override
    protected void revokeUriPermission(Context context) {
        context.revokeUriPermission(mContentUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }

    /**
     * Downloads the MMS through through the carrier app.
     */
    private final class CarrierDownloadManager extends CarrierMessagingServiceManager {
        // Initialized in downloadMms
        private volatile CarrierDownloadCompleteCallback mCarrierDownloadCallback;

        void downloadMms(Context context, String carrierMessagingServicePackage,
                CarrierDownloadCompleteCallback carrierDownloadCallback) {
            mCarrierDownloadCallback = carrierDownloadCallback;
            if (bindToCarrierMessagingService(context, carrierMessagingServicePackage)) {
                Log.v(MmsService.TAG, "bindService() for carrier messaging service succeeded");
            } else {
                Log.e(MmsService.TAG, "bindService() for carrier messaging service failed");
                carrierDownloadCallback.onDownloadMmsComplete(
                        CarrierMessagingService.DOWNLOAD_STATUS_RETRY_ON_CARRIER_NETWORK);
            }
        }

        @Override
        protected void onServiceReady(ICarrierMessagingService carrierMessagingService) {
            try {
                carrierMessagingService.downloadMms(mContentUri, mSubId, Uri.parse(mLocationUrl),
                        mCarrierDownloadCallback);
            } catch (RemoteException e) {
                Log.e(MmsService.TAG,
                        "Exception downloading MMS using the carrier messaging service: " + e);
                mCarrierDownloadCallback.onDownloadMmsComplete(
                        CarrierMessagingService.DOWNLOAD_STATUS_RETRY_ON_CARRIER_NETWORK);
            }
        }
    }

    /**
     * A callback which notifies carrier messaging app send result. Once the result is ready, the
     * carrier messaging service connection is disposed.
     */
    private final class CarrierDownloadCompleteCallback extends
            MmsRequest.CarrierMmsActionCallback {
        private final Context mContext;
        private final CarrierDownloadManager mCarrierDownloadManager;

        public CarrierDownloadCompleteCallback(Context context,
                CarrierDownloadManager carrierDownloadManager) {
            mContext = context;
            mCarrierDownloadManager = carrierDownloadManager;
        }

        @Override
        public void onSendMmsComplete(int result, byte[] sendConfPdu) {
            Log.e(MmsService.TAG, "Unexpected onSendMmsComplete call with result: " + result);
        }

        @Override
        public void onDownloadMmsComplete(int result) {
            Log.d(MmsService.TAG, "Carrier app result for download: " + result);
            mCarrierDownloadManager.disposeConnection(mContext);

            if (!maybeFallbackToRegularDelivery(result)) {
                processResult(mContext, toSmsManagerResult(result), null/* response */,
                        0/* httpStatusCode */);
            }
        }
    }
}
