// OnDeleteNotificationListener.aidl
package com.mstarc.watchservice.notification;

// Declare any non-default types here with import statements

interface OnDeleteNotificationListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void OnDeleteOneNotification(long id);

    void OnDeleteAllNotification();
}
