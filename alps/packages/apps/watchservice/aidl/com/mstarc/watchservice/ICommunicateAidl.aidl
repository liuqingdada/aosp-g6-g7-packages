// ICommunicateAidl.aidl
package com.mstarc.watchservice;
import com.mstarc.watchservice.common.IStringTransmitListener;
import com.mstarc.watchservice.notification.OnReceiveNotification;
import com.mstarc.watchservice.notification.OnDeleteNotificationListener;
import com.mstarc.watchservice.notification.OnUpdataNotificationListener;

// Declare any non-default types here with import statements

interface ICommunicateAidl {

    void setOnReceiveMessageListener(in IStringTransmitListener istl);

    void sendMessage(String json, String filePath);

    // Notification 相关接口
    String getAll();
    void remove(long id);
    void removeAll();
    void update(long id, String title, String content);
    void setRead(long id);
    void setOnReceiveNotification(in OnReceiveNotification ornl);
    void setOnDeleteNotificationListener(in OnDeleteNotificationListener odnl);
    void setOnUpdataNotificationListener(in OnUpdataNotificationListener ounl);
}
