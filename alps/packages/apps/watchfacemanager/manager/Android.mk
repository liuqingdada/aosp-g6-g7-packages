LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-java-files-under, java) \
        $(call all-Iaidl-files-under, java)

LOCAL_MODULE := com.mstarc.watchfacemanager

include $(BUILD_STATIC_JAVA_LIBRARY)

