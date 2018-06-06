package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.utils.KeyboardUtil;
import com.mstarc.wearablesettings.utils.ThemeUtils;

import static com.mstarc.wearablesettings.utils.Constants.PASSWORD;
import static com.mstarc.wearablesettings.utils.Constants.WIFI_PASSWORD_REQUESTCODE;

public class KeyBoardActivity extends Activity {

    private EditText mPassword;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_board);
        mPassword = (EditText)findViewById(R.id.password);
        mPassword.setInputType(InputType.TYPE_NULL);

        updateImageView(findViewById(R.id.password_sure), R.mipmap.queding);
        new KeyboardUtil(this, this, mPassword).showKeyboard();
    }
    public void sureClick(View v){
        Intent intent = new Intent();
        intent.putExtra(PASSWORD, mPassword.getText().toString());
        // 设置结果，并进行传送
        this.setResult(WIFI_PASSWORD_REQUESTCODE, intent);
        finish();
    }

    private void updateImageView(final View view, final int resId) {
        view.post(new Runnable() {
            @Override
            public void run() {
                int color = ThemeUtils.getCurrentPrimaryColor();
                ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
                Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);
                drawable.clearColorFilter();
                drawable.mutate().setColorFilter(filter);
                view.setBackground(drawable);
            }
        });
    }

}
