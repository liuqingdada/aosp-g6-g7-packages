package com.mstarc.sweat.wearablesweat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mstarc.fakewatch.sweetwords.SweetWords;
import com.mstarc.fakewatch.sweetwords.api.bean.SweetApiBean;
import com.mstarc.sweat.wearablesweat.utils.ThemeUtils;

public class SweatProfileActivity extends Activity implements SweetWords.SweetWordsListener{


    ImageView mAvata;
    TextView mName;
    TextView mUnbind;
    TextView mSweetPerson;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sweat);
        //SweetWords.getInstance().init(getApplicationContext());
        mAvata = (ImageView) findViewById(R.id.person_avata);
        mName = (TextView) findViewById(R.id.person_name);
        mSweetPerson = (TextView) findViewById(R.id.person_text);
        mUnbind = (TextView) findViewById(R.id.img_unbind);
        mSweetPerson.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        updateImageView(mAvata, R.mipmap.icon_sweat_avata);
        mUnbind.setBackgroundColor(ThemeUtils.getCurrentPrimaryColor());
        mUnbind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SweetWords.getInstance().doUnbound(getApplicationContext());
            }
        });
    }

    private void updateImageView(final ImageView view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = SweatProfileActivity.this.getDrawable(resId);
                //Drawable drawable = ContextCompat.getDrawable(SweatProfileActivity.this, resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SweetWords.getInstance().setSweetWordsListener(this);
        SweetWords.getInstance().getFriendInfo();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }

    @Override
    public void onBound() {

    }

    @Override
    public void onBoundFailure() {

    }

    @Override
    public void onUnbind() {
        Intent intent = new Intent();
        intent.setClass(SweatProfileActivity.this, QrcodeActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onUnbindFailure() {
		Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onGetFriendInfo(final SweetApiBean sweetApiBean) {
        mName.post(new Runnable() {
            @Override
            public void run() {
                mName.setText(sweetApiBean.getName());
                mAvata.setImageBitmap(sweetApiBean.getHeadBitmap());
            }
        });
    }

    @Override
    public void onGetQRBitmap(final Bitmap bitmap) {

    }

    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
        overridePendingTransition(0, 0);
    }
}
