/*
* Copyright (C) 2011-2014 MediaTek Inc.
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

package com.android.services.telephony;

/// M: for VoLTE enhanced conference call. @{
import android.telecom.Conference;
import android.telecom.ConnectionRequest;
/// @}

import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.SubscriptionController;


import com.android.internal.telephony.gsm.GsmMmiCode;
import com.android.internal.telephony.cdma.CdmaMmiCode;
import com.android.internal.telephony.uicc.UiccCardApplication;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.cdma.CDMAPhone;
import com.android.internal.telephony.gsm.GSMPhone;
import android.os.SystemProperties;

import java.util.List;
import java.util.ArrayList;

/* M: Get the caller info, add for OP01 Plug in. @{ */
import com.android.internal.telephony.Call;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.PhoneProxy;
/** @} */

/**
 * Service for making GSM and CDMA connections.
 */
public class TelephonyConnectionServiceUtil {

    private static final TelephonyConnectionServiceUtil INSTANCE = new TelephonyConnectionServiceUtil();
    private TelephonyConnectionService mService;

    TelephonyConnectionServiceUtil() {
        mService = null;
    }

    public static TelephonyConnectionServiceUtil getInstance() {
        return INSTANCE;
    }

    public void setService(TelephonyConnectionService s) {
        Log.d(this, "setService: " + s);
        mService = s;
    }

    /**
     * unset TelephonyConnectionService to be bind.
     */
    public void unsetService() {
        Log.d(this, "unSetService: " + mService);
        mService = null;
    }

    /**
     * Force Supplementary Message update once TelephonyConnection is created.
     * @param conn The connection to update supplementary messages.
     */
    public void forceSuppMessageUpdate(TelephonyConnection conn) {
        Log.d(this, "forceSuppMessageUpdate NOT supported for BSP");
    }
    /// @}

    public boolean isDualtalk() {
        return (TelephonyManager.getDefault().getMultiSimConfiguration()
            == TelephonyManager.MultiSimVariants.DSDA);
    }

    public boolean isECCExists() {

        if (mService == null) {
            // it means that never a call exist
            // so still not register in telephonyConnectionService
            // ECC doesn't exist
            return false;
        }

        if (mService.getFgConnection() == null) {
            return false;
        }
        if (mService.getFgConnection().getCall() == null ||
            mService.getFgConnection().getCall().getEarliestConnection() == null) {
            return false;
        }

        String activeCallAddress = mService.getFgConnection().getCall().
                getEarliestConnection().getAddress();

        boolean bECCExists;

        bECCExists = (PhoneNumberUtils.isEmergencyNumber(activeCallAddress)
                     && !PhoneNumberUtils.isSpecialEmergencyNumber(activeCallAddress));

        if (bECCExists) {
            Log.d(this, "ECC call exists.");
        }
        else {
            Log.d(this, "ECC call doesn't exists.");
        }

        return bECCExists;
    }

    public boolean isUssdNumber(Phone phone, String dialString) {
        boolean bIsUssdNumber = false;
        PhoneProxy phoneProxy = (PhoneProxy) phone;
        int slot = SubscriptionController.getInstance().getSlotId(phone.getSubId());

        if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_GSM) {
            UiccCardApplication cardApp = UiccController.getInstance().
                    getUiccCardApplication(slot, UiccController.APP_FAM_3GPP);
            Log.d(this, "isUssdNumber [UiccCardApplication]cardApp " + cardApp);

            GSMPhone gsmPhone = (GSMPhone) phoneProxy.getActivePhone();
            bIsUssdNumber = GsmMmiCode.isUssdNumber(dialString, gsmPhone, cardApp);
        } else if (phone.getPhoneType() == PhoneConstants.PHONE_TYPE_CDMA) {
            UiccCardApplication cardApp = UiccController.getInstance().
                    getUiccCardApplication(slot, UiccController.APP_FAM_3GPP2);
            Log.d(this, "isUssdNumber [UiccCardApplication]cardApp " + cardApp);

            CDMAPhone cdmaPhone = (CDMAPhone) phoneProxy.getActivePhone();
            CdmaMmiCode cdmaMmiCode = CdmaMmiCode.newFromDialString(dialString, cdmaPhone, cardApp);
            if (cdmaMmiCode != null) {
                bIsUssdNumber = cdmaMmiCode.isUssdRequest();
            }
        }

