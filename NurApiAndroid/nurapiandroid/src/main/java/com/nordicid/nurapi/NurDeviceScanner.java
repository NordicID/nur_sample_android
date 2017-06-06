package com.nordicid.nurapi;

/*
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
*/
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
import java.util.List;
import java.util.Set;

import static android.content.Context.LOCATION_SERVICE;

public class NurDeviceScanner {

    public static final String TAG = "NurDeviceScanner";
    private static final String NID_FILTER = "exa";
    public static final long MIN_SCAN_PERIOD = 1000;
    public static final long MAX_SCAN_PERIOD = 60000;
    public static final long DEF_SCAN_PERIOD = 10000;
    public static final int REQ_BLE_DEVICES = (1 << 0);
    public static final int REQ_USB_DEVICES = (1 << 1);
    public static final int REQ_ETH_DEVICES = (1 << 2);
    public static final int LAST_DEVICE = REQ_ETH_DEVICES;
    public static final int ALL_DEVICES = (LAST_DEVICE << 1) - 1;
    private int mRequestedDevices = ALL_DEVICES;
    private Long mScanPeriod = DEF_SCAN_PERIOD;
    private boolean mCheckNordicID;
    private BluetoothAdapter mBluetoothAdapter;
    //private BluetoothLeScannerCompat mScanner;
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

        if (timeout < MIN_SCAN_PERIOD)
            timeout = MIN_SCAN_PERIOD;
        else if (timeout > MAX_SCAN_PERIOD)
            timeout = MAX_SCAN_PERIOD;

        mScanPeriod = timeout;
        Log.i(TAG, "scanDevices; timeout " + timeout);

        /** notify scan started **/
        mListener.onScanStarted();

        if (requestingUSBDevice()) {
            Log.i(TAG,"Scanning USB Devices");
            addDevice(getUsbDeviceSpec());
        }

        if (requestingETHDevice()) {
            Log.i(TAG,"Scanning Local Ethernet Devices");
            queryEthernetDevices();
        }

        if (requestingBLEDevices()) {
            Log.i(TAG,"Scanning BLE Devices");
            queryBLEDevices();
        }

        return true;
    }

    public void scanDevices(){
        if(mScanning == false)
            scanDevices(mScanPeriod, mCheckNordicID);
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
            Log.i(TAG,"New device found : " + device.getSpec());
            mDeviceList.add(device);
            if(mListener != null)
                mListener.onDeviceFound(device);
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

    private boolean isLocationServicesEnabled() {
        LocationManager lm = (LocationManager) mOwner.getSystemService(LOCATION_SERVICE);
        if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
        {
            return true;
        }
        return false;
    }

    public void queryBLEDevices(){
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!mOwner.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "BT not supported; missing feature: " + PackageManager.FEATURE_BLUETOOTH_LE);
            // Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) mOwner.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.w(TAG, "BT not supported; BT service not available");
            Toast.makeText(mOwner, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
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

        Log.i(TAG, "Start BT scan");

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (isBleDevice(device) && checkNIDBLEFilter(device.getName()))
                    addDevice(getBtDeviceSpec(device, device.getName(), true, 0));
            }
        }
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable)
    {
        /*if (mScanner == null) {
            mScanner = BluetoothLeScannerCompat.getScanner();
        }*/

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(!mScanning)
                        return;
                    mScanning = false;
                    Log.i(TAG,"Scanning STOP BLE");
                    //mScanner.stopScan(mScanCallback);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mListener.onScanFinished();
                }
            }, mScanPeriod);

            mScanning = true;
            Log.i(TAG,"Scanning START BLE");
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            /*ScanSettings settings = new ScanSettings.Builder()
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(1000).build();
            mScanner.startScan(mScanFilters, settings, mScanCallback);*/

        } else {
            Log.i(TAG,"Scanning STOP BLE");
            mHandler.removeCallbacksAndMessages(null);
            //mScanner.stopScan(mScanCallback);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
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

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
    new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord)
        {
            final String name = parseName(scanRecord);
            if (name == null || !isBleDevice(device)) {
                return;
            }

            // Log.i(TAG, "BLE device " + name + "; " + device.getAddress() + "; rssi " + rssi);

            if (checkNIDBLEFilter(name))
            {
                if( mApi != null && mApi.getUiThreadRunner() != null) {
                    mApi.getUiThreadRunner().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addDevice(getBtDeviceSpec(device, name, false, rssi));
                        }
                    });
                } else {
                    addDevice(getBtDeviceSpec(device, name, false, rssi));
                }
            }
        }
    };

    boolean isBleDevice(BluetoothDevice device) {
        if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE && device.getType() != BluetoothDevice.DEVICE_TYPE_UNKNOWN) {
            // Log.w(TAG, "NOT BLE device; " + device.getAddress() + "; " + device.getType());
            return false;
        }
        return true;
    }
/*
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(final int callbackType, final ScanResult result) {
            // empty
        }

        @Override
        public void onBatchScanResults(final List<ScanResult> results) {
            Log.i(TAG, "Found " + results.size() + " BLE devices");
            if (!mScanning) {
                Log.e(TAG, "Got event while NOT scanning");
                return;
            }

            for (final ScanResult result : results)
            {
                final BluetoothDevice device = result.getDevice();
                final String name = result.getScanRecord().getDeviceName();
                if (name == null || !isBleDevice(device)) {
                    continue;
                }

                Log.i(TAG, "BLE device " + name + "; " + device.getAddress());

                if (checkNIDBLEFilter(name))
                {
                    if( mApi != null && mApi.getUiThreadRunner() != null) {
                        mApi.getUiThreadRunner().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addDevice(getBtDeviceSpec(device, name, false, result.getRssi()));
                            }
                        });
                    } else {
                        addDevice(getBtDeviceSpec(device, name, false, result.getRssi()));
                    }
                }

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
*/
    private boolean checkNIDBLEFilter(String deviceName)
    {
        if (!mCheckNordicID)
            return true;
        if (deviceName == null)
            return false;
        if (deviceName.toLowerCase().contains(NID_FILTER))
            return true;
        return false;
    }

    private NurDeviceSpec getBtDeviceSpec(BluetoothDevice device, String name, boolean bonded, int rssi) {
        if (name == null || name.equals("null")) {
            name = device.getAddress().toString();
        }

        return new NurDeviceSpec("type=BLE;addr="+device.getAddress()+";name="+name+";bonded="+bonded+";rssi="+rssi);
    }

    //endregion
}
