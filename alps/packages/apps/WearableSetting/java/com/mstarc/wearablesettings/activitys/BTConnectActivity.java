package com.mstarc.wearablesettings.activitys;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.WindowManager;
import com.mstarc.wearablesettings.R;
import com.mstarc.wearablesettings.common.DecorationSettingItem;
import com.mstarc.wearablesettings.common.RecyclerViewItemTouchListener;
import com.mstarc.wearablesettings.utils.ThemeUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class BTConnectActivity extends Activity implements RecyclerViewItemTouchListener.OnItemClickEventListener{

    private final static String TAG = BTConnectActivity.class.getSimpleName();
    private final static int SCAN_BT_MESSAGE = 1;
    private final static int SCAN_DEL_MESSAGE = 2;
    private final static int STOP_SCAN_MESSAGE = 3;
    private ImageView mLoading;
    private Animation mAnimation;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mBTList = new ArrayList<>();
    private BTAdapter mConnectAdapter;
    private boolean mSearchAgain = true;
    private boolean mIsReceivered = false;
    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothHeadset mBluetoothHeadset;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SCAN_BT_MESSAGE:
//                    if(mBluetoothAdapter != null) {
//                        if (mBluetoothAdapter.isDiscovering()) {
//                            mBluetoothAdapter.cancelDiscovery();
//                            sendEmptyMessageDelayed(SCAN_DEL_MESSAGE,500);
//                        }
//                    }
                    sendEmptyMessageDelayed(SCAN_DEL_MESSAGE,100);
                    break;
                case SCAN_DEL_MESSAGE:
                    mBluetoothAdapter.startDiscovery();
                    removeMessages(STOP_SCAN_MESSAGE);
                    sendEmptyMessageDelayed(STOP_SCAN_MESSAGE,12000);
                    getBluetoothA2DP();
                    break;
		case STOP_SCAN_MESSAGE:
		    if(mBluetoothAdapter != null) {
                       Log.i(TAG,"cancelDiscovery");
 	               mBluetoothAdapter.cancelDiscovery();
		    }
		    break;
            }
            super.handleMessage(msg);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btconnect);

        mLoading = (ImageView) findViewById(R.id.loading);
        //动画
        mAnimation = AnimationUtils.loadAnimation(this, R.anim.img_animation);
        LinearInterpolator lin = new LinearInterpolator();//设置动画匀速运动
        mAnimation.setInterpolator(lin);
        mLoading.setVisibility(View.GONE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        initListView();
        getBluetoothA2DP();
//        startPlay();
        final ImageView status = (ImageView)findViewById(R.id.iv_switch);
        (findViewById(R.id.titlelayout)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                operatDeivce(status);
            }
        });

        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            status.setImageBitmap(null);
            updateImageView(status, R.mipmap.kai);
            mLoading.setVisibility(View.VISIBLE);
            mLoading.clearAnimation();
            mLoading.startAnimation(mAnimation);
            openBluetooth();
        } else {
            status.setBackground(null);
            status.setImageResource(R.mipmap.guanbi);
        }

        // 设置广播信息过滤   
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
// 注册广播接收器，接收并处理搜索结果   
        if(!mIsReceivered) {
            registerReceiver(mReceiver, intentFilter);
            mIsReceivered = true;
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln 蓝牙打开常亮-----------");
    }
    private void initListView()   {
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.scanresult);
        mConnectAdapter = new BTAdapter(BTConnectActivity.this);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DecorationSettingItem(this, LinearLayoutManager.VERTICAL, R.drawable.list_divider));
        recyclerView.addOnItemTouchListener(new RecyclerViewItemTouchListener(this,this));
        recyclerView.setAdapter(mConnectAdapter);
    }

    private void operatDeivce (ImageView status){
        if(mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()){
            mBluetoothAdapter.disable();
            mLoading.setVisibility(View.GONE);
            mLoading.clearAnimation();
            status.setBackground(null);
            status.setImageResource(R.mipmap.guanbi);
            mBluetoothAdapter.cancelDiscovery();
            mBTList.clear();
            mConnectAdapter.notifyDataSetChanged();
        }else if(mBluetoothAdapter != null){
            openBluetooth();
            mLoading.setVisibility(View.VISIBLE);
            mLoading.clearAnimation();
            mLoading.startAnimation(mAnimation);
            status.setImageBitmap(null);
            updateImageView(status, R.mipmap.kai);
        }
    }

    private void openBluetooth() {
        mBTList.clear();
        mConnectAdapter.notifyDataSetChanged();
        // 打开蓝牙   
        if (!mBluetoothAdapter.isEnabled()) {
            boolean a = mBluetoothAdapter.enable();
//            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            // 设置蓝牙可见性，最多300秒   
//            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            startActivity(intent);
            // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去   
            mHandler.sendEmptyMessageDelayed(SCAN_BT_MESSAGE,5000);
        }else{
            // 寻找蓝牙设备，android会将查找到的设备以广播形式发出去   
            mHandler.sendEmptyMessageDelayed(SCAN_BT_MESSAGE,1000);
        }
    }
    //自定义广播类

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e(TAG, action);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                mLoading.setVisibility(View.GONE);
                mLoading.clearAnimation();
                if (!mBTList.contains(device)) {
                    mBTList.add(device);
                    mConnectAdapter.notifyDataSetChanged();
                    Log.e(TAG, device.getName() + "__add");
                }


                Log.e(TAG, device.getName() + "__");
                // 搜索到的不是已经绑定的蓝牙设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // 显示在TextView上
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mLoading.setVisibility(View.GONE);
                mLoading.clearAnimation();
                if(mSearchAgain) {
                    if(mBTList.size() ==0 ) {
                        mHandler.sendEmptyMessageDelayed(SCAN_DEL_MESSAGE,100);
                    }
                    mSearchAgain = false;
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE);
                if(bondState == BluetoothDevice.BOND_BONDED) {
                    int deviceClassType = device.getBluetoothClass().getDeviceClass();
                    if ((deviceClassType == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET
                            || deviceClassType == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)){
                        connect(device);
                    }
                }
            }else {
                if (device != null) {
                    if (!mBTList.contains(device)) {
                        mBTList.add(device);
                        mConnectAdapter.notifyDataSetChanged();
                        Log.e(TAG, device.getName() + "__add");
                    }
                }
            }
            Log.e(TAG, "size = " + mBTList.size());

            mConnectAdapter.notifyDataSetChanged();
        }
    };

    protected void connectDevice(BluetoothDevice bluetoothDevice) {
        try {
            // 连接建立之前的先配对
            if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                Method creMethod = BluetoothDevice.class
                        .getMethod("createBond");
                Log.e(TAG, "开始配对");
                creMethod.invoke(bluetoothDevice);
            } else {
                removeBond(BluetoothDevice.class, bluetoothDevice);
                Log.e(TAG, "配对失败" + bluetoothDevice.getBondState() + "____" + BluetoothDevice.BOND_NONE);
                int deviceClassType = bluetoothDevice.getBluetoothClass().getDeviceClass();
                if ((deviceClassType == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET
                        || deviceClassType == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES)){
                    disconnect(bluetoothDevice);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            //DisplayMessage("无法配对！");
            Log.e(TAG, "配对失败");
            e.printStackTrace();
        }
    }

    @Override
    public void onItemLongClick(View longClickedView, int adapterPosition) {

    }

    @Override
    public void onItemClick(View clickedView, int adapterPosition) {
        connectDevice(mBTList.get(adapterPosition));
    }

    @Override
    public void onItemDoubleClick(View doubleClickedView, int adapterPosition) {

    }

    public class BTAdapter extends RecyclerView.Adapter<ViewHolder> {
        // Set numbers of Card in RecyclerView.
        // Only one line for current job
        private LayoutInflater mInflater;

        public BTAdapter (Context context){
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(mInflater.inflate(R.layout.setting_bt_connect_item_layout,null));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
//            for(int i=0;i<=mBTList.size();i++)
//            {
//                if(i!=position)
//                {
//                    BluetoothDevice otherDevice = mBTList.get(position);
//                    disconnect(otherDevice);
//                    Log.i(TAG,"otherDevice "+i+"getBondState()=="+otherDevice.getBondState());
//                    if(otherDevice == null || TextUtils.isEmpty(otherDevice.getName()) || otherDevice.getName().equals("null")) {
//                        return;
//                    }
//                    holder.mTitle.setText(otherDevice.getName() + getState(otherDevice.getBondState()));
//                    if(otherDevice.getBondState() == BluetoothDevice.BOND_BONDED){
//                        holder.mTitle.setTextColor(ThemeUtils.getCurrentPrimaryColor());
//                    }else{
//                        holder.mTitle.setTextColor(getResources().getColor(R.color.color_little_white));
//                    }
//                }
//            }
            BluetoothDevice device = mBTList.get(position);
            Log.i(TAG,"device "+device+"getBondState()=="+device.getBondState()+"   position="+position+ "device.getName()=="+device.getName());
            if(device == null || TextUtils.isEmpty(device.getName()) || device.getName().equals("null")) {
                return;
            }
            holder.mTitle.setText(device.getName() + getState(device.getBondState()));
            if(device.getBondState() == BluetoothDevice.BOND_BONDED){
                holder.mTitle.setTextColor(ThemeUtils.getCurrentPrimaryColor());
            }else{
                holder.mTitle.setTextColor(getResources().getColor(R.color.color_little_white));
            }
        }

        @Override
        public int getItemCount() {
            return mBTList.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            mTitle = (TextView)itemView.findViewById(R.id.title);
        }
    }

    private boolean removeBond(Class btClass, BluetoothDevice btDevice) throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        mConnectAdapter.notifyDataSetChanged();
        return returnValue.booleanValue();
    }

    private String getState(int state) {
        switch (state) {
            case BluetoothDevice.BOND_NONE:
                Log.i(TAG,"-----------BluetoothDevice.BOND_NONE-----  ----");
                return "";
            case BluetoothDevice.BOND_BONDING:
                Log.i(TAG,"-----------BluetoothDevice.BOND_BONDING-----正在连接----");
                return "\n正在连接";
            case BluetoothDevice.BOND_BONDED:
                Log.i(TAG,"-----------BluetoothDevice.BOND_BONDED----已连接-----");
                return "\n已连接";
            default:
                Log.i(TAG,"-----------BluetoothDevice.default----   ----");
                return "";
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mIsReceivered) {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
                mIsReceivered = false;
            }
        }
        mHandler.removeMessages(SCAN_BT_MESSAGE);
        mHandler.removeMessages(STOP_SCAN_MESSAGE);
        mHandler.sendEmptyMessage(STOP_SCAN_MESSAGE);
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

    private void getBluetoothA2DP(){
        Log.i(TAG,"getBluetoothA2DP");
        if(mBluetoothAdapter == null){
            return;
        }

        if(mBluetoothA2dp == null) {
            mBluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if(profile == BluetoothProfile.A2DP){
                        //Service连接成功，获得BluetoothA2DP
                        mBluetoothA2dp = (BluetoothA2dp)proxy;
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {

                }
            },BluetoothProfile.A2DP);
        }

        if(mBluetoothHeadset == null) {
            mBluetoothAdapter.getProfileProxy(this, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if(profile == BluetoothProfile.HEADSET){
                        mBluetoothHeadset = (BluetoothHeadset)proxy;
                    }
                }
                @Override
                public void onServiceDisconnected(int profile) {

                }
            },BluetoothProfile.HEADSET);
        }

    }

    private void connect(BluetoothDevice bluetoothDevice){
        Log.i(TAG,"connect");
        if(mBluetoothA2dp == null){
            return;
        }
        if(bluetoothDevice == null){
            return;
        }
        if(mBluetoothA2dp != null) {
            try {
                Method connect = mBluetoothA2dp.getClass().getDeclaredMethod("connect", BluetoothDevice.class);
                connect.setAccessible(true);
                connect.invoke(mBluetoothA2dp,bluetoothDevice);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(TAG,"connect exception:"+e);
                e.printStackTrace();
            }
        }
        if(mBluetoothHeadset != null) {
            try {
                Method connect = mBluetoothHeadset.getClass().getDeclaredMethod("connect", BluetoothDevice.class);
                connect.setAccessible(true);
                connect.invoke(mBluetoothHeadset,bluetoothDevice);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(TAG,"connect exception:"+e);
                e.printStackTrace();
            }
        }
    }

    private void disconnect(BluetoothDevice bluetoothDevice){
        Log.i(TAG,"disconnect");
        if(bluetoothDevice == null){
            return;
        }
        if(mBluetoothA2dp != null) {
            try {
                Method disconnect = mBluetoothA2dp.getClass().getDeclaredMethod("disconnect", BluetoothDevice.class);
                disconnect.setAccessible(true);
                disconnect.invoke(mBluetoothA2dp,bluetoothDevice);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(TAG,"connect exception:"+e);
                e.printStackTrace();
            }
        }
        if(mBluetoothHeadset != null) {
            try {
                Method disconnect = mBluetoothHeadset.getClass().getDeclaredMethod("disconnect", BluetoothDevice.class);
                disconnect.setAccessible(true);
                disconnect.invoke(mBluetoothHeadset,bluetoothDevice);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                Log.e(TAG,"connect exception:"+e);
                e.printStackTrace();
            }
        }
    }

