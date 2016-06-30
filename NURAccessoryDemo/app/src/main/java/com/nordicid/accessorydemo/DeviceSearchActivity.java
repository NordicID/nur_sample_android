/*
  Copyright 2016- Nordic ID
  NORDIC ID DEMO SOFTWARE DISCLAIMER

  You are about to use Nordic ID Demo Software ("Software").
  It is explicitly stated that Nordic ID does not give any kind of warranties,
  expressed or implied, for this Software. Software is provided "as is" and with
  all faults. Under no circumstances is Nordic ID liable for any direct, special,
  incidental or indirect damages or for any economic consequential damages to you
  or to any third party.

  The use of this software indicates your complete and unconditional understanding
  of the terms of this disclaimer.

  IF YOU DO NOT AGREE OF THE TERMS OF THIS DISCLAIMER, DO NOT USE THE SOFTWARE.
*/

package com.nordicid.accessorydemo;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Activity for accessory / reader search using BLE.
 */
public class DeviceSearchActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {

    public static final String TAG = "DeviceListActivity";

    private BluetoothAdapter mBluetoothAdapter;

    private Button mControlBtn;
    private ProgressBar mProgress;

    // For progress bar and scan run checking.
    private Runnable mSearchWatcher;
    private int mCurrentProgress = -1;
    private Runnable mProgressRunnable;
    private boolean mScanning = false;
    private boolean mScanInterrupted = false;

    /** For various post during the appearance. */
    private Handler mHandler;

    /** For progress bar. */
    private static final int SEARCH_CONT_CHECK_INTERVAL = 50;

    /** Unit is milliseconds. */
    private int mSearchTimeout;
    /** Maximum the search progress bar can reach. */
    private int mProgressMax;

    // Hash map for the found devices; key is the devices MAC address string in lower case.
    private HashMap<String, BLEDeviceDescription> mDeviceMap = new HashMap<String, BLEDeviceDescription>();
    // Device array/list that matches to the indices in the list.
    private ArrayList<BLEDeviceDescription> mDescriptions = new ArrayList<BLEDeviceDescription>();