        Log.d(this, "isUssdNumber = " + bIsUssdNumber);
        return bIsUssdNumber;
    }

    /// M: CC032: Proprietary incoming call handling @{
    public void setIncomingCallIndicationResponse(GSMPhone phone) {

        if (mService == null) {
            // it means that never a call exist
            // so still not register in telephonyConnectionService
            // Accept the MT
            phone.setIncomingCallIndicationResponse(true);
            return;
        }

        boolean isRejectNewRingCall = false;
        boolean isECCExists = isECCExists();

        if (!isDualtalk()) {
            if (mService.getRingingCallCount() > 0) {
                isRejectNewRingCall = true;
            }
        } else {
            if (mService.getRingingCallCount() > 1) {
                isRejectNewRingCall = true;
            }
        }

        // only gsmphone sends this event
        if (isECCExists || isRejectNewRingCall) {
            phone.setIncomingCallIndicationResponse(false);
        } else {
            phone.setIncomingCallIndicationResponse(true);
        }
    }
    /// @}

    /// M: CC021: Error message due to CellConnMgr checking @{
    public boolean cellConnMgrShowAlerting(int subId) {
            Log.d(this, "cellConnMgrShowAlerting NOT supported for BSP");
            return false;
    }
    /// @}

   /// M: CC033: OP01 Plugin in to block incoming call from black number @{
   /**
    * M: add for OP01 plug in. To check whether it is a black number,
    * if so need to block it and log it as rejected call.
    */
    public boolean shouldBlockNumber(PhoneProxy phoneProxy, Call call, Connection connection) {
        Log.d(this, "shouldBlockNumber NOT supported for BSP");
        return false;
    }
    /// @}

    /// M: CC022: Error message due to VoLTE SS checking @{
    /**
     * This function used to check whether we should notify user to open data connection.
     * For now, we judge certain mmi code + "IMS-phoneAccount" + data connection is off.
     * @param number The number to dial
     * @param phone The target phone
     * @return {@code true} if the notification should pop up and {@code false} otherwise.
     */
    public boolean shouldOpenDataConnection(String number,  Phone phone) {
        Log.d(this, "shouldOpenDataConnection NOT supported for BSP");
        return false;
    }
    /// @}

    /// M: CC077: 2/3G CAPABILITY_HIGH_DEF_AUDIO @{
    /**
     * This function used to check whether the input value is of HD type.
     * @param value The speech codec type value
     * @return {@code true} if the codec type is of HD type and {@code false} otherwise.
     */
    public boolean isHighDefAudio(int value) {
        Log.d(this, "isHighDefAudio NOT supported for BSP");
        return false;
    }
    /// @}

    /// M: for VoLTE Conference. @{
    boolean isVoLTEConferenceFull(ImsConferenceController imsConfController) {
        Log.d(this, "isVoLTEConferenceFull NOT supported for BSP");
        return false;
    }

    boolean canHoldImsConference(ImsConference conference) {
        Log.d(this, "canHoldImsConference ALWAYS supported for BSP");
        return true;
    }

    boolean canUnHoldImsConference(ImsConference conference) {
        Log.d(this, "canUnHoldImsConference ALWAYS supported for BSP");
        return true;
    }
    /// @}

    /// M: For VoLTE enhanced conference call. @{
    /**
     * This can be used by telecom to either create a new outgoing conference call or attach
     * to an existing incoming conference call.
     */
    Conference createConference(
            ImsConferenceController imsConfController,
            Phone phone,
            final ConnectionRequest request,
            final List<String> numbers,
            boolean isIncoming) {
        Log.d(this, "createConference NOT supported for BSP");
        return null;
    }

    Conference createFailedConference(int disconnectCause, String reason) {
        return createFailedConference(
            DisconnectCauseUtil.toTelecomDisconnectCause(disconnectCause, reason));
    }

    Conference createFailedConference(android.telecom.DisconnectCause disconnectCause) {
        Conference failedConference = new Conference(null) { };
        failedConference.setDisconnected(disconnectCause);
        return failedConference;
    }
    /// @}

    /// M: Add for GSM+CDMA ecc. @{
    /**
     * Checked if the ecc request need to handle by internal rules.
     * @param request The connection request.
     * @param number The ecc number.
     * @return A null object for BSP package.
     */
    public Phone selectPhoneBySpecialEccRule(ConnectionRequest request, String number) {
        return null;
    }
    /// @}
}
    
