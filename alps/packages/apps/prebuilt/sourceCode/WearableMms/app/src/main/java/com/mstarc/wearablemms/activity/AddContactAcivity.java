package com.mstarc.wearablemms.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.mstarc.wearablemms.R;
import com.mstarc.wearablemms.data.ToastUtils;
import com.mstarc.wearablemms.fragment.AddMessageFragment;

public class AddContactAcivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showFragment(FRAGMENT_ADD_NEW_MESSAGE, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle bundle = getIntent().getExtras();
       // Toast.makeText(this, "bundle:"+bundle, Toast.LENGTH_SHORT).show();
        showFragment_2(FRAGMENT_ADD_NEW_MESSAGE, bundle);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    public void showFragment_2(int index, Bundle bundle) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment fragment = mFragments.get(index);
        fragment=null;
        fragment=new AddMessageFragment();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
    }
}
