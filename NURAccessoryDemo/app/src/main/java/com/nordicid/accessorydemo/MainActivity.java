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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.nordicid.nurapi.BleScanner;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

public class MainActivity extends AppCompatActivity implements NurApiListener
{
    public static final String TAG = "AccessoryDemo_MAIN";

    // Carries class instances and application settings.
    private DataBroker mDataBroker = DataBroker.getInstance();
    // Bluetooth support detection.
    private BluetoothAdapter mMainBTAdapter = null;
    // Combines the device address and its name.
    private NurDeviceSpec mAutoConnectDevice = null;

    // "Disconnecting".
    private boolean mDisconnectOnResume = false;

    // Automatically connecting transport layer.
    private NurApiBLEAutoConnect mAutoConnectTransport = null;
    // NUR  accessory extension (barcode, battery etc.).
    private NurAccessoryExtension mAccessoryExtension = null;

    // The API available across all activities via the API handler.
    private NurApi mApi;
    // Some helper functions.
    Helpers mHelpers = new Helpers(this);

    // Prevent unnecessary connection status changes.
    private boolean mSwappingActivity = false;

    private Button mDevSearchBtn;
    private Button mInfoBtn;
    private Button mAccesoryActionsBtn;

    private Button mAccessorySetupBtn;
    private Button mAppSettingsBtn;

    private Button mRestartBtn;

    private TextView mConnectionText;
    private TextView mVersionText;

    private static final int CONN_RED = Color.rgb(200, 0, 0);
    private static final int CONN_YELLOW = Color.rgb(180, 180, 0);
    private static final int CONN_GREEN = Color.rgb(0, 200, 0);

    /** The device name changed during the connection. */
    private boolean mBLERestart = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BleScanner.init(this);
        mDataBroker.loadSettings(this);
        mAutoConnectDevice = mDataBroker.getAutoconnectDevice();

        mApi = mDataBroker.getNurApi();
        Helpers.lockToPortrait(this);

