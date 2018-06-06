package com.mstarc.watchface;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;

/**
 * Created by hawking on 17-4-21.
 */

abstract public class CanvasWatchFaceService extends WallpaperService {
    private static final String TAG = CanvasWatchFaceService.class.getSimpleName();
    private boolean mRegisteredCommonReceiver = false;
    private PowerManager mPowerManager;
    private boolean mAmbient;
    public static final String INTENT_AMBIENT_ON = "com.mstarc.ambient.on";
    public static final String INTENT_AMBIENT_OFF = "com.mstarc.ambient.off";
    public static final String INTENT_SPORT_STEPS = "com.mstarc.action.totalsteps";
    int mSteps = 0;

    public CanvasWatchFaceService() {
    }

    public abstract class Engine extends WallpaperService.Engine {
        private static final int MSG_INVALIDATE = 0;
        private boolean mDrawRequested;
        private boolean mDestroyed;
        private final Choreographer mChoreographer = Choreographer.getInstance();
        private final Choreographer.FrameCallback mFrameCallback = new Choreographer.FrameCallback() {
            public void doFrame(long frameTimeNs) {
                if (!Engine.this.mDestroyed) {
                    if (Engine.this.mDrawRequested) {
                        Engine.this.draw(Engine.this.getSurfaceHolder());
                    }

                }
            }
        };
        private final Handler mHandler = new Handler() {
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_INVALIDATE:
                        Engine.this.invalidate();
                    default:
                }
            }
        };

        public Engine() {
            mPowerManager = (PowerManager) getSystemService(POWER_SERVICE);
            mAmbient = !mPowerManager.isInteractive();

        }

        public void onDestroy() {
            this.mDestroyed = true;
            this.mHandler.removeMessages(0);
            this.mChoreographer.removeFrameCallback(this.mFrameCallback);
            super.onDestroy();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceChanged");
            }

            super.onSurfaceChanged(holder, format, width, height);
            this.invalidate();
        }

        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceRedrawNeeded");
            }

            super.onSurfaceRedrawNeeded(holder);
            this.draw(holder);
        }

        public void onSurfaceCreated(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceCreated");
            }

            super.onSurfaceCreated(holder);
            this.invalidate();
        }

        public void invalidate() {
            if (!this.mDrawRequested) {
                this.mDrawRequested = true;
                this.mChoreographer.postFrameCallback(this.mFrameCallback);
            }

        }

        public void postInvalidate() {
            this.mHandler.sendEmptyMessage(0);
        }

        public void onDraw(Canvas canvas, Rect bounds) {
        }

        private void draw(SurfaceHolder holder) {
            this.mDrawRequested = false;
            Canvas canvas = holder.lockCanvas();
            if (canvas != null) {
                try {
                    this.onDraw(canvas, holder.getSurfaceFrame());
                } finally {
                    holder.unlockCanvasAndPost(canvas);
                }

            }
        }


        private final BroadcastReceiver mCommonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, intent.getAction());
                if (intent.getAction().equals(INTENT_AMBIENT_OFF)) {
                    mAmbient = false;
                    onAmbientModeChanged(mAmbient);
                    KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                    boolean locked = km.inKeyguardRestrictedInputMode();
                    Log.d(TAG, "keygaurd is locked ? " + locked);
                } else if (intent.getAction().equals(INTENT_AMBIENT_ON)) {
                    mAmbient = true;
                    onAmbientModeChanged(mAmbient);
                } else if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                    onTimeTick();
                } else if (intent.getAction().equals(INTENT_SPORT_STEPS)) {
                    mSteps = intent.getIntExtra("totalsteps", 0);
                }

            }
        };

        private void registerReceiver() {
            if (mRegisteredCommonReceiver) {
                return;
            }
            mRegisteredCommonReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(INTENT_AMBIENT_ON);
            filter.addAction(INTENT_AMBIENT_OFF);
            filter.addAction(INTENT_SPORT_STEPS);
            getApplicationContext().registerReceiver(mCommonReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredCommonReceiver) {
                return;
            }
            mRegisteredCommonReceiver = false;
            getApplicationContext().unregisterReceiver(mCommonReceiver);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                registerReceiver();
            } else {
                unregisterReceiver();
            }
        }

        abstract public void onAmbientModeChanged(boolean inAmbientMode);
        abstract public void onTimeTick();

        public final boolean isInAmbientMode() {
            return mAmbient;
        }

        int getSteps() {
            return mSteps;
        }
    }
}
