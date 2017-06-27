package com.nordicid.nurapi;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RunnableFuture;

import static android.content.Context.LOCATION_SERVICE;

/**
 * Created by Mikko on 13.6.2017.
 */

public class BleScanner {

    public static final String TAG = "BleScanner";

    public interface BleScannerListener {
        void onBleDeviceFound(final BluetoothDevice device, final String name, final int rssi);
    }

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScannerCompat mScanner;
    private Handler mHandler;

    private boolean mScanning = false;

    private Context mOwner = null;

    private List<BleScannerListener> mListeners = new ArrayList<BleScannerListener>();

    private int mScanPeriod = 20000;

    private BleScanner(Context context) {
        mOwner = context;
        mHandler = new Handler();
        Log.i(TAG, "BleScanner() mOwner " + mOwner);
    }

    static BleScanner gInstance = null;
    static public void init(Context context) {
        if (context == null) {
            Log.e(TAG, "init() Context is NULL");
        }

        if (gInstance == null)
            gInstance = new BleScanner(context);
        else
            gInstance.mOwner = context;
    }

    static public BleScanner getInstance() {
        return gInstance;
    }

    public void registerScanListener(BleScannerListener listener){
        if (!mListeners.contains(listener))
            mListeners.add(listener);

        scanDevices();
    }

    public void unregisterListener(BleScannerListener listener) {
        if (mListeners.contains(listener))
            mListeners.remove(listener);
    }

    private boolean isLocationServicesEnabled() {
        LocationManager lm = (LocationManager) mOwner.getSystemService(LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            return true;
        }
        return false;
    }

    private void onScanStarted() {
        Log.i(TAG,"onScanStarted() mScanning " + mScanning);

        if (mScanning)
            return;

        mScanning = true;

        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                onScanFinished();
            }
        }, mScanPeriod);

        //mBluetoothAdapter.startLeScan(mLeScanCallback);
        ScanSettings settings = new ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0).build();
        mScanner.startScan(null, settings, mScanCallback);
    }

    static public Set<BluetoothDevice> getPairedDevices() {
        BluetoothManager bluetoothManager = (BluetoothManager)getInstance().mOwner.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return new HashSet<BluetoothDevice>();
        }
        BluetoothAdapter adapter = bluetoothManager.getAdapter();
        if (adapter == null) {
            return new HashSet<BluetoothDevice>();
        }
        return adapter.getBondedDevices();
    }

    static public boolean isBleDevice(BluetoothDevice device) {
        if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE && device.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            // Log.w(TAG, "NOT BLE device; " + device.getAddress() + "; " + device.getType());
            return false;
        }
        return true;
    }

    private void onDeviceFound(final BluetoothDevice device, final String name, final int rssi) {
        //Log.i(TAG, "onDeviceFound() " + device.getAddress() + "; name " + name + "; rssi " + rssi);

        if (mListeners.size() == 0) {
            //Log.i(TAG, "onDeviceFound() No listeners");
            return;
        }

        if (name == null || !isBleDevice(device)) {
            return;
        }

        Log.i(TAG, "onDeviceFound() " + device.getAddress() + "; name " + name + "; rssi " + rssi);
        for (BleScannerListener l : mListeners) {
            l.onBleDeviceFound(device, name, rssi);
        }
    }

    private void onScanFinished() {
        Log.i(TAG, "onScanFinished() mScanning " + mScanning + "; mListeners " + mListeners.size());

        if (mScanning) {
            mScanning = false;
            mScanner.stopScan(mScanCallback);
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        if (mListeners.size() > 0) {
            Log.i(TAG, "onScanFinished() restart scan");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onScanStarted();
                }
            }, 500);
        }
    }

    private void scanDevices() {

        Log.i(TAG, "scanDevices() mOwner " + mOwner);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (mOwner.getPackageManager() != null && !mOwner.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "BT not supported; missing feature: " + PackageManager.FEATURE_BLUETOOTH_LE);
            // Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        BluetoothManager bluetoothManager =
                (BluetoothManager) mOwner.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.w(TAG, "BT not supported; BT service not available");
            Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mBluetoothAdapter == null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "BT not supported; BT adapter not available");
            // Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // If requesting only BT devices, check for BT on
        if (!mBluetoothAdapter.isEnabled())
        {
            Log.w(TAG, "BT not ON");
            Toast.makeText(mOwner, R.string.text_bt_not_on, Toast.LENGTH_LONG).show();
            return;
        }

        // Location ON is required for android M or newer
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isLocationServicesEnabled())
        {
            Log.w(TAG, "Location not ON; BT search not available");
            Toast.makeText(mOwner, R.string.text_location_not_on, Toast.LENGTH_LONG).show();
            // return; // Do not return, it might still work.. some xiaomi miui8 phones for example
        }

        if (mScanner == null)
            mScanner = BluetoothLeScannerCompat.getScanner();

        onScanStarted();
    }

    static String parseName(byte[] advData) {
        int ptr = 0;

        while (ptr < advData.length - 2) {
            int length = advData[ptr++] & 0xff;
            if (length == 0)
                break;

            final int ad_type = (advData[ptr++] & 0xff);

            switch (ad_type) {
                case 0xff:
                    break;
                case 0x08:
                case 0x09:
                    byte[] name = new byte[length - 1];
                    int i = 0;
                    length = length - 1;
                    while (length > 0) {
                        length--;
                        name[i++] = advData[ptr++];
                    }
                    String nameString = new String(name);

                    return nameString;
            }
            ptr += (length - 1);
        }
        return null;
    }

    /*private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
                {
                    if (!mScanning) {
                        Log.e(TAG, "onLeScan() Got event while NOT scanning");
                        return;
                    }
                    onDeviceFound(device, parseName(scanRecord), rssi);
                }
            };*/

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            if (!mScanning) {
                Log.e(TAG, "onScanResult() Got event while NOT scanning");
                return;
            }
            onDeviceFound(result.getDevice(), result.getScanRecord().getDeviceName(), result.getRssi());
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            Log.i(TAG, "onBatchScanResults() Found " + results.size() + " BLE devices");
            if (!mScanning) {
                Log.e(TAG, "onBatchScanResults() Got event while NOT scanning");
                return;
            }

            for (final ScanResult result : results)
            {
                final BluetoothDevice device = result.getDevice();
                onDeviceFound(device, result.getScanRecord().getDeviceName(), result.getRssi());
                if (!mScanning) {
                    break;
                }
            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            Log.e(TAG, "onScanFailed " + errorCode);
        }
    };

}
