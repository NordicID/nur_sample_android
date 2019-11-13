package example.nordicid.com.nursampleandroid;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.nurapi.BleScanner;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

import nordicid.com.nurupdate.NurDeviceUpdate;
import nordicid.com.nurupdate.NurUpdateParams;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "NUR_SAMPLE"; //Can be used for filtering Log's at Logcat
    private final int APP_PERMISSION_REQ_CODE = 41;

    private NurApiAutoConnectTransport hAcTr;
    private static NurApi mNurApi;
    private static AccessoryExtension mAccExt;
    private static NurAccessoryExtension mNurAndroidApiAccessoryApi = null;

    //Need to keep track connection state with NurApi IsConnected
    private boolean mIsConnected;

    private Button mConnectButton;
    private TextView mConnectionStatusTextView;

    public static NurApi GetNurApi() {return mNurApi;}
    public static AccessoryExtension GetAccessoryExtensionApi() {return mAccExt;}

    public static NurAccessoryExtension GetNurAccessory() {return mNurAndroidApiAccessoryApi;}

    //When connected, this flag is set depending if Accessories like barcode scan, beep etc supported.
    private static boolean mIsAccessorySupported;
    public static boolean IsAccessorySupported() {return mIsAccessorySupported;}

    //These values will be shown in the UI
    private String mUiConnStatusText;
    private int mUiConnStatusTextColor;
    private String mUiConnButtonText;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.i(TAG, "onCreate()");

        /** Bluetooth Permission checks **/
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED  ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED  ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)  ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)  ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {

            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        APP_PERMISSION_REQ_CODE);
            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //This app uses portrait orientation only
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );

        //Init beeper to make some noise at various situations
        Beeper.init(this);
        Beeper.setEnabled(true);

        //Bluetooth LE scanner need to find EXA's near
        BleScanner.init(this);

        mIsConnected = false;

        //Create NurApi handle.
        mNurApi = new NurApi();

        //Accessory extension contains device specific API like barcode read, beep etc..
        //This included in NurApi.jar
        mAccExt = new AccessoryExtension(mNurApi);

        //Obsolete: Accessory extension contains device specific API like barcode read, beep etc..
        //This is included in NurApiAndroid.aar and needed by NurUpdate library
        mNurAndroidApiAccessoryApi = new NurAccessoryExtension(mNurApi);

        // In this activity, we use mNurApiListener for receiving events
        mNurApi.setListener(mNurApiListener);

        mConnectButton = (Button)findViewById(R.id.button_connect);
        mConnectionStatusTextView = (TextView)findViewById((R.id.text_conn_status));

        mUiConnStatusText = "Disconnected!";
        mUiConnStatusTextColor = Color.RED;
        mUiConnButtonText = "CONNECT";
        showOnUI();
    }

    static boolean mShowingSmartPair = false;
    static boolean mAppPaused = false;

    boolean showNurUpdateUI()
    {
        try {
            Log.d(TAG, "showNurUpdateUI()");
            Intent startIntent = new Intent(this, Class.forName ("nordicid.com.nurupdate.NurDeviceUpdate"));
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    boolean showSmartPairUI()
    {
        if (mNurApi.isConnected() || mAppPaused)
            return false;

        try {
            Log.d(TAG, "showSmartPairUI()");
            Intent startIntent = new Intent(this, Class.forName ("com.nordicid.smartpair.SmartPairConnActivity"));
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Update content of some global variables to UI items
     */
    private void showOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionStatusTextView.setText(mUiConnStatusText);
                mConnectionStatusTextView.setTextColor(mUiConnStatusTextColor);
                mConnectButton.setText(mUiConnButtonText);
            }
        });
    }

    /**
     * NurApi event handlers.
     * NOTE: All NurApi events are called from NurApi thread, thus direct UI updates are not allowed.
     * If you need to access UI controls, you can use runOnUiThread(Runnable) or Handler.
     */
    private NurApiListener mNurApiListener = new NurApiListener()
    {
        @Override
        public void triggeredReadEvent(NurEventTriggeredRead event) { }
        @Override
        public void traceTagEvent(NurEventTraceTag event) { }
        @Override
        public void programmingProgressEvent(NurEventProgrammingProgress event) { }
        @Override
        public void nxpEasAlarmEvent(NurEventNxpAlarm event) { }
        @Override
        public void logEvent(int level, String txt) { }
        @Override
        public void inventoryStreamEvent(NurEventInventory event) { }
        @Override
        public void inventoryExtendedStreamEvent(NurEventInventory event) {}
        @Override
        public void frequencyHopEvent(NurEventFrequencyHop event) { }
        @Override
        public void epcEnumEvent(NurEventEpcEnum event) { }
        @Override
        public void disconnectedEvent() {
            mIsConnected = false;
            Log.i(TAG, "Disconnected!");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Reader disconnected", Toast.LENGTH_SHORT).show();
                    showConnecting();

                    // Show smart pair ui
                    if (!mShowingSmartPair && hAcTr != null) {
                        String clsName = hAcTr.getClass().getSimpleName();
                        if (clsName.equals("NurApiSmartPairAutoConnect")) {
                            mShowingSmartPair = showSmartPairUI();
                        }
                    } else {
                        mShowingSmartPair = false;
                    }
                }
            });
        }
        @Override
        public void deviceSearchEvent(NurEventDeviceInfo event) { }
        @Override
        public void debugMessageEvent(String event) { }
        @Override
        public void connectedEvent() {
            //Device is connected.
            // Let's find out is device provided with accessory support (Barcode reader, battery info...) like EXA
            try {
                if(mAccExt.isSupported())
                {
                    //Yes. Accessories supported
                    mIsAccessorySupported=true;
                    //Let's take name of device from Accessory api
                    mUiConnStatusText = "Connected to " + mAccExt.getConfig().name;
                }
                else {
                    //Accessories not supported. Probably fixed reader.
                    mIsAccessorySupported=false;
                    NurRespReaderInfo ri = mNurApi.getReaderInfo();
                    mUiConnStatusText = "Connected to " + ri.name;
                }
            }
            catch (Exception ex) {
                mUiConnStatusText = ex.getMessage();
            }

            mIsConnected  = true;
            Log.i(TAG, "Connected!");
            Beeper.beep(Beeper.BEEP_100MS);

            mUiConnStatusTextColor = Color.GREEN;
            mUiConnButtonText = "DISCONNECT";
            showOnUI();
        }
        @Override
        public void clientDisconnectedEvent(NurEventClientInfo event) { }
        @Override
        public void clientConnectedEvent(NurEventClientInfo event) { }
        @Override
        public void bootEvent(String event) {}
        @Override
        public void IOChangeEvent(NurEventIOChange event) {
            Log.i(TAG, "Key " + event.source);
        }
        @Override
        public void autotuneEvent(NurEventAutotune event) { }
        @Override
        public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
        //@Override
        public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
    };

    public void onButtonSensors(View v) {
        try {
            if (mNurApi.isConnected()) {
                if(IsAccessorySupported()) {
                    Intent sensorIntent = new Intent(MainActivity.this, Sensor.class);
                    startActivityForResult(sensorIntent, 0);
                }
                else
                    Toast.makeText(MainActivity.this, "Sensors not supported!", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Device firmware update from local zip file or from Nordic ID server
     * Update packets are uncompressed zip files containing firmware files and UpdateInfo.json file.
     * UpdateInfo.json described files and versions
     */
    private void UpdateFirmware(int selection) {

        try {
            if (mNurApi.isConnected()) {

                //Set parameters for Update job
                NurUpdateParams updateParams = new NurUpdateParams();
                //NurApi instance
                updateParams.setNurApi(mNurApi);
                //Possible Nur accessory instance
                updateParams.setNurAccessoryExtension(GetNurAccessory());
                //If we are connected to device via Bluetooth, give current ble address.
                updateParams.setDeviceAddress(hAcTr.getAddress());

                /**
                 *
                 * Zip path string may be empty or URL or Uri
                 * Empty zipPath allow users to browse zip file from local file system
                 */

                try {
                    if (selection == 0) {
                        //Force user to select zip from the filesystem
                        updateParams.setZipPath("");
                    } else if (selection == 1) {
                        //Load from Nordic ID server.
                        updateParams.setZipPath("https://raw.githubusercontent.com/NordicID/nur_firmware/master/zip/NIDLatestFW.zip");
                    }
                    else
                        return;

                    //Update params has been given. Show update Intent
                    showNurUpdateUI();
                }
                catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Error loading ZIP " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     *
     * @param v
     */
    public void onButtonUpdateServer(View v)
    {
        UpdateFirmware(1);
    }

    public void onButtonUpdateLocal(View v)
    {
        UpdateFirmware(0);
    }
    /**
     * Handle barcode scan click. Start Barcode activity (only if reader support acessories). See Barcode.java
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onButtonBarcode(View v)
    {
        try {
            if (mNurApi.isConnected()) {
                if(IsAccessorySupported()) {
                    //Accessories is supported but all devices doesn't have barcode scanner
                    AccConfig cfg = mAccExt.getConfig();
                    if(cfg.hasImagerScanner() == false) {
                        Toast.makeText(MainActivity.this, "Barcode not supported!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent barcodeIntent = new Intent(MainActivity.this, Barcode.class);
                    startActivityForResult(barcodeIntent, 0);
                }
                else
                    Toast.makeText(MainActivity.this, "No accessories on device!", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle inventory click. Start Inventory activity. See inventory.java
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onButtonInventory(View v)
    {
        try {
            if (mNurApi.isConnected()) {
                    Intent inventoryIntent = new Intent(MainActivity.this, Inventory.class);
                    startActivityForResult(inventoryIntent, 0);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle tag write click. Start Write Tag activity. See Write.java
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onButtonWrite(View v)
    {
        try {
            if (mNurApi.isConnected()) {
                Intent writeTagIntent = new Intent(MainActivity.this, WriteTag.class);
                startActivityForResult(writeTagIntent, 0);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    int ReadNur2Reg(int type, int _reg) throws Exception
    {
        byte[] data = new byte[5];
        data[0] = (byte)type;
        NurPacket.PacketDword(data, 1, _reg);
        byte[] ret = mNurApi.customCmd(0x90, data);
        int retval = NurPacket.BytesToDword(ret, 0);
        Log.w(TAG, "Read reg [" + type + "]  " + String.format("%x", _reg) + " = " + String.format("%x", retval));
        return retval;
    }

    void WriteNur2Reg(int type, int _reg, int _val) throws Exception
    {
        byte[] data = new byte[9];

        data[0] = (byte)type;
        NurPacket.PacketDword(data, 1, _reg);
        NurPacket.PacketDword(data, 5, _val);
        byte[] ret = mNurApi.customCmd(0x91, data);
        Log.w(TAG, "Write reg [" + type + "]  " + String.format("%x", _reg) + " = " + String.format("%x", _val));
        ReadNur2Reg(type, _reg);
    }

    int BandToFreq(int band)
    {
        return 850 + (band * 20);
    }

    public void onButtonProdTune(View v)
    {
        final Button b = (Button)findViewById(R.id.button_prod_tune);

        try {
            int A0 = 0xfaffec57;

            // Write MAC
            WriteNur2Reg(0, 0x0B2B, A0);
            WriteNur2Reg(0, 0xF000, 0x21);

            // Write OEM
            WriteNur2Reg(1, 0x93, A0);

            // Read back
            //int ival = ReadNur2Reg(0, 0x0B2B);
            int ival = ReadNur2Reg(1, 0x93);
            Log.w(TAG, "ival " + ival);
            double dval = (double) ival / 16777216.0;
            Log.w(TAG, "dval " + dval);

            b.setEnabled(false);
            b.setText("Calibration in progress..");
            Toast.makeText(MainActivity.this, "Calibration started\nPlease wait", Toast.LENGTH_SHORT).show();

            Thread calThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String result = "Calibration done\n";
                    try {
                        // Run calibration
                        NurTuneResponse[] resp = ((NurCmdProdTune) mNurApi.exchangeCommand(new NurCmdProdTune(), 10000)).getResponse();

                        result = "Calibration done\n";
                        for (int n = 0; n < resp.length; n++) {
                            result += String.format("%d = %f; ", resp[n].frequency, resp[n].dBm);
                        }
                    }
                    catch (Exception ex)
                    {
                        result = ex.getMessage();
                    }

                    final String uiMsg = result;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            b.setEnabled(true);
                            b.setText("RF Calibration");
                            Toast.makeText(MainActivity.this, uiMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
            calThread.start();

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            b.setEnabled(true);
            b.setText("RF Calibration");
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle tag trace click. Start Write Tag activity. See Trace.java
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onButtonTrace(View v)
    {
        try {
            if (mNurApi.isConnected()) {
                Intent traceIntent = new Intent(MainActivity.this, Trace.class);
                startActivityForResult(traceIntent, 0);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Handle power off click.
     * Sends PowerOff command to reader.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onPowerOffClick(View v)
    {
        try {
            if (mNurApi.isConnected()) {

                if(IsAccessorySupported()) { //Only device with accessory can be power off by command
                    mAccExt.powerDown(); //Power off device
                    Toast.makeText(MainActivity.this, "Device Power OFF!", Toast.LENGTH_LONG).show();
                }
                else Toast.makeText(MainActivity.this, "PowerOff not supported!", Toast.LENGTH_LONG).show();
            }
            else
            {
                Toast.makeText(MainActivity.this, "Reader not connected!", Toast.LENGTH_LONG).show();
            }
        }
        catch(Exception ex)
        {
            Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    /**
     * Handle reader connection button click.
     * First is check if Bluetooth adapter is ON or OFF.
     * Then Bluetooth scan is performed to search devices from near.
     * User can select device from list to connect.
     * It's useful to store last connected device MAC to persistent memory inorder to reconnect later on to same device without selecting from list. This demo doesn't do MAC storing.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onConnectClick(View v)
    {
        if(mNurApi.isConnected()) {
            hAcTr.dispose();
            hAcTr = null;
        }
        else {
            Toast.makeText(MainActivity.this, "Start searching. Make sure device power ON!", Toast.LENGTH_LONG).show();
            NurDeviceListActivity.startDeviceRequest(MainActivity.this, mNurApi);
        }
    }

    void showConnecting()
    {
        if (hAcTr != null) {
            mUiConnStatusText = "Connecting to " + hAcTr.getAddress();
            mUiConnStatusTextColor = Color.YELLOW;
        } else {
            mUiConnStatusText = "Disconnected";
            mUiConnStatusTextColor = Color.RED;
            mUiConnButtonText = "CONNECT";
        }
        showOnUI();
    }

    /**
     * DeviceList activity result
     * @param requestCode We are intrest code "NurDeviceListActivity.REQUEST_SELECT_DEVICE" (32778)
     * @param resultCode If RESULT_OK user has selected device and then we create NurDeviceSpec (spec) and transport (hAcTr)
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case NurDeviceListActivity.REQUEST_SELECT_DEVICE: {
                if (data == null || resultCode != NurDeviceListActivity.RESULT_OK)
                    return;

                try {
                    NurDeviceSpec spec = new NurDeviceSpec(data.getStringExtra(NurDeviceListActivity.SPECSTR));

                    if (hAcTr != null) {
                        System.out.println("Dispose transport");
                        hAcTr.dispose();
                    }

                    String strAddress;
                    hAcTr = NurDeviceSpec.createAutoConnectTransport(this, mNurApi, spec);
                    strAddress = spec.getAddress();
                    Log.i(TAG, "Dev selected: code = " + strAddress);
                    hAcTr.setAddress(strAddress);

                    showConnecting();

                    //If you want connect to same device automatically later on, you can save 'strAddress" and use that for connecting at app startup for example.
                    //saveSettings(spec);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            break;
        }
        super.onActivityResult(requestCode,resultCode,data);
    }

    @Override
    protected void onPause() {
        mAppPaused = true;
        Log.i(TAG, "onPause()");
        super.onPause();
        //if (hAcTr != null)
            //hAcTr.onPause();
    }

    @Override
    protected void onResume() {
        mAppPaused = false;
        Log.i(TAG, "onResume()" );
        super.onResume();

        mNurApi.setListener(mNurApiListener);
        if(!mNurApi.isConnected() && mIsConnected)
            mNurApiListener.disconnectedEvent();

        if (!mShowingSmartPair && hAcTr != null) {
            String clsName = hAcTr.getClass().getSimpleName();
            if (clsName.equals("NurApiSmartPairAutoConnect")) {
                mShowingSmartPair = showSmartPairUI();
            }
        } else {
            mShowingSmartPair = false;
        }
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop()" );
        super.onStop();
        mShowingSmartPair = false;
        //if (hAcTr != null)
        //    hAcTr.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
        //Kill connection when app killed
        if (hAcTr != null) {
            hAcTr.onDestroy();
            hAcTr = null;
        }
    }

    /**
     * Handle barcode scan click.
     * @param v View parameter as passed from the system when the button is clicked.
     */
    public void onButtonReaderInfo(View v)
    {
        handleAboutClick();
    }

    /**
     * Show some general information about the reader and application
     */
    void handleAboutClick() {

        String appversion = "0.0";
        try {
            appversion = this.getPackageManager().getPackageInfo("example.nordicid.com.nursampleandroid", 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final View dialogLayout = getLayoutInflater().inflate(R.layout.about_dialog, null);

        final TextView appVersion = (TextView) dialogLayout.findViewById(R.id.app_version);
        appVersion.setText(getString(R.string.about_dialog_app) + " " + appversion);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogLayout);

        final TextView readerAttachedTextView = (TextView) dialogLayout.findViewById(R.id.reader_attached_is);
        readerAttachedTextView.setText(getString(R.string.attached_reader_info));

        final TextView nurApiVersion = (TextView) dialogLayout.findViewById(R.id.nur_api_version);
        nurApiVersion.setText(getString(R.string.about_dialog_nurapi) + " " + mNurApi.getFileVersion());
        nurApiVersion.setVisibility(View.VISIBLE);

        final TextView nurApiAndroidVersion = (TextView) dialogLayout.findViewById(R.id.nur_apiandroid_version);
        nurApiAndroidVersion.setText(getString(R.string.about_dialog_nurapiandroid) + " " + NurApiAndroid.getVersion());
        nurApiAndroidVersion.setVisibility(View.VISIBLE);

        final TextView nurUpdateLibVersion = (TextView) dialogLayout.findViewById(R.id.nur_updatelib_version);
        nurUpdateLibVersion.setText(getString(R.string.about_dialog_updatelib) + " " + NurDeviceUpdate.getVersion());
        nurUpdateLibVersion.setVisibility(View.VISIBLE);

        if (mNurApi != null && mNurApi.isConnected()) {

            readerAttachedTextView.setText(getString(R.string.attached_reader_info));

            try {
                NurRespReaderInfo readerInfo = mNurApi.getReaderInfo();
                NurRespDevCaps devCaps = mNurApi.getDeviceCaps();


                final TextView modelTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_model);
                modelTextView.setText(getString(R.string.about_dialog_model) + " " + readerInfo.name);
                modelTextView.setVisibility(View.VISIBLE);

                final TextView serialTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_serial);
                serialTextView.setText(getString(R.string.about_dialog_serial) + " " + readerInfo.serial);
                serialTextView.setVisibility(View.VISIBLE);

                final TextView serialDeviceTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_device_serial);
                serialDeviceTextView.setText(getString(R.string.about_dialog_device_serial) + " " + readerInfo.altSerial);
                serialDeviceTextView.setVisibility(View.VISIBLE);

                final TextView firmwareTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_firmware);
                firmwareTextView.setText(getString(R.string.about_dialog_firmware) + " " + readerInfo.swVersion);
                firmwareTextView.setVisibility(View.VISIBLE);

                final TextView bootloaderTextView = (TextView) dialogLayout.findViewById(R.id.reader_bootloader_version);
                bootloaderTextView.setText(getString(R.string.about_dialog_bootloader) + " " + mNurApi.getVersions().secondaryVersion);
                bootloaderTextView.setVisibility(View.VISIBLE);

                final TextView secChipTextView = (TextView) dialogLayout.findViewById(R.id.reader_sec_chip_version);
                secChipTextView.setText(getString(R.string.about_dialog_sec_chip) + " " + devCaps.secChipMajorVersion+"." + devCaps.secChipMinorVersion+"."+ devCaps.secChipMaintenanceVersion+"." + devCaps.secChipReleaseVersion);
                secChipTextView.setVisibility(View.VISIBLE);

                if (IsAccessorySupported()) {
                    final TextView accessoryTextView = (TextView) dialogLayout.findViewById(R.id.accessory_version);
                    accessoryTextView.setText(getString(R.string.about_dialog_accessory) + " " + mAccExt.getFwVersion().getFullApplicationVersion());
                    accessoryTextView.setVisibility(View.VISIBLE);

                    final TextView accessoryBldrTextView = (TextView) dialogLayout.findViewById(R.id.accessory_bootloader_version);
                    accessoryBldrTextView.setText(getString(R.string.about_dialog_accessory_bldr) + " " + mAccExt.getFwVersion().getBootloaderVersion());
                    accessoryBldrTextView.setVisibility(View.VISIBLE);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            readerAttachedTextView.setText(getString(R.string.no_reader_attached));
        }

        builder.show();
    }
}
