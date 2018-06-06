LOCAL_PATH:= $(call my-dir)


include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_AIDL_INCLUDES := $(LOCAL_PATH)/aidl
LOCAL_SRC_FILES := \
    aidl/com/mstarc/wearablephonebtadapter/IRemoteServiceCallback.aidl \
    aidl/com/mstarc/wearablephonebtadapter/IRemoteService.aidl

LOCAL_MODULE := com.mstarc.wearablephonebtadapter

LOCAL_PROGUARD_ENABLED := disabled
include $(BUILD_STATIC_JAVA_LIBRARY)



include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional 

LOCAL_SRC_FILES := $(call all-java-files-under, $(java))
LOCAL_STATIC_JAVA_LIBRARIES += \
    com.mstarc.wearablephonebtadapter \
    mstarc.xutils \
    mstarc.fastjson \
    mstarc.util \
    mstarc.base.policy

LOCAL_REQUIRED_MODULES := com.mstarc.wearablephonebtadapter WearableUIlib
LOCAL_PROGUARD_FLAG_FILES := proguard.flags
LOCAL_PACKAGE_NAME := WearablePhoneBtAdapter
include $(BUILD_PACKAGE)
