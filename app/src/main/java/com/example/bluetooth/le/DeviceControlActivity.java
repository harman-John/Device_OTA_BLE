/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluetooth.le;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.bluetooth.le.R;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements OnMTUChangeListener {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    private TextView mIsSendByACKStatus,mack_text,show_index_error,where_index_error,finish,total;
    private EditText mMtuValue, mPackageSizeValue, mACKDelayTime;
    private Button mSetMTU, mReqMTU, mIsSendByACKBtn;
    private Boolean mIsSendByACK = true;
    private int mACKDelay = 0;

    private int MTU = 517;

    private PowerManager.WakeLock wakeLock = null;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mShowIndex;
    private TextView mRealText;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
    private boolean mConnected = false;
//    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private BluetoothGattCharacteristic mWriteCharacteristic = null;
    private int mIndex = 0;
    private int mPackageSize = 56;
    private int mTimeDelay = 3000;
    private RandomAccessFile mRandomAccessFile = null;
    private String mPath = Environment.getExternalStorageDirectory()+"/OTA.dfu";

    private Handler mHander = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 0:
                    startDFUUpgrade(mIndex);

                    if(!mIsSendByACK){
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                for (int i = 1; i < getPackageCount(); i++) {
                                    try {
                                        Thread.sleep(mACKDelay);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    startDFUUpgrade(i);
                                }
                            }
                        }).start();
                    }
                    break;
            }
        }
    };

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);

            mBluetoothLeService.setMTUChangeListener(DeviceControlActivity.this);
            if(null != mBluetoothLeService){
                mBluetoothLeService.requestMtu(MTU);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                Log.i(TAG,"mGattUpdateReceiver ACTION_GATT_CONNECTED");
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG,"mGattUpdateReceiver ACTION_GATT_DISCONNECTED");
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.i(TAG,"mGattUpdateReceiver ACTION_GATT_SERVICES_DISCOVERED");
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //Log.i(TAG,"mGattUpdateReceiver ACTION_DATA_AVAILABLE "+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if(charaProp == BluetoothGattCharacteristic.PROPERTY_WRITE){
                            if(null == mWriteCharacteristic){
                                mWriteCharacteristic = characteristic;
                            }

