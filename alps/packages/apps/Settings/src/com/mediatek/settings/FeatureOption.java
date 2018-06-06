package com.mediatek.settings;

import android.os.SystemProperties;

public class FeatureOption {
    public static final boolean MTK_TETHERINGIPV6_SUPPORT = getValue("ro.mtk_tetheringipv6_support");
    public static final boolean MTK_EAP_SIM_AKA = getValue("ro.mtk_eap_sim_aka");
    public static final boolean MTK_GEMINI_SUPPORT = getValue("ro.mtk_gemini_support");
    public static final boolean MTK_WAPI_SUPPORT = getValue("ro.mtk_wapi_support");
    public static final boolean MTK_TC1_FEATURE = getValue("ro.mtk_tc1.feature");
    public static final boolean MTK_SMARTBOOK_SUPPORT = getValue("ro.mtk_smartbook_support");

    private static boolean getValue(String key) {
    	return SystemProperties.get(key).equals("1");
    }
}
