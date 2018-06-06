package com.mstarc.wearablesport.view;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cleveroad.loopbar.adapter.ICategoryItem;
import com.cleveroad.loopbar.model.CategoryItem;
import com.mstarc.wearablesport.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by wangxinzhi on 17-3-15.
 */

public class ResultView extends RelativeLayout {
    RecyclerView mList;
    Button mButton;
    ArrayList<ICategoryItem> mData = new ArrayList<>();
    ResultAdatper mAdapter;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ResultView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        init();
    }

    void init() {
        mList = (RecyclerView) findViewById(R.id.resultlist);
        mAdapter = new ResultAdatper(getContext());
        mList.setLayoutManager(new LinearLayoutManager(this.getContext(), LinearLayoutManager.VERTICAL, false));
        mList.setAdapter(mAdapter);
        mList.hasFixedSize();
        if (isInEditMode()) {
            List<ICategoryItem> items = new ArrayList<>();
            items.add(new CategoryItem(getContext().getString(R.string.sport_progress_item_bushu),"3548",null,true));
            items.add(new CategoryItem(getContext().getString(R.string.sport_progress_item_licheng),"2.35",getContext().getString(R.string.sport_progress_item_qianmi),true));
            items.add(new CategoryItem(getContext().getString(R.string.sport_progress_item_reliang),"35.4",getContext().getString(R.string.sport_progress_item_qianka),false));
            items.add(new CategoryItem(getContext().getString(R.string.sport_progress_item_sudu),"7'30''",null,false));
            setResult(items);
        }
    }

    class ResultAdatper extends RecyclerView.Adapter<VH> {
        Context mContext;
        LayoutInflater mInflater;

        public ResultAdatper(Context context) {
            this.mContext = context;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            return new VH(mInflater.inflate(R.layout.result_item, null));
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            ICategoryItem item = mData.get(position);
            holder.mName.setText(item.getName());
            holder.mValue.setText(item.getValue());
            holder.mDanwei.setText(item.getDanwei());
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class VH extends RecyclerView.ViewHolder {
        TextView mName, mValue, mDanwei;

        public VH(View itemView) {
            super(itemView);
            mName = (TextView) itemView.findViewById(R.id.sport_progress_item_name);
            mValue = (TextView) itemView.findViewById(R.id.sport_progress_item_value);
            mDanwei = (TextView) itemView.findViewById(R.id.sport_progress_item_danwei);
        }
    }

    public void setResult(Collection<ICategoryItem> list) {
        mData.clear();
        mData.addAll(list);
        mAdapter.notifyDataSetChanged();
    }
}
