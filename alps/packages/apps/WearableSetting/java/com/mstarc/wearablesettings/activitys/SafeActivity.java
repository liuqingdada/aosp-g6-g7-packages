package com.mstarc.wearablesettings.activitys;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;
import com.mstarc.wearablesettings.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablesettings.utils.SharedPreferencesHelper;
import com.mstarc.wearablesettings.utils.ThemeUtils;

import static com.mstarc.wearablesettings.utils.SharedPreferencesHelper.IS_NEED_PW;

public class SafeActivity extends BaseActivity implements RecyclerViewItemTouchListener.OnItemClickEventListener{

    private String[] mListItems;
    private SharedPreferencesHelper mSph;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSph = SharedPreferencesHelper.getInstance(this);
        initListView();
//        Intent intent = new Intent();
//        Bundle bundle = new Bundle();
//        bundle.putString("function", "offwrist");
//        intent.setAction("com.mstarc.watch.action.notification");
//        intent.putExtras(bundle);
//        sendBroadcast(intent);
    }

    private void initListView()   {
        mListItems = getResources().getStringArray(R.array.safe);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclelist);
        SafeAdapter mAdapter = new SafeAdapter(SafeActivity.this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addOnItemTouchListener(new RecyclerViewItemTouchListener(this,this));
        recyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        switch (adapterPosition) {
            case 0:
                Intent intent = new Intent();
                intent.putExtra("start_from_setting",true);
                intent.setClass(SafeActivity.this, ServicePassActivity.class);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    public class SafeAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public SafeAdapter (Context context){
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_safe_item_layout,null));
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, final int position) {
            holder.mTitle.setText(mListItems[position]);
            switch (position) {
                case 0:
                    updateImageView(holder.mImageNext, R.mipmap.jiantou);
                    holder.mImageBg.setVisibility(View.GONE);

                    break;
                case 1:
                    if(mSettings.isSwayWristLockScreen()) {
                        holder.mImageNext.setImageBitmap(null);
                        updateImageView(holder.mImageNext, R.mipmap.kai);
                        mSph.putBoolean(IS_NEED_PW,true);
                    }else{
                        holder.mImageNext.setBackground(null);
                        holder.mImageNext.setImageResource(R.mipmap.guanbi);
                        mSph.putBoolean(IS_NEED_PW,false);
                    }
                    break;
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(position == 1) {
                        mSettings.setSwayWristLockScreen(!mSettings.isSwayWristLockScreen());
                        if(mSettings.isSwayWristLockScreen()) {
                            holder.mImageNext.setImageBitmap(null);
                            updateImageView(holder.mImageNext, R.mipmap.kai);
                            mSph.putBoolean(IS_NEED_PW,true);
                        }else{
                            holder.mImageNext.setBackground(null);
                            holder.mImageNext.setImageResource(R.mipmap.guanbi);
                            mSph.putBoolean(IS_NEED_PW,false);
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return mListItems.length;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;
        ImageView mImageNext;
        ImageView mImageBg;
        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView)itemView.findViewById(R.id.title);
            mImageNext = (ImageView)itemView.findViewById(R.id.imagenext);
            mImageBg = (ImageView)itemView.findViewById(R.id.imagebg);
        }
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
                view.setBackground(drawable);
            }
        });
    }
}
