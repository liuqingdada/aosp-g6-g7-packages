package com.mstarc.wearablesport;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cleveroad.loopbar.adapter.ICategoryItem;
import com.cleveroad.loopbar.model.CategoryItem;
import com.mstarc.watchbase.service.sportservice.bean.SportNodeInfo;
import com.mstarc.watchbase.service.sportservice.bean.SportType;
import com.mstarc.wearablesport.view.ResultView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangxinzhi on 17-3-15.
 */

public class ResultFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = ResultFragment.class.getSimpleName();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.result, container, false);
        List<ICategoryItem> items = new ArrayList<>();
        TextView heartBit = (TextView) rootView.findViewById(R.id.text_heartbit);
        SportNodeInfo sportNodeInfo;
        SportType sportType = ((MainActivity) getActivity()).getSportType();
        sportNodeInfo = (SportNodeInfo) getArguments().getSerializable("RESULT");
        boolean isG7Target = getResources().getBoolean(R.bool.g7_target);
        ImageView typeIcon = (ImageView) rootView.findViewById(R.id.progress_type_icon);
        if (sportType == SportType.RUN_INDOOR) {
            setImageViewDrawablewithTint(typeIcon, R.drawable.indor_paobu_small, !isG7Target);
        } else if (sportType == SportType.RUN_OUTDOOR) {
            setImageViewDrawablewithTint(typeIcon, R.drawable.outdor_paobu_small, !isG7Target);
        } else if (sportType == SportType.RIDE) {
            setImageViewDrawablewithTint(typeIcon, R.drawable.qixing, !isG7Target);
        }
        if (!isG7Target) {
            ImageView heartIcon = (ImageView) rootView.findViewById(R.id.sport_progress_heart_icon);
            setImageViewDrawablewithTint(heartIcon, R.drawable.xin, true);
        }
        if (sportNodeInfo == null) {
            if (sportType != SportType.RIDE) {
                items.add(new CategoryItem(getString(R.string.sport_progress_item_bushu), "0", null, false));
            }
            items.add(new CategoryItem(getString(R.string.sport_progress_item_licheng), "0", getString(R.string.sport_progress_item_qianmi), false));
            items.add(new CategoryItem(getString(R.string.sport_progress_item_reliang), "0", getString(R.string.sport_progress_item_qianka), false));
            items.add(new CategoryItem(getString(R.string.sport_progress_item_sudu), "0", null, false));

        } else {
            if (sportType != SportType.RIDE) {
				items.add(new CategoryItem(getString(R.string.sport_progress_item_bushu), "" + Math.round(sportNodeInfo.getStep()), null, false));
            }
            items.add(new CategoryItem(getString(R.string.sport_progress_item_licheng), Double.toString(sportNodeInfo.getDistance()), getString(R.string.sport_progress_item_qianmi), false));
            items.add(new CategoryItem(getString(R.string.sport_progress_item_reliang), "" + sportNodeInfo.getCal(), getString(R.string.sport_progress_item_qianka), false));
            items.add(new CategoryItem(getString(R.string.sport_progress_item_sudu), sportNodeInfo.getPace(), null, false));
            heartBit.setText("" + sportNodeInfo.getHeartRate());
        }
        TextView timeText = (TextView) rootView.findViewById(R.id.text_time);
        timeText.setText(getArguments().getString("TIME"));
        Log.d(TAG, "" + timeText.getText());
        ResultView resultView = (ResultView) rootView.findViewById(R.id.resultview);
        updateImageView(rootView.findViewById(R.id.resultconfirm), R.drawable.result_confirm);
        resultView.setResult(items);
        return rootView;
    }

    private void setImageViewDrawablewithTint(ImageView view, int resId, boolean needTint) {
        if (needTint) {
            Drawable drawable = getResources().getDrawable(resId);
            drawable.setTint(Color.WHITE);
            view.setImageDrawable(drawable);
        } else {
            updateImageView(view, resId);
        }

    }

    @Override
    public void onClick(View v) {
        getActivity().finish();
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
                if (view instanceof ImageView) {
                    ((ImageView) view).setImageDrawable(drawable);
                } else {
                    view.setBackground(drawable);
                }
            }
        });
    }
}