        // Use this check to determine whether BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            mHelpers.longToast(getString(R.string.ble_not_supported));
            finish();
        }

        mMainBTAdapter = mDataBroker.getBtAdapter();
        setupUpdateControls();
        showVersions(false);

        // Start connecting if stored device.
        checkAutoConnectStart();

        mApi.setUiThreadRunner(new NurApiUiThreadRunner() {
            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }
        });
    }

    private void setupUpdateControls()
    {
        mDevSearchBtn = (Button)findViewById(R.id.btn_device_search);
        mInfoBtn = (Button)findViewById(R.id.btn_information);
        mAccesoryActionsBtn = (Button)findViewById(R.id.btn_accessory_actions);

        mAppSettingsBtn = (Button)findViewById(R.id.btn_app_settings);
        mRestartBtn = (Button)findViewById(R.id.btn_ble_restart);
        mAccessorySetupBtn = (Button)findViewById(R.id.btn_accessory_settings);
        mVersionText = (TextView)findViewById(R.id.api_version_text);
        mConnectionText = (TextView)findViewById(R.id.main_connection_ind);

        mDevSearchBtn.setEnabled(true);

        mInfoBtn.setEnabled(false);
        mRestartBtn.setEnabled(false);
        mAccesoryActionsBtn.setEnabled(false);
        mAccessorySetupBtn.setEnabled(false);
    }

    private void checkAutoConnectStart()
    {
        if (mAutoConnectDevice != null && DataBroker.rememberDevice())
        {
            String connectionText = "Connect: " + mAutoConnectDevice.getAddress();
            mConnectionText.setTextColor(CONN_YELLOW);
            mConnectionText.setText(connectionText);
            tryConnect(mAutoConnectDevice);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        handleActivityResult(requestCode, resultCode, data);
    }

    /** Call when the currently connected device is selected to be "forgotten". */
    private void doDisconnect()
    {
        try {
            Log.d(TAG, "MainActivity::doDisconnect()");
            if (mAutoConnectTransport != null) {
                mAutoConnectTransport.setAddress("");
            }
        }
        catch (Exception ex) { }
    }

    /**
     * Handle device search button click.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onDeviceSearchButtonClick(View v)
    {
        handleDeviceSearchStart();
    }

    /**
     * Handle connected devices / accessory's information.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onDeviceInformationButtonClick(View v)
    {
        if (mApi != null && mApi.isConnected())
        {
            NurRespReaderInfo ri = null;
            String strMessage = "";
            String strAccessory = "";
            String strBattery = "";
            boolean isAccessory;

            isAccessory = mAccessoryExtension.isSupported();

            try
            {
                ri = mApi.getReaderInfo();
            }
            catch (Exception ex)
            {
                mHelpers.shortToast("Reader info error:\n" + ex.getMessage());
                return;
            }

            if (isAccessory)
            {
                NurAccessoryConfig cfg;
                NurAccessoryBattery battery;

                try
                {
                    battery = mAccessoryExtension.getBatteryInfo();
                    strBattery = "\n\nBattery:";
                    strBattery += ("\nCharging: " + Helpers.yesNo(battery.charging));
                    strBattery += ("\nPercentage: " + battery.getPercentageString());
                    strBattery += ("\nVoltage mV: " + battery.getVoltageString());
                    strBattery += ("\nCurrent mA: " + battery.getCurrentString());
                    strBattery += ("\nCapacity mAh: " + battery.getCapacityString());
                }
                catch (Exception ex)
                {
                    strBattery += " N/A";
                }

                try
                {
                    cfg = mAccessoryExtension.getConfig();
                    strAccessory = "\n\nAccessory information:";
                    strAccessory += ("\nBarcode / RFID HID on: " + Helpers.yesNo(cfg.getHidBarCode()) + " / " + Helpers.yesNo(cfg.getHidRFID()));
                }
                catch (Exception ex)
                {
                    strAccessory = "\n\nAccessory: N/A";
                }

            }

            strMessage = "Name: " + ri.name + "\nFW: " + ri.swVersion;

            strMessage += strAccessory;

            if (!strBattery.isEmpty())
                strMessage += strBattery;

            mHelpers.okDialog("Reader information", strMessage);
        }
    }

    /**
     * Handle barcode scan click.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onAccessoryActionsClick(View v)
    {
        if (mApi.isConnected())
        {
            Intent actionsIntent = new Intent(MainActivity.this, ActionsActivity.class);
            mSwappingActivity = true;
            startActivityForResult(actionsIntent, ApplicationConstants.REQ_ACCESSORY_ACTIONS);
        }
    }

    /**
     * Handle Reader settings.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onAccessorySettingsButtonClick(View v)
    {
        doReaderAccessorySettings();
    }

    public void onAccessoryRestartButtonClick(View v)
    {
        startReaderAccessoryRestart();
    }

    /**
     * Handle application settings.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onAppSettingsButtonClick(View v)
    {
        doApplicationSettings();
    }

    /**
     * Handles the device search start
     */
    private void handleDeviceSearchStart()
    {
        if (!mMainBTAdapter.isEnabled())
        {
            // Adapter not on. Query from user.
            queryBluetoothAdapterStart();
        }
        else
            startBLEScan();
    }

    // Query for the Bluetooth start if not on.
    private void queryBluetoothAdapterStart()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String msg = "Bluetooth adapter is currently off.\nWould you like to turn it on in order to perform the BLE scan?";

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == Dialog.BUTTON_POSITIVE) {
                    if (btEnable(true)) {
                        try {
                            Thread.sleep(250);
                        }
                        catch (Exception ex) { }
                        startBLEScan();
                    }
                    else
                        mHelpers.longToast("Bluetooth adapter start error");

                }
            }
        };

        builder.setMessage(msg);
        builder.setPositiveButton("Yes", dialogClickListener);
        builder.setNegativeButton("Back", dialogClickListener);
        builder.create();
        builder.show();
    }

    /**
     * Start activity for scanning nearby BLE devices.
     */
    private void startBLEScan()
    {
        //int timeout;
        //timeout = DataBroker.getDeviceSearchTimeout();
        //NurDeviceListActivity.startDeviceRequest(MainActivity.this, NurDeviceListActivity.REQ_BLE_DEVICES, timeout, false);
        NurDeviceListActivity.startDeviceRequest(MainActivity.this, mApi);
    }

    /**
     * Application settings.
     */
    private void doApplicationSettings()
    {
        mSwappingActivity = true;
        Intent appSettingsIntent = new Intent(MainActivity.this, AppSettingsActivity.class);
        startActivityForResult(appSettingsIntent, ApplicationConstants.REQ_APP_SETTINGS);
    }

    /**
     * Reader and accessory module settings.
     */
    private void doReaderAccessorySettings()
    {
        mSwappingActivity = true;
        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivityForResult(settingsIntent, ApplicationConstants.REQ_READER_SETTINGS);
    }

    /**
     * Enable/disable Bluetooth.
     *
     * @param adapterOn True if turned on and false to turn off.
     *
     * @return Returns true if the operation was OK.
     */
    private boolean btEnable(boolean adapterOn)
    {
        // Make sure not to call before any BT/BLE checking...(DataBroker init).
        if (adapterOn)
            return mMainBTAdapter.enable();

        return mMainBTAdapter.disable();
    }

    /**
     * Try to connect (make autoconnection) to a BLE device.
     *
     * @param deviceDescription Address and name ofthe device.
     */
    private void tryConnect(NurDeviceSpec deviceDescription)
    {
        try {
            if (mAutoConnectTransport == null)
                mAutoConnectTransport = new NurApiBLEAutoConnect(this, mApi);

            String connectionText = "Connect: " + mAutoConnectDevice.getAddress();
            mConnectionText.setTextColor(CONN_YELLOW);
            mConnectionText.setText(connectionText);

            mAutoConnectTransport.setAddress(deviceDescription.getAddress());
            mHelpers.shortToast("Connect: " + deviceDescription.getAddress());
        }
        catch (Exception ex)
        {
            mHelpers.shortToast("Connection error:\n" + ex.getMessage());
        }
    }

    /**
     * Handle any result coming from the other activities such as the device search activity.
     *
     * @param requestCode This is the code what was requested.
     * @param resultCode The generic result / error code from the operation.
     * @param data Data related to the result if any.
     *
     * @see com.nordicid.accessorydemo.ApplicationConstants
     */
    private void handleActivityResult(int requestCode, int resultCode, Intent data)
    {
        int knownDevCount = 0;
        mSwappingActivity = false;

        Log.d(TAG, "activityResult: known devices = " + knownDevCount);

        if (requestCode == NurDeviceListActivity.REQUEST_SELECT_DEVICE) {
            if (resultCode == NurDeviceListActivity.RESULT_OK)
            {
                mAutoConnectDevice = new NurDeviceSpec(data.getStringExtra(NurDeviceListActivity.SPECSTR));
                mDataBroker.setAutoconnectDevice(mAutoConnectDevice);
            }

            if (mAutoConnectDevice != null) {
                String connectionText = "Connect: " + mAutoConnectDevice.getAddress();
                mConnectionText.setTextColor(CONN_YELLOW);
                mConnectionText.setText(connectionText);
                tryConnect(mAutoConnectDevice);
            }
        }
        else if (requestCode == ApplicationConstants.REQ_READER_SETTINGS)
        {
            if (resultCode == ApplicationConstants.RESULT_OK_BLE_RESTART)
            {
                try
                {
                    String name = data.getStringExtra(ApplicationConstants.BLE_DEVICE_NEW_NAME);
                    mAutoConnectDevice.setPart("name", name);
                }
                catch (Exception ex)
                {
                    mHelpers.shortToast("Expected new device name, but failed...");
                    return;
                }

                mDataBroker.setAutoconnectDevice(mAutoConnectDevice);
                startReaderAccessoryRestart();
            }
        }
        else if (requestCode == ApplicationConstants.REQ_APP_SETTINGS)
        {
            if (resultCode == ApplicationConstants.RESULT_OK_FORGET_DEVICE) {
                // Application settings already removed it.
                mHelpers.shortToast("Forgetting device...");
                mAutoConnectDevice = null;
                mDisconnectOnResume = true;
            }
        }
        else if (requestCode == ApplicationConstants.REQ_ACCESSORY_ACTIONS && resultCode == ApplicationConstants.RESULT_UNEXPECTED_DISCONNECT)
        {
            disconnectedEvent();
        }
    }

    /**
     * Begin the accessory unit restart. Needed e.g. when the device's name changes.
     */
    private void startReaderAccessoryRestart()
    {
        mBLERestart = true;

        try {
            mAccessoryExtension.restartBLEModule();
            mConnectionText.setTextColor(CONN_YELLOW);
            mConnectionText.setText(R.string.text_restart);
            buttonDisconnectState();
        }
        catch (Exception ex)
        {
            mBLERestart = false;
            mHelpers.longToast("FATAL ERROR: restarting BLE failed!");
        }
    }

    // Some version information.
    private void showVersions(boolean connected)
    {
        String strVersion;
        strVersion = "2016 Nordic ID\nAPI version " + mApi.getFileVersion();

        if (connected && mAccessoryExtension != null && mAccessoryExtension.isSupported())
        {
            try {
                strVersion += ("\nFW: " + mAccessoryExtension.getFwVersion());
            }
            catch (Exception ex)
            {

            }
        }

        mVersionText.setText(strVersion);
    }

    /**
     * Handle an appearing connection to the reader / accessory module.
     */
    private void handleReaderConnection()
    {
        Log.d(TAG, "*** CONNECTED ***");

        boolean accessorySupported = false;

        mBLERestart = false;
        mDevSearchBtn.setEnabled(true);

        // mAccessoryExtension = new NurAccessoryExtension(mApi);

        // For example
        // mAccessoryExtension.setBarcodeDecodingScheme("Shift_JIS");

        DataBroker.setAccessoryExtension(mAccessoryExtension);
        DataBroker.setAutoTransport(mAutoConnectTransport);

        mInfoBtn.setEnabled(true);
        mRestartBtn.setEnabled(true);

        accessorySupported = mAccessoryExtension.isSupported();
        mAccesoryActionsBtn.setEnabled(accessorySupported);
        mAccessorySetupBtn.setEnabled(accessorySupported);

        String connectionText = "Address: " + mAutoConnectDevice.getAddress();
        mConnectionText.setTextColor(CONN_GREEN);
        mConnectionText.setText(connectionText);

        mRestartBtn.setEnabled(true);

        showVersions(true);
    }

    /**
     * Handle reader / accessory connection loss.
     */
    private void handleReaderDisconnect()
    {
        buttonDisconnectState();

        if (mBLERestart || mAutoConnectDevice != null) {
            // Indicates restart/reconnect.
            Log.d(TAG, "*** DISCONNECTED, reconnection WAIT: BLE restart = " + Helpers.yesNo(mBLERestart) + ", device not null = " + Helpers.yesNo(mAutoConnectDevice != null));
            mConnectionText.setTextColor(CONN_YELLOW);
            mConnectionText.setText("Connect: " + mAutoConnectDevice.getAddress());
        }
        else
        {
            Log.d(TAG, "*** DISCONNECTED ***");
            mConnectionText.setTextColor(CONN_RED);
            mConnectionText.setText(R.string.text_disconnected);
        }

        DataBroker.setAutoTransport(null);
        buttonDisconnectState();

        showVersions(false);
    }

    // Dosconnect UI update.
    private void buttonDisconnectState()
    {
        mInfoBtn.setEnabled(false);
        mRestartBtn.setEnabled(false);
        mAccesoryActionsBtn.setEnabled(false);
        mAccessorySetupBtn.setEnabled(false);
        mRestartBtn.setEnabled(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mApi.setListener(this);

        if (mAccessoryExtension == null)
            mAccessoryExtension = new NurAccessoryExtension(mApi);

        Log.d(TAG, "onResume");

        if (mDisconnectOnResume) {
            mDisconnectOnResume = false;
            doDisconnect();
        }
        else if (mAutoConnectTransport != null && mBLERestart == false) {

            if (!mApi.isConnected() && !mSwappingActivity)
                mAutoConnectTransport.onResume();
        }

        mSwappingActivity = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, " ** STOP **");

        if (mSwappingActivity)
            return;

        if (mAutoConnectTransport != null) {
            mAutoConnectTransport.onStop();
        }
    }

    // The NUR API listeners for main activity.
    @Override
    public void logEvent(int logLevel, String logMessage) {

    }

    @Override
    public void IOChangeEvent(NurEventIOChange eventIOChange) {
    }

    @Override
    public void bootEvent(String bootString) {
        // "APP" or "LOADER"
    }

    @Override
    public void clientConnectedEvent(NurEventClientInfo eventClientInfo) {
        // A Sampo in a client mode has connected to NurApiSocketServer.
    }

    @Override
    public void clientDisconnectedEvent(NurEventClientInfo eventClientInfo) {
        // A Sampo in a client mode has disconnected from NurApiSocketServer.
    }

    @Override
    public void connectedEvent() {
        handleReaderConnection();
    }

    @Override
    public void debugMessageEvent(String dbgMessage) {
        handleReaderDisconnect();
    }

    @Override
    public void deviceSearchEvent(NurEventDeviceInfo eventDeviceInfo) {
        // Device search response as an event, here: server information from network after Ethernet device query.
    }

    @Override
    public void disconnectedEvent() {
        handleReaderDisconnect();
    }

    @Override
    public void epcEnumEvent(NurEventEpcEnum eventEpcEnum) {
        // Event triggered by newly autonomously enumerated (written) EPC.
    }

    @Override
    public void frequencyHopEvent(NurEventFrequencyHop eventHop) {
        // Notification when module hopped to next frequency.
    }

    @Override
    public void inventoryExtendedStreamEvent(NurEventInventory eventInventory) {
        // Extended inventory stream event.
    }

    @Override
    public void inventoryStreamEvent(NurEventInventory eventInventory) {
        // Inventory stream event.
    }

    @Override
    public void nxpEasAlarmEvent(NurEventNxpAlarm eventNxpAlarm) {
        // Event triggered by an NXP that has its EAS alarm bit set.
    }

    @Override
    public void programmingProgressEvent(NurEventProgrammingProgress eventProgramming) {
        // Information about software update progress.
    }

    @Override
    public void traceTagEvent(NurEventTraceTag eventTrace) {
        // Event triggered by currently traced tag.
    }

    @Override
    public void triggeredReadEvent(NurEventTriggeredRead eventTrgRead) {
        // Event triggered by a sensor / GPIO.
    }

    @Override
    public void autotuneEvent(NurEventAutotune eventAutoTune) {
        // Event triggered by autotune completion.
    }

    @Override
    public void tagTrackingChangeEvent(NurEventTagTrackingChange nurEventTagTrackingChange) {
        // Tag tracking event.
        // Currently N/A (not tested yet).
    }

    @Override
    public void tagTrackingScanEvent(NurEventTagTrackingData nurEventTagTrackingData) {
        // Tag tracking scan event.
        // Currently N/A (not tested yet).
    }
}
