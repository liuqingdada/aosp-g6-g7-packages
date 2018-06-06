LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

###################### aar
# google
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := vrplayer-release:libs/vrplayer-release.aar

##################### jar
# google
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += support-annotations:libs/google/support-annotations-25.3.1.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += WearableVRLib:libs/WearableVRLib.jar


LOCAL_PACKAGE_NAME := subor.vrbase.player

include $(BUILD_MULTI_PREBUILT)
include $(CLEAR_VARS)

#LOCAL_SRC_FILES :=  $(call all-java-files-under, java)


#LOCAL_SRC_FILES :=  $(call all-java-files-under, java) \
#    $(call all-java-files-under, javaB)

#LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res_v7 \
#    $(LOCAL_PATH)/res_vrtest


# aar config
LOCAL_STATIC_JAVA_AAR_LIBRARIES := vrplayer-release
#LOCAL_STATIC_JAVA_AAR_LIBRARIES += support-v4

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.subor.vr.player \


# jar confic
LOCAL_STATIC_JAVA_LIBRARIES := WearableVRLib

# All config
LOCAL_REQUIRED_MODULES += vrplayer-release \
    WearableVRLib \


LOCAL_MODULE := subor.policy.player

LOCAL_DX_FLAGS := --multi-dex

include $(BUILD_JAVA_LIBRARY)
