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

package com.nordicid.nurapi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by Nordic ID on 18.7.2016.
 */

public class NurDeviceListActivity extends Activity  {
    public static final String TAG = "NurDeviceListActivity";

    public static final String REQUESTED_DEVICE_TYPES = "TYPE_LIST";
    public static final int REQUEST_SELECT_DEVICE = 0x800A;

    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_OK = 1;

    public static final int REQ_BLE_DEVICES = (1 << 0);
    public static final int REQ_USB_DEVICES = (1 << 1);
    public static final int REQ_ETH_DEVICES = (1 << 2);
    public static final int LAST_DEVICE = REQ_ETH_DEVICES;
    // public static final int LAST_DEVICE = REQ_USB_DEVICES;
    public static final int ALL_DEVICES = (LAST_DEVICE << 1) - 1;

    public static final String STR_SCANTIMEOUT = "SCAN_TIMEOUT";
    public static final String STR_CHECK_NID = "NID_FILTER_CHECK";

    public static final String SPECSTR = "SPECSTR";

    private BluetoothAdapter mBluetoothAdapter;

    private TextView mEmptyList;
    private int mRequestedDevices = 0;

    private static final String NID_FILTER = "nordicid_";
    private boolean mCheckNordicID = false;

    List<NurDeviceSpec> mDeviceList;

    private DeviceAdapter deviceAdapter;
    private static final long MIN_SCAN_PERIOD = 3000;
    private static final long MAX_SCAN_PERIOD = 3000;
    private static final long DEF_SCAN_PERIOD = 5000;

    // Default
    private long mScanPeriod = DEF_SCAN_PERIOD;
    private Handler mHandler;
    private boolean mScanning;

    private Runnable mEthQueryRunnable;
    private  boolean mEthQueryRunning = false;
    private NurApi mApi = null;

    private ProgressBar mScanProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        checkIfUsBOnly();

        Log.d(TAG, "onCreate");

        setContentView(R.layout.device_list);
        android.view.WindowManager.LayoutParams layoutParams = this.getWindow().getAttributes();
        layoutParams.gravity = Gravity.TOP;
        layoutParams.y = 200;

        mRequestedDevices = getIntent().getIntExtra(REQUESTED_DEVICE_TYPES, ALL_DEVICES);
        mScanPeriod = getIntent().getLongExtra(STR_SCANTIMEOUT, DEF_SCAN_PERIOD);
        mCheckNordicID = getIntent().getBooleanExtra(STR_CHECK_NID, true);

        if (mScanPeriod < MIN_SCAN_PERIOD)
            mScanPeriod = MIN_SCAN_PERIOD;