//                            mBluetoothLeService.requestMtu();
                            ReqDFUStart(characteristic);

                            return true;
                        }
                        if(charaProp == BluetoothGattCharacteristic.FORMAT_UINT16){
                            mBluetoothLeService.readCharacteristic(characteristic);
                            mBluetoothLeService.setCharacteristicNotification(characteristic, true);
                            return true;
                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
//                            // If there is an active notification on a characteristic, clear
//                            // it first so it doesn't update the data field on the user interface.
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            mBluetoothLeService.readCharacteristic(characteristic);
//                        }
//                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                            mNotifyCharacteristic = characteristic;
//                            mBluetoothLeService.setCharacteristicNotification(
//                                    characteristic, true);
//                        }
//                        return true;
                    }
                    return false;
                }
    };

    private void ReqDFUStart(final BluetoothGattCharacteristic characteristic){
        new Thread(new Runnable() {
            @Override
            public void run() {
//                                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                mBluetoothLeService.requestMtu(MTU);
//                                    }else {
//                                        mPackageSize = 20;
//                                    }

                try{
                    Thread.sleep(1000);
                }catch (Exception e){
                    e.printStackTrace();
                }

//                                    if(0==0)
//                                        return;

//                                    startDFUUpgrade(0);

//                                    byte[]CMD_REQ_DEVICE_SOFTWARE_VERSION=new byte[]{
//                                            (byte)0xaa,
//                                            (byte)0x41,
//                                            (byte)0x00
//                                    };
//
//                                    characteristic.setValue(CMD_REQ_DEVICE_SOFTWARE_VERSION);
//                                    mBluetoothLeService.writeCharacteristic(characteristic);

                byte[]CMD_REQ_DFU_START=new byte[]
                        {
                                (byte)0xaa,
                                (byte)0x43,
                                (byte)0x8,//8 bytes
                        };
                byte[] data=new byte[CMD_REQ_DFU_START.length+8];
                data[0] = CMD_REQ_DFU_START[0];
                data[1] = CMD_REQ_DFU_START[1];
                data[2] = CMD_REQ_DFU_START[2];
                byte[] fileCrc = getCRC32();
                if (fileCrc == null){
                    return;
                }
                //CRC
                data[3] = fileCrc[2];
                data[4] = fileCrc[3];
                data[5] = fileCrc[0];
                data[6] = fileCrc[1];
                //Index
                //                            data[7] = 0x0;
                //size
                byte[] sizeDfu = getDfuSize();
                data[8] =  sizeDfu[1];
                data[9] =  sizeDfu[2];
                data[10] = sizeDfu[3];

                Log.i(TAG,"Dfu Start, Write to Device, Data = "+Util.bytesToHexString(data));
                characteristic.setValue(data);
                mBluetoothLeService.writeCharacteristic(characteristic);
            }
        }).start();
    }

    public byte[] getDfuSize(){
        InputStream inStream = null;
        try{
            inStream = new FileInputStream(mPath);
            final int size = inStream.available();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    total.setText(getString(R.string.total)+String.valueOf(size));
                }
            });
            return BinaryHelper.int2ByteArray(size);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (inStream != null) {
                try{
                    inStream.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public byte[] getCRC32() {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(mPath);
            return  BinaryHelper.int2ByteArray((int) DfuCRC.fileCrc(inputStream));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
            }
        }
        return null;
    }

    private void startDFUUpgrade(final int position){
        //读取文件
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mShowIndex.setText(String.valueOf(position));
            }
        });
        byte[]  mData = new byte[mPackageSize];
        int len = -1;
        try {
            mRandomAccessFile.seek(position * mPackageSize);
            len = mRandomAccessFile.read(mData);
            if(len == -1){
                Log.w(TAG, "mRandomAccessFile len is -1 , mIndex = "+mIndex);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[]CMD_REQ_DEVICE_SOFTWARE_VERSION=new byte[]{
                (byte)0xaa,
                (byte)0x44,
                (byte)len
        };
        byte[] mPackage = new byte[CMD_REQ_DEVICE_SOFTWARE_VERSION.length+len];
        mPackage[0] = CMD_REQ_DEVICE_SOFTWARE_VERSION[0];
        mPackage[1] = CMD_REQ_DEVICE_SOFTWARE_VERSION[1];
        mPackage[2] = CMD_REQ_DEVICE_SOFTWARE_VERSION[2];

        Log.w(TAG, "mRandomAccessFile len is "+len);

        for (int j = 0; j < len; j++) {
            mPackage[j+3] = mData[j];
        }

        StringBuilder dataLog = new StringBuilder();
        for (int k = 0; k < mPackage.length; k++) {
//                    dataLog.append(String.format("%x", mPackage[k]));
            dataLog.append(mPackage[k]);
        }
        Log.i(TAG,"UUID = "+mWriteCharacteristic.getUuid());
        Log.i(TAG,"Write Data to Device, Position:"+position+",Package length = "+mPackage.length+",data ="+Util.bytesToHexString(mPackage));
        mWriteCharacteristic.setValue(mPackage);
        mBluetoothLeService.writeCharacteristic(mWriteCharacteristic);
//                Log.w(TAG, "-----------------Bright.SUNAAA-------StartTime-->");
    }

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);
        mShowIndex = (TextView) findViewById(R.id.show_index);
        mRealText = (TextView) findViewById(R.id.real_mtu);
        mMtuValue = (EditText) findViewById(R.id.mtu_value);
        mPackageSizeValue = (EditText) findViewById(R.id.package_size);
        mSetMTU = (Button) findViewById(R.id.set_vaule);
        mSetMTU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String MTUvalue = mMtuValue.getText().toString().trim();
                String PackageSizeValue = mPackageSizeValue.getText().toString().trim();
                if(MTUvalue != null && PackageSizeValue != null
                        && !MTUvalue.equals("") && !PackageSizeValue.equals("")
                        && Integer.parseInt(MTUvalue) >= Integer.parseInt(PackageSizeValue)){

                    MTU = Integer.parseInt(MTUvalue);
                    mPackageSize = Integer.parseInt(PackageSizeValue)-6;
                    if (mPackageSize <6 ){
                        Toast.makeText(DeviceControlActivity.this,"Package Size bigger than 6 ",Toast.LENGTH_SHORT).show();
                        mPackageSizeValue.setText(String.valueOf(6));
                    }
                    if(null != mBluetoothLeService){
                        mBluetoothLeService.requestMtu(MTU);
                    }
                }else {
                    Toast.makeText(DeviceControlActivity.this, "Invalid data", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mReqMTU = (Button) findViewById(R.id.req_vaule);
        mReqMTU.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != mBluetoothLeService){
                    mBluetoothLeService.requestMtu(MTU);
                }
                mMtuValue.setText(String.valueOf(MTU));
                mPackageSizeValue.setText(String.valueOf(mPackageSize));

                //Toast.makeText(DeviceControlActivity.this, "current MTU is "+ MTU +" PackageSize is "+ mPackageSize, Toast.LENGTH_SHORT).show();
            }
        });
        mIsSendByACKStatus = (TextView) findViewById(R.id.ack_status);
        mIsSendByACKStatus.setText(Boolean.toString(mIsSendByACK));
        mack_text = (TextView)findViewById(R.id.ack_text);
        show_index_error= (TextView)findViewById(R.id.show_index_error);
        where_index_error= (TextView)findViewById(R.id.where_index_error);
        finish= (TextView)findViewById(R.id.finish);
        total=(TextView)findViewById(R.id.total);
        mack_text.setVisibility(View.GONE);
        mIsSendByACKBtn = (Button) findViewById(R.id.ack_btn);
        mIsSendByACKBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mIsSendByACK){
                    mack_text.setVisibility(View.VISIBLE);
                    mACKDelayTime.setVisibility(View.VISIBLE);
                    String delayTimeVaule = mACKDelayTime.getText().toString().trim();
                    if(null != delayTimeVaule && !delayTimeVaule.equals("") && Integer.parseInt(delayTimeVaule) > 0){
                        mACKDelay = Integer.parseInt(delayTimeVaule);
                    }

                    mIsSendByACK = false;
                    mACKDelayTime.setText(String.valueOf(mACKDelay));
                    //Toast.makeText(DeviceControlActivity.this, "Current Dealy Time is "+mACKDelay+"MS", Toast.LENGTH_SHORT).show();
                }else {
                    mack_text.setVisibility(View.GONE);
                    mACKDelayTime.setVisibility(View.GONE);
                    mIsSendByACK = true;
                }
                mIsSendByACKStatus.setText(Boolean.toString(mIsSendByACK));
            }
        });
        mACKDelayTime = (EditText) findViewById(R.id.ack_delay_time);


        mACKDelayTime.setVisibility(View.GONE);
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        initFile();
    }

    private void initFile(){
        Log.w(TAG, "-------Bright.SUN------------>"+mPath);
        File mFile=new File(mPath);
        //若该文件存在
        if (mFile.exists()) {
            try{
                mRandomAccessFile = new RandomAccessFile(mFile, "r");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        if(null != wakeLock){
            wakeLock.acquire();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(null != wakeLock){
            wakeLock.release();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mGattUpdateReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            data = data.trim();
            if(data.length()>=4&& !data.substring(0,4).equalsIgnoreCase("aa45")){
                return;
            }
            mDataField.setText(data);
            int status = Integer.parseInt(data.substring(data.length()-1, data.length()));
            if(status == 1){
                finish.setText("StartOTA");
                mIndexErrorCount = 0;
                show_index_error.setText(String.valueOf(mIndexErrorCount));
                mHander.sendEmptyMessageDelayed(0, mTimeDelay);
                Log.w(TAG, "FirstTime Status  = 1");
            }else if(status == 2){
                finish.setText("Upgrading...");
                if(mIsSendByACK){
                    mIndex = mIndex + 1;
                    if(mIndex >= getPackageCount()){
                        Log.w(TAG, "PackageLength = "+mIndex);
                        return;
                    }else {
                        mHander.sendEmptyMessageDelayed(0, 0);
                        Log.w(TAG, "mIndex = "+mIndex+",PackageCount = "+getPackageCount());
                    }
                }
            }else if(status == 3){
                finish.setText("OTA Finish");
                Log.w(TAG, "Send package finish, status = 3");
            }else if (status == 0){
                mIndexErrorCount ++;
                show_index_error.setText(String.valueOf(mIndexErrorCount));
                where_index_error.setText(String.valueOf(mIndex));
            }
        }
    }
    private int mIndexErrorCount = 0;
    public int getPackageCount(){
        try {
            if(0 == mRandomAccessFile.length() % mPackageSize){
                return (int)(mRandomAccessFile.length() / mPackageSize);
            }else {
                return (int)(mRandomAccessFile.length() / mPackageSize) + 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            String exitUUID = SampleGattAttributes.lookup(uuid, unknownServiceString);
            if (exitUUID.equalsIgnoreCase(unknownServiceString)){
                continue;
            }
            currentServiceData.put(LIST_NAME, exitUUID);
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onMTUChanged(final int mtu) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MTU = mtu;
                mRealText.setText(String.valueOf(mtu));
                //Toast.makeText(DeviceControlActivity.this, "Real MTU is"+ mtu, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
