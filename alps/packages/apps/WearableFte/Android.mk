LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

#LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-appcompat-v7-22_2_1
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-core-ui-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-compat-25_2_0_aar
#LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-design-22_2_1

LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(MSTARC_INTERNAL_RESOURCE_VERSION))
#LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, g6_res)

LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_PACKAGE_NAME := WearableFte
LOCAL_SDK_VERSION := current
LOCAL_CERTIFICATE := platform
LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_PROGUARD_FLAG_FILES := proguard.flags

#LOCAL_AAPT_FLAGS := \
  --auto-add-overlay \
  --extra-packages android.support.v4 \
  --extra-packages android.support.v7.recyclerview \

#LOCAL_REQUIRED_MODULES := WearableUIlib

include $(BUILD_PACKAGE)

