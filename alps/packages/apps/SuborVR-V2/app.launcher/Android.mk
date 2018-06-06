LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

###################### aar
# google
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := app.launcher-release:libs/app.launcher-release.aar

##################### jar
# google
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += SuborUiLib:libs/SuborUiLib.jar


LOCAL_PACKAGE_NAME := subor.vrbase-v2.app.launcher

include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)


# aar config
LOCAL_STATIC_JAVA_AAR_LIBRARIES := app.launcher-release

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.subor.vr.app.launcher \


# jar confic
LOCAL_STATIC_JAVA_LIBRARIES := SuborUiLib

# All config
LOCAL_REQUIRED_MODULES += app.launcher-release \
    SuborUiLib \


LOCAL_MODULE := subor.policy.SuborVR-V2.app.launcher

LOCAL_DX_FLAGS := --multi-dex

include $(BUILD_JAVA_LIBRARY)
