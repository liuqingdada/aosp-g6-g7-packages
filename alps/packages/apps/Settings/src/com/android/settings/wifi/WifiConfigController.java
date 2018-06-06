/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.net.wifi.WifiConfiguration.INVALID_NETWORK_ID;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.ProxyInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiConfiguration.PairwiseCipher;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiEnterpriseConfig.Phase2;
import android.net.wifi.WifiConfiguration.Protocol;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.security.Credentials;
import android.security.KeyStore;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.ProxySelector;
import com.android.settings.R;

import com.mediatek.settings.FeatureOption;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.util.Iterator;

/**
 * The class for allowing UIs like {@link WifiDialog} and {@link WifiConfigUiBase} to
 * share the logic for controlling buttons, text fields, etc.
 */
public class WifiConfigController implements TextWatcher,
       AdapterView.OnItemSelectedListener, OnCheckedChangeListener {
    private static final String TAG = "WifiConfigController";

    private final WifiConfigUiBase mConfigUi;
    private final View mView;
    private final AccessPoint mAccessPoint;

    /* This value comes from "wifi_ip_settings" resource array */
    private static final int DHCP = 0;
    private static final int STATIC_IP = 1;

    /* These values come from "wifi_proxy_settings" resource array */
    public static final int PROXY_NONE = 0;
    public static final int PROXY_STATIC = 1;
    public static final int PROXY_PAC = 2;

    /* These values come from "wifi_eap_method" resource array */
    public static final int WIFI_EAP_METHOD_PEAP = 0;
    public static final int WIFI_EAP_METHOD_TLS  = 1;
    public static final int WIFI_EAP_METHOD_TTLS = 2;
    public static final int WIFI_EAP_METHOD_PWD  = 3;
    public static final int WIFI_EAP_METHOD_FAST = 6;
    /// M: sim/aka @{
    public static final int WIFI_EAP_METHOD_SIM = 4;
    public static final int WIFI_EAP_METHOD_AKA = 5;
    /// @}
    private static final int BUFFER_LENGTH = 40;
    private static final int MNC_SUB_BEG = 3;
    private static final int MNC_SUB_END = 5;
    private static final int MCC_SUB_BEG = 0;
    private static final int MCC_MNC_LENGTH = 5;

    /* These values come from "wifi_peap_phase2_entries" resource array */
    public static final int WIFI_PEAP_PHASE2_NONE 	    = 0;
    public static final int WIFI_PEAP_PHASE2_MSCHAPV2 	= 1;
    public static final int WIFI_PEAP_PHASE2_GTC        = 2;
    
    /* EAP-FAST */
    private static final String FAST_PAC_FILE = "/data/misc/wifi/wpa_supplicant.eap-fast-pac";
    private static final String FAST_PHASE1 = "fast_provisioning=2";

    /* Phase2 methods supported by PEAP are limited */
    private final ArrayAdapter<String> PHASE2_PEAP_ADAPTER;
    /* Full list of phase2 methods */
    private final ArrayAdapter<String> PHASE2_FULL_ADAPTER;

    // True when this instance is used in SetupWizard XL context.
    private final boolean mInXlSetupWizard;

    private final Handler mTextViewChangedHandler;

    // e.g. AccessPoint.SECURITY_NONE
    private int mAccessPointSecurity;
    private TextView mPasswordView;

    private String unspecifiedCert = "unspecified";
    private static final int unspecifiedCertIndex = 0;

    private Spinner mSecuritySpinner;
    private Spinner mEapMethodSpinner;
    private Spinner mEapCaCertSpinner;
    private Spinner mPhase2Spinner;
    // Associated with mPhase2Spinner, one of PHASE2_FULL_ADAPTER or PHASE2_PEAP_ADAPTER
    private ArrayAdapter<String> mPhase2Adapter;
    private Spinner mEapUserCertSpinner;
    private TextView mEapIdentityView;
    private TextView mEapAnonymousView;

    private Spinner mIpSettingsSpinner;
    private TextView mIpAddressView;
    private TextView mGatewayView;
    private TextView mNetworkPrefixLengthView;
    private TextView mDns1View;
    private TextView mDns2View;

    private Spinner mProxySettingsSpinner;
    private TextView mProxyHostView;
    private TextView mProxyPortView;
    private TextView mProxyExclusionListView;
    private TextView mProxyPacView;

    /// M: add for EAP_SIM/AKA @{
    private Spinner mSimSlot;
    private TelephonyManager mTm;
    /// @}

    /// M: add for WAPI @{
    private Spinner mWapiAsCert;
    private Spinner mWapiClientCert;
    private boolean mHex;
    private static final String WLAN_PROP_KEY = "persist.sys.wlan";
    private static final String WIFI = "wifi";
    private static final String WAPI = "wapi";
    private static final String WIFI_WAPI = "wifi-wapi";
    private static final String DEFAULT_WLAN_PROP = WIFI_WAPI;
    private static final int SSID_MAX_LEN = 32;
    /// @}

    private IpAssignment mIpAssignment = IpAssignment.UNASSIGNED;
    private ProxySettings mProxySettings = ProxySettings.UNASSIGNED;
    private ProxyInfo mHttpProxy = null;
    private StaticIpConfiguration mStaticIpConfiguration = null;

    private String[] mLevels;
    private boolean mEdit;
    private TextView mSsidView;

    private Context mContext;

    public WifiConfigController(
            WifiConfigUiBase parent, View view, AccessPoint accessPoint, boolean edit) {
        mConfigUi = parent;
        mInXlSetupWizard = (parent instanceof WifiConfigUiForSetupWizardXL);

        mView = view;
        mAccessPoint = accessPoint;
        mAccessPointSecurity = (accessPoint == null) ? AccessPoint.SECURITY_NONE :
                accessPoint.security;
        mEdit = edit;

        mTextViewChangedHandler = new Handler();
        mContext = mConfigUi.getContext();
        final Resources res = mContext.getResources();

        /// M: get telephonyManager @{
        if(FeatureOption.MTK_EAP_SIM_AKA){
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }
        /// @}

        mLevels = res.getStringArray(R.array.wifi_signal);
        PHASE2_PEAP_ADAPTER = new ArrayAdapter<String>(
            mContext, android.R.layout.simple_spinner_item,
            res.getStringArray(R.array.wifi_peap_phase2_entries));
        PHASE2_PEAP_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        PHASE2_FULL_ADAPTER = new ArrayAdapter<String>(
                mContext, android.R.layout.simple_spinner_item,
                res.getStringArray(R.array.wifi_phase2_entries));
        PHASE2_FULL_ADAPTER.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        unspecifiedCert = mContext.getString(R.string.wifi_unspecified);
        mIpSettingsSpinner = (Spinner) mView.findViewById(R.id.ip_settings);
        mIpSettingsSpinner.setOnItemSelectedListener(this);
        mProxySettingsSpinner = (Spinner) mView.findViewById(R.id.proxy_settings);
        mProxySettingsSpinner.setOnItemSelectedListener(this);

        if (mAccessPoint == null) { // new network
            mConfigUi.setTitle(R.string.wifi_add_network);

            mSsidView = (TextView) mView.findViewById(R.id.ssid);
            mSsidView.addTextChangedListener(this);
            mSecuritySpinner = ((Spinner) mView.findViewById(R.id.security));
            mSecuritySpinner.setOnItemSelectedListener(this);
            if (mInXlSetupWizard) {
                mView.findViewById(R.id.type_ssid).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.type_security).setVisibility(View.VISIBLE);
                // We want custom layout. The content must be same as the other cases.

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(mContext,
                        R.layout.wifi_setup_custom_list_item_1, android.R.id.text1,
                        res.getStringArray(R.array.wifi_security_no_eap));
                mSecuritySpinner.setAdapter(adapter);
            } else {
                mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
            }
            /// M: set array for wifi security @{
            int viewId = R.id.security;
            Log.d(TAG, "FeatureOption.MTK_WAPI_SUPPORT = " + FeatureOption.MTK_WAPI_SUPPORT);
            if (FeatureOption.MTK_WAPI_SUPPORT) {
                viewId = R.id.wifi_security_wfa;  
            }
            switchWlanSecuritySpinner((Spinner) mView.findViewById(viewId));
            /// @}

            showIpConfigFields();
            showProxyFields();
            mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
            ((CheckBox)mView.findViewById(R.id.wifi_advanced_togglebox))
                    .setOnCheckedChangeListener(this);


            mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
        } else {
            mConfigUi.setTitle(mAccessPoint.ssid);

            ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);

            boolean showAdvancedFields = false;
            if (mAccessPoint.networkId != INVALID_NETWORK_ID) {
                WifiConfiguration config = mAccessPoint.getConfig();
                if (config.getIpAssignment() == IpAssignment.STATIC) {
                    mIpSettingsSpinner.setSelection(STATIC_IP);
                    showAdvancedFields = true;
                    // Display IP address.
                    StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                    if (staticConfig != null && staticConfig.ipAddress != null) {
                        addRow(group, R.string.wifi_ip_address,
                           staticConfig.ipAddress.getAddress().getHostAddress());
                    }
                } else {
                    mIpSettingsSpinner.setSelection(DHCP);
                }


                if (config.getProxySettings() == ProxySettings.STATIC) {
                    mProxySettingsSpinner.setSelection(PROXY_STATIC);
                    showAdvancedFields = true;
                } else if (config.getProxySettings() == ProxySettings.PAC) {
                    mProxySettingsSpinner.setSelection(PROXY_PAC);
                    showAdvancedFields = true;
                } else {
                    mProxySettingsSpinner.setSelection(PROXY_NONE);
                }
            }

            if ((mAccessPoint.networkId == INVALID_NETWORK_ID && !mAccessPoint.isActive())
                    || mEdit) {
                showSecurityFields();
                showIpConfigFields();
                showProxyFields();
                mView.findViewById(R.id.wifi_advanced_toggle).setVisibility(View.VISIBLE);
                ((CheckBox)mView.findViewById(R.id.wifi_advanced_togglebox))
                    .setOnCheckedChangeListener(this);
                if (showAdvancedFields) {
                    ((CheckBox)mView.findViewById(R.id.wifi_advanced_togglebox)).setChecked(true);
                    mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);
                }
            }

            if (mEdit) {
                mConfigUi.setSubmitButton(res.getString(R.string.wifi_save));
            } else {
                final DetailedState state = mAccessPoint.getState();
                final String signalLevel = getSignalString();

                if (state == null && signalLevel != null) {
                    mConfigUi.setSubmitButton(res.getString(R.string.wifi_connect));
                } else {
                    if (state != null) {
                        addRow(group, R.string.wifi_status, Summary.get(mConfigUi.getContext(),
                                state, mAccessPoint.networkId ==
                                WifiConfiguration.INVALID_NETWORK_ID));
                    }

                    if (signalLevel != null) {
                        addRow(group, R.string.wifi_signal, signalLevel);
                    }

                    WifiInfo info = mAccessPoint.getInfo();
                    if (info != null && info.getLinkSpeed() != -1) {
                        addRow(group, R.string.wifi_speed, info.getLinkSpeed()
                                + WifiInfo.LINK_SPEED_UNITS);
                    }

                    if (info != null && info.getFrequency() != -1) {
                        final int frequency = info.getFrequency();
                        String band = null;

                        if (frequency >= AccessPoint.LOWER_FREQ_24GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_24GHZ) {
                            band = res.getString(R.string.wifi_band_24ghz);
                        } else if (frequency >= AccessPoint.LOWER_FREQ_5GHZ
                                && frequency < AccessPoint.HIGHER_FREQ_5GHZ) {
                            band = res.getString(R.string.wifi_band_5ghz);
                        } else {
                            Log.e(TAG, "Unexpected frequency " + frequency);
                        }
                        if (band != null) {
                            addRow(group, R.string.wifi_frequency, band);
                        }
                    }

                    addRow(group, R.string.wifi_security, mAccessPoint.getSecurityString(false));
                    mView.findViewById(R.id.ip_fields).setVisibility(View.GONE);
                }
                if ((mAccessPoint.networkId != INVALID_NETWORK_ID || mAccessPoint.isActive())
                         && ActivityManager.getCurrentUser() == UserHandle.USER_OWNER) {
                    mConfigUi.setForgetButton(res.getString(R.string.wifi_forget));
                }
            }
        }

        if ((mEdit) || (mAccessPoint != null
                && mAccessPoint.getState() == null && mAccessPoint.getLevel() != -1)){
            mConfigUi.setCancelButton(res.getString(R.string.wifi_cancel));
        }else{
            mConfigUi.setCancelButton(res.getString(R.string.wifi_display_options_done));
        }
        if (mConfigUi.getSubmitButton() != null) {
            enableSubmitIfAppropriate();
        }
    }
    
    /**
     * M: make NAI
     * @param simOperator mnc+mcc
     * @param imsi eapMethod
     * @return the string of NAI
     */
    public static String makeNAI(String simOperator, String imsi, String eapMethod) {

          // airplane mode & select wrong sim slot
          if (imsi == null) {
                return addQuote("error");
          }

          StringBuffer NAI = new StringBuffer(BUFFER_LENGTH);
          // s = sb.append("a = ").append(a).append("!").toString();
          System.out.println("".length());

          if (eapMethod.equals("SIM")) {
                NAI.append("1");
          } else if (eapMethod.equals("AKA")) {
                NAI.append("0");
          }

          // add imsi
          NAI.append(imsi);
          NAI.append("@wlan.mnc");
          // add mnc
          // for some operator
          Log.i(TAG, "simOperator = " + simOperator);
          if (simOperator.length() == MCC_MNC_LENGTH) {
              NAI.append("0");
              NAI.append(imsi.substring(MNC_SUB_BEG, MNC_SUB_END));
          } else {
              NAI.append(imsi.substring(MNC_SUB_BEG, MNC_SUB_END + 1));
          }
          NAI.append(".mcc");
          // add mcc
          NAI.append(imsi.substring(MCC_SUB_BEG, MNC_SUB_BEG));

          // NAI.append(imsi.substring(5));
          NAI.append(".3gppnetwork.org");
          Log.d(TAG, NAI.toString());
          Log.d(TAG, "\"" + NAI.toString() + "\"");
          return addQuote(NAI.toString());
    }

    /**
     * M: add quote for strings
     * @param string
     * @return add quote to the string
     */
    public static String addQuote(String s) {
          return "\"" + s + "\"";
    }


    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }

    private String getSignalString(){
        final int level = mAccessPoint.getLevel();

        return (level > -1 && level < mLevels.length) ? mLevels[level] : null;
    }

    void hideSubmitButton() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        submit.setVisibility(View.GONE);
    }

    /* show submit button if password, ip and proxy settings are valid */
    void enableSubmitIfAppropriate() {
        Button submit = mConfigUi.getSubmitButton();
        if (submit == null) return;

        boolean enabled = false;
        boolean passwordInvalid = false;

        if (mPasswordView != null &&
            ((mAccessPointSecurity == AccessPoint.SECURITY_WEP && mPasswordView.length() == 0) ||
            (mAccessPointSecurity == AccessPoint.SECURITY_PSK && mPasswordView.length() < 8) ||
            (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_PSK && (mPasswordView.length() < 8
                    || 64 < mPasswordView.length() || (mHex && !mPasswordView
                            .getText().toString().matches("[0-9A-Fa-f]*")))))) {
            passwordInvalid = true;
        }
        /// M: verify WAPI information @{
        if (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_CERT
                    && (mWapiAsCert != null
                                && mWapiAsCert.getSelectedItemPosition() == 0 || mWapiClientCert != null
                                && mWapiClientCert.getSelectedItemPosition() == 0)) {
              passwordInvalid = true;
        }
        /// @}

        if ((mSsidView != null && mSsidView.length() == 0) ||
            ((mAccessPoint == null || mAccessPoint.networkId == INVALID_NETWORK_ID) &&
            passwordInvalid)) {
            enabled = false;
        } else {
            if (ipAndProxyFieldsAreValid()) {
                enabled = true;
            } else {
                enabled = false;
            }
        }
        submit.setEnabled(enabled);
    }

    /* package */ WifiConfiguration getConfig() {
        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID && !mEdit) {
            return null;
        }

        WifiConfiguration config = new WifiConfiguration();
        /// M: init eap information @{
        if (FeatureOption.MTK_EAP_SIM_AKA) {
              config.imsi = addQuote("none");
              config.simSlot = addQuote("-1");
              config.pcsc = addQuote("none");
        }
        /// @}

        if (mAccessPoint == null) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mSsidView.getText().toString());
            // If the user adds a network manually, assume that it is hidden.
            config.hiddenSSID = true;
        } else if (mAccessPoint.networkId == INVALID_NETWORK_ID) {
            config.SSID = AccessPoint.convertToQuotedString(
                    mAccessPoint.ssid);
        } else {
            config.networkId = mAccessPoint.networkId;
        }

        switch (mAccessPointSecurity) {
            case AccessPoint.SECURITY_NONE:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                break;

            case AccessPoint.SECURITY_WEP:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.SHARED);
                if (mPasswordView.length() != 0) {
                    int length = mPasswordView.length();
                    String password = mPasswordView.getText().toString();
                    // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                    if ((length == 10 || length == 26 || length == 58) &&
                            password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = password;
                    } else {
                        config.wepKeys[0] = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_PSK:
                config.allowedKeyManagement.set(KeyMgmt.WPA_PSK);
                if (mPasswordView.length() != 0) {
                    String password = mPasswordView.getText().toString();
                    if (password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = password;
                    } else {
                        config.preSharedKey = '"' + password + '"';
                    }
                }
                break;

            case AccessPoint.SECURITY_EAP:
                config.allowedKeyManagement.set(KeyMgmt.WPA_EAP);
                config.allowedKeyManagement.set(KeyMgmt.IEEE8021X);
                config.enterpriseConfig = new WifiEnterpriseConfig();
                int eapMethod = convertEapMethod(mEapMethodSpinner.getSelectedItemPosition(), 1);
                int phase2Method = mPhase2Spinner.getSelectedItemPosition();
                config.enterpriseConfig.setEapMethod(eapMethod);
                //config.eap.setValue((String) mEapMethodSpinner.getSelectedItem());

                if (!"AKA".equals((String) mEapMethodSpinner.getSelectedItem())
                              && !"SIM".equals((String) mEapMethodSpinner.getSelectedItem())) {
                switch (eapMethod) {
                    case Eap.FAST:
                        config.pacFile = addQuote(FAST_PAC_FILE);
                        config.phase1 = addQuote(FAST_PHASE1);
                    case Eap.PEAP:
                        // PEAP supports limited phase2 values
                        // Map the index from the PHASE2_PEAP_ADAPTER to the one used
                        // by the API which has the full list of PEAP methods.
                        switch(phase2Method) {
                            case WIFI_PEAP_PHASE2_NONE:
                                config.enterpriseConfig.setPhase2Method(Phase2.NONE);
                                break;
                            case WIFI_PEAP_PHASE2_MSCHAPV2:
                                config.enterpriseConfig.setPhase2Method(Phase2.MSCHAPV2);
                                break;
                            case WIFI_PEAP_PHASE2_GTC:
                                config.enterpriseConfig.setPhase2Method(Phase2.GTC);
                                break;
                            default:
                                Log.e(TAG, "Unknown phase2 method" + phase2Method);
                                break;
                        }
                        break;
                    default:
                        // The default index from PHASE2_FULL_ADAPTER maps to the API
                        config.enterpriseConfig.setPhase2Method(phase2Method);
                        break;
                }
                } else {
                    eapSimAkaConfig(config);
                    Log.d(TAG, "eap-sim/aka, config.toString(): " + config.toString());
                }
                String caCert = (String) mEapCaCertSpinner.getSelectedItem();
                if (caCert.equals(unspecifiedCert)) caCert = "";
                config.enterpriseConfig.setCaCertificateAlias(caCert);
                String clientCert = (String) mEapUserCertSpinner.getSelectedItem();
                if (clientCert.equals(unspecifiedCert)) clientCert = "";
                config.enterpriseConfig.setClientCertificateAlias(clientCert);
                config.enterpriseConfig.setIdentity(mEapIdentityView.getText().toString());
                config.enterpriseConfig.setAnonymousIdentity(
                        mEapAnonymousView.getText().toString());

                if (mPasswordView.isShown()) {
                    // For security reasons, a previous password is not displayed to user.
                    // Update only if it has been changed.
                    if (mPasswordView.length() > 0) {
                        config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                    }
                } else {
                    // clear password
                    config.enterpriseConfig.setPassword(mPasswordView.getText().toString());
                }
                break;
            /// M: add WAPI_PSK & WAPI_CERT @{
            case AccessPoint.SECURITY_WAPI_PSK:
                  config.allowedKeyManagement.set(KeyMgmt.WAPI_PSK);
                  config.allowedProtocols.set(Protocol.WAPI);
                  config.allowedPairwiseCiphers.set(PairwiseCipher.SMS4);
                  config.allowedGroupCiphers.set(GroupCipher.SMS4);
                  if (mPasswordView.length() != 0) {
                        String password = mPasswordView.getText().toString();
                        Log.v(TAG, "getConfig(), mHex=" + mHex);
                        if (mHex) { /* Hexadecimal */
                              config.preSharedKey = password;
                        } else { /* ASCII */
                              config.preSharedKey = '"' + password + '"';
                        }
                  }
                  break;

            case AccessPoint.SECURITY_WAPI_CERT:
                  config.allowedKeyManagement.set(KeyMgmt.WAPI_CERT);
                  config.allowedProtocols.set(Protocol.WAPI);
                  config.allowedPairwiseCiphers.set(PairwiseCipher.SMS4);
                  config.allowedGroupCiphers.set(GroupCipher.SMS4);
                  config.enterpriseConfig.setCaCertificateWapiAlias((mWapiAsCert.getSelectedItemPosition() == 0) ? ""
                                          : (String) mWapiAsCert.getSelectedItem());
                  config.enterpriseConfig.setClientCertificateWapiAlias((mWapiClientCert.getSelectedItemPosition() == 0) ? ""
                                          : (String) mWapiClientCert.getSelectedItem());
                  break;
            /// @}
            default:
                return null;
        }

        config.setIpConfiguration(
                new IpConfiguration(mIpAssignment, mProxySettings,
                                    mStaticIpConfiguration, mHttpProxy));

        return config;
    }

    private boolean ipAndProxyFieldsAreValid() {
        mIpAssignment = (mIpSettingsSpinner != null &&
                mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) ?
                IpAssignment.STATIC : IpAssignment.DHCP;

        if (mIpAssignment == IpAssignment.STATIC) {
            mStaticIpConfiguration = new StaticIpConfiguration();
            int result = validateIpConfigFields(mStaticIpConfiguration);
            if (result != 0) {
                return false;
            }
        }

        final int selectedPosition = mProxySettingsSpinner.getSelectedItemPosition();
        mProxySettings = ProxySettings.NONE;
        mHttpProxy = null;
        if (selectedPosition == PROXY_STATIC && mProxyHostView != null) {
            mProxySettings = ProxySettings.STATIC;
            String host = mProxyHostView.getText().toString();
            String portStr = mProxyPortView.getText().toString();
            String exclusionList = mProxyExclusionListView.getText().toString();
            int port = 0;
            int result = 0;
            try {
                port = Integer.parseInt(portStr);
                result = ProxySelector.validate(host, portStr, exclusionList);
            } catch (NumberFormatException e) {
                result = R.string.proxy_error_invalid_port;
            }
            if (result == 0) {
                mHttpProxy = new ProxyInfo(host, port, exclusionList);
            } else {
                return false;
            }
        } else if (selectedPosition == PROXY_PAC && mProxyPacView != null) {
            mProxySettings = ProxySettings.PAC;
            CharSequence uriSequence = mProxyPacView.getText();
            if (TextUtils.isEmpty(uriSequence)) {
                return false;
            }
            Uri uri = Uri.parse(uriSequence.toString());
            if (uri == null) {
                return false;
            }
            mHttpProxy = new ProxyInfo(uri);
        }
        return true;
    }

    private Inet4Address getIPv4Address(String text) {
        try {
            return (Inet4Address) NetworkUtils.numericToInetAddress(text);
        } catch (IllegalArgumentException|ClassCastException e) {
            return null;
        }
    }

    private int validateIpConfigFields(StaticIpConfiguration staticIpConfiguration) {
        if (mIpAddressView == null) return 0;

        String ipAddr = mIpAddressView.getText().toString();
        if (TextUtils.isEmpty(ipAddr)) return R.string.wifi_ip_settings_invalid_ip_address;

        Inet4Address inetAddr = getIPv4Address(ipAddr);
        if (inetAddr == null) {
            return R.string.wifi_ip_settings_invalid_ip_address;
        }

        int networkPrefixLength = -1;
        try {
            networkPrefixLength = Integer.parseInt(mNetworkPrefixLengthView.getText().toString());
            if (networkPrefixLength < 0 || networkPrefixLength > 32) {
                return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
            staticIpConfiguration.ipAddress = new LinkAddress(inetAddr, networkPrefixLength);
        } catch (NumberFormatException e) {
            // Set the hint as default after user types in ip address
            mNetworkPrefixLengthView.setText(mConfigUi.getContext().getString(
                    R.string.wifi_network_prefix_length_hint));
        }

        String gateway = mGatewayView.getText().toString();
        if (TextUtils.isEmpty(gateway)) {
            try {
                //Extract a default gateway from IP address
                InetAddress netPart = NetworkUtils.getNetworkPart(inetAddr, networkPrefixLength);
                byte[] addr = netPart.getAddress();
                addr[addr.length-1] = 1;
                mGatewayView.setText(InetAddress.getByAddress(addr).getHostAddress());
            } catch (RuntimeException ee) {
            } catch (java.net.UnknownHostException u) {
            }
        } else {
            InetAddress gatewayAddr = getIPv4Address(gateway);
            if (gatewayAddr == null) {
                return R.string.wifi_ip_settings_invalid_gateway;
            }
            staticIpConfiguration.gateway = gatewayAddr;
        }

        String dns = mDns1View.getText().toString();
        InetAddress dnsAddr = null;

        if (TextUtils.isEmpty(dns)) {
            //If everything else is valid, provide hint as a default option
            mDns1View.setText(mConfigUi.getContext().getString(R.string.wifi_dns1_hint));
        } else {
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }

        if (mDns2View.length() > 0) {
            dns = mDns2View.getText().toString();
            dnsAddr = getIPv4Address(dns);
            if (dnsAddr == null) {
                return R.string.wifi_ip_settings_invalid_dns;
            }
            staticIpConfiguration.dnsServers.add(dnsAddr);
        }
        return 0;
    }

    private void showSecurityFields() {
        if (mInXlSetupWizard) {
            // Note: XL SetupWizard won't hide "EAP" settings here.
            if (!((WifiSettingsForSetupWizardXL)mConfigUi.getContext()).initSecurityFields(mView,
                        mAccessPointSecurity)) {
                return;
            }
        }
        if (mAccessPointSecurity == AccessPoint.SECURITY_NONE) {
            mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
            /// M: hide WAPI_CERT fileds
            mView.findViewById(R.id.wapi_cert_fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.security_fields).setVisibility(View.VISIBLE);
        /// M: Hexadecimal checkbox only for WAPI_PSK @{
        mView.findViewById(R.id.hex_password).setVisibility(View.GONE);
        if (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_PSK) {
              mView.findViewById(R.id.hex_password).setVisibility(View.VISIBLE);
              ((CheckBox) mView.findViewById(R.id.hex_password)).setChecked(mHex);
        }
        /// @}
        /// M: show WAPI CERT field @{
        if (mAccessPointSecurity == AccessPoint.SECURITY_WAPI_CERT) {
              mView.findViewById(R.id.security_fields).setVisibility(View.GONE);
              mView.findViewById(R.id.wapi_cert_fields).setVisibility(
                          View.VISIBLE);
              mWapiAsCert = (Spinner) mView.findViewById(R.id.wapi_as_cert);
              mWapiClientCert = (Spinner) mView.findViewById(R.id.wapi_user_cert);
              mWapiAsCert.setOnItemSelectedListener(this);
              mWapiClientCert.setOnItemSelectedListener(this);
              loadCertificates(mWapiAsCert, Credentials.WAPI_SERVER_CERTIFICATE);
              loadCertificates(mWapiClientCert, Credentials.WAPI_USER_CERTIFICATE);

              if (mAccessPoint != null && mAccessPoint.networkId != -1) {
                    WifiConfiguration config = mAccessPoint.getConfig();
                    setCertificate(mWapiAsCert, Credentials.WAPI_SERVER_CERTIFICATE,
                            config.enterpriseConfig.getCaCertificateWapiAlias());
                    setCertificate(mWapiClientCert,
                                Credentials.WAPI_USER_CERTIFICATE, config.enterpriseConfig.getClientCertificateWapiAlias());
              }
              return;
        } else {
              mView.findViewById(R.id.wapi_cert_fields).setVisibility(View.GONE);
        }
        /// @}

        if (mPasswordView == null) {
            mPasswordView = (TextView) mView.findViewById(R.id.password);
            mPasswordView.addTextChangedListener(this);
            ((CheckBox) mView.findViewById(R.id.show_password))
                .setOnCheckedChangeListener(this);
            /// M: set setOnClickListener for hex password
            ((CheckBox) mView.findViewById(R.id.hex_password))
                        .setOnCheckedChangeListener(this);

            if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
                mPasswordView.setHint(R.string.wifi_unchanged);
            }
        }

        if (mAccessPointSecurity != AccessPoint.SECURITY_EAP) {
            mView.findViewById(R.id.eap).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.eap).setVisibility(View.VISIBLE);

        if (mEapMethodSpinner == null) {
            mEapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
            /// M: set array for eap method spinner. show simslot in gemini
            // load @{
            Log.d(TAG, "showSecurityFields, FeatureOption.MTK_EAP_SIM_AKA =  " + FeatureOption.MTK_EAP_SIM_AKA);
            Log.d(TAG, "showSecurityFields, FeatureOption.MTK_TC1_FEATURE =  " + FeatureOption.MTK_TC1_FEATURE);
            if (FeatureOption.MTK_EAP_SIM_AKA || FeatureOption.MTK_TC1_FEATURE) {
                  int spinnerId = R.array.wifi_eap_method;
                  if (FeatureOption.MTK_EAP_SIM_AKA && FeatureOption.MTK_TC1_FEATURE) {
                      spinnerId = R.array.wifi_eap_method_fast_sim_aka;
                  } else if (FeatureOption.MTK_EAP_SIM_AKA) {
                      spinnerId = R.array.wifi_eap_method_sim_aka;
                  } else if (FeatureOption.MTK_TC1_FEATURE) {
                      spinnerId = R.array.wifi_eap_method_fast;
                  }
                  Context context = mConfigUi.getContext();
                  final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                              context, android.R.layout.simple_spinner_item, context.getResources().getStringArray(
                                      spinnerId));
                  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                  mEapMethodSpinner.setAdapter(adapter);
            }
            /// @}

            mEapMethodSpinner.setOnItemSelectedListener(this);
            mPhase2Spinner = (Spinner) mView.findViewById(R.id.phase2);
            mEapCaCertSpinner = (Spinner) mView.findViewById(R.id.ca_cert);
            mEapUserCertSpinner = (Spinner) mView.findViewById(R.id.user_cert);
            mEapIdentityView = (TextView) mView.findViewById(R.id.identity);
            mEapAnonymousView = (TextView) mView.findViewById(R.id.anonymous);

            loadCertificates(mEapCaCertSpinner, Credentials.CA_CERTIFICATE);
            loadCertificates(mEapUserCertSpinner, Credentials.USER_PRIVATE_KEY);

            // Modifying an existing network
            if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
                WifiEnterpriseConfig enterpriseConfig = mAccessPoint.getConfig().enterpriseConfig;
                int eapMethod = enterpriseConfig.getEapMethod();
                int phase2Method = enterpriseConfig.getPhase2Method();
                mEapMethodSpinner.setSelection(convertEapMethod(eapMethod, 0));
                showEapFieldsByMethod(eapMethod);
                switch (eapMethod) {
                    case Eap.FAST:
                    case Eap.PEAP:
                        switch (phase2Method) {
                            case Phase2.NONE:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_NONE);
                                break;
                            case Phase2.MSCHAPV2:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_MSCHAPV2);
                                break;
                            case Phase2.GTC:
                                mPhase2Spinner.setSelection(WIFI_PEAP_PHASE2_GTC);
                                break;
                            default:
                                Log.e(TAG, "Invalid phase 2 method " + phase2Method);
                                break;
                        }
                        break;
                    default:
                        mPhase2Spinner.setSelection(phase2Method);
                        break;
                }
                setSelection(mEapCaCertSpinner, enterpriseConfig.getCaCertificateAlias());
                setSelection(mEapUserCertSpinner, enterpriseConfig.getClientCertificateAlias());
                mEapIdentityView.setText(enterpriseConfig.getIdentity());
                mEapAnonymousView.setText(enterpriseConfig.getAnonymousIdentity());
            } else {
                // Choose a default for a new network and show only appropriate
                // fields
                mEapMethodSpinner.setSelection(Eap.PEAP);
                showEapFieldsByMethod(Eap.PEAP);
            }
        } else {
            showEapFieldsByMethod(convertEapMethod(mEapMethodSpinner.getSelectedItemPosition(), 1));
        }
        /// M: eap-sim/aka
        if (convertEapMethod(mEapMethodSpinner.getSelectedItemPosition(), 1) >= WIFI_EAP_METHOD_SIM) {
              mView.findViewById(R.id.l_phase2).setVisibility(View.GONE);
              mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);
              mView.findViewById(R.id.l_user_cert).setVisibility(View.GONE);
              mView.findViewById(R.id.l_anonymous).setVisibility(View.GONE);
        }
        /// M: @{
        if (convertEapMethod(mEapMethodSpinner.getSelectedItemPosition(), 1) == WIFI_EAP_METHOD_SIM
                    || convertEapMethod(mEapMethodSpinner.getSelectedItemPosition(), 1) == WIFI_EAP_METHOD_AKA) {
              mEapIdentityView.setEnabled(false);
              mPasswordView.setEnabled(false);
              ((CheckBox) mView.findViewById(R.id.show_password))
                          .setEnabled(false);
              if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    mView.findViewById(R.id.sim_slot_fields).setVisibility(
                                View.VISIBLE);
                    mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
                    /// M:Geminu plus @{
                    Context context = mConfigUi.getContext();
                    String[] tempSimAkaMethods = context.getResources().getStringArray(R.array.sim_slot);
                    int sum = mTm.getPhoneCount() + 1;
                    Log.d(TAG, "the num of sim slot is :" + (sum - 1));
                    String[] simAkaMethods = new String[sum];
                    for (int i = 0; i < sum; i++) {
                        if (i < tempSimAkaMethods.length) {
                            simAkaMethods[i] = tempSimAkaMethods[i];
                        } else {
                            simAkaMethods[i] = tempSimAkaMethods[1].replaceAll("1", "" + i);
                        }
                     }
                    final ArrayAdapter<String> adapter = new ArrayAdapter<String>
                                      (context, android.R.layout.simple_spinner_item,simAkaMethods);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mSimSlot.setAdapter(adapter);
                    ///@}

                    ///M: setting had selected simslot @{
                    if (mAccessPoint != null
                            && mAccessPoint.networkId != INVALID_NETWORK_ID) {
                        WifiConfiguration config = mAccessPoint.getConfig();
                        if (config != null && config.simSlot != null) {
                        String[] simslots = config.simSlot.split("\"");
                            if (simslots.length > 1) {
                            int slot = Integer.parseInt(simslots[1]) + 1;
                            mSimSlot.setSelection(slot);
                            }
                        }  
                    }
                    /// @}
              }
        } else {
              mEapIdentityView.setEnabled(true);
              mPasswordView.setEnabled(true);
              ((CheckBox) mView.findViewById(R.id.show_password))
                          .setEnabled(true);
              if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    mView.findViewById(R.id.sim_slot_fields).setVisibility(
                                View.GONE);
              }
        }
        /// @}
        
        /// M: eap method changed, and current eap method not equals config's eap method@{
        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID
                && mEapMethodSpinner != null && mEapAnonymousView != null) {
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) mEapMethodSpinner.getAdapter();
            WifiConfiguration config = mAccessPoint.getConfig();
            int i = convertEapMethod(mEapMethodSpinner.getSelectedItemPosition(), 1);
            if (config.enterpriseConfig != null
                    && adapter != null && !(config.enterpriseConfig.getEapMethod() == i)) {
                mEapAnonymousView.setText(null);
            }
        }
        /// @}
    }

    /**
     * EAP-PWD valid fields include
     *   identity
     *   password
     * EAP-PEAP valid fields include
     *   phase2: MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     * EAP-TLS valid fields include
     *   user_cert
     *   ca_cert
     *   identity
     * EAP-TTLS valid fields include
     *   phase2: PAP, MSCHAP, MSCHAPV2, GTC
     *   ca_cert
     *   identity
     *   anonymous_identity
     *   password
     */
    private void showEapFieldsByMethod(int eapMethod) {
        // Common defaults
        mView.findViewById(R.id.l_method).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.l_identity).setVisibility(View.VISIBLE);

        // Defaults for most of the EAP methods and over-riden by
        // by certain EAP methods
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.password_layout).setVisibility(View.VISIBLE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.VISIBLE);

        Context context = mConfigUi.getContext();
        switch (eapMethod) {
            case WIFI_EAP_METHOD_PWD:
                setPhase2Invisible();
                setCaCertInvisible();
                setAnonymousIdentInvisible();
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TLS:
                mView.findViewById(R.id.l_user_cert).setVisibility(View.VISIBLE);
                setPhase2Invisible();
                setAnonymousIdentInvisible();
                setPasswordInvisible();
                break;
            case WIFI_EAP_METHOD_FAST:
            case WIFI_EAP_METHOD_PEAP:
                // Reset adapter if needed
                if (mPhase2Adapter != PHASE2_PEAP_ADAPTER) {
                    mPhase2Adapter = PHASE2_PEAP_ADAPTER;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                break;
            case WIFI_EAP_METHOD_TTLS:
                // Reset adapter if needed
                if (mPhase2Adapter != PHASE2_FULL_ADAPTER) {
                    mPhase2Adapter = PHASE2_FULL_ADAPTER;
                    mPhase2Spinner.setAdapter(mPhase2Adapter);
                }
                mView.findViewById(R.id.l_phase2).setVisibility(View.VISIBLE);
                mView.findViewById(R.id.l_anonymous).setVisibility(View.VISIBLE);
                setUserCertInvisible();
                break;
        }
    }

    private void setPhase2Invisible() {
        mView.findViewById(R.id.l_phase2).setVisibility(View.GONE);
        mPhase2Spinner.setSelection(Phase2.NONE);
    }

    private void setCaCertInvisible() {
        mView.findViewById(R.id.l_ca_cert).setVisibility(View.GONE);
        mEapCaCertSpinner.setSelection(unspecifiedCertIndex);
    }

    private void setUserCertInvisible() {
        mView.findViewById(R.id.l_user_cert).setVisibility(View.GONE);
        mEapUserCertSpinner.setSelection(unspecifiedCertIndex);
    }

    private void setAnonymousIdentInvisible() {
        mView.findViewById(R.id.l_anonymous).setVisibility(View.GONE);
        mEapAnonymousView.setText("");
    }

    private void setPasswordInvisible() {
        mPasswordView.setText("");
        mView.findViewById(R.id.password_layout).setVisibility(View.GONE);
        mView.findViewById(R.id.show_password_layout).setVisibility(View.GONE);
    }

    private void showIpConfigFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.ip_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
            config = mAccessPoint.getConfig();
        }

        if (mIpSettingsSpinner.getSelectedItemPosition() == STATIC_IP) {
            mView.findViewById(R.id.staticip).setVisibility(View.VISIBLE);
            if (mIpAddressView == null) {
                mIpAddressView = (TextView) mView.findViewById(R.id.ipaddress);
                mIpAddressView.addTextChangedListener(this);
                mGatewayView = (TextView) mView.findViewById(R.id.gateway);
                mGatewayView.addTextChangedListener(this);
                mNetworkPrefixLengthView = (TextView) mView.findViewById(
                        R.id.network_prefix_length);
                mNetworkPrefixLengthView.addTextChangedListener(this);
                mDns1View = (TextView) mView.findViewById(R.id.dns1);
                mDns1View.addTextChangedListener(this);
                mDns2View = (TextView) mView.findViewById(R.id.dns2);
                mDns2View.addTextChangedListener(this);
            }
            if (config != null) {
                StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                if (staticConfig != null) {
                    if (staticConfig.ipAddress != null) {
                        mIpAddressView.setText(
                                staticConfig.ipAddress.getAddress().getHostAddress());
                        mNetworkPrefixLengthView.setText(Integer.toString(staticConfig.ipAddress
                                .getNetworkPrefixLength()));
                    }

                    if (staticConfig.gateway != null) {
                        mGatewayView.setText(staticConfig.gateway.getHostAddress());
                    }

                    Iterator<InetAddress> dnsIterator = staticConfig.dnsServers.iterator();
                    if (dnsIterator.hasNext()) {
                        mDns1View.setText(dnsIterator.next().getHostAddress());
                    }
                    if (dnsIterator.hasNext()) {
                        mDns2View.setText(dnsIterator.next().getHostAddress());
                    }
                }
            }
        } else {
            mView.findViewById(R.id.staticip).setVisibility(View.GONE);
        }
    }

    private void showProxyFields() {
        WifiConfiguration config = null;

        mView.findViewById(R.id.proxy_settings_fields).setVisibility(View.VISIBLE);

        if (mAccessPoint != null && mAccessPoint.networkId != INVALID_NETWORK_ID) {
            config = mAccessPoint.getConfig();
        }

        if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_STATIC) {
            setVisibility(R.id.proxy_warning_limited_support, View.VISIBLE);
            setVisibility(R.id.proxy_fields, View.VISIBLE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
            if (mProxyHostView == null) {
                mProxyHostView = (TextView) mView.findViewById(R.id.proxy_hostname);
                mProxyHostView.addTextChangedListener(this);
                mProxyPortView = (TextView) mView.findViewById(R.id.proxy_port);
                mProxyPortView.addTextChangedListener(this);
                mProxyExclusionListView = (TextView) mView.findViewById(R.id.proxy_exclusionlist);
                mProxyExclusionListView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyProperties = config.getHttpProxy();
                if (proxyProperties != null) {
                    mProxyHostView.setText(proxyProperties.getHost());
                    mProxyPortView.setText(Integer.toString(proxyProperties.getPort()));
                    mProxyExclusionListView.setText(proxyProperties.getExclusionListAsString());
                }
            }
        } else if (mProxySettingsSpinner.getSelectedItemPosition() == PROXY_PAC) {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.VISIBLE);

            if (mProxyPacView == null) {
                mProxyPacView = (TextView) mView.findViewById(R.id.proxy_pac);
                mProxyPacView.addTextChangedListener(this);
            }
            if (config != null) {
                ProxyInfo proxyInfo = config.getHttpProxy();
                if (proxyInfo != null) {
                    mProxyPacView.setText(proxyInfo.getPacFileUrl().toString());
                }
            }
        } else {
            setVisibility(R.id.proxy_warning_limited_support, View.GONE);
            setVisibility(R.id.proxy_fields, View.GONE);
            setVisibility(R.id.proxy_pac_field, View.GONE);
        }
    }

    private void setVisibility(int id, int visibility) {
        final View v = mView.findViewById(id);
        if (v != null) {
            v.setVisibility(visibility);
        }
    }

    private void loadCertificates(Spinner spinner, String prefix) {
        final Context context = mConfigUi.getContext();

        String[] certs = KeyStore.getInstance().saw(prefix, android.os.Process.WIFI_UID);
        if (certs == null || certs.length == 0) {
            certs = new String[] {unspecifiedCert};
        } else {
            final String[] array = new String[certs.length + 1];
            array[0] = unspecifiedCert;
            System.arraycopy(certs, 0, array, 1, certs.length);
            certs = array;
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_spinner_item, certs);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    
    private void setCertificate(Spinner spinner, String prefix, String cert) {
        if (cert != null && cert.startsWith(prefix)) {
            setSelection(spinner, cert.substring(prefix.length()));
        }
    }

    private void setSelection(Spinner spinner, String value) {
        if (value != null) {
            @SuppressWarnings("unchecked")
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
            for (int i = adapter.getCount() - 1; i >= 0; --i) {
                if (value.equals(adapter.getItem(i))) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }
    }

    public boolean isEdit() {
        return mEdit;
    }

    @Override
    public void afterTextChanged(Editable s) {
        mTextViewChangedHandler.post(new Runnable() {
                public void run() {
                    enableSubmitIfAppropriate();
                }
            });
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // work done in afterTextChanged
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // work done in afterTextChanged
    }

    @Override
    public void onCheckedChanged(CompoundButton view, boolean isChecked) {
        if (view.getId() == R.id.show_password) {
            int pos = mPasswordView.getSelectionEnd();
            mPasswordView.setInputType(
                    InputType.TYPE_CLASS_TEXT | (isChecked ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                                InputType.TYPE_TEXT_VARIATION_PASSWORD));
            if (pos >= 0) {
                ((EditText)mPasswordView).setSelection(pos);
            }
        } else if (view.getId() == R.id.wifi_advanced_togglebox) {
            if (isChecked) {
                mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.VISIBLE);
            } else {
                mView.findViewById(R.id.wifi_advanced_fields).setVisibility(View.GONE);
            }
        } else if (view.getId() == R.id.hex_password) {
            /// M: verify password if hex check box is clicked @{
            mHex = ((CheckBox) view).isChecked();
            enableSubmitIfAppropriate();
            Log.d(TAG, "onClick mHex is=" + mHex
                        + ",enableSubmitIfAppropriate");
            /// @}
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mSecuritySpinner) {
            mAccessPointSecurity = position;
            Log.d(TAG, "onItemSelected, mAccessPointSecurity = " + mAccessPointSecurity);
            showSecurityFields();
        } else if (parent == mEapMethodSpinner) {
            showSecurityFields();
        } else if (parent == mProxySettingsSpinner) {
            showProxyFields();
        } else {
            showIpConfigFields();
        }
        enableSubmitIfAppropriate();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        //
    }

    /**
     * Make the characters of the password visible if show_password is checked.
     */
    private void updatePasswordVisibility(boolean checked) {
        int pos = mPasswordView.getSelectionEnd();
        mPasswordView.setInputType(
                InputType.TYPE_CLASS_TEXT | (checked ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                            InputType.TYPE_TEXT_VARIATION_PASSWORD));
        if (pos >= 0) {
            ((EditText)mPasswordView).setSelection(pos);
        }
    }
    

    /**
     * M: switch WLAN security spinner
     */
    private void switchWlanSecuritySpinner(Spinner securitySpinner) {
          mSecuritySpinner = securitySpinner;
          ((Spinner) mView.findViewById(R.id.security)).setVisibility(View.GONE);
          ((Spinner) mView.findViewById(R.id.wifi_security_wfa))
                      .setVisibility(View.GONE);

          securitySpinner.setVisibility(View.VISIBLE);
          securitySpinner.setOnItemSelectedListener(this);
    }

  /**
   * M: Geminu plus
   */
   private void eapSimAkaConfig(WifiConfiguration config) {
     if (mSimSlot == null) {
         Log.d(TAG, "mSimSlot is null");
         mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
     }
     String strSimAka = (String) mEapMethodSpinner.getSelectedItem();
     if (FeatureOption.MTK_EAP_SIM_AKA) {
         if (FeatureOption.MTK_GEMINI_SUPPORT) {
             Log.d(TAG, "((String) mSimSlot.getSelectedItem()) " + ((String) mSimSlot.getSelectedItem()));
             //Log.d(TAG, "R.string.eap_sim_slot_0 " + context.getString(R.string.eap_sim_slot_0));
             simSlotConfig(config, strSimAka);
             Log.d(TAG, "eap-sim, choose sim_slot" + (String) mSimSlot.getSelectedItem());
         } else {
             config.imsi = makeNAI(mTm.getSimOperator(), mTm.getSubscriberId(), strSimAka);
             Log.d(TAG, "config.imsi: " + config.imsi);
             config.simSlot = addQuote("0");
             config.pcsc = addQuote("rild");
         }
         Log.d(TAG, "eap-sim, config.imsi: " + config.imsi);
         Log.d(TAG, "eap-sim, config.simSlot: " + config.simSlot);
     }
 }
 
 /**
  * M: Geminu plus
  */   
 private void simSlotConfig(WifiConfiguration config, String strSimAka) {
     int simSlot = mSimSlot.getSelectedItemPosition() - 1;
     if (simSlot > -1) {
    	 int[] SubIds = SubscriptionManager.getSubId(simSlot);
         config.imsi = makeNAI(mTm.getSimOperator(SubIds[0]),
                 mTm.getSubscriberId(SubIds[0]), strSimAka);
         Log.d(TAG, "config.imsi: " + config.imsi);
         config.simSlot = addQuote("" + simSlot);
         Log.d(TAG, "config.simSlot " + addQuote("" + simSlot));
         config.pcsc = addQuote("rild");
         Log.d(TAG, "config.pcsc: " + addQuote("rild"));
     } 
 }

 /**
  * M: because eap-fast , eap-sim and eap-aka use feature option, so not always show in UI
  * @param eapMethod
  * @param getOrSet 0: get eapMethod from framework; 1: UI will set eapMethod to framework
  * @return convert index
  */
 private int convertEapMethod(int eapMethod, int getOrSet) {
     Log.d(TAG, "convertEapMethod, eapMethod =  " + eapMethod);
     Log.d(TAG, "convertEapMethod, FeatureOption.MTK_EAP_SIM_AKA =  " + FeatureOption.MTK_EAP_SIM_AKA);
     Log.d(TAG, "convertEapMethod, FeatureOption.MTK_TC1_FEATURE =  " + FeatureOption.MTK_TC1_FEATURE);
     int convertIndex = eapMethod;
     if (getOrSet == 0) {
         if (eapMethod >= WIFI_EAP_METHOD_SIM) {
             if (FeatureOption.MTK_EAP_SIM_AKA && FeatureOption.MTK_TC1_FEATURE) {
                 convertIndex = eapMethod;
             } else if (FeatureOption.MTK_EAP_SIM_AKA) {
                 if (eapMethod >= WIFI_EAP_METHOD_AKA) {
                     convertIndex = eapMethod - 1;                      
                 }
             } else if (FeatureOption.MTK_TC1_FEATURE) {
                 if (eapMethod >= WIFI_EAP_METHOD_AKA) {
                     Log.e(TAG, "convertEapMethod, eapMethod is wrong, and we set eap-fast to adapt");
                     convertIndex = WIFI_EAP_METHOD_SIM;                      
                 }
             }           
             
         }
         
     } else if (getOrSet == 1) {
         if (eapMethod >= WIFI_EAP_METHOD_SIM) {
             if (FeatureOption.MTK_EAP_SIM_AKA && FeatureOption.MTK_TC1_FEATURE) {
                 convertIndex = eapMethod;
             } else if (FeatureOption.MTK_EAP_SIM_AKA) {
                 if (eapMethod >= WIFI_EAP_METHOD_SIM) {
                     convertIndex = eapMethod ;                      
                 }
             } else if (FeatureOption.MTK_TC1_FEATURE) {
                 if (eapMethod >= WIFI_EAP_METHOD_AKA) {
                     Log.e(TAG, "convertEapMethod, eapMethod is wrong, and we set eap-fast to adapt");
                     convertIndex = WIFI_EAP_METHOD_FAST;                      
                 }
             }           
         }           
     }
     
     Log.d(TAG, "convertEapMethod, convertIndex =  " + convertIndex);
     return convertIndex;
 }
}
