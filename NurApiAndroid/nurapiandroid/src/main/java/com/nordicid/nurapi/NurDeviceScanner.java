package com.nordicid.nurapi;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.nordicid.nurapi.BleScanner.BleScannerListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static android.content.Context.LOCATION_SERVICE;

public class NurDeviceScanner implements BleScannerListener {

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

        mScanning = true;
        mScanPeriod = timeout;
        Log.i(TAG, "scanDevices; timeout " + timeout);

        /** notify scan started **/
        mListener.onScanStarted();

        if (requestingIntDevice()) {
            Log.i(TAG,"Add internal reader device");
            addDevice(getIntDeviceSpec());
        }

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

        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScan();
            }
        }, mScanPeriod);

        return true;
    }

    public void scanDevices() {
        if(mScanning == false)
            scanDevices(mScanPeriod, mCheckNordicID);
    }

    public void stopScan() {
        if (mScanning) {
            mScanning = false;
            BleScanner.getInstance().unregisterListener(this);
            if (mListener != null)
                mListener.onScanFinished();
        }
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

    private boolean requestingIntDevice() {
        if (Build.MANUFACTURER.toLowerCase().contains("nordicid"))
            return true;

        return false;
    }

    private void addDevice(NurDeviceSpec device) {
        if (!mScanning || device.getName() == null)
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
            Log.i(TAG, "New device found : " + device.getSpec());
            mDeviceList.add(device);

            if(mListener != null)
                mListener.onDeviceFound(device);
        }
    }

    public List<NurDeviceSpec> getDeviceList(){ return mDeviceList; }

    public NurDeviceSpec getIntDeviceSpec() {
        return new NurDeviceSpec("type=INT;addr=integrated_reader;name=Integrated Reader");
    }

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
            while (mScanning) {
                theDevices = mApi.queryEthDevices();
                for (NurEthConfig cfg : theDevices) {
                    if (cfg.hostMode == 0) // Only show server mode devices
                        postNewDevice(getEthDeviceSpec(cfg));
                }
            }
        }
        catch (Exception ex)
        {
            // TODO
        }
        mEthQueryRunning = false;
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

    @Override
    public void onBleDeviceFound(final BluetoothDevice device, final String name, final int rssi)
    {
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

    public NurDeviceSpec getNearbyBleDeviceSpec() {
        return new NurDeviceSpec("type=BLE;addr=nearby;name=Nearby Bluetooth");
    }

    public void queryBLEDevices()
    {
        // Add nearby
        addDevice(getNearbyBleDeviceSpec());

        // Add paired
        Set<BluetoothDevice> pairedDevices = BleScanner.getInstance().getPairedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (BleScanner.isBleDevice(device) && checkNIDBLEFilter(device.getName()))
                    addDevice(getBtDeviceSpec(device, device.getName(), true, 0));
            }
        }

        // Start BLE scan
        Log.i(TAG, "Start BLE scan; " + mOwner);
        BleScanner.getInstance().registerScanListener(this);
    }

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
