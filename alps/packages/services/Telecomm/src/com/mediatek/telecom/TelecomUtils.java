
package com.mediatek.telecom;

import android.content.Context;
import android.content.Intent;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telecom.CallState;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.android.internal.telephony.PhoneConstants;
import com.android.server.telecom.Constants;
import com.android.server.telecom.Log;
import com.mediatek.common.dm.DmAgent;

import java.util.ArrayList;
import java.util.List;

public class TelecomUtils {

    private static final String TAG = TelecomUtils.class.getSimpleName();

    // Add temp feature option for ip dial.
    public static final boolean MTK_IP_PREFIX_SUPPORT = true;

    private Context mContext;


    public TelecomUtils(Context context) {
        mContext = context;
    }

    /*
     * M: get initial number from intent.
     */
    public static String getInitialNumber(Context context, Intent intent) {
        Log.d(TAG, "getInitialNumber(): " + intent);

        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) {
            return "";
        }

        // If the EXTRA_ACTUAL_NUMBER_TO_DIAL extra is present, get the phone
        // number from there.  (That extra takes precedence over the actual data
        // included in the intent.)
        if (intent.hasExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL)) {
            String actualNumberToDial =
                intent.getStringExtra(Constants.EXTRA_ACTUAL_NUMBER_TO_DIAL);
            return actualNumberToDial;
        }

        return PhoneNumberUtils.getNumberFromIntent(intent, context);
    }

    public static boolean isDMLocked() {
        boolean locked = false;
        try {
            IBinder binder = ServiceManager.getService("DmAgent");
            DmAgent agent = null;
            if (binder != null) {
                agent = DmAgent.Stub.asInterface(binder);
            }
            if (agent != null) {
                locked = agent.isLockFlagSet();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return locked;
    }

    /**
     * if the ip prefix is null for specified phone account,
     * we need to navigate to setting page for user to set a ip prefix.
     * @param context
     * @param account
     * @param isIpCall
     * @return if no ip prefix we return true to tell navigated or else false returned.
     */
    public static boolean startIpPrefixSetting(Context context, PhoneAccountHandle account, boolean isIpCall){
        if(!isIpCall || account == null || "E".equalsIgnoreCase(account.getId())) return false;
        String ipPrefix = getIpPrefix(context, account);
        if (TextUtils.isEmpty(ipPrefix)) {
            Log.d(TAG, " sub id  = " + account.getId());    
            final Intent intentSettings = new Intent(Intent.ACTION_MAIN);
            intentSettings.setClassName(Constants.PHONE_PACKAGE, Constants.IP_PREFIX_SETTING_CLASS_NAME);
            intentSettings.putExtra(PhoneConstants.SUBSCRIPTION_KEY, Integer.parseInt(account.getId()));
            intentSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intentSettings);
            return true;
        }
        return false;
    }

    /*
     * Get ip prefix from provider.
     */
    public static String getIpPrefix(Context context, PhoneAccountHandle account) {
        String ipPrefix = null;
        if(account != null && !"E".equalsIgnoreCase(account.getId())) {
            ipPrefix = Settings.System.getString(context.getContentResolver(),
                            "ipprefix" + account.getId());
        }
        Log.d(TAG, "ip prefix = " + ipPrefix);
        return ipPrefix;
    }

    /**
     * to check if the airplane mode is on or off.
     * @param ctx
     * @return boolean  true is on
     */
    public static boolean isAirPlaneModeOn(Context ctx) {
        int airplaneMode = Settings.Global.getInt(ctx.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0);
        if (airplaneMode == 0) {
            return false;
        } else {
            return true;
        }
    }

    /*
     * If ip prefix is not null, then return handl with ip prefix.
     * Else return original handle.
     */
    public static Uri getHandleWithIpPrefix(Context context,
            PhoneAccountHandle account, Uri handle) {
        String scheme = handle.getScheme();
        if (!scheme.equalsIgnoreCase(PhoneAccount.SCHEME_TEL)) return handle;

        String ipPrefix = TelecomUtils.getIpPrefix(context, account);
        String uriString = handle.getSchemeSpecificPart();
        Log.d(TAG, "ip prefix = " + ipPrefix + " phone number = " + uriString);
        if (!TextUtils.isEmpty(ipPrefix)) {
            if (uriString.indexOf(ipPrefix) < 0) {
                uriString = ipPrefix + filtCountryCode(context, uriString);
            }
            Log.d(TAG, " uri String = " + uriString);
        }
        handle = Uri.fromParts(scheme, uriString, handle.getFragment());
        Log.d(TAG, "handle with ip prefix = " + handle.toString());
        return handle;
    }

    /**
     * remove the country code from the number in international format.
     *
     * @param number
     * @return
     */
    private static String filtCountryCode(Context context, String number) {
        String countryIso = null;
        if (!TextUtils.isEmpty(number) && number.contains("+")) {
            try {
                CountryDetector mDetector = (CountryDetector) context
                        .getSystemService(Context.COUNTRY_DETECTOR);
                PhoneNumberUtil numUtil = PhoneNumberUtil.getInstance();
                if (mDetector != null && mDetector.detectCountry() != null) {
                    countryIso = mDetector.detectCountry().getCountryIso();
                } else {
                    countryIso = context.getResources().getConfiguration().locale
                            .getCountry();
                }
                PhoneNumber num = numUtil.parse(number, countryIso);
                return num == null ? number: String.valueOf(num
                        .getNationalNumber());
            } catch (NumberParseException e) {
                e.printStackTrace();
                Log.d(TAG, "parse phone number ... " + e);
            }
        }
        return number;
    }

    /**
     * Return a list of PhoneAccountHandles that are subscription/SIM accounts.
     * @return List<PhoneAccountHandle>
     */
    public List<PhoneAccountHandle> getSubscriptionPhoneAccounts() {
        final TelecomManager telecomManager = (TelecomManager) mContext
                .getSystemService(Context.TELECOM_SERVICE);

        List<PhoneAccountHandle> subscriptionAccountHandles = new ArrayList<PhoneAccountHandle>();
        List<PhoneAccountHandle> accountHandles = telecomManager.getCallCapablePhoneAccounts();
        for (PhoneAccountHandle accountHandle : accountHandles) {
            PhoneAccount account = telecomManager.getPhoneAccount(accountHandle);
            if (account.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)) {
                subscriptionAccountHandles.add(accountHandle);
            }
        }
        return subscriptionAccountHandles;
    }

    /**
     * Update default account handle when there has a valid suggested account
     * handle which not same with default.
     * @param extras The extra got from Intent.
     * @param accounts The all available accounts for current call.
     * @param defaultAccountHandle The default account handle.
     * @return newAccountHandle
     */
    public static boolean shouldShowAccountSuggestion(Bundle extras,
            List<PhoneAccountHandle> accounts, PhoneAccountHandle defaultAccountHandle) {
        boolean shouldShowAccountSuggestion = false;
        PhoneAccountHandle suggestedAccountHandle = getSuggestedPhoneAccountHandle(extras);

        if (accounts != null && defaultAccountHandle != null && suggestedAccountHandle != null
                && accounts.contains(suggestedAccountHandle)
                && !suggestedAccountHandle.equals(defaultAccountHandle)) {
            shouldShowAccountSuggestion = true;
        }
        Log.d(TAG, "shouldShowAccountSuggestion: " + shouldShowAccountSuggestion);
        return shouldShowAccountSuggestion;
    }

    /**
     * Added for suggesting phone account feature.
     * @param extras The extra got from Intent.
     * @param accounts The available PhoneAccounts.
     * @return The available suggested PhoneAccountHandle.
     */
    public static PhoneAccountHandle getSuggestedPhoneAccountHandle(Bundle extras) {
        PhoneAccountHandle suggestedAccountHandle = null;
        if (extras != null) {
            suggestedAccountHandle = extras
                    .getParcelable(TelecomManagerEx.EXTRA_SUGGESTED_PHONE_ACCOUNT_HANDLE);
        }
        Log.d(TAG, "Suggested PhoneAccountHandle is " + suggestedAccountHandle);
        return suggestedAccountHandle;
    }

    /**
     * original defined in CallsManager, we add it here for prevent MMI call when current is guest user
     * however, we still keep the original implementation in CallsManager.
     * @param handle
     * @return
     */
    public static boolean isPotentialMMICode(Uri handle) {
        return (handle != null && handle.getSchemeSpecificPart() != null
                && handle.getSchemeSpecificPart().contains("#"));
    }

}