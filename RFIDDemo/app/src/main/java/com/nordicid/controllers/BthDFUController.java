package com.nordicid.controllers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.nordicid.helpers.UpdateContainer;
import com.nordicid.rfiddemo.DfuService;

import java.util.List;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceController;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * DFU Controller Used to update bluetooth firmware
 * https://github.com/NordicSemiconductor/Android-DFU-Library
 */

public class BthDFUController extends UpdateController{

    private final String TAG = "BTHDFUCONTROLLER";
    private String mDFUDeviceAddress = null;
    private BthFirmwareControllerListener mBthFwControllerListener = null;
    private Context mContext = null;
    private DfuServiceController mDFUServiceController = null;
    Handler mHandler;

    public interface BthFirmwareControllerListener {
        void onDeviceConnected(String address);
        void onDeviceConnecting(String address);
        void onDeviceDisconnected(String address);
        void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal);
        void onDfuAborted(String deviceAddress);
        void onUpdateError(String deviceAddress, int error, int errorType, String message);
        void onFirmwareValidating(String deviceAddress);
        void onDfuProcessStarting(String deviceAddress);
        void onDfuProcessStarted(String deviceAddress);
        void onEnablingDfuMode(String deviceAddress);
        void onDeviceDisconnecting(String deviceAddress);
        void onDfuCompleted(String deviceAddress);
    }

    public String getTargerAddress() { return mDFUDeviceAddress; }

    public void setBthFwControllerListener(BthFirmwareControllerListener l){
        mBthFwControllerListener = l;
    }

    public BthDFUController(Context context, String appSource, String bldrSource){
        mContext = context;
        mAppUpdateSource = appSource;
        mBldrUpdateSource = bldrSource;
        mHandler = new Handler();
    }

    public void setTargetAddress(String address){
        mDFUDeviceAddress = address;
    }

    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            mBthFwControllerListener.onDeviceConnecting(deviceAddress);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            mBthFwControllerListener.onDeviceConnected(deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            mBthFwControllerListener.onDfuProcessStarting(deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            mBthFwControllerListener.onDfuProcessStarted(deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            mBthFwControllerListener.onEnablingDfuMode(deviceAddress);
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            mBthFwControllerListener.onProgressChanged(deviceAddress,percent,speed,avgSpeed,currentPart,partsTotal);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            mBthFwControllerListener.onFirmwareValidating(deviceAddress);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            mBthFwControllerListener.onDeviceDisconnecting(deviceAddress);
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            mBthFwControllerListener.onDeviceDisconnected(deviceAddress);
        }

        /** only called when operation succeeded **/
        @Override
        public void onDfuCompleted(String deviceAddress) {
            mBthFwControllerListener.onDfuCompleted(deviceAddress);
            mDFUServiceController = null;
            disposeGatt();
        }
        /** called when the operation was aborted **/
        @Override
        public void onDfuAborted(String deviceAddress) {
            mBthFwControllerListener.onDfuAborted(deviceAddress);
            mDFUServiceController = null;
            disposeGatt();
        }
        /** on any error this will be triggered **/
        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            mBthFwControllerListener.onUpdateError(deviceAddress,error,errorType,message);
            mDFUServiceController = null;
            disposeGatt();
        }
    };

    public void registerListener(){
        DfuServiceListenerHelper.registerProgressListener(mContext,mDfuProgressListener);
    }

    public void unregisterListener(){
        DfuServiceListenerHelper.unregisterProgressListener(mContext,mDfuProgressListener);
    }

    /**
     *  Takes device address as input and calculates DFU Target address
     *  Dfu targer @ = device @ + 1
     *  ToUpperCase is necessary android does not accept lower case addresses.
     */
    public String getDfuTargetAddress(String deviceAddress){
        try {
            return String.format("%16s", Long.toHexString(Long.parseLong(deviceAddress.replace(":",""),16)+1).replaceAll("(.{2})", "$1"+':')).replace(" ", "00:").substring(0,17).toUpperCase();
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public boolean startUpdate()
    {
        if (mDFUDeviceAddress != null && mFilePath != null && mContext != null) {
            // Start with discoring services, this fixes bug in android 7 BT stack where it cannot connect to DFU service if services are not in cache
            discoverDeviceServices(mDFUDeviceAddress);
            return true;
        }
        return false;
    }

    public void abortUpdate(){
        if (mDFUServiceController != null)
            mDFUServiceController.abort();
    }

    public void pauseUpdate(){
        if (mDFUServiceController != null)
            mDFUServiceController.pause();
    }

    public void resumeUpdate(){
        if (mDFUServiceController != null)
            mDFUServiceController.resume();
    }

    /**
     * Compares remote and current versions
     * @param currentVersion
     * @param remoteVersion
     * @return true if remote is newer false if not
     */
    public boolean checkVersion(String currentVersion, String remoteVersion){
        String[] currentSplits = currentVersion.split("\\.");
        String[] remoteSplits = remoteVersion.split("\\.");
        int i = 0;
        while (i < currentSplits.length && i < remoteSplits.length && currentSplits[i].equals(remoteSplits[i])){
            i++;
        }
        if (i < currentSplits.length && i < remoteSplits.length) {
            return Integer.valueOf(currentSplits[i]).compareTo(Integer.valueOf(remoteSplits[i])) < 0;
        }
        return currentSplits.length - remoteSplits.length < 0;
    }

    private BluetoothGatt mGatt = null;
    private boolean mWaitingForConnect = false;
    private boolean mWaitingForDiscover = false;
    private boolean mRetry = true;

    private void startDfuUpdate()
    {
        DfuServiceListenerHelper.registerProgressListener(mContext,mDfuProgressListener);
        final DfuServiceInitiator dfuStarter = new DfuServiceInitiator(mDFUDeviceAddress)
                .setKeepBond(true)
                .setDeviceName("Nordic ID device")
                // .setDisableNotification(true)
                .setForceDfu(true)
                .setZip(mFilePath);
        mDFUServiceController = dfuStarter.start(mContext, DfuService.class);
    }

    private void disposeGatt()
    {
        if (mGatt != null) {
            Log.w(TAG, "disposeGatt");
            try {
                mGatt.disconnect();
                mGatt.close();
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            mGatt = null;
        }
    }

    public void discoverDeviceServices(String address) {
        mWaitingForDiscover = false;
        mWaitingForConnect = true;
        mRetry = true;
        disposeGatt();

        BluetoothManager bluetoothManager = (BluetoothManager)mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mGatt = device.connectGatt(mContext, true, mGattCallback);
    }

    Runnable mDiscoverServicesJamCheck = new Runnable() {
        @Override
        public void run() {
            Log.w(TAG, "discoverServices jammed, restart");
            if(mGatt != null)
                mGatt.discoverServices();
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("onConnectionStateChange", "Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "gattCallback: STATE_CONNECTED");
                    mWaitingForConnect = false;
                    mWaitingForDiscover = true;
                    // post 1sec delayed, fix some huawei phones..
                    mHandler.postDelayed(new Runnable() {
                                      @Override
                                      public void run() {
                                          if (mGatt==null)
                                              return;
                                          Log.i(TAG, "gattCallback: start discover");
                                          mHandler.postDelayed(mDiscoverServicesJamCheck, 5000);
                                          mGatt.discoverServices();
                                      }
                                  }, 1000);
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    mHandler.removeCallbacks(mDiscoverServicesJamCheck);
                    Log.e(TAG, "gattCallback: STATE_DISCONNECTED; " + mWaitingForConnect + "; " + mWaitingForDiscover + "; " + mRetry);
                    if (mWaitingForConnect || mWaitingForDiscover)
                    {
                        mWaitingForConnect = false;
                        mWaitingForDiscover = false;
                        if (mRetry) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    startUpdate();
                                    mRetry = false;
                                }
                            }, 1000);
                        } else {
                            final int _status = status;
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mBthFwControllerListener.onUpdateError(mDFUDeviceAddress, _status, BluetoothProfile.STATE_DISCONNECTED, "Could not connect to device");
                                }
                            });
                        }
                    }
                    break;
                default:
                    Log.e(TAG, "gattCallback: STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mHandler.removeCallbacks(mDiscoverServicesJamCheck);
            mWaitingForDiscover = false;

            List<BluetoothGattService> services = gatt.getServices();
            if (services != null) {
                for (BluetoothGattService s : services) {
                    Log.i(TAG, "Service: " + s.toString());
                }
            }

            // post 1sec delayed, going around some sony phone bugs..
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "onServicesDiscovered disposeGatt");
                    disposeGatt();
                }
            }, 1000);

            // post 10sec delayed, some huawei phones needs long delay before start..
            // Otherwise update will fail during update to CRC error
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "onServicesDiscovered startdfu");
                    startDfuUpdate();
                }
            }, 10000);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(TAG, "onCharacteristicRead: " + characteristic.toString());
        }
    };
}