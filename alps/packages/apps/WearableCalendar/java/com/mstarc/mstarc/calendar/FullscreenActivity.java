package com.mstarc.mstarc.calendar;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.text.style.ForegroundColorSpan;
import android.text.style.LineBackgroundSpan;
import android.util.DisplayMetrics;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.CalendarMode;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnMonthChangedListener;
import com.prolificinteractive.materialcalendarview.format.TitleFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends Activity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private LunarDecorator mLunarDecorator = null;
    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    //private View mContentView;
//    private final Runnable mHidePart2Runnable = new Runnable() {
//        @SuppressLint("InlinedApi")
//        @Override
//        public void run() {
//            // Delayed removal of status and navigation bar
//
//            // Note that some of these constants are new as of API 16 (Jelly Bean)
//            // and API 19 (KitKat). It is safe to use them, as they are inlined
//            // at compile-time and do nothing on earlier devices.
//            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        }
//    };
    //private View mControlsView;
//    private final Runnable mShowPart2Runnable = new Runnable() {
//        @Override
//        public void run() {
//            // Delayed display of UI elements
//            ActionBar actionBar = getSupportActionBar();
//            if (actionBar != null) {
//                actionBar.show();
//            }
//            mControlsView.setVisibility(View.VISIBLE);
//        }
//    };
    private boolean mVisible;