//    private void startPlay(){
//        Log.i(TAG, "startPlay");
//        AudioManager mAudioManager= (AudioManager)getSystemService(AUDIO_SERVICE);
//        if(mAudioManager!=null){
//            int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,maxVolume,0);
//        }
//
//        Uri uri = Uri.parse("android.resource://"+getPackageName()+"/"+ R.raw.speaker_test);
//        MediaPlayer mMediaPlayer = new MediaPlayer();
//        mMediaPlayer.reset();
//        try {
//            mMediaPlayer.setDataSource(this,uri);
//            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
//                @Override
//                public void onCompletion(MediaPlayer mp) {
//                    //播放完成，可以考虑断开连接
//                    disconnect();
//                }
//            });
//            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//                @Override
//                public boolean onError(MediaPlayer mp, int what, int extra) {
//                    Log.e(TAG, "Playback error.");
//                    return false;
//                }
//            });
//            mMediaPlayer.prepare();
//            mMediaPlayer.start();
//        } catch(IllegalStateException|IOException e) {
//            Log.e(TAG, "Exception: prepare or start mediaplayer");
//        }
//    }
@Override
public void finish() {
    super.finish();
    overridePendingTransition(R.anim.fade_in, R.anim.slide_right);

}
 @Override
    protected void onPause(){
        super.onPause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.d(TAG,"gln 蓝牙关闭常亮-----------");
    }
}
