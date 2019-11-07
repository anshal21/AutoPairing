package com.example.autopairing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class Server {

    MessageListener messageListener;
    Context mContext;
    BluetoothGattServer mGattServer;
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothLeAdvertiser mAdvertiser;
    BluetoothSocket mSocket;
    boolean stopAllConnection = false;
    public static UUID SERVICE_UUID = UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE929");
    public static UUID MESSAGE_UUID = UUID.fromString("1706BBC0-88AB-4B8D-877E-2237916EE930");

    public static UUID COMM_UUID = UUID.fromString("83769a57-e930-4496-8ece-fec16420c77c");
    public static String COMM_NAME = "uMesh";

    public Server(Context context, MessageListener messageListener) {
        this.messageListener = messageListener;
        this.mContext = context;
    }

    public void initServer() {
        mBluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            this.messageListener.onMessage("Bluetooth not supported");
            return;
        }

        if(!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            this.messageListener.onMessage("Does not have BLE");
            return;
        }

        mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if(mAdvertiser == null) {
            this.messageListener.onMessage("Failed to initialize advertiser");
            return;
        }

        mGattServer = mBluetoothManager.openGattServer(mContext, mGattServerCallback);

        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic messageCharacteristic =
                new BluetoothGattCharacteristic(MESSAGE_UUID,
                        //Read-write characteristic, supports notifications
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        service.addCharacteristic(messageCharacteristic);
        mGattServer.addService(service);
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            messageListener.onMessage("Bluetooth LE Peripheral mode is not supported in this device!!");
            return;
        }
        if (mAdvertiser == null) {
            mAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            if (mAdvertiser == null) {
                messageListener.onMessage("Error initializing BLE Advertiser");
                return;
            }
        }

        mAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        this.messageListener.onMessage("Init Success");

        this.messageListener.onMessage("Listening for connection...");
        AcceptThread acceptThread = new AcceptThread();
        acceptThread.start();
    }

    AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            messageListener.onMessage("Started advertising: " + settingsInEffect.toString());
        }
        @Override
        public void onStartFailure(int errorCode) {
            messageListener.onMessage("GATT Server Error " + errorCode);
        }
    };

    BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                messageListener.onMessage("Successul Gatt status");
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    messageListener.onMessage("New state connected with device: " + device.getName());
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    messageListener.onMessage("New state disconnected with device: " + device.getName());
                }
            } else {
                messageListener.onMessage("Unsuccessful Gatt status");
            }
        }
    };

    public class AcceptThread extends Thread {
        BluetoothServerSocket serverSocket = null;

        public void run() {
            try {
                serverSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(COMM_NAME, COMM_UUID);
                BluetoothSocket socket = serverSocket.accept();
                messageListener.onMessage("Accepted connection");
                mSocket = socket;
                while(!stopAllConnection) {
                    InputStream is = socket.getInputStream();
                    DataInputStream dis = new DataInputStream(is);
//                    byte[] buffer = new byte[1024];
//                    is.read(buffer);
                    String val = dis.readUTF();
                    messageListener.onMessage(val);
                }
            } catch (IOException e) {
                messageListener.onMessage("Error: " + e.toString());
            }

        }
    }

}