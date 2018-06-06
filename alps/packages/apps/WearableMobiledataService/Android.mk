LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_AAR_LIBRARIES := android-support-recyclerview-v7-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-fragment-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-core-ui-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-compat-25_2_0_aar
#LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-appcompat-v7-22_2_1
#LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-design-22_2_1

LOCAL_STATIC_JAVA_LIBRARIES += android-support-annotations-25_2_0 \
			                   mstarc.xutils \
			                   mstarc.fastjson \
			                   mstarc.util \
			                   mstarc.base.policy \
                                           mstarc.watchfacemanager
LOCAL_JAVA_LIBRARIES := telephony-common ims-common
LOCAL_SRC_FILES := $(call all-java-files-under, java)
#LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, g7_res)
#LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(MSTARC_INTERNAL_RESOURCE_VERSION))
LOCAL_PACKAGE_NAME := WearableMobiledataService

LOCAL_AAPT_FLAGS := \
  --auto-add-overlay \
  --extra-packages android.support.v4 \
  --extra-packages android.support.v7.recyclerview \

LOCAL_REQUIRED_MODULES := WearableUIlib

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_DEX_PREOPT := false

include $(BUILD_PACKAGE)


