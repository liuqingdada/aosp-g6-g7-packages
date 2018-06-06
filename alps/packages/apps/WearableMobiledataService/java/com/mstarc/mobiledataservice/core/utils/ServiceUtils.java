package com.mstarc.mobiledataservice.core.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.UserHandle;
import android.util.Log;

import net.vidageek.mirror.dsl.Mirror;

/**
 * Created by liuqing
 * 2017/7/19.
 * Email: 1239604859@qq.com
 */

public class ServiceUtils {
    /**
     * Flag for {@link Context#bindService}: allows application hosting service to manage whitelists
     * such as temporary allowing a {@code PendingIntent} to bypass Power Save mode.
     */
    public static final int BIND_ALLOW_WHITELIST_MANAGEMENT = 0x01000000;

    /**
     * Flag for {@link Context#bindService}: For only the case where the binding
     * is coming from the system, set the process state to FOREGROUND_SERVICE
     * instead of the normal maximum of IMPORTANT_FOREGROUND.  That is, this is
     * saying that the process shouldn't participate in the normal power reduction
     * modes (removing network access etc).
     */
    public static final int BIND_FOREGROUND_SERVICE = 0x04000000;

    // ===================
    private static final String TAG = "ServiceUtils";

    public static final String ALL = "ALL";

    public static final String CURRENT = "CURRENT";

    public static final String CURRENT_OR_SELF = "CURRENT_OR_SELF";

    public static final String OWNER = "OWNER";

    public static ComponentName startServiceAsUser(Context context, Intent intent,
                                                   UserHandle user) {
        try {
            return (ComponentName) new Mirror().on(context)
                                               .invoke()
                                               .method("startServiceAsUser")
                                               .withArgs(intent, user);
        } catch (Exception e) {
            Log.e(TAG, "startServiceAsUser: " + e.toString());
            return null;
        }
    }

    public static boolean bindServiceAsUser(Context context, Intent intent, ServiceConnection conn,
                                            int flags, UserHandle user) {
        try {
            return (boolean) new Mirror().on(context)
                                         .invoke()
                                         .method("bindServiceAsUser")
                                         .withArgs(intent, conn, flags, user);
        } catch (Exception e) {
            Log.e(TAG, "bindServiceAsUser: " + e.toString());
            return false;
        }
    }

    public static UserHandle getUserHandle(String user) {
        try {
            return (UserHandle) new Mirror().on(UserHandle.class)
                                            .get()
                                            .field(user);
        } catch (Exception e) {
            Log.e(TAG, "getUserHandle: " + e.toString());
            return null;
        }
    }
}
