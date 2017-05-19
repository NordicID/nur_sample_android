package com.nordicid.nurapi;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NurDeviceScanner {

    public static final String TAG = "NurDeviceScanner";
    private static final String NID_FILTER = "nordicid_";
    public static final long MIN_SCAN_PERIOD = 1000;
    public static final long MAX_SCAN_PERIOD = 10000;
    public static final long DEF_SCAN_PERIOD = 60000;
    public static final int REQ_BLE_DEVICES = (1 << 0);
    public static final int REQ_USB_DEVICES = (1 << 1);
    public static final int REQ_ETH_DEVICES = (1 << 2);
    public static final int LAST_DEVICE = REQ_ETH_DEVICES;
    public static final int ALL_DEVICES = (LAST_DEVICE << 1) - 1;
    private int mRequestedDevices = ALL_DEVICES;
    private Long mScanPeriod = DEF_SCAN_PERIOD;
    private boolean mCheckNordicID;
    private BluetoothAdapter mBluetoothAdapter;
    private NurApi mApi;
    private List<NurDeviceSpec> mDeviceList;
    private Handler mHandler;
    private Runnable mEthQueryRunnable;
    private  boolean mEthQueryRunning = false;
    private boolean mScanning = false;
    private Context mOwner = null;
    private NurDeviceScannerListener mListener = null;

    public interface NurDeviceScannerListener{
        void onScanStarted();
        void onDeviceFound(NurDeviceSpec device);
        void onScanFinished();
    }

    public NurDeviceScanner(Context context,int requestedDevices, NurApi mApi){
        this(context,requestedDevices,null, mApi);
    }

    public NurDeviceScanner(Context context,int requestedDevices, NurDeviceScannerListener listener, NurApi api){
        mDeviceList = new ArrayList<NurDeviceSpec>();
        mOwner = context;
        mRequestedDevices = requestedDevices;
        mHandler = new Handler();
        mListener = listener;
        mApi = api;
    }

    public void registerScanListener(NurDeviceScannerListener listener){
        mListener = listener;
    }

    public void unregisterListener(){
        mListener = null;
    }

    public boolean scanDevices(Long timeout, boolean checkFilter){

        if(mListener == null)
            return false;

        mCheckNordicID = checkFilter;

        if (mScanPeriod < MIN_SCAN_PERIOD)
            mScanPeriod = MIN_SCAN_PERIOD;
        else if (mScanPeriod > MAX_SCAN_PERIOD)
            mScanPeriod = MAX_SCAN_PERIOD;
        else
            mScanPeriod = timeout;

        /** notify scan started **/
        mListener.onScanStarted();

        if (requestingUSBDevice()) {
            Log.e(TAG,"Scanning USB Devices");
            addDevice(getUsbDeviceSpec());
        }

        if (requestingETHDevice()) {
            Log.e(TAG,"Scanning Local Ethernet Devices");
            queryEthernetDevices();
        }

        if (requestingBLEDevices()) {
            Log.e(TAG,"Scanning BLE Devices");
            queryBLEDevices();
        }

        return true;
    }

    public void scanDevices(){
        if(mScanning == false)
            scanDevices(mScanPeriod,mCheckNordicID);
    }

    public void stopScan(){
        if(mScanning){
            scanLeDevice(false);
        }
        if(mListener != null)
            mListener.onScanFinished();
    }

    public void purge(){
        mDeviceList.clear();
    }

    private boolean requestingBLEDevices() {
        if ((mRequestedDevices & REQ_BLE_DEVICES) != 0)
            return true;
        return false;
    }

    private boolean requestingUSBDevice() {
        if ((mRequestedDevices & REQ_USB_DEVICES) != 0)
            return true;

        return false;
    }

    private boolean requestingETHDevice() {
        if ((mRequestedDevices & REQ_ETH_DEVICES) != 0)
            return true;

        return false;
    }

    private void addDevice(NurDeviceSpec device) {
        if (device.getName() == null /*|| device.getName().equals("null")*/)
            return;
        boolean deviceFound = false;
        for (NurDeviceSpec listDev : mDeviceList) {
            if (listDev.getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }
        /** device is new **/
        if (!deviceFound) {
            mDeviceList.add(device);
            if(mListener != null)
                mListener.onDeviceFound(device);
            Log.e(TAG,"New device found : " + device.getName() + " " + device.getAddress() + " " + device.getType());
        }
    }

    public List<NurDeviceSpec> getDeviceList(){ return mDeviceList; }

    //region Ethernet devices

    public boolean isEthQueryRunning(){
        return mEthQueryRunning;
    }

    public void queryEthernetDevices(){
        mEthQueryRunnable = new Runnable() {
            @Override
            public void run() {
                ethQueryWorker();
            }
        };
        mEthQueryRunning = true;
        (new Thread(mEthQueryRunnable)).start();
    }

    private void ethQueryWorker()
    {
        ArrayList<NurEthConfig> theDevices = null;
        try {
            theDevices = mApi.queryEthDevices();
            for (NurEthConfig cfg : theDevices) {
                if (cfg.hostMode==0) // Only show server mode devices
                    postNewDevice(getEthDeviceSpec(cfg));
            }
        }
        catch (Exception ex)
        {
            // TODO
        }
        mEthQueryRunning = false;
        if(!requestingBLEDevices())
            mListener.onScanFinished();
    }

    private void postNewDevice(final NurDeviceSpec device)
    {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                addDevice(device);
            }
        });
    }

    private NurDeviceSpec getEthDeviceSpec(NurEthConfig ethCfg) {
        String tr = "LAN";
        if (ethCfg.transport==2)
            tr = "WLAN";
        return new NurDeviceSpec("type=TCP;addr="+ethCfg.ip+":"+ethCfg.serverPort+";port="+ethCfg.serverPort+";name="+ethCfg.title+";transport="+tr);
    }

    //endregion

    //region USB devices

    public NurDeviceSpec getUsbDeviceSpec() {
        return new NurDeviceSpec("type=USB;addr=USB;name=USB Device");
    }

    //endregion

    //region BLE Devices

    public void queryBLEDevices(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!mOwner.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mOwner.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // If requesting only BT devices, check for BT on
        if (mRequestedDevices == REQ_BLE_DEVICES &&  !mBluetoothAdapter.isEnabled())
        {
            Toast.makeText(mOwner, R.string.text_bt_not_on, Toast.LENGTH_SHORT).show();
            return;
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (checkNIDBLEFilter(device.getName()))
                    addDevice(getBtDeviceSpec(device, true, 0));
            }
        }
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!mScanning)
                        return;
                    mScanning = false;
                    Log.e(TAG,"Scanning STOP BLE");
                    //BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
                    //scanner.stopScan(mScanCallback);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mListener.onScanFinished();
                }
            }, mScanPeriod);

            mScanning = true;
            Log.e(TAG,"Scanning START BLE");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            /*BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1000).build();
            scanner.startScan(null, settings, mScanCallback);*/

        } else {
            Log.e(TAG,"Scanning STOP BLE");
            //BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            //scanner.stopScan(mScanCallback);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    if( mApi != null && mApi.getUiThreadRunner() != null) {
                        mApi.getUiThreadRunner().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (checkNIDBLEFilter(device.getName()))
                                    addDevice(getBtDeviceSpec(device, false, rssi));
                            }
                        });
                    } else {
                        if (checkNIDBLEFilter(device.getName()))
                            addDevice(getBtDeviceSpec(device, false, rssi));
                    }
                }
            };
