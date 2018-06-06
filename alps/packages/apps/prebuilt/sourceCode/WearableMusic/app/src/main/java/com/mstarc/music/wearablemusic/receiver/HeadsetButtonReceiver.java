package com.mstarc.music.wearablemusic.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class HeadsetButtonReceiver extends BroadcastReceiver {
    private static final String TAG = "MusicSuhen";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "HeadsetButtonReceiver action: " + action);

        assert action != null;
        switch (action) {
            case Intent.ACTION_MEDIA_BUTTON:
                assert intent.getExtras() != null;
                KeyEvent keyEvent = (KeyEvent) intent.getExtras()
                                                     .get(Intent.EXTRA_KEY_EVENT);
                Log.i(TAG, "HeadsetButtonReceiver key event:\n" + keyEvent);

                assert keyEvent != null;
                switch (keyEvent.getKeyCode()) {
                    case KeyEvent.KEYCODE_CALL:
                        break;

                    case KeyEvent.KEYCODE_ENDCALL:
                        break;

                    case KeyEvent.KEYCODE_HEADSETHOOK:
                        // Used to hang up calls
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PLAY:
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        break;

                    case KeyEvent.KEYCODE_MEDIA_STOP:
                        break;

                    case KeyEvent.KEYCODE_MEDIA_NEXT:
                        break;

                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                        break;
                }

                break;
        }
    }
}
