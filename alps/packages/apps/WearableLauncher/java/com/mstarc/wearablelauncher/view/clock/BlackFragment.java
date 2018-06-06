package com.mstarc.wearablelauncher.view.clock;

/**
 * Created by hawking on 17-4-21.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mstarc.wearablelauncher.CommonManager;
import com.mstarc.wearablelauncher.R;

import com.mstarc.wearablelauncher.view.common.DepthPageTransformer;

public class BlackFragment extends Fragment implements View.OnLongClickListener {
    public BlackFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.black, container,
                false);
        view.setTag(DepthPageTransformer.ITEM_CENTER);
        view.setOnLongClickListener(this);
        return view;
    }

    @Override
    public boolean onLongClick(View v) {
        if (CommonManager.getInstance(getContext().getApplicationContext()).getmPowerMode() == CommonManager.POWERMODE_WATCH) {
            return false;
        }
        Intent intent = new Intent(getContext(), WatchFaceSelectActivity.class);
        startActivity(intent);
        return true;
    }
}