    private ListView mListView;
    private ArrayList<String> mStringList;
    private ArrayAdapter<String> mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_device_search);

        mBluetoothAdapter = DataBroker.getInstance().getBtAdapter();

        Helpers.lockToPortrait(this);
        // By default nothing was selected.
        setResult(ApplicationConstants.RESULT_CANCELED);

        mControlBtn = (Button)findViewById(R.id.dtb_device_search_control);
        mProgress = (ProgressBar)findViewById(R.id.progress_search);

        mListView = (ListView) findViewById(R.id.found_devices);
        mStringList = new ArrayList<String>();
        mListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mStringList);
        mListView.setAdapter(mListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleItemSelection(view, position, id);
            }
        });

        mSearchWatcher = new Runnable() {
            @Override
            public void run() {
                watchSearchInProgress();
            }
        };

        mProgressRunnable = new Runnable() {
            @Override
            public void run() {
                threadProgress();
            }
        };

        mHandler = new Handler(Looper.getMainLooper());

        mSearchTimeout = DataBroker.getInstance().getDeviceSearchTimeout();
        mProgressMax = (mSearchTimeout / SEARCH_CONT_CHECK_INTERVAL);

        beginSearch();
    }

    /** Handle the item i.e. BLE device selection. */
    private void handleItemSelection(View v, int position, long id)
    {
        final BLEDeviceDescription selectedDevice;

        selectedDevice = mDescriptions.get(position);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        DialogInterface.OnClickListener dialogClickListener;
        String msg = "Connect to:\n" + selectedDevice.getName() + ", " + selectedDevice.getAddress();

        dialogClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == Dialog.BUTTON_POSITIVE) {
                    handleDeviceSelection(selectedDevice);
                }
            }
        };

        builder.setMessage(msg);
        builder.setPositiveButton("Connect to this device", dialogClickListener);
        builder.setNegativeButton("Back", dialogClickListener);
        builder.create();
        builder.show();
    }

    /** Step progress bar to indicate ongoing device search. */
    private void threadProgress()
    {
        if (mCurrentProgress < 0) {
            // Case init.
            mCurrentProgress = 0;
            mProgress.setMax(mProgressMax);
        }

        if (mCurrentProgress <= mProgressMax)
            mProgress.setProgress(mCurrentProgress);
        else
            mProgress.setProgress(mProgressMax);
    }

    /** Watch if the search has been canceled; progress for tiem to time. */
    private void watchSearchInProgress()
    {
        int i;
        boolean panic = false;

        mCurrentProgress = -1;
        mHandler.post(mProgressRunnable);

        for (i = 0; i < mProgressMax; i++) {
            try {
                Thread.sleep(SEARCH_CONT_CHECK_INTERVAL);
                mCurrentProgress++;
                mProgress.post(mProgressRunnable);
                // mHandler.post(mProgressRunnable);

            } catch (Exception ex) {
                // TODO
                // Should not happen...
                panic = true;
            }

            if (!mScanning || mScanInterrupted || panic)
                break;
        }

        mCurrentProgress = -1;
        mHandler.post(mProgressRunnable);

        if (!mScanInterrupted)
            mBluetoothAdapter.stopLeScan(this);

        mScanning = false;
        mScanInterrupted = false;

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mControlBtn.setText(R.string.text_start_search);
            }
        });
    }

    /** Tries to initiate a BLE device search. */
    private void beginSearch()
    {
        mScanning = true;
        mCurrentProgress = -1;
        mControlBtn.setText(R.string.text_stop_device_search);

        try {
            scanLeDevice(true);
        }
        catch (Exception ex) {
            mScanning = false;
            Helpers.shortToast(this, "Scan start error:\n" + ex.getMessage());
        }
        if (mScanning) {
            (new Thread(mSearchWatcher)).start();
        }
    }

    /** Stop an ongoing BLE device search. */
    private void stopSearch()
    {
        mScanning = false;
        scanLeDevice(false);
    }

    /** Search start/stop. */
    public void onSearchControlButtonClick(View v)
    {
        if (mScanning)
            stopSearch();
        else
            beginSearch();
    }

    // Selected index is the list view index.
    private void handleDeviceSelection(BLEDeviceDescription selectedDevice)
    {
        stopSearch();

        Intent resultIntent = new Intent();
        String address, name;

        address = selectedDevice.getAddress();
        name = selectedDevice.getName();

        resultIntent.putExtra(ApplicationConstants.BLE_SELECTED_ADDRESS, address);
        resultIntent.putExtra(ApplicationConstants.BLE_SELECTED_NAME, name);

        setResult(ApplicationConstants.RESULT_OK, resultIntent);

        finish();
    }

    @Override
    /** Cancel. */
    public void onBackPressed() {
        stopSearch();
        setResult(ApplicationConstants.RESULT_CANCELED);
        finish();
    }

    /** Enable/disable BLE scan. */
    private void scanLeDevice(boolean enable) {
        if (enable) {
            mBluetoothAdapter.startLeScan(this);
            mScanning = true;
        }
        else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(this);
        }
    }

    /** Checks whether found device is already known. */
    private void checkAddNewDevice(BluetoothDevice device)
    {
        String strName;
        String strAddress, strAddressLwr;
        String strDesc = "";

        BLEDeviceDescription devObj;

        strName = device.getName();
        strAddress = device.getAddress();
        strAddressLwr = strAddress.toLowerCase();
        devObj = mDeviceMap.get(strAddressLwr);

        if (devObj == null) {
            BLEDeviceDescription newDevice = new BLEDeviceDescription(strAddress, strName);
            strDesc = newDevice.getStringDesc();

            mDeviceMap.put(strAddressLwr, newDevice);
            mDescriptions.add(newDevice);
            mStringList.add(strDesc);
            mListAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi,
                         byte[] scanRecord) {
        checkAddNewDevice(device);
    }
}

