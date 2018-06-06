package com.mstarc.wearablemms.common;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.os.Vibrator;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mstarc.wearablemms.R;

import java.util.List;

public class KeyboardUtil {
	private KeyboardView keyboardView;
	private Keyboard k1;// 字母键盘

	private EditText ed;
	private Context ctx;

	public KeyboardUtil(ViewGroup act, Context ctx, EditText edit) {
		this.ed = edit;
		if(ThemeUtils.isProductG7())
		    k1 = new Keyboard(ctx, R.xml.qwerty);
		else
	            k1 = new Keyboard(ctx, R.xml.qwerty_g6);
		keyboardView = (KeyboardView) act.findViewById(R.id.keyboard_view);
		keyboardView.setKeyboard(k1);
		keyboardView.setEnabled(true);
		keyboardView.setPreviewEnabled(true);
		keyboardView.setOnKeyboardActionListener(listener);
		this.ctx = ctx;
		List<Keyboard.Key> keylist = k1.getKeys();
		for(Keyboard.Key key:keylist){
			if(key.codes[0] == Keyboard.KEYCODE_DELETE) {
				int color = ThemeUtils.getCurrentPrimaryColor();
				ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
				Drawable drawable = ContextCompat.getDrawable(ctx, R.mipmap.input_delete);
				drawable.clearColorFilter();
				drawable.mutate().setColorFilter(filter);
				key.icon = drawable;
			}
			if(key.codes[0] == Keyboard.KEYCODE_DONE) {
				int color = ThemeUtils.getCurrentPrimaryColor();
				ColorFilter filter = new LightingColorFilter(Color.BLACK, color);
				Drawable drawable = ContextCompat.getDrawable(ctx, R.mipmap.input_sure);
				drawable.clearColorFilter();
				drawable.mutate().setColorFilter(filter);
				key.icon = drawable;
			}
		}
	}

	private OnKeyboardActionListener listener = new OnKeyboardActionListener() {
		@Override
		public void swipeUp() {
		}

		@Override
		public void swipeRight() {
		}

		@Override
		public void swipeLeft() {
		}

		@Override
		public void swipeDown() {
		}

		@Override
		public void onText(CharSequence text) {
		}

		@Override
		public void onRelease(int primaryCode) {
		}

		@Override
		public void onPress(int primaryCode) {
		}

		@Override
		public void onKey(int primaryCode, int[] keyCodes) {
			Editable editable = ed.getText();
			int start = ed.getSelectionStart();
			if (primaryCode == Keyboard.KEYCODE_DONE) {// 完成
				hideKeyboard();
				if(mInputFinishedListener !=null) {
					mInputFinishedListener.getPhoneNum(editable.toString());
				}
			} else if (primaryCode == Keyboard.KEYCODE_DELETE) {// 回退
				if (editable != null && editable.length() > 0) {
					if (start > 0) {
						editable.delete(start - 1, start);
					}
				}
			} else {
				editable.insert(start, Character.toString((char) primaryCode));
			}
			Vibrator vibrator=(Vibrator)ctx.getSystemService(Service.VIBRATOR_SERVICE);
			vibrator.vibrate(new long[]{0,10}, -1);
		}
	};

	/**
	 * 键盘大小写切换
	 */
    public void showKeyboard() {
        int visibility = keyboardView.getVisibility();
        if (visibility == View.GONE || visibility == View.INVISIBLE) {
            keyboardView.setVisibility(View.VISIBLE);
        }
    }
    
    public void hideKeyboard() {
        int visibility = keyboardView.getVisibility();
        if (visibility == View.VISIBLE) {
            keyboardView.setVisibility(View.INVISIBLE);
        }
    }

    public interface InputFinishedListener {
		public void getPhoneNum(String phoneNum);
	}

	private InputFinishedListener mInputFinishedListener;

	public void registerListener(InputFinishedListener listener){
		mInputFinishedListener = listener;
	}

	public void unregisterListener(){
		mInputFinishedListener = null;
	}

}