/*
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            // empty
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            Log.e(TAG,"Scanning BLE " + results.size());
            for (final ScanResult result : results)
            {
                final BluetoothDevice device = result.getDevice();
                Log.e(TAG,"Scanning BLE device " + device.getName() + "; " + device.getAddress());
                if (checkNIDBLEFilter(device.getName()))
                {
                    if( mApi != null && mApi.getUiThreadRunner() != null) {
                        mApi.getUiThreadRunner().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addDevice(getBtDeviceSpec(device, false, result.getRssi()));
                            }
                        });
                    } else {
                        addDevice(getBtDeviceSpec(device, false, result.getRssi()));
                    }
                }

            }
        }

        @Override
        public void onScanFailed(final int errorCode) {
            // empty
        }
    };
*/
    private boolean checkNIDBLEFilter(String deviceName)
    {
        if (!mCheckNordicID)
            return true;
        if (deviceName == null)
            return false;
        if (deviceName.trim().toLowerCase().startsWith(NID_FILTER))
            return true;
        return false;
    }

    private NurDeviceSpec getBtDeviceSpec(BluetoothDevice device, boolean bonded, int rssi) {
        return new NurDeviceSpec("type=BLE;addr="+device.getAddress()+";name="+device.getName()+";bonded="+bonded+";rssi="+rssi);
    }

    //endregion
}
