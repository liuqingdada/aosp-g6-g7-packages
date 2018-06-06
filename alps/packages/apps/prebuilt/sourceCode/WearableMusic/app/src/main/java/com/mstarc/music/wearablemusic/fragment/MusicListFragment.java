package com.mstarc.music.wearablemusic.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mstarc.music.ThemeUtils;
import com.mstarc.music.wearablemusic.MusicManager;
import com.mstarc.music.wearablemusic.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class MusicListFragment extends Fragment implements MusicManager.Listener {


    LinearLayout noMusicTip;
    private MusicRecyclerViewAdapter mAdapter;
    RecyclerView recyclerView;
    public MusicListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_record_list, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.record_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        mAdapter = new MusicRecyclerViewAdapter();
        recyclerView.setAdapter(mAdapter);
        noMusicTip = (LinearLayout) view.findViewById(R.id.no_record_tip);
        TextView text = (TextView) view.findViewById(R.id.no_record_text);
        text.setTextColor(ThemeUtils.getCurrentPrimaryColor());
        refreshView();
        MusicManager.getInstance().setListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    private void refreshView() {
        noMusicTip.post(new Runnable() {
            @Override
            public void run() {
                if (MusicManager.getInstance().getMusicList() != null) {
                    if (MusicManager.getInstance().getMusicList().size() == 0) {
                        noMusicTip.setVisibility(View.VISIBLE);
                    } else {
                        noMusicTip.setVisibility(View.INVISIBLE);
                    }
                } else {
                    noMusicTip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public void onDataChanged() {
        final int index = MusicManager.getInstance().getCurrentPlayingIndex();
        noMusicTip.post(new Runnable() {
            @Override
            public void run() {
                mAdapter.setIndex(index);
                refreshView();
                mAdapter.notifyDataSetChanged();
            }
        });
    }
}
