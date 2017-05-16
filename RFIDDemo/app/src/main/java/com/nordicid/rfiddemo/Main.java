package com.nordicid.rfiddemo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubAppList;
import com.nordicid.controllers.BthDFUController;
import com.nordicid.controllers.NURFirmwareController;
import com.nordicid.controllers.UpdateController;
import com.nordicid.nurapi.*;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends AppTemplate {

    // Check whether this string is found in the given filename.
    private final String NUR_AUTH_IDENT_STR = "nur_auth_keyset";
    final private String TAG = "MAIN";
    // Authentication app requirements.
    public static final int AUTH_REQUIRED_VERSION = 0x050500 + ('A' & 0xFF);
    public static final String AUTH_REQUIRED_VERSTRING = "5.5-A";
    private static SharedPreferences mApplicationPrefences = null;

    private boolean isApplicationMode = true;

    /**
     * Update Controllers
     */
    BthDFUController mDFUController;
    NURFirmwareController mNURAPPController;

    /**
     * Requesting file for key reading.
     */
    public static final int REQ_FILE_OPEN = 4242;

    Timer timer;
    TimerTask timerTask;
    final Handler timerHandler = new Handler();

    private NurApiAutoConnectTransport mAcTr;

    public static final String KEYFILE_PREFNAME = "TAM1_KEYFILE";
    public static final String KEYNUMBER_PREFNAME = "TAM1_KEYNUMBER";

    private static Main gInstance;

    private boolean mShowSwipeHint = false;

    public NURFirmwareController getNURUpdateController(){
        return mNURAPPController;
    }

    public BthDFUController getDFUUpdateController(){
        return mDFUController;
    }

    // TODO GEt transport
    public NurApiAutoConnectTransport getAutoConnectTrasport()
    {
        return mAcTr;
    }

    public void disposeTrasport()
    {
        if (mAcTr != null) {
            System.out.println("Dispose transport");
            mAcTr.dispose();
        }
        mAcTr = null;
    }

    public void toggleScreenRotation(boolean enable) {
        setRequestedOrientation((enable) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR : ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    }

    public static SharedPreferences getApplicationPrefences() { return mApplicationPrefences; }

    public NurApiAutoConnectTransport getNurAutoConnect() { return mAcTr;}

    public void startTimer() {

        stopTimer();

        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 10000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 10000, 10000); //
    }

    public void stopTimer() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            @Override
            public void run() {
                timerHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateStatus();
                    }
                });
            }
        };
    }

    public static Main getInstance() {
        return gInstance;
    }

    public void handleKeyFile() {
        Intent intent;
        Intent chooser;

        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        chooser = Intent.createChooser(intent, "Select file");

        try {
            startActivityForResult(chooser, REQ_FILE_OPEN);
        } catch (Exception ex) {
            String strErr = ex.getMessage();
            Toast.makeText(this, "Error:\n" + strErr, Toast.LENGTH_SHORT).show();
        }
    }

    void saveSettings(NurDeviceSpec connSpec) {
        SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        if (mAcTr == null) {
            editor.putString("specStr", "");
        } else {
            editor.putString("specStr", connSpec.getSpec());
        }
        editor.apply();

        updateStatus();
    }

    void saveHintStatus() {
        SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putBoolean("SwipeHint", mShowSwipeHint);
        editor.apply();
        updateStatus();
    }

    void loadHintStatus()
    {
        mShowSwipeHint = mApplicationPrefences.getBoolean("SwipeHint", true);
    }

    public void saveKeyFilename(String fileName) {
        SharedPreferences.Editor editor = mApplicationPrefences.edit();

        editor.putString(KEYFILE_PREFNAME, fileName);

        editor.apply();
    }

    public String getKeyFileName() {
        return mApplicationPrefences.getString(KEYFILE_PREFNAME, "");
    }

    public void saveUsedKeyNumber(int keyNumber) {
        SharedPreferences.Editor editor = mApplicationPrefences.edit();

        editor.putInt(KEYNUMBER_PREFNAME, keyNumber);

        editor.apply();
    }

    public int getUsedKeyNumber() {
        return mApplicationPrefences.getInt(KEYNUMBER_PREFNAME, -1);
    }

    public boolean checkUpdatesEnabled() { return mApplicationPrefences.getBoolean("CheckUpdate",true); }

    public void loadSettings() {
        String type = mApplicationPrefences.getString("connType", "");

        /* Get rotation setting enable / disable rotation sensors */
        toggleScreenRotation(mApplicationPrefences.getBoolean("Rotation", false));
        Beeper.setEnabled(mApplicationPrefences.getBoolean("Sounds", true));

        String specStr = mApplicationPrefences.getString("specStr", "");
        if (specStr.length() > 0)
        {
            NurDeviceSpec spec = new NurDeviceSpec(specStr);

            if (mAcTr != null) {
                System.out.println("Dispose transport");
                mAcTr.dispose();
            }

            try {
                String strAddress;
                mAcTr = NurDeviceSpec.createAutoConnectTransport(this, getNurApi(), spec);
                strAddress = spec.getAddress();

                mAcTr.setAddress(strAddress);
            } catch (NurApiException e) {
                e.printStackTrace();
            }
        }
        updateStatus();
    }

    void updateStatus() {
        String str;
        if (mAcTr != null) {
            if (getNurApi().isConnected())
                str = "CONNECTED ";
            else
                str = "DISCONNECTED ";

            str += mAcTr.getType();

            String addr = mAcTr.getAddress();
            if (addr.length() > 0)
                str += " (" + mAcTr.getAddress() + ")";

            String details = mAcTr.getDetails();
            if (details.length() > 0)
				str += " " + details;

        } else {
            str = "No connection defined";
        }
        setStatusText(str);

        // getWindow().getDecorView().invalidate();
    }

    @Override
    protected void onPause() {
        super.onPause();

        stopTimer();

        if (mAcTr != null) {
            mAcTr.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Beeper.init();

        if (mAcTr == null)
            loadSettings();

        if (mAcTr != null) {
            mAcTr.onResume();
        }


        startTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mAcTr != null) {
            mAcTr.onStop();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAcTr != null) {
            mAcTr.onDestroy();
        }
    }

    private int getFwIntVersion(NurApi api) {
        int iVersion = 0;
        mDetectedFw = "";

        if (api != null && api.isConnected()) {
            try {
                NurRespReaderInfo ri;
                ri = api.getReaderInfo();

                mDetectedFw = ri.swVersion;

                iVersion = ri.swVerMajor & 0xFF;
                iVersion <<= 8;
                iVersion |= (ri.swVerMinor & 0xFF);
                iVersion <<= 8;
                iVersion |= (ri.swVerDev & 0xFF);

            } catch (Exception ex) {

            }
        }

        return iVersion;
    }

    private String mDetectedFw = "";

    // Visible app choices / not.
    public void syncViewContents() {
        super.onResume();
    }

    void beginHint()
    {
        if (!mShowSwipeHint)
            return;

        (new Handler(getMainLooper())).postDelayed(new Runnable() {
            @Override
            public void run() {
                doHint();
            }
        }, 250);
    }

    void doHint()
    {
        final Dialog dlg = new Dialog(this);
        final Button okBtn;
        final Button gotItBtn;

        dlg.setTitle(R.string.hint_title);
        dlg.setContentView(R.layout.layout_swipe_note);

        okBtn = (Button) dlg.findViewById(R.id.btn_hint_ok);
        gotItBtn = (Button) dlg.findViewById(R.id.btn_hint_dont_show);

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.btn_hint_dont_show) {
                    mShowSwipeHint = false;
                    saveHintStatus();
                }
                dlg.dismiss();
            }
        };

        okBtn.setOnClickListener(onClickListener);
        gotItBtn.setOnClickListener(onClickListener);

        dlg.show();
    }

    @Override
    public void onCreateSubApps(SubAppList subAppList) {
        gInstance = this;
        NurApi theApi = getNurApi();

        /* Set update sources */
        mDFUController = new BthDFUController(Main.getInstance(),getString(R.string.DFU_APP_SRC),getString(R.string.DFU_BLDR_SRC));
        mNURAPPController = new NURFirmwareController(mApi,getString(R.string.NUR_APP_SRC),getString(R.string.NUR_BLDR_SRC));
        mApplicationPrefences = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
        loadHintStatus();

        copyAuthenticationFilesToDevice();

        if (AppTemplate.LARGE_SCREEN) {
            subAppList.addSubApp(new InventoryApp());
        } else {
            subAppList.addSubApp(new InventoryAppTabbed());
        }
		
		/* Tag trace application. */
        subAppList.addSubApp(new TraceApp());
		
		/* Tag write application. */
        subAppList.addSubApp(new WriteApp());

		/* Barcode application. */
        subAppList.addSubApp(new BarcodeApp());

		/* Authentication application. */
        subAppList.addSubApp(new AuthenticationAppTabbed());
        getSubAppList().getApp("Authentication").setIsVisibleInMenu(false);

		/* Test mode application. */
        subAppList.addSubApp(new TestModeApp());

        /* Reader settings application. */
        subAppList.addSubApp(new SettingsAppTabbed());

        //theApi.setLogLevel(NurApi.LOG_VERBOSE | NurApi.LOG_USER | NurApi.LOG_DATA| NurApi.LOG_ERROR);
        theApi.setLogLevel(NurApi.LOG_ERROR);

        setAppListener(new NurApiListener() {
            @Override
            public void disconnectedEvent() {
                if (exitingApplication())
                    return;
                updateStatus();
                Toast.makeText(Main.this, getString(R.string.reader_disconnected), Toast.LENGTH_SHORT).show();
                getSubAppList().getApp("Barcode").setIsVisibleInMenu(false);
                getSubAppList().getApp("Authentication").setIsVisibleInMenu(false);
                // If current app not available anymore, return to main menu
                if (!isApplicationPaused() && getSubAppList().getCurrentOpenSubApp() == null)
                    setApp(null);
            }

            @Override
            public void connectedEvent() {
                try {
                    updateStatus();
                    isApplicationMode = mApi.getMode().equalsIgnoreCase("A");
                    final String module = getModuleType();
                    /**
                     *  manually set for now
                     *  Adding device type later to API
                     **/
                    mDFUController.setHWType("EXA51");
                    mDFUController.setAPPVersion(getBLEAppVersion());
                    mDFUController.setBldrVersion("1.0.0");
                    mNURAPPController.setAPPVersion(getNurAppVersion());
                    mNURAPPController.setHWType(module);
                    mNURAPPController.setBldrVersion(getNurBldrVersion());
                    /**
                    * trigger update checking ?
                    * //TODO implement Check for updates when sources available
                    */
                    if(checkUpdatesEnabled()){
                        boolean dfuApp = mDFUController.isAppUpdateAvailable();
                        boolean dfuBldr = mDFUController.isBldrUpdateAvailable();
                        boolean nurApp = mNURAPPController.isAppUpdateAvailable();
                        boolean nurBldr = mNURAPPController.isBldrUpdateAvailable();
                        if(dfuApp || dfuBldr || nurApp || nurBldr) {
                            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(gInstance);
                            alertDialog.setTitle("Available Updates:");
                            String message = "";
                            if(dfuApp)
                                message += "Device application available : " + mDFUController.getAvailableAppUpdateVerion();
                            if(dfuBldr)
                                message += "\nDevice bootloader available : " + mDFUController.getAvailableBldrUpdateVerion();
                            if(nurApp)
                                message += "\nNUR application available : " + mNURAPPController.getAvailableAppUpdateVerion();
                            if(nurBldr)
                                message += "\nNUR bootloader available : " + mNURAPPController.getAvailableBldrUpdateVerion();
                            alertDialog.setMessage(message);
                            alertDialog.setNegativeButton("Dismiss",null);
                            alertDialog.setPositiveButton("Open update app", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    SettingsAppTabbed.setPreferredTab("Updates");
                                    setApp("Settings");
                                }
                            });
                            alertDialog.show();
                        }
                    }
                    Toast.makeText(Main.this, getString(R.string.reader_connected), Toast.LENGTH_SHORT).show();
                    if(!isApplicationMode)
                        Toast.makeText(Main.this, getString(R.string.device_boot_mode), Toast.LENGTH_LONG).show();
                    // Show barcode app only for accessory devices
                    getSubAppList().getApp("Barcode").setIsVisibleInMenu(getAccessorySupported());
                    getSubAppList().getApp("Authentication").setIsVisibleInMenu(/*fwVer >= AUTH_REQUIRED_VERSION*/ false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void triggeredReadEvent(NurEventTriggeredRead event) {
            }

            @Override
            public void traceTagEvent(NurEventTraceTag event) {
            }

            @Override
            public void programmingProgressEvent(NurEventProgrammingProgress eventProgramming) {
            }

            @Override
            public void nxpEasAlarmEvent(NurEventNxpAlarm event) {
            }

            @Override
            public void logEvent(int level, String txt) {
            }

            @Override
            public void inventoryStreamEvent(NurEventInventory event) {
            }

            @Override
            public void inventoryExtendedStreamEvent(NurEventInventory event) {
            }

            @Override
            public void frequencyHopEvent(NurEventFrequencyHop event) {
            }

            @Override
            public void epcEnumEvent(NurEventEpcEnum event) {
            }

            @Override
            public void deviceSearchEvent(NurEventDeviceInfo event) {
            }

            @Override
            public void debugMessageEvent(String event) {
            }

            @Override
            public void clientDisconnectedEvent(NurEventClientInfo event) {
            }

            @Override
            public void clientConnectedEvent(NurEventClientInfo event) {
            }

            @Override
            public void bootEvent(String event) {
            }

            @Override
            public void autotuneEvent(NurEventAutotune event) {
            }

            @Override
            public void IOChangeEvent(NurEventIOChange event) {
            }

            @Override
            public void tagTrackingScanEvent(NurEventTagTrackingData event) {
            }

            @Override
            public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {
            }
        });

        (findViewById(R.id.app_statustext)).setOnClickListener(mStatusBarOnClick);

        beginHint();
    }

    int testmodeClickCount = 0;
    long testmodeClickTime = 0;

    View.OnClickListener mStatusBarOnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (testmodeClickCount < 10) {
                if (testmodeClickTime != 0 && System.currentTimeMillis() - testmodeClickTime > 5000) {
                    testmodeClickCount = 0;
                }
                testmodeClickTime = System.currentTimeMillis();
                testmodeClickCount++;

                if (testmodeClickCount == 10) {
                    Toast.makeText(Main.this, "Test Mode enabled", Toast.LENGTH_SHORT).show();
                    getSubAppList().getApp("Test Mode").setIsVisibleInMenu(true);
                }
            }
        }
    };

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ((TextView) findViewById(R.id.app_statustext)).setOnClickListener(mStatusBarOnClick);
        updateStatus();
    }

    @Override
    public void onCreateDrawerItems(Drawer drawer) {
        drawer.addTitle("Connection");
        drawer.addTitle("Contact");
        drawer.addTitle("Quick guide");
        drawer.addTitle("About");
    }

    void handleQuickGuide() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        // looks ugly
        //alert.setTitle("Quick guide");
        WebView wv = new WebView(this);
        wv.loadUrl("file:///android_res/raw/guide.html");
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        alert.setView(wv);
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    public String getModuleType(){
        try {
            if (mApi.isConnected()) {
                return mApi.getFwInfo().get("MODULE");
            }
        } catch (Exception e){
            Log.e(TAG,e.getMessage());
        }
        return null;
    }

    public String getNurAppVersion(){
        try{
            if(mApi.isConnected())
                return ((isApplicationMode) ? mApi.getVersions().primaryVersion : mApi.getVersions().secondaryVersion);
        } catch (Exception e){
            //
        }
        return null;
    }

    public String getNurBldrVersion(){
        try{
            if(mApi.isConnected())
                return ((isApplicationMode) ? getNurApi().getVersions().secondaryVersion : mApi.getVersions().primaryVersion);
        } catch (Exception e){
            //
        }
        return null;
    }

    public String getBLEAppVersion(){
        try{
            if(mApi.isConnected())
                return getAccessoryApi().getFwVersion();
        } catch (Exception e){
            //
        }
        return null;
    }

    // Parse the URI the get the actual file name.
    private String getActualFileName(String strUri) {
        String strFileName = null;
        Uri uri;
        String scheme;

        uri = Uri.parse(strUri);
        scheme = uri.getScheme();

        if (scheme.equalsIgnoreCase("content")) {
            String primStr;
            primStr = uri.getLastPathSegment().replace("primary:", "");
            strFileName = Environment.getExternalStorageDirectory() + "/" + primStr;
        }

        return strFileName;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

			case REQ_FILE_OPEN:
			{
				if (data != null) {
					String fullPath;

					fullPath = getActualFileName(data.getDataString());

					if (fullPath == null)
						Toast.makeText(Main.this, "No file selected.", Toast.LENGTH_SHORT).show();
					else {
						saveKeyFilename(fullPath);

						SettingsAppAuthTab authTab = SettingsAppAuthTab.getInstance();

						if (authTab != null)
							authTab.updateViews();
					}
				}
			}
			break;

			case NurDeviceListActivity.REQUEST_SELECT_DEVICE: {
				if (data == null || resultCode != NurDeviceListActivity.RESULT_OK)
					return;

				try {
					NurDeviceSpec spec = new NurDeviceSpec(data.getStringExtra(NurDeviceListActivity.SPECSTR));

					if (mAcTr != null) {
						System.out.println("Dispose transport");
						mAcTr.dispose();
					}

					String strAddress;
                    mAcTr = NurDeviceSpec.createAutoConnectTransport(this, getNurApi(), spec);
                    strAddress = spec.getAddress();
                    mAcTr.setAddress(strAddress);
					saveSettings(spec);
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			break;
		}
        super.onActivityResult(requestCode,resultCode,data);
    }

	void handleConnectionClick()
	{
		NurDeviceListActivity.startDeviceRequest(this, mApi);
	}

    void handleContactClick() {
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        AlertDialog.Builder builder = new Builder(this);
        builder.setView(dialogLayout);
        builder.show();
    }

    void handleAboutClick() {
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_about, null);
        AlertDialog.Builder builder = new Builder(this);
        builder.setView(dialogLayout);

        final TextView readerAttachedTextView = (TextView) dialogLayout.findViewById(R.id.reader_attached_is);

        String appversion = "0.0";
        try {
            appversion = this.getPackageManager().getPackageInfo("com.nordicid.rfiddemo", 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        final TextView appVersion = (TextView) dialogLayout.findViewById(R.id.app_version);
        appVersion.setText(getString(R.string.about_dialog_app) + " " + appversion);

        final TextView nurApiVersion = (TextView) dialogLayout.findViewById(R.id.nur_api_version);
        nurApiVersion.setText(getString(R.string.about_dialog_nurapi) + " " + getNurApi().getFileVersion());

        if (getNurApi().isConnected()) {

            readerAttachedTextView.setText(getString(R.string.attached_reader_info));

            try {
                NurRespReaderInfo readerInfo = getNurApi().getReaderInfo();

                final TextView modelTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_model);
                modelTextView.setText(getString(R.string.about_dialog_model) + " " + getModuleType());
                modelTextView.setVisibility(View.VISIBLE);

                final TextView serialTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_serial);
                serialTextView.setText(getString(R.string.about_dialog_serial) + " " + readerInfo.serial);
                serialTextView.setVisibility(View.VISIBLE);

                final TextView firmwareTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_firmware);
                firmwareTextView.setText(getString(R.string.about_dialog_firmware) + " " + getNurAppVersion());
                firmwareTextView.setVisibility(View.VISIBLE);

                final TextView bootloaderTextView = (TextView) dialogLayout.findViewById(R.id.reader_bootloader_version);
                bootloaderTextView.setText(getString(R.string.about_dialog_bootloader) + " " + getNurBldrVersion());
                bootloaderTextView.setVisibility(View.VISIBLE);

                if (getAccessorySupported()) {
                    final TextView accessoryTextView = (TextView) dialogLayout.findViewById(R.id.accessory_version);
                    accessoryTextView.setText(getString(R.string.about_dialog_accessory) + " " + getBLEAppVersion());
                    accessoryTextView.setVisibility(View.VISIBLE);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            readerAttachedTextView.setText(getString(R.string.no_reader_attached));
        }

        builder.show();
    }

    @Override
    public void onDrawerItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                handleConnectionClick();
                break;
            case 1:
                handleContactClick();
                break;
            case 2:
                handleQuickGuide();
                break;
            case 3:
                handleAboutClick();
                break;
            default:
                break;
        }
    }

    // Get "raw" filename from given resource ID.
    String getRawFileName(int resourceID)
    {
        TypedValue tv = new TypedValue();
        String fullRawName;
        String []split;
        int i;

        getResources().getValue(resourceID, tv, true);

        fullRawName = tv.string.toString();

        if (fullRawName.isEmpty())
            return "";

        split = fullRawName.split("/");

        // Last entry is expected to be the file's name without an extension.
        return split[split.length-1];
    }

    // Checks if the given file already exists.
    private boolean fileAlreadyExists(String fullPath)
    {
        File f = new File(fullPath);

        if (f.exists() && f.isFile() && !f.isDirectory())
            return true;

        return false;
    }

    // Makes a target file name in the external storage.
    private String makeTargetName(String fileName)
    {
        return Environment.getExternalStorageDirectory() + "/" + fileName;
    }

    // List the file is the raw resources that may present an authentication key file.
    private ArrayList<ResourceIdTargetName> listAuthenticationKeyfileCandidates()
    {
        int i, resourceID;
        String rawName, targetName;
        ArrayList<ResourceIdTargetName> result = new ArrayList<>();

        Log.d(TAG, "Raw file list:");

        Field[]fields;

        fields = R.raw.class.getFields();

        for(i=0; i < fields.length; i++){
            Log.d(TAG, "Checking: " + fields[i].getName() + " / " + fields[i].getGenericType().toString());
            if (!fields[i].getName().toLowerCase().contains(NUR_AUTH_IDENT_STR))
                continue;

            Log.e(TAG, " -> OK, continue check.");

            if (fields[i].getGenericType().toString().equalsIgnoreCase("int")) {
                try {
                    resourceID = fields[i].getInt(R.raw.class);
                    rawName = getRawFileName(resourceID);
                    targetName =  makeTargetName(rawName);
                    Log.d(TAG, "Add pair: " + resourceID + " -> " + targetName);
                    result.add(new ResourceIdTargetName(resourceID, targetName));
                }
                catch (Exception ex) {
                    Log.e(TAG, "Exception: "+ ex.getMessage());
                }
            }
            else
                Log.e(TAG, "No match");

        }

        return result;
    }

    // Copies a file from raw resource to device root directory so can be browsed to.
    void copyToDeviceRoot(ResourceIdTargetName rawRes)
    {
        InputStream inputStream = null;
        FileOutputStream outputFile = null;
        boolean ok;

        if (fileAlreadyExists(rawRes.getTargetName())) {
            Log.d(TAG, "Copy to device: file \"" + rawRes.getTargetName() + "\" already exists.");
            return;
        }

        try {
            inputStream = getResources().openRawResource(rawRes.getID());
        }
        catch (Exception ex) {
            Log.e(TAG, "Copy to device SOURCE file error");
            Log.e(TAG, ex.getMessage());
            return;
        }

        try {
            File f = new File(rawRes.getTargetName());
            f.setWritable(true);
            outputFile = new FileOutputStream(f);

            ok = true;
        }
        catch (Exception ex) {
            Log.e(TAG, "Copy to device TARGET file error");
            Log.e(TAG, ex.getMessage());
            ok = false;
        }

        if (!ok ) {
            try { inputStream.close(); } catch (Exception ex) { }
            return;
        }

        Log.d(TAG, "Copy from resources to " + rawRes.getTargetName());

        try {
            int read;
            int total = 0;
            byte []buf = new byte[1024];
            while ((read = inputStream.read(buf)) > 0) {
                outputFile.write(buf, 0, read);
                total += read;
            }

            Log.d(TAG, "Wrote " + total + " + bytes.");
        }
        catch (Exception ex) {
            Log.e(TAG, "Error during copy: " + ex.getMessage());
        }

        try {
            inputStream.close();
        }
        catch (Exception ex) { }

        try {
            outputFile.close();
        }
        catch (Exception ex) { }

    }

    // Go through the list of files and copy them into the device's external storage directory.
    void tryCopyAuthFilesFromResources(ArrayList<ResourceIdTargetName> resourceDefinitions)
    {
        int i;

        for (i=0;i<resourceDefinitions.size();i++)
            copyToDeviceRoot(resourceDefinitions.get(i));
    }

    // The main method that copies the (possibly) present authentication key files from the raw resource.
    private void copyAuthenticationFilesToDevice()
    {
        ArrayList<ResourceIdTargetName> tamFileSourceTargets = listAuthenticationKeyfileCandidates();
        tryCopyAuthFilesFromResources(tamFileSourceTargets);
    }
}
