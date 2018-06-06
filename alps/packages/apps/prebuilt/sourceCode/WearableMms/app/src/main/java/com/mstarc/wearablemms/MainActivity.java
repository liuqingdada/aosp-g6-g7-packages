package com.mstarc.wearablemms;

import android.content.Intent;
import android.os.Bundle;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.mstarc.wearablemms.activity.BaseActivity;

/**
 * Created by wangxinzhi on 17-3-8.
 */

public class MainActivity extends BaseActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_MESSAGE = 1;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showFragment(FRAGMENT_INDEX_MESSAGE_LISTS, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=5926df8c");
//        getPermisson();
        showFragment(FRAGMENT_INDEX_MESSAGE_LISTS, null);
        String defaultSmsApp = null;
        String currentPn = getPackageName();//获取当前程序包名
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT)
//        {
//            defaultSmsApp = Telephony.Sms.getDefaultSmsPackage(this);//获取手机当前设置的默认短信应用的包名
//        }
//        if (!defaultSmsApp.equals(currentPn)) {
//            Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
//            intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, currentPn);
//            startActivity(intent);
//        }
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

//    private void getPermisson(){
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.READ_SMS)
//                != PackageManager.PERMISSION_GRANTED)
//        {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.READ_SMS},
//                    MY_PERMISSIONS_REQUEST_READ_MESSAGE);
//        }
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.READ_CONTACTS)
//                != PackageManager.PERMISSION_GRANTED)
//        {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.READ_CONTACTS},
//                    MY_PERMISSIONS_REQUEST_READ_MESSAGE);
//        }
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.RECORD_AUDIO)
//                != PackageManager.PERMISSION_GRANTED)
//        {
//
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.RECORD_AUDIO},
//                    MY_PERMISSIONS_REQUEST_READ_MESSAGE);
//        }
//    }
}