        if (mScanPeriod > MAX_SCAN_PERIOD)
            mScanPeriod = MAX_SCAN_PERIOD;

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // If requesting only BT devices, check for BT on
        if (mRequestedDevices == REQ_BLE_DEVICES && !checkForBluetooth())
        {
            Toast.makeText(this, R.string.text_bt_not_on, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mEmptyList = (TextView) findViewById(R.id.empty);
        Button cancelButton = (Button) findViewById(R.id.btn_cancel);

        if (requestingBLEDevices()) {
            cancelButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (mScanning == false) {
                        if (requestingETHDevice())
                            queryEthernetDevices();
                        scanLeDevice(true);
                    }
                    else {
                        if (!mEthQueryRunning)
                            finish();
                        else
                            showMessage("Ethernet query not ready...");
                    }
                }
            });
        } else
            cancelButton.setEnabled(false);

        mScanProgress = (ProgressBar) findViewById(R.id.scan_progress);
        mScanProgress.setVisibility(View.GONE);
        mScanProgress.setScaleY(0.5f);
        mScanProgress.setScaleX(0.5f);
        populateList();
    }

    private boolean checkForBluetooth()
    {
        if (!mBluetoothAdapter.isEnabled())
            return false;

        return true;
    }

    // TODO: USB only?
    private void checkIfUsBOnly() {
        if (mRequestedDevices == REQ_USB_DEVICES) {
            // Finish an return USB connection:
            // finishWithUSBResultResult();
        }
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

    public NurDeviceSpec getBtDeviceSpec(BluetoothDevice device, boolean bonded, int rssi) {
        return new NurDeviceSpec("type=BLE;addr="+device.getAddress()+";name="+device.getName()+";bonded="+bonded+";rssi="+rssi);
    }

    public NurDeviceSpec getEthDeviceSpec(NurEthConfig ethCfg) {
        String tr = "LAN";
        if (ethCfg.transport==2)
            tr = "WLAN";
        return new NurDeviceSpec("type=TCP;addr="+ethCfg.ip+":"+ethCfg.serverPort+";port="+ethCfg.serverPort+";name="+ethCfg.title+";transport="+tr);
    }

    public NurDeviceSpec getUsbDeviceSpec() {
        return new NurDeviceSpec("type=USB;addr=USB;name=USB Device");
    }

    private void populateList() {

        /* Initialize device list container */
        Log.d(TAG, "populateList");
        ListView newDevicesListView;

        mDeviceList = new ArrayList<NurDeviceSpec>();
        deviceAdapter = new DeviceAdapter(this, mDeviceList);
        newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(deviceAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        if (requestingUSBDevice()) {
            addDevice(getUsbDeviceSpec());
        }

        if (requestingETHDevice()) {
            queryEthernetDevices();
        }

        if (requestingBLEDevices()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (checkNIDBLEFilter(device.getName()))
                        addDevice(getBtDeviceSpec(device, true, 0));
                }
            }

            scanLeDevice(true);
        }
    }

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

    private void scanLeDevice(final boolean enable) {
        final Button cancelButton = (Button) findViewById(R.id.btn_cancel);
        if (enable) {
            mScanProgress.setVisibility(View.VISIBLE);
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    cancelButton.setText(R.string.text_scan);
                    mScanProgress.setVisibility(View.GONE);
                }
            }, mScanPeriod);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            cancelButton.setText(R.string.text_cancel);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            cancelButton.setText(R.string.text_scan);
            mScanProgress.setVisibility(View.GONE);
        }
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

        if (!mScanning)
        {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mScanProgress.setVisibility(View.GONE);
                }
            });
        }

        mEthQueryRunning = false;
    }

    private void queryEthernetDevices() {
        mApi = new NurApi();

        mScanProgress.setVisibility(View.VISIBLE);

        mEthQueryRunnable = new Runnable() {
            @Override
            public void run() {
                ethQueryWorker();
            }
        };

        mEthQueryRunning = true;
        (new Thread(mEthQueryRunnable)).start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                	
                	  runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (checkNIDBLEFilter(device.getName()))
                                addDevice(getBtDeviceSpec(device, false, rssi));
                          }
                      });
                }
            });
        }
    };

    private void postNewDevice(final NurDeviceSpec device)
    {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                addDevice(device);
            }
        });
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
        
        if (!deviceFound) {
        	mDeviceList.add(device);
            mEmptyList.setVisibility(View.GONE);
            deviceAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
       
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    @Override
    public void onStop() {
        super.onStop();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
    	
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            NurDeviceSpec deviceSpec;

            if (requestingBLEDevices())
                mBluetoothAdapter.stopLeScan(mLeScanCallback);

            deviceSpec = mDeviceList.get(position);

            Bundle b = new Bundle();

            // e.g. "type=BLE;addr=00:00:00:00:00:00;name=XXX;rssi=-44"
            b.putString(SPECSTR, deviceSpec.getSpec());

            Intent result = new Intent();
            result.putExtras(b);
            setResult(RESULT_OK, result);
            finish();
        }
    };

    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
    }
    
    class DeviceAdapter extends BaseAdapter {
        Context context;
        List<NurDeviceSpec> devices;
        LayoutInflater inflater;

        public DeviceAdapter(Context context, List<NurDeviceSpec> devices) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            this.devices = devices;
        }

        @Override
        public int getCount() {
            return devices.size();
        }

        @Override
        public Object getItem(int position) {
            return devices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;

            if (convertView != null) {
                vg = (ViewGroup) convertView;
            } else {
                vg = (ViewGroup) inflater.inflate(R.layout.device_element, null);
            }

            NurDeviceSpec  deviceSpec = devices.get(position);
            final TextView tvadd = ((TextView) vg.findViewById(R.id.address));
            final TextView tvname = ((TextView) vg.findViewById(R.id.name));
            final TextView tvpaired = (TextView) vg.findViewById(R.id.paired);
            final TextView tvrssi = (TextView) vg.findViewById(R.id.rssi);

            tvrssi.setVisibility(View.VISIBLE);

            if (deviceSpec.getType().equals("TCP")) {
                tvrssi.setText("Port: " + deviceSpec.getPort());
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.WHITE);
            }
            else if (deviceSpec.getType().equals("BLE")) {
                int rssiVal = deviceSpec.getRSSI();
                if (rssiVal < 0)    // Might be also != 0...
                    tvrssi.setText("RSSI: " + rssiVal);
                else
                    tvrssi.setText("RSSI: N/A");

                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.WHITE);
            }

            tvname.setText(deviceSpec.getName());
            tvrssi.setVisibility(View.GONE);

            if (deviceSpec.getType().equals("TCP")) {
                tvadd.setText(deviceSpec.getAddress() + " ("+deviceSpec.getPart("transport", "LAN")+")");
            } else {
                tvadd.setText(deviceSpec.getAddress());
            }

            if (deviceSpec.getBondState()) {
                tvname.setTextColor(Color.WHITE);
                tvadd.setTextColor(Color.WHITE);
                tvpaired.setTextColor(Color.GRAY);
                tvpaired.setVisibility(View.VISIBLE);
                tvpaired.setText(R.string.text_paired);
                tvrssi.setVisibility(View.VISIBLE);
                tvrssi.setTextColor(Color.WHITE);
            } else {
                tvname.setTextColor(Color.WHITE);
                tvadd.setTextColor(Color.WHITE);
                tvpaired.setVisibility(View.GONE);
            }
            return vg;
        }
    }
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public static void startDeviceRequest(Activity activity) throws InvalidParameterException
    {
        startDeviceRequest(activity, ALL_DEVICES, 0, false);
    }

    public static void startDeviceRequest(Activity activity, int devMask) throws InvalidParameterException
    {
        startDeviceRequest(activity, devMask, 0, false);
    }

    public static void startDeviceRequest(Activity activity, int devMask, long scanTimeout, boolean filterNID) throws InvalidParameterException
    {
        if (devMask == 0 || (devMask & ALL_DEVICES) == 0)
            throw new InvalidParameterException("startDeviceRequest(): no devices specified or context is invalid");

        Intent newIntent = new Intent(activity.getApplicationContext(), NurDeviceListActivity.class);
        newIntent.putExtra(REQUESTED_DEVICE_TYPES, devMask & ALL_DEVICES);
        newIntent.putExtra(STR_SCANTIMEOUT, scanTimeout);
        newIntent.putExtra(STR_CHECK_NID, filterNID);

        activity.startActivityForResult(newIntent, NurDeviceListActivity.REQUEST_SELECT_DEVICE);
    }
}

