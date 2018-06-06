LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-appcompat-v7-22_2_1
LOCAL_STATIC_JAVA_AAR_LIBRARIES := android-support-recyclerview-v7-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-core-ui-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-compat-25_2_0_aar
#LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-design-22_2_1

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := phoneMsc:libs/Msc.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += phoneSunflower:libs/Sunflower.jar
LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_STATIC_JAVA_LIBRARIES += android-support-annotations-25_2_0
LOCAL_STATIC_JAVA_LIBRARIES += mstarc.base.policy \
                               phoneMsc \
                               phoneSunflower \
                               com.mstarc.wearablephonebtadapter
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(MSTARC_INTERNAL_RESOURCE_VERSION))
LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_PACKAGE_NAME := WearableSetting
LOCAL_SDK_VERSION := current
LOCAL_CERTIFICATE := platform
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_AAPT_FLAGS := \
  --auto-add-overlay \
  --extra-packages android.support.v4 \
  --extra-packages android.support.v7.recyclerview \
  --extra-packages com.android.contacts.common \
  --extra-packages com.android.phone.common
LOCAL_REQUIRED_MODULES := WearableUIlib
#LOCAL_CERTIFICATE := platform
include $(BUILD_PACKAGE)


