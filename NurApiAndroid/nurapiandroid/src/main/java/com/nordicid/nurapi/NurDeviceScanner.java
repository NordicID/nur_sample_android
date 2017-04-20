package com.nordicid.nurapi;

import android.app.Activity;
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
    public static final long MIN_SCAN_PERIOD = 3000;
    public static final long MAX_SCAN_PERIOD = 10000;
    public static final long DEF_SCAN_PERIOD = 5000;
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
    private Activity mOwner = null;
    private NurDeviceScannerListener mListener = null;

    public interface NurDeviceScannerListener{
        void onScanStarted();
        void onDeviceFound(NurDeviceSpec device);
        void onScanFinished();
    }

    public NurDeviceScanner(Activity context,int requestedDevices){
        this(context,requestedDevices,null);
    }

    public NurDeviceScanner(Activity context,int requestedDevices, NurDeviceScannerListener listener){
        mDeviceList = new ArrayList<NurDeviceSpec>();
        mOwner = context;
        mRequestedDevices = requestedDevices;
        mHandler = new Handler();
        mListener = listener;
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
        if (device.getName() == null)
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
        mApi = new NurApi();
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

    private boolean checkForBluetooth()
    {
        if (!mBluetoothAdapter.isEnabled())
            return false;
        return true;
    }

    public void queryBLEDevices(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!mOwner.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            mOwner.finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mOwner.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            mOwner.finish();
            return;
        }

        // If requesting only BT devices, check for BT on
        if (mRequestedDevices == REQ_BLE_DEVICES && !checkForBluetooth())
        {
            Toast.makeText(mOwner, R.string.text_bt_not_on, Toast.LENGTH_SHORT).show();
            mOwner.finish();
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
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mListener.onScanFinished();
                }
            }, mScanPeriod);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    mOwner.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (checkNIDBLEFilter(device.getName()))
                                addDevice(getBtDeviceSpec(device, false, rssi));
                        }
                    });
                }
            };

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
