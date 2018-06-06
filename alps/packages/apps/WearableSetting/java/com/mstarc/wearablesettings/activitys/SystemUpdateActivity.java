package com.mstarc.wearablesettings.activitys;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.fragments.UpdateConfirmFragment;
import com.mstarc.wearablesettings.fragments.VersionFragment;

import java.util.ArrayList;

public class SystemUpdateActivity extends BaseActivity{

    public static final int FRAGMENT_INDEX_VERSION = 0;
    public static final int FRAGMENT_INDEX_CONFIRM = 1;
    private ArrayList<Fragment> mFragments;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_update);
        setOTAUpdate(null);
        showFragment(FRAGMENT_INDEX_VERSION,null);
    }


    public SystemUpdateActivity() {
        mFragments = new ArrayList<Fragment>();
        mFragments.add(new VersionFragment());
        mFragments.add(new UpdateConfirmFragment());
    }

    public void showFragment(int index, Bundle bundle) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.animator.slide_fragment_in, 0, 0,
                R.animator.slide_fragment_out);
        Fragment fragment = mFragments.get(index);
        fragment.setArguments(bundle);
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

}
