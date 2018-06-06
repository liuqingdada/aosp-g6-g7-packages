LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_STATIC_JAVA_LIBRARIES += android-support-annotations-25_2_0
LOCAL_SRC_FILES := $(call all-java-files-under, java)
LOCAL_PACKAGE_NAME := WearableWatchface

LOCAL_CERTIFICATE := platform

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)


