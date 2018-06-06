package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.utils.ThemeUtils;
import com.mstarc.wearablesettings.views.WheelView;

import java.util.ArrayList;


public class LockTimeActivity extends Activity {
    WheelView mWheelView;
    TextView tv_top, tv_bot;
    ArrayList<String> mList;
    private final static int COLOR_ROSE_GOLDEN = 0xFFFF1D79;
    private final static int COLOR_HIGH_BLACK = 0xFFF2D34E;
    private final static int COLOR_APPLE_GREEN = 0xFFAFFF00;

    private final static int COLOR_G6_PLUS = 0xFFEBB553;
    float startX;
    float offSetX;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locktime);
        initView();
        setColor();
        initData();
        setDefaultTime();
        mWheelView.setOnSelectListener(new WheelView.OnSelectListener() {
            @Override
            public void endSelect(int id, String text) {
                int time = Integer.parseInt(text.substring(0, text.indexOf("秒")));
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, time * 1000 + 5 * 1000);
                Log.d("LockTimeActivity", "设置息屏时间" + time);
            }

            @Override
            public void selecting(int id, String text) {

            }
        });
        mWheelView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = motionEvent.getX();
                        break;

                    case MotionEvent.ACTION_UP:
                        offSetX = motionEvent.getX() - startX;
                        if (offSetX > 120) {
                            finish();
                            Log.d("LockTimeActivity", "右滑退出");
                        }
                        break;
                }
                return false;
            }
        });
    }

    private void initView() {
        mWheelView = (WheelView) findViewById(R.id.wv);
        tv_top = (TextView) findViewById(R.id.tv_top);
        tv_bot = (TextView) findViewById(R.id.tv_bot);
    }

    private void setColor() {
        int color = ThemeUtils.getCurrentPrimaryColor();
        mWheelView.setSelectedColor(color);
        switch (color) {
            case COLOR_APPLE_GREEN:
                tv_top.setBackgroundResource(R.mipmap.jianbian_green);
                tv_bot.setBackgroundResource(R.mipmap.jianbian_green);
                break;
            case COLOR_HIGH_BLACK:
                tv_top.setBackgroundResource(R.mipmap.jianbian_gold);
                tv_bot.setBackgroundResource(R.mipmap.jianbian_gold);
                break;
            case COLOR_ROSE_GOLDEN:
                tv_top.setBackgroundResource(R.mipmap.jianbian_red);
                tv_bot.setBackgroundResource(R.mipmap.jianbian_red);
                break;
            case COLOR_G6_PLUS:
                tv_top.setBackgroundResource(R.mipmap.jianbiantiao);
                tv_bot.setBackgroundResource(R.mipmap.jianbiantiao);
                break;
        }
    }

    private void setDefaultTime() {
        int time = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 10 * 1000 + 5 * 1000);
        Log.d("LockTimeActivity", "当前息屏时间" + time);
        switch (time / 1000 - 5) {
            case 10:
            default:
                mWheelView.setDefault(0);
                break;
            case 15:
                mWheelView.setDefault(1);
                break;
            case 20:
                mWheelView.setDefault(2);
                break;
        }
    }

    private void initData() {
        mList = new ArrayList<>();
        // mList.add("15秒");
        mList.add("10秒");
        mList.add("15秒");
        mList.add("20秒");
        // mList.add("5秒");
        // mList.add("10秒");
        mWheelView.setData(mList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("LockTimeActivity", "ac销毁");
    }
}
