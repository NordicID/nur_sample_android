package com.nordicid.nurapi;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Provide "UART service" to the autoconnect.
 */
public class UartService extends Service
{
    private final static String TAG = UartService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private UartServiceEvents mEvents;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;

    public static final UUID TX_POWER_UUID = UUID.fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    RingBuffer mTxBuf = new RingBuffer();
    boolean mTxActive = false;

    BluetoothGattService mRxService = null;
    BluetoothGattCharacteristic mTxChar = null;
    BluetoothGattCharacteristic mRxChar = null;
    Context mContext;
    Handler mHandler;

    public void setEventListener(UartServiceEvents ev, Context ctx)
    {
        mContext = ctx;
        mEvents = ev;
        mHandler = new Handler();
    }

    public int getConnState()
    {
        return mConnectionState;
    }

    private String stateToString(int state)
    {
        if (state == BluetoothProfile.STATE_CONNECTED)
            return "CONNECTED";
        if (state == BluetoothProfile.STATE_CONNECTING)
            return "CONNECTING";
        if (state == BluetoothProfile.STATE_DISCONNECTED)
            return "DISCONNECTED";
        if (state == BluetoothProfile.STATE_DISCONNECTING)
            return "DISCONNECTING";

        return String.format("Unknown state %d (0x%08X)", state, state);
    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
        	if (mClosed) {
        		Log.e(TAG, "onConnectionStateChange; CLOSED");
        		return;
        	}
            
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (mBluetoothGatt == null)
                {
                    return;
                }

                // Attempts to discover services after successful connection.

                Log.i(TAG, "Attempting to start service discovery");
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
	                    Log.w(TAG, "start discoverServices");
	                    mBluetoothGatt.discoverServices();
                    }
                }, 100);
                
                mHandler.postDelayed(mDiscoverServicesJamCheck, 5000);
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                mConnectionState = STATE_DISCONNECTED;
                if (mEvents != null)
                    mEvents.onConnStateChanged();

                mTxActive = false;
                mRxService = null;
                mTxChar = null;
                mRxChar = null;
                Log.i(TAG, "Disconnected from GATT server.");
            }
            else {
                Log.e(TAG, "onConnectionStateChange, unhandled state \"" + stateToString(newState) + "\".");
            }
        }
        
        Runnable mDiscoverServicesJamCheck = new Runnable() {
        	@Override
            public void run() {
                Log.w(TAG, "discoverServices jammed, restart");
                mBluetoothGatt.discoverServices();
            }
        };

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        	
        	mHandler.removeCallbacks(mDiscoverServicesJamCheck);
        	
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mRxService = mBluetoothGatt.getService(RX_SERVICE_UUID);

                if (mRxService != null) {
                    mTxChar = mRxService.getCharacteristic(TX_CHAR_UUID);
                    mRxChar = mRxService.getCharacteristic(RX_CHAR_UUID);
                } else {
                    mTxChar = mRxChar = null;
                }

                if (mRxService != null && mTxChar != null && mRxChar != null && enableTXNotification())
                {
                    Log.i(TAG, "CONNECTED");
                    if (mEvents != null)
                        mEvents.onConnStateChanged();
                } else {
                    disconnect();
                }

            } else {
                Log.e(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                //Log.i(TAG, "onCharacteristicRead " + status);

                if (mEvents != null && TX_CHAR_UUID.equals(characteristic.getUuid()))
                    mEvents.onDataAvailable(characteristic.getValue());
            }
            else
            {
                Log.e(TAG, "onCharacteristicRead FAIL " + status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            //Log.e(TAG, "onCharacteristicWrite " + status + " c " + mTxBuf.getCount() + " mTxActive " + mTxActive);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Log.d(TAG,"Characteristic write successful");

                byte [] subArray = null;
                synchronized (mTxBuf)
                {
                    if (mTxBuf.getCount() > 0)
                    {
                        int len = 20;
                        if (len > mTxBuf.getCount())
                            len = mTxBuf.getCount();
                        subArray = new byte[len];
                        mTxBuf.Read(subArray);
                    }
                }

                if (subArray != null) {
                    //Log.d(TAG,"RESTART WRITE " + subArray.length);
                    writeRXCharacteristic2(subArray);
                    return;
                }
            }
            mTxActive = false;
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            if (mEvents != null && TX_CHAR_UUID.equals(characteristic.getUuid()))
                mEvents.onDataAvailable(characteristic.getValue());
        }
    };

    public class LocalBinder extends Binder {
        UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address)
    {
        if (mBluetoothAdapter == null || address == null || address.isEmpty()) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            disconnect();
            return false;
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }

		mClosed = false;

        // Samsung BT stack..
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "connect " + address);

                if (mBluetoothGatt != null) {
                    mBluetoothGatt.disconnect();
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }

                // We want to directly connect to the device, so we are setting the autoConnect
                // parameter to false.
                //mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
                Log.i(TAG, "Trying to create a new connection");
                mConnectionState = STATE_CONNECTING;
                mBluetoothGatt = device.connectGatt(UartService.this, true, mGattCallback);
            }
        });

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.w(TAG, "mBluetoothGatt disconnect");
        mBluetoothGatt.disconnect();
        mConnectionState = STATE_DISCONNECTED;
        Log.w(TAG, "mBluetoothGatt disconnected");
    }
    
    boolean mClosed = false;

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Log.w(TAG, "mBluetoothGatt close");
                disconnect();
                if (mBluetoothGatt != null) {
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                    
                }                
                else {
                    Log.e(TAG, "mBluetoothGatt : tried to close null instance!");
                }
                Log.w(TAG, "mBluetoothGatt closed");
            }
        });
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enable TXNotification
     *
     * @return
     */
    public boolean enableTXNotification()
    {
        mBluetoothGatt.setCharacteristicNotification(mTxChar, true);

        BluetoothGattDescriptor descriptor = mTxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean ret = mBluetoothGatt.writeDescriptor(descriptor);
        mConnectionState = STATE_CONNECTED;
        return ret;
    }

    private boolean writeRXCharacteristic2(byte[] value)
    {
        int len = value.length;
        if (len > 20) {
            mTxBuf.Write(value, 20, value.length-20);
            len = 20;
            value = Arrays.copyOf(value, len);
        }

        mRxChar.setValue(value);
        mTxActive = true;
        boolean status = mBluetoothGatt.writeCharacteristic(mRxChar);
        if (!status)
            mTxActive = false;

        //Log.d(TAG, "writeRXCharacteristic2; "+len+" - status=" + status);
        return status;
    }

    public boolean writeRXCharacteristic(byte[] value)
    {
        synchronized (mTxBuf)
        {
            if (mTxActive)
            {
                mTxBuf.Write(value);
                return true;
            }
        }

        return writeRXCharacteristic2(value);
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }
}
