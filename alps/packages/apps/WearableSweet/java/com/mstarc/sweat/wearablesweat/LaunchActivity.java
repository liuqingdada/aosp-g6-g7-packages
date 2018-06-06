package com.mstarc.sweat.wearablesweat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mstarc.fakewatch.sweetwords.SweetWords;
import com.mstarc.sweat.wearablesweat.utils.NetUtil;

public class LaunchActivity extends Activity implements SweetWords.SweetWordsUpdateListener {
    private SweetWords mSweetWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSweetWords = SweetWords.getInstance();
        mSweetWords.init(this);
        mSweetWords.setSweetWordsUpdateListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (NetUtil.hasNet(this)) {
            mSweetWords.updateBoundState(this);
        } else {
            followOn();
        }
    }

    private void followOn() {
        boolean bBound = SweetWords.getInstance()
                                   .isBound(this);
        if (bBound) {
            Log.d("TAG", "LoginActivity_已登录");
            Intent intent = new Intent(LaunchActivity.this, SweatProfileActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            //  intent.putExtras(token.toBundle());
            startActivity(intent);
            finish();
        } else {
            Log.d("TAG", "LoginActivity_未登录");
            Intent intent = new Intent();
            intent.setClass(LaunchActivity.this, QrcodeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
    }

    // --------------------------------

    @Override
    public void onUpdateState() {
        followOn();
    }

    @Override
    public void onUpdateStateFailure() {
        followOn();
        Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT)
             .show();
    }

    // --------------------------------
}