//    private final Runnable mHideRunnable = new Runnable() {
//        @Override
//        public void run() {
//            hide();
//        }
//    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
//    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            if (AUTO_HIDE) {
//                delayedHide(AUTO_HIDE_DELAY_MILLIS);
//            }
//            return false;
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mVisible = true;
        MaterialCalendarView mcv = (MaterialCalendarView) findViewById(R.id.calendarView);
        Date date = new Date(2000, 1, 1);
        mcv.setCurrentDate(date);
        mcv.state().edit()
                        //设置一周的第一天是周日还是周一
                .setFirstDayOfWeek(Calendar.SUNDAY)
                        //设置日期范围
                .setMinimumDate(CalendarDay.from(1988, 1, 1))
                .setMaximumDate(CalendarDay.from(2035, 1, 1))
                .setCalendarDisplayMode(CalendarMode.MONTHS)
                .commit();
        mcv.setTitleFormatter(new title());

        //添加修饰
        long time=System.currentTimeMillis();
        final Calendar mCalendar=Calendar.getInstance();
        mCalendar.setTimeInMillis(time);
        int month = mCalendar.get(Calendar.MONTH) + 1;
        int year = mCalendar.get(Calendar.YEAR);
        mLunarDecorator = new LunarDecorator(String.valueOf(year), String.valueOf(month));
        mcv.addDecorators(new HighlightWeekendsDecorator(), new SameDayDecorator(),
                mLunarDecorator,
                new LunarDecoratorHighLight(String.valueOf(year), String.valueOf(month)));

        mcv.setOnMonthChangedListener(new OnMonthChangedListener() {
            @Override
            public void onMonthChanged(MaterialCalendarView widget, CalendarDay date) {
                // remove previous lunar decorator
                widget.removeDecorator(mLunarDecorator);
                int month = date.getMonth() + 1;
                int year = date.getYear();
                // add new decorator
                mLunarDecorator = new LunarDecorator(String.valueOf(year), String.valueOf(month));
                widget.addDecorators(mLunarDecorator, new LunarDecoratorHighLight(String.valueOf(year), String.valueOf(month)));
            }
        });
    }

    private class title implements TitleFormatter {
        @Override
        public CharSequence format(CalendarDay day) {
            int year = day.getYear();
            int month = day.getMonth() + 1;
            String str = String.format("%d-%02d", year, month);
            return str;
        }
    }

    //给周末日期设置特殊字体颜色
    public class HighlightWeekendsDecorator implements DayViewDecorator {

        private final Calendar calendar = Calendar.getInstance();

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            day.copyTo(calendar);
            int weekDay = calendar.get(Calendar.DAY_OF_WEEK);
            return weekDay == Calendar.SATURDAY || weekDay == Calendar.SUNDAY;
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new ForegroundColorSpan(Color.parseColor("#ffffff")));
        }
    }

    //给日历当天进行圆背景修饰
    public class SameDayDecorator implements DayViewDecorator {
        @Override
        public boolean shouldDecorate(CalendarDay day) {
            Date date = new Date();
            Date parse = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = dateFormat.format(date);
            try {
                parse = dateFormat.parse(dateStr);
            } catch (ParseException e) {

            }
            //String dateStr = TimeUtils.date2String(date, "yyyy-MM-dd");
            //Date parse = TimeUtils.string2Date(dateStr, "yyyy-MM-dd");
            if (parse!= null && day.getDate().equals(parse)) {
                return true;
            }
            return false;
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new CircleBackGroundSpan());
            view.addSpan(new ForegroundColorSpan(Color.parseColor("#000000")));
        }
    }
    public class CircleBackGroundSpan implements LineBackgroundSpan {
        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            Paint paint = new Paint();
            paint.setColor(Color.parseColor("#f7ce71"));
            c.drawCircle((right - left) / 2, (int)((bottom - top) / 2.5) + dip2px(8), dip2px(21), paint);
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dip2px(int dpValue) {
        final DisplayMetrics metrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final float scale = metrics.density;
        return (int) ((dpValue * scale) + 0.5f);
    }

    //给日历每一天下方添加农历字体
    public class LunarDecorator implements DayViewDecorator {
        private String year;
        private String month;

        public LunarDecorator(String year, String month) {
            this.year = year;
            this.month = month;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return true;
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new LunarSpan(year,month));
        }
    }
    public class LunarSpan implements LineBackgroundSpan {
        private String year;
        private String month;

        public LunarSpan(String year, String month) {
            this.year = year;
            this.month = month;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            StringBuffer buffer = new StringBuffer();
            Date parse = null;
//            Log.d("dingyichen", "year = " + year + " , month = " + month);
            buffer.append(year).append("-").append(month).append("-").append(text);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                parse = dateFormat.parse(buffer.toString());
            } catch (ParseException e) {

            }
//            Log.d("dingyichen", "parse : " + parse.toString()  + " , year = " + year + " , month =" + month);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parse);
//            Log.d("dingyichen", "parse : " + " , year = " + calendar.toString());

            Lunar lunar = new Lunar(calendar);
            String chinaDayString = lunar.getChinaDayString();
            //String chinaDayString = "初一";
            Paint paint = new Paint();
            paint.setTextSize(dip2px(10));
            paint.setColor(Color.parseColor("#cccccc"));
            c.drawText(chinaDayString, (right - left) / 2 - dip2px(10), (bottom - top) / 2 + dip2px(20), paint);
        }
    }


    public class LunarDecoratorHighLight implements DayViewDecorator {
        private String year;
        private String month;

        public LunarDecoratorHighLight(String year, String month) {
            this.year = year;
            this.month = month;
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            Date date = new Date();
            Date parse = null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = dateFormat.format(date);
            try {
                parse = dateFormat.parse(dateStr);
            } catch (ParseException e) {

            }
            //String dateStr = TimeUtils.date2String(date, "yyyy-MM-dd");
            //Date parse = TimeUtils.string2Date(dateStr, "yyyy-MM-dd");
            if (parse!= null && day.getDate().equals(parse)) {
                return true;
            }
            return false;
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new LunarSpanHighLight(year,month));
        }
    }
    public class LunarSpanHighLight implements LineBackgroundSpan {
        private String year;
        private String month;

        public LunarSpanHighLight(String year, String month) {
            this.year = year;
            this.month = month;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            StringBuffer buffer = new StringBuffer();
            Date parse = null;
            buffer.append(year).append("-").append(month).append("-").append(text);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            try {
                parse = dateFormat.parse(buffer.toString());
            } catch (ParseException e) {

            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parse);

            Lunar lunar = new Lunar(calendar);
            String chinaDayString = lunar.getChinaDayString();
            //String chinaDayString = "初一";
            Paint paint = new Paint();
            paint.setTextSize(dip2px(10));
            paint.setColor(Color.parseColor("#000000"));
            c.drawText(chinaDayString, (right - left) / 2 - dip2px(10), (bottom - top) / 2 + dip2px(20), paint);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.slide_right);
    }
}
