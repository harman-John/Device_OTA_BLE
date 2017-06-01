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
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@SuppressLint("NewApi")
public class DeviceScanActivity extends ListActivity {

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    private static final int REQUEST_ENABLE_BT = 1;

    // 3秒后停止查找搜索.
    private static final long SCAN_PERIOD = 6000;
    private static final long SEELP_TIME = 7000;
    private final  Timer timer = new Timer();
    private TimerTask task;
    private Object mScanCallback = null;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            scanLeDevice(true);
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);

        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, Util.PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                }
            }
        });

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);

        task = new TimerTask() {
            @Override
            public void run() {
                mHandler.sendEmptyMessage(1);
            }
        };
        timer.schedule(task, SEELP_TIME, SEELP_TIME);
        scanLeDevice(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case Util.PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finish();
                } else {
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//                    builder.setTitle("Functionality limited");
//                    builder.setMessage("Since location access has not been granted,app will not be able to discover beacons when in the background.");
//                    builder.setPositiveButton(android.R.string.ok, null);
//                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
//                        @Override
//                        public void onDismiss(DialogInterface dialog) {
//                        }
//
//                    });
//                    builder.show();
                }
                return;
            }
        }
    }

//    public final boolean isGpsEnable(final Context context) {
//        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
//        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
//        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
//        if (gps || network) {
//            return false;
//        }
//        return true;
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
//            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        timer.cancel();
//        scanLeDevice(false);
//        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	System.out.println("==position=="+position);
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                        mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    }else {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan((ScanCallback) initcall());
                    }
                    System.out.println("-------------Bright.Sun------------------------>stop LeScan");
//                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;

//            List<ScanFilter> bleScanFilters = new ArrayList<>();
//            bleScanFilters.add(new ScanFilter.Builder().setServiceUuid(MY_FLIP4_0).build());
//            bleScanFilters.add(new ScanFilter.Builder().setServiceUuid(MY_FLIP4_1).build());
//            bleScanFilters.add(new ScanFilter.Builder().setServiceUuid(MY_FLIP4_2).build());
//            bleScanFilters.add(new ScanFilter.Builder().setServiceUuid(MY_FLIP4_3).build());
//            bleScanFilters.add(new ScanFilter.Builder().setServiceUuid(MY_FLIP4_5).build());
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                mBluetoothAdapter.startLeScan(mLeScanCallback);
//            mBluetoothAdapter.startLeScan(mScanRequest, mLeScanCallback);
            }else {
                ScanSettings bleScanSettings = new ScanSettings.Builder().build();
                if (mBluetoothAdapter != null && mBluetoothAdapter.getBluetoothLeScanner()!= null) {
                    mBluetoothAdapter.getBluetoothLeScanner().startScan(null, bleScanSettings, (ScanCallback) initcall());
                }
            }
            System.out.println("-------------Bright.Sun------------------------>start LeScan");
        } else {
            mScanning = false;
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }else {
                mBluetoothAdapter.getBluetoothLeScanner().stopScan((ScanCallback) initcall());
            }
        }
//        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();

//        timer.cancel();
//        scanLeDevice(false);
//        mLeDeviceListAdapter.clear();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            }else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    @TargetApi(23)
    public Object initcall(){
        if(mScanCallback == null){
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    final BluetoothDevice device = result.getDevice();
                    final byte[] scanRecord = result.getScanRecord().getBytes();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("---------------------Bright.WWW------------->"+device.getName());
                            if(device.getName() != null && device.getName().equalsIgnoreCase("Bright z")){
                                System.out.print("---------------------Bright.AAA------------->"+device.getType());
                                String value =  new String(encodeHex(scanRecord, new char[] { '0', '1', '2', '3', '4', '5',
                                        '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' }));
                                String bb =  new String(value);
                                System.out.println("-------value--------->: "+value);
                            }
                            if (device.getName()!= null ) {
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }

            };
        }
        return mScanCallback;
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    System.out.println("---------------------Bright.WWW------------->"+device.getName());
                    if(device.getName() != null && device.getName().equalsIgnoreCase("Bright z")){
                        System.out.print("---------------------Bright.AAA------------->"+device.getType());
                        String value =  new String(encodeHex(scanRecord, new char[] { '0', '1', '2', '3', '4', '5',
                                '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' }));
                        String bb =  new String(value);
                        System.out.println("-------value--------->: "+value);
//                        for (byte value: scanRecord){
//                            System.out.println("-------value--------->: "+value);
//                        }
                    }

                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    public  char[] encodeHex(byte[] data, char[] toDigits) {
        int l = data.length;
        char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = toDigits[(0xF0 & data[i]) >>> 4];
            out[j++] = toDigits[0x0F & data[i]];
        }
        return out;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}