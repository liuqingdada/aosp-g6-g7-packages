package com.mstarc.wearablemms.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.activity.MessageActivity;
import com.mstarc.wearablemms.common.BitmapDrawableUtils;
import com.mstarc.wearablemms.common.ThemeUtils;
import com.mstarc.wearablemms.data.Constant;
import com.mstarc.wearablemms.data.ContactDao;
import com.mstarc.wearablemms.data.NativeImageLoader;
import com.mstarc.wearablemms.data.Sms;
import com.mstarc.wearablemms.database.DatabaseWizard;
import com.mstarc.wearablemms.database.bean.JunkMMS;
import com.mstarc.wearablemms.database.greendao.JunkMMSDao;
import com.mstarc.wearablemms.view.BaseSwipeListViewListener;
import com.mstarc.wearablemms.view.SwipeListView;

import org.byteam.superadapter.SuperAdapter;
import org.byteam.superadapter.SuperViewHolder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by liuqing
 * 17-11-24.
 * Email: 1239604859@qq.com
 */

public class JunkMmsFragment extends Fragment {
    private static final String TAG = JunkMmsFragment.class.getSimpleName();
    private View noJunkMmsView;
    private SwipeListView mJunkMmsList;
    private List<JunkMMS> mMMSList;
    private JunkMmsListAdapter mJunkMmsListAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initData();
    }

    private void initData() {
        JunkMMSDao junkMMSDao = DatabaseWizard.getInstance()
                                              .getDaoSession()
                                              .getJunkMMSDao();
        mMMSList = junkMMSDao.loadAll();
    }

    /**
     * 查询此id的所有短信
     */
    private List<Sms> getAllJunkSms(JunkMMS junkMMS) {
        List<Sms> smsList = new ArrayList<>();
        String[] projection = {
                "_id",
                "body",
                "type",
                "date",
                "address"
        };
        String selection = "thread_id = " + junkMMS.getThread_id();
        Cursor cursor = getActivity().getContentResolver()
                                     .query(Constant.URI.URI_SMS,
                                            projection,
                                            selection,
                                            null,
                                            "date asc");
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Sms sms = Sms.createFromCursor(cursor);
                Log.i(TAG, "getAllJunkSms: \n" + sms);
                smsList.add(sms);
            }
            cursor.close();
        }
        return smsList;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_junk_mms, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        noJunkMmsView = view.findViewById(R.id.no_junkmessages);
        mJunkMmsList = view.findViewById(R.id.lv_junkmms_list);
        mJunkMmsList.setSwipeListViewListener(new BaseSwipeListViewListener() {
            @Override
            public void onClickFrontView(int position) {
                Intent intent = new Intent(getActivity(), MessageActivity.class);
                //携带数据：address和会话thread_id
                JunkMMS junkMMS = mMMSList.get(position);
                intent.putExtra("address", junkMMS.getAddress());
                intent.putExtra("thread_id", junkMMS.getThread_id());
                startActivity(intent);
            }

            @Override
            public void onClickBackView(int position) {
                mJunkMmsList.closeOpenedItems();
            }

            @Override
            public void onDismiss(int[] reverseSortedPositions) {
            }

            @Override
            public void frontLongPress() {
                ConfirmDialog.Listener listener = new ConfirmDialog.Listener() {
                    @Override
                    public void onConfirm() {
                        getActivity().getContentResolver()
                                     .delete(Constant.URI.URI_SMS, null, null);
                        DatabaseWizard.getInstance()
                                      .getDaoSession()
                                      .getJunkMMSDao()
                                      .deleteAll();
                        mMMSList.clear();
                        mJunkMmsListAdapter.clear();
                    }

                    @Override
                    public void onCancel() {

                    }
                };

                new ConfirmDialog(getActivity(), R.layout.delete_confirm_dialog,
                                  listener,
                                  getResources().getString(R.string.clear_message)).show();
            }
        });

        mJunkMmsListAdapter = new JunkMmsListAdapter(getActivity(),
                                                     mMMSList,
                                                     R.layout.item_junk_mms_list);
        mJunkMmsList.setAdapter(mJunkMmsListAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        initData();
        mJunkMmsListAdapter.setData(mMMSList);
        mJunkMmsListAdapter.notifyDataSetHasChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private class JunkMmsListAdapter extends SuperAdapter<JunkMMS> {
        private ExecutorService taskService = Executors.newSingleThreadExecutor();

        JunkMmsListAdapter(Context context, List<JunkMMS> items, int layoutResId) {
            super(context, items, layoutResId);
            //enableLoadAnimation(200, new AlphaInAnimation());
            //setOnlyOnce(false);
        }

        @Override
        public void onBind(SuperViewHolder holder, int viewType, int layoutPosition,
                           final JunkMMS item) {
            if (Integer.parseInt(item.getMsg_count()) > 1) {
                holder.setVisibility(R.id.item_rl_pop_count, View.VISIBLE);

                holder.setText(R.id.item_tv_pop_count, String.valueOf(getAllJunkSms(item).size()));
            }
            //按号码查询是否存有联系人
            String name = ContactDao.getNameAddress(getActivity().getContentResolver(),
                                                    item.getAddress());
            TextView tvName = holder.findViewById(R.id.msg_junk_contact);
            if (TextUtils.isEmpty(name)) {
                tvName.setText(item.getAddress()
                                   .replace("+86", ""));
            } else {
                tvName.setText(name);
            }
            tvName.setSelected(true);
            //
            holder.setText(R.id.msg_junk_content, item.getSnippet());
            //设置时间
            //判断是否是今天
            SimpleDateFormat dateFormat;
            if (DateUtils.isToday(item.getDate())) {
                dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
                Date d = new Date(item.getDate());
                String date = dateFormat.format(d);
                //如果是,显示时分
                holder.setText(R.id.msg_junk_time, date);
            } else {
                dateFormat = new SimpleDateFormat("MM-dd", Locale.getDefault());
                Date d = new Date(item.getDate());
                String date = dateFormat.format(d);
                //如果不是,显示年月日
                holder.setText(R.id.msg_junk_time, date);
            }
            //
            String path = ContactDao.getUriByAddress(getActivity().getContentResolver(),
                                                     item.getAddress());
            ImageView ivHead = holder.findViewById(R.id.msg_junk_list_profile);
            ivHead.setTag(path);

            NativeImageLoader.NativeImageCallBack imageCallBack = new NativeImageLoader
                    .NativeImageCallBack() {
                @Override
                public void onImageLoader(Drawable bitmap, String path) {
                    ImageView mImageView = mJunkMmsList.findViewWithTag(path);
                    if (bitmap != null && mImageView != null) {
                        mImageView.setImageDrawable(bitmap);
                    }
                }
            };
            if (path != null) {
                Drawable bitmap = NativeImageLoader.getInstance()
                                                   .loadNativeImage(path, getActivity(),
                                                                    imageCallBack);
                if (bitmap != null) {
                    ivHead.setImageDrawable(bitmap);
                } else {
                    ivHead.setBackground(BitmapDrawableUtils.getTintDrawable(
                            ContextCompat.getDrawable(getActivity(), R.drawable.ic_item_junk_head),
                            BitmapDrawableUtils.getStateList(ThemeUtils.getCurrentPrimaryColor())));
                    //ThemeUtils.updateImageView(ivHead,
                    //                           R.drawable.ic_item_junk_head);
                }
            } else {
                ivHead.setImageDrawable(BitmapDrawableUtils.getTintDrawable(
                        ContextCompat.getDrawable(getActivity(), R.drawable.ic_item_junk_head),
                        BitmapDrawableUtils.getStateList(ThemeUtils.getCurrentPrimaryColor())));
                //ThemeUtils.updateImageView(ivHead,
                //                           R.drawable.ic_item_junk_head);
            }

            ImageView ivJunkDelete = holder.findViewById(R.id.delete);
            ivJunkDelete.setImageDrawable(BitmapDrawableUtils.getTintDrawable(
                    ContextCompat.getDrawable(getActivity(), R.drawable.ic_delete),
                    BitmapDrawableUtils.getStateList(ThemeUtils.getCurrentPrimaryColor())));

            ivJunkDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.i(TAG, Telephony.Sms.getDefaultSmsPackage(getActivity()));
                    mMMSList.remove(item);
                    JunkMmsListAdapter.this.setData(mMMSList);
                    JunkMmsListAdapter.this.notifyDataSetHasChanged();
                    DeleteMmsRunnable deleteMmsRunnable = new DeleteMmsRunnable(
                            item.getId(), item.getThread_id());
                    taskService.execute(deleteMmsRunnable);
                }
            });
        }

        private class DeleteMmsRunnable implements Runnable {
            private Long id;
            private int mmsID;

            DeleteMmsRunnable(Long id, int mmsID) {
                this.id = id;
                this.mmsID = mmsID;
            }

            @Override
            public void run() {
                deleteSms(getActivity(), this.mmsID, this.id);
                Log.d(TAG, "run: delete mms");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: update ui");
                        try {
                            mJunkMmsList.closeOpenedItems();
                        } catch (Exception e) {
                            Log.e(TAG, "run: closeOpenedItems. ", e);
                        }
                    }
                });
            }
        }

        /**
         * 短信的删除
         */
        private void deleteSms(Context context, int threadId, Long id) {
            String where = "thread_id = " + threadId;
            int result = context.getContentResolver()
                                .delete(Uri.parse("content://sms/conversations/" + threadId),
                                        null, null);
            int row = context.getContentResolver()
                             .delete(Constant.URI.URI_SMS, where, null);
            Log.i(TAG, "row g7= " + row);
            Log.i(TAG, "result = " + result);

            JunkMMSDao junkMMSDao = DatabaseWizard.getInstance()
                                                  .getDaoSession()
                                                  .getJunkMMSDao();
            junkMMSDao.deleteByKeyInTx(id);
        }

        @Override
        public int getCount() {
            int count = super.getCount();
            if (count == 0) {
                noJunkMmsView.setVisibility(View.VISIBLE);
            } else {
                noJunkMmsView.setVisibility(View.GONE);
            }
            return count;
        }
    }
}
