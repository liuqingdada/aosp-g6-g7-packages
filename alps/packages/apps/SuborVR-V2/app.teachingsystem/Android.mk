LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

###################### aar
# google
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := app.teachingsystem-release:libs/app.teachingsystem-release.aar

##################### jar
# google
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += SuborUiLib:libs/SuborUiLib.jar


LOCAL_PACKAGE_NAME := subor.vrbase-v2.app.teachingsystem

include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)


# aar config
LOCAL_STATIC_JAVA_AAR_LIBRARIES := app.teachingsystem-release

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.subor.vr.app.teachingsystem \


# jar confic
LOCAL_STATIC_JAVA_LIBRARIES := SuborUiLib

# All config
LOCAL_REQUIRED_MODULES += app.teachingsystem-release \
    SuborUiLib \


LOCAL_MODULE := subor.policy.SuborVR-V2.app.teachingsystem

LOCAL_DX_FLAGS := --multi-dex

include $(BUILD_JAVA_LIBRARY)
