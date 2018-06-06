package com.mstarc.record.wearablerecorder.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mstarc.record.wearablerecorder.R;
import com.mstarc.record.wearablerecorder.RecordManager;
import com.mstarc.record.wearablerecorder.ThemeUtils;
import com.mstarc.record.wearablerecorder.view.DecorationSettingItem;

/**
 * A simple {@link Fragment} subclass.
 */
public class RecordListFragment extends Fragment implements RecordManager.Listener {


    LinearLayout noRecordTip;
    private RecoderRecyclerViewAdapter mAdapter;
    public RecordListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_record_list, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.record_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext(), LinearLayoutManager.VERTICAL, false));
        mAdapter = new RecoderRecyclerViewAdapter();
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DecorationSettingItem(getActivity(), LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        RecordManager.getInstance().setListener(this);
        noRecordTip = (LinearLayout) view.findViewById(R.id.no_record_tip);
        ((TextView) view.findViewById(R.id.text_view_no_record)).setTextColor(ThemeUtils.getCurrentPrimaryColor());
        refreshView();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void refreshView() {
        noRecordTip.post(new Runnable() {
            @Override
            public void run() {
                if (RecordManager.getInstance().getRecordList().size() == 0) {
                    noRecordTip.setVisibility(View.VISIBLE);
                } else {
                    noRecordTip.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onDataChanged() {
        mAdapter.notifyDataSetChanged();
        refreshView();
    }
}
