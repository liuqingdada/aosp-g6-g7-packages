package com.mstarc.wearablesettings.common;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.mstarc.wearablesettings.activitys.InputPassActivity;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;

import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.AMBIENT_OFF_SHOW_PS;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.OFF_WRIST;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.ON_AMBIENT_OFF;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.ON_OFF_WRIST;
import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.ON_WRIST;

public class MyService extends Service {

    private final static String TAG = MyService.class.getSimpleName();
    final static String ACTION_ENABLE_KEYGAURD = "ACTION_ENABLE_KEYGAURD";
    final static String ACTION_AMBIENT_OFF = "com.mstarc.ambient.off";
    final static String ACTION_HOME_SCROLL = "ACTION_HOME_SCROLL";
    final static String QUIT_CHARGE_SCREEN = "QUIT_CHARGE_SCREEN";
    private ScreenBroadcastReceiver mReceiver;
    private SharedPreferencesHelper mSph;
    public MyService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG,"onStartCommand");
        mReceiver = new ScreenBroadcastReceiver();
        mSph = SharedPreferencesHelper.getInstance(this);
        flags = START_STICKY;
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("com.mstarc.watch.action.notification");
        filter.addAction(ACTION_ENABLE_KEYGAURD);
        filter.addAction(ACTION_AMBIENT_OFF);
        filter.addAction(ACTION_HOME_SCROLL);
        filter.addAction(QUIT_CHARGE_SCREEN);
        registerReceiver(mReceiver,filter);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if(mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        Intent sevice = new Intent(this, MyService.class);
        this.startService(sevice);
        Log.i(TAG,"onDestroy");
        super.onDestroy();
    }

    private class ScreenBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intent.ACTION_SCREEN_ON.equals(intent.getAction()) || QUIT_CHARGE_SCREEN.equals(intent.getAction())){
                Log.i(TAG,"ACTION_SCREEN_ON");
                final String pwd = android.provider.Settings.Secure.getString(context.getContentResolver(),"wearable_password");
                if(mSph.getBoolean(SharedPreferencesHelper.IS_NEED_PW,true) && !TextUtils.isEmpty(pwd)
                        && !mSph.getBoolean(SharedPreferencesHelper.IS_SHOW_PW,false)) {
                    if(mSph.getBoolean(OFF_WRIST,false) || mSph.getBoolean(ON_OFF_WRIST,false)) {
                        Log.i(TAG,"offwrist true");
                        if(!mSph.getBoolean(ON_AMBIENT_OFF,false)) {
                            Intent i = new Intent(MyService.this, InputPassActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(i);
                            if(mSph.getBoolean(ON_OFF_WRIST,false)) {
                                Log.i(TAG,"wrist change on off");
                                mSph.putBoolean(ON_OFF_WRIST,false);
                            }
                        }else{
                            mSph.putBoolean(AMBIENT_OFF_SHOW_PS,true);
                            mSph.putBoolean(ON_AMBIENT_OFF,false);
                        }
                    }
                }
            } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                Log.i(TAG,"ACTION_SCREEN_OFF");
            }else if(intent.getAction().equals("com.mstarc.watch.action.notification")) {
                if(intent.getStringExtra("function").equals(OFF_WRIST)) {
                    Log.i(TAG,"get offwrist");
                    mSph.putBoolean(OFF_WRIST,true);
                }else if(intent.getStringExtra("function").equals(ON_WRIST)) {
                    Log.i(TAG,"get onwrist");
                    if(mSph.getBoolean(OFF_WRIST,false)) {
                        mSph.putBoolean(ON_OFF_WRIST,true);
                        mSph.putBoolean(OFF_WRIST,false);
                    }
                }
            }else if(intent.getAction().equals(ACTION_ENABLE_KEYGAURD)) {
                Log.i(TAG,"ACTION_ENABLE_KEYGAURD");
                if(mSph.getBoolean(SharedPreferencesHelper.IS_CALL,false)) {
                    Log.i(TAG,"show password view");
                    mSph.putBoolean(SharedPreferencesHelper.IS_CALL,false);
                    Intent i = new Intent(MyService.this, InputPassActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                }
            }else if(intent.getAction().equals(ACTION_AMBIENT_OFF)) {
                Log.i(TAG,"ACTION_AMBIENT_OFF");
                mSph.putBoolean(ON_AMBIENT_OFF,true);
            }else if(intent.getAction().equals(ACTION_HOME_SCROLL)) {
                Log.i(TAG,"ACTION_HOME_SCROLL");
                if(mSph.getBoolean(AMBIENT_OFF_SHOW_PS,false)) {
                    Log.i(TAG,"ACTION_HOME_SCROLL show ps");
                    Intent i = new Intent(MyService.this, InputPassActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    if(mSph.getBoolean(ON_OFF_WRIST,false)) {
                        Log.i(TAG,"wrist change on off");
                        mSph.putBoolean(ON_OFF_WRIST,false);
                    }
                }
                mSph.putBoolean(AMBIENT_OFF_SHOW_PS,false);
            }
        }
    }
}
