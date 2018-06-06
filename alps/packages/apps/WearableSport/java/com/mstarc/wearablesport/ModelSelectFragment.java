package com.mstarc.wearablesport;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mstarc.watchbase.service.sportservice.bean.SportType;
import com.mstarc.wearablesport.common.ClickHelper;

/**
 * Created by wangxinzhi on 17-3-13.
 */

public class ModelSelectFragment extends Fragment implements View.OnClickListener {
    View mViewPaobu, mViewBuxing, mViewQixing;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.mode_select, container, false);
        ViewGroup root = (ViewGroup) rootView.findViewById(R.id.model_selector_root);
        ClickHelper.addOnClickListenersRecursive(root, this);
        mViewPaobu = rootView.findViewById(R.id.selector_run_indor);
        mViewBuxing = rootView.findViewById(R.id.selector_run_out_door);
        mViewQixing = rootView.findViewById(R.id.selector_qixing);
        ImageView outdoor = (ImageView) rootView.findViewById(R.id.run_out_door_image);
        ImageView indoor = (ImageView) rootView.findViewById(R.id.run_indor_image);
        ImageView qixing = (ImageView) rootView.findViewById(R.id.qixing_image);

        updateImageView(outdoor, R.drawable.outdor_paobu);
        updateImageView(indoor, R.drawable.indor_paobu);
        updateImageView(qixing, R.drawable.qixing_mode_select);
        Settings.System.putLong(getActivity().getContentResolver(), "navigation", System.currentTimeMillis());
        return rootView;
    }

    @Override
    public void onClick(View v) {
        SportType sportType;
        switch (v.getId()) {
            case R.id.selector_run_indor:
                mViewPaobu.setSelected(true);
                mViewBuxing.setSelected(false);
                mViewQixing.setSelected(false);
                sportType = SportType.RUN_INDOOR;
                break;
            case R.id.selector_run_out_door:
                mViewPaobu.setSelected(false);
                mViewBuxing.setSelected(true);
                mViewQixing.setSelected(false);
                sportType = SportType.RUN_OUTDOOR;
                break;
            case R.id.selector_qixing:
                mViewPaobu.setSelected(false);
                mViewBuxing.setSelected(false);
                mViewQixing.setSelected(true);
                sportType = SportType.RIDE;
                break;
            default:
                return;
        }
        if (sportType!=null&&sportType.getCode() > 2) {
            showDilog(sportType);
        } else {
            ((MainActivity) getActivity()).mSportService.setMode(sportType);
            ((MainActivity) getActivity()).showFragment(MainActivity.FRAGMENT_INDEX_TARGET, null);
        }

    }

    private void showDilog(final SportType sportType) {
        final Dialog dialog = new Dialog(getActivity(), R.style.tip_dialog);//指定自定義樣式
        //1. 先获取布局的view
        RelativeLayout view = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(R.layout.switch_dialog, null);
        //2. 加载 view
        dialog.setContentView(view);//指定自定義layout
        dialog.setCanceledOnTouchOutside(false);
        //3. 获取dialog view 下的控件
        ImageView ok = (ImageView) view.findViewById(R.id.btn_Y);
        updateImageView(ok, R.drawable.btn_ok);
        //4.对控件做设置或者设置listenner
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) getActivity()).mSportService.setMode(sportType);
                ((MainActivity) getActivity()).showFragment(MainActivity.FRAGMENT_INDEX_TARGET, null);
                dialog.dismiss();
            }
        });
        //  5. 直接
        dialog.show();
    }

    private void updateImageView(final ImageView view, final int resId) {
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
