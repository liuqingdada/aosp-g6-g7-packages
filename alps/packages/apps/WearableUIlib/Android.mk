LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := android-support-recyclerview-v7-25_2_0_aar:libs/recyclerview-v7-25.2.0.aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-fragment-25_2_0_aar:libs/support-fragment-25.2.0.aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-core-ui-25_2_0_aar:libs/support-core-ui-25.2.0.aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-compat-25_2_0_aar:libs/support-compat-25.2.0.aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-annotations-25_2_0:libs/support-annotations-25.2.0.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-core-utils-25_2_0:libs/support-core-utils-25.2.0.aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-gif-drawable-1.2.6:libs/android-gif-drawable-1.2.6.aar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += universal-image-loader-1.9.5:libs/universal-image-loader-1.9.5.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += mstarc.base.policy:libs/mstarc.policy.jar

LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += fastjson-1.2.5:libs/fastjson-1.2.5.jar 
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += httpclient-4.2.5:libs/httpclient-4.2.5.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += httpcore-4.2.4:libs/httpcore-4.2.4.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += jsoup-1.8.3:libs/jsoup-1.8.3.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += msc-base:libs/Msc.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += com.mcxiaoke.volley:libs/com.mcxiaoke.volley.jar
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += mstarc.watchfacemanager:libs/com.mstarc.watchfacemanager.jar

#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-appcompat-v7-22_2_1:extralibs/appcompat-v7-22.2.1.aar
#LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES += android-support-design-22_2_1:extralibs/design-22.2.1.aar
LOCAL_PACKAGE_NAME := WearableUIlib

include $(BUILD_MULTI_PREBUILT)

