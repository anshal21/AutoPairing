package com.example.autopairing;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Client {
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    Context mContext;
    MessageListener messageListener;
    Activity mActivity;
    ConnectThread mConnectThread;

    public Client(Activity activity, Context context, MessageListener messageListener) {
        this.mActivity = activity;
        this.messageListener = messageListener;
        this.mContext = context;
    }

    public void initClient() {
        mBluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            this.messageListener.onMessage("Bluetooth not supported");
            return;
        }
        checkPermission();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        mContext.registerReceiver(mReceiver, filter);
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED));
        startScan();

    }

    public void startScan() {
        messageListener.onMessage("Started Scan");
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(Server.SERVICE_UUID)).build();

        ArrayList<ScanFilter> filters = new ArrayList<>();
        filters.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, scanSettings, mScanCallback);
    }

    private final ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //super.onScanResult(callbackType, result);
            mConnectThread = new ConnectThread(result.getDevice());
            mConnectThread.start();
            messageListener.onMessage("Something: " + result.getDevice().toString());
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
            messageListener.onMessage("Stopped Scannnnnnnn");

            // messageListener.onMessage(result.getDevice().getUuids().toString());
            //sLogger.info("New BLE device found: {}", result.getDevice().getAddress());
            //mBeaconAdapter.addScanResult(result);
        }
        public void onBatchScanResults(List<ScanResult> results) {
        }
        public void onScanFailed(int errorCode) {
        }
    };


    private boolean checkPermission() {
        // Check Android permission
        if (ContextCompat.checkSelfPermission(mActivity,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {

                messageListener.onMessage("Missing Permission 1");
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
            else {
                messageListener.onMessage("Missing Permission 2");
                ActivityCompat.requestPermissions(mActivity,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        0);
            }
            return false;
        }

        return false;
    }

    private ScanCallback stopScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            messageListener.onMessage("Stopped Scan");
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            messageListener.onMessage("Stopped Scan");
        }

        @Override
        public void onScanFailed(int errorCode) {
            messageListener.onMessage("LE Scan Failed: "+errorCode);
        }
    };

    public class ConnectThread extends Thread {
        BluetoothDevice device = null;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;
        }

        public void run() {
           // BluetoothSocket socket = BluetoothGa
            messageListener.onMessage("Started connect thread");
            try {
                BluetoothSocket socket = this.device.createInsecureRfcommSocketToServiceRecord(Server.COMM_UUID);
                socket.connect();
                messageListener.onMessage("Connected to beacon");
                DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                dos.writeUTF("Hello World!");
                messageListener.onMessage("Connected with beacon");
            } catch (Exception e) {
                messageListener.onMessage("Failed to establish connnection " + e.toString());
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                messageListener.onMessage("La la la la la");
                // Get the BluetoothDevice object from the Intent
//                BluetoothDevice classicBtDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                mConnectThread = new ConnectThread(classicBtDevice);
//                mConnectThread.start();start
//                if (mRfcommSocketAddress.compareTo(classicBtDevice.getAddress()) == 0) {
//                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                messageListener.onMessage("Some action found");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                messageListener.onMessage("Started Scannnnnnnnnnerrrrrr");
            }
        }
    };
}
