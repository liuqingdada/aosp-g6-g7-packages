LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := phoneMsc:libs/Msc.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += phoneSunflower:libs/Sunflower.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += mstarc_os_api:libs/mstarc_os_api.jar
# LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += phonefastjson:libs/fastjson-1.2.5.jar
#added by suhen, greendao
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += greendao:libs/greendao-3.2.2.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += greendao-api:libs/greendao-api-3.2.2.jar
LOCAL_PREBUILT_LIBS := libmsc:libs/armeabi/libmsc.so
include $(BUILD_MULTI_PREBUILT)


include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional

LOCAL_STATIC_JAVA_AAR_LIBRARIES := android-support-recyclerview-v7-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-fragment-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-core-ui-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-compat-25_2_0_aar
LOCAL_STATIC_JAVA_AAR_LIBRARIES += android-support-core-utils-25_2_0

#added by suhen, greendao
LOCAL_STATIC_JAVA_LIBRARIES := greendao
LOCAL_STATIC_JAVA_LIBRARIES += greendao-api
LOCAL_STATIC_JAVA_LIBRARIES += mstarc_os_api

contacts_common_dir := ../ContactsCommon
phone_common_dir := ../PhoneCommon

src_dirs := java \
    $(contacts_common_dir)/src \
    $(phone_common_dir)/src

res_dirs := $(MSTARC_INTERNAL_RESOURCE_VERSION) \
    $(contacts_common_dir)/res \
    $(phone_common_dir)/res

#res_dirs := g6_res \
     $(contacts_common_dir)/res \
     $(phone_common_dir)/res

LOCAL_SRC_FILES := $(call all-java-files-under, $(src_dirs))
LOCAL_RESOURCE_DIR := $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_AAPT_FLAGS := \
    --auto-add-overlay \
    --extra-packages com.android.contacts.common \
    --extra-packages com.android.phone.common \
    --extra-packages android.support.v4 \
    --extra-packages android.support.v7.recyclerview

LOCAL_JAVA_LIBRARIES := telephony-common
LOCAL_STATIC_JAVA_LIBRARIES += \
    android-support-annotations-25_2_0 \
    com.android.services.telephony.common \
    com.android.vcard \
    android-common \
    guava \
    android-ex-variablespeed \
    libphonenumber \
    libgeocoding \
    phoneMsc \
    phoneSunflower \
    com.mstarc.wearablephonebtadapter

LOCAL_JNI_SHARED_LIBRARIES := libmsc

LOCAL_SHARED_LIBRARIES := libphonemsc

LOCAL_REQUIRED_MODULES := libvariablespeed com.mstarc.wearablephonebtadapter

LOCAL_PACKAGE_NAME := WearablePhone
LOCAL_CERTIFICATE := shared
LOCAL_PRIVILEGED_MODULE := true

LOCAL_OVERRIDES_PACKAGES := Dialer

LOCAL_PROGUARD_ENABLED := disabled


#LOCAL_MODULE_INCLUDE_LIBRARY := true
include $(BUILD_PACKAGE)

