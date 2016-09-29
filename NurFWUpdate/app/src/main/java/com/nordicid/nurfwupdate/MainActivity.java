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

package com.nordicid.nurfwupdate;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.nurapi.*;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements NurApiListener {

    public static final String TAG = "NUR_FW_Update";

    /** Requesting file for uploading. */
    private static final int REQ_FILE_OPEN = 1;

    /** Device seach timeout in ms. */
    public static final int DEVICE_SEARCH_TIMEOUT_MS = 4000;

    /** Something to use with the direct call to the boot event. */
    private static final String BOOT_STRING = "LOADER";

    /** FW version string to show in status. */
    private String mVersionStr = null;
    /** When tru the boot event begins the FW upload. */
    private boolean mStartProgramOnBoot = false;
    /** True when the the last programmed page event arrived or when an error occurred. */
    private boolean mUpdateComplete = false;

    /** True if the updload completed OK. */
    private boolean mUpdateCompleteWasOK = true;
    /** Error string to show upon upload error. */
    private String mStrCompletionError = "";

    /** Used for connect button's text selection. */
    private boolean mProgramFailed = false;
    /** True if the selected file is an application binary. */
    private boolean mIsApplication = false;
    /** Used to detect the progress change during upload. */
    private int mCurrentProgress = -1;

    /** File type detection. */
    private NurBinFileType mFwFileType = null;

    // Controls/views/indicators.
    private TextView mModeView;
    private Button mConnectBtn;
    private Button mFileBtn;
    private Button mUploadBtn;
    private TextView mFileView;
    private TextView mProgressViev;
    private ProgressBar mUploadProgress;

    // API related things.
    private NurApi mApi;
    private NurApiAutoConnectTransport mAcTr = null;
    /** True if file selection is ongoing. Prevents unecessary resume calls when connected. */
    private boolean mSelectingFile = false;
    /** Currently selected device's MAC string. */
    private String mDeviceAddress = "";

    /** Full FW file name (path + name). */
    private String mFullFilePath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e(TAG, "CREATE");

        setContentView(R.layout.activity_main);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mModeView = (TextView) findViewById(R.id.text_current_mode);
        mConnectBtn = (Button) findViewById(R.id.btn_device);
        mFileBtn = (Button) findViewById(R.id.btn_file_select);
        mUploadBtn = (Button) findViewById(R.id.btn_file_program);
        mFileView = (TextView) findViewById(R.id.text_filename);
        mProgressViev = (TextView) findViewById(R.id.text_progress);
        mUploadProgress = (ProgressBar) findViewById(R.id.progress_upload);

        mUploadBtn.setEnabled(false);
        mUploadProgress.setProgress(0);

        mApi = new NurApi();

        mApi.setUiThreadRunner(new NurApiUiThreadRunner() {
            @Override
            public void runOnUiThread(Runnable runnable) {
                MainActivity.this.runOnUiThread(runnable);
            }
        });

        mApi.setListener(this);

        String appversion = "0.0";
        try {
            appversion = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        shortToast(this, this.getString(R.string.app_name) + " v"+appversion+"\nNurApi v" + mApi.getFileVersion());
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.e(TAG, "RESUME");

        if (mAcTr != null) {
            mAcTr.onResume();
        }

        if (mStartProgramOnBoot)
            return;

        if (mAcTr != null && !mSelectingFile) {
            showConnecting();
            // mConnectBtn.setEnabled(mDeviceAddress == null);
        } else if (mSelectingFile) {
            if (mApi.isConnected()) {
                enableAll();
            }
        }

        mSelectingFile = false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.e(TAG, "PAUSE");

        if (mStartProgramOnBoot)
            return;

        if (mAcTr != null && mDeviceAddress != null && !mSelectingFile) {
            disableAll();
            showConnecting();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "STOP");

        if (mSelectingFile)
            return;

        if (mAcTr != null)
            mAcTr.onStop();
    }

    public void handleButtonClick(View v) {
        int id;

        id = v.getId();

        switch (id) {
            case R.id.btn_device:
                handleConnectDisconnect();
                break;

            case R.id.btn_file_select:
                tryGetFile();
                break;

            case R.id.btn_file_program:
                prepareUpload();
                break;
        }
    }

    private void handleBootEvent()
    {
        tryVersion();

        if (mUpdateComplete || mStartProgramOnBoot)
        {
            setModeTextByMode();

            if (mStartProgramOnBoot) {
                mStartProgramOnBoot = false;
                beginUpload();
            }

            if (mUpdateComplete) {
                if (!mUpdateCompleteWasOK) {
                    mProgressViev.setText(mStrCompletionError);
                }
                else
                    mProgressViev.setText(R.string.text_upload_complete_ok);
                mUploadProgress.setProgress(0);
            }

            mUpdateComplete = false;
        }
        else {
            setModeTextByMode();
        }
    }

    // Prepare uploading by generating a boot event
    // either by booting the module or calling the boot event directly (already in bootloader mode).
    private void prepareUpload() {
        boolean startOk = false;

        if (mApi.isConnected() && mFullFilePath != null) {
            mStartProgramOnBoot = true;
            try {
                if (isAppMode())
                    mApi.moduleBoot(false);
                else
                    bootEvent(BOOT_STRING); // Fake.

                startOk = true;
            } catch (Exception ex) {
                mStartProgramOnBoot = false;
                shortToast(this, "Start error: " + ex.getMessage());
            }
        }

        if (startOk) {
            disableAll();
        }
    }

    private void disableAll() {
        enableButtons(false);
    }

    private void enableAll() {
        enableButtons(true);
    }

    private void enableButtons(boolean en) {
        if (mApi.isConnected()) {
            mConnectBtn.setEnabled(en);
            mFileBtn.setEnabled(en);
            mUploadBtn.setEnabled(en && mFullFilePath != null);
        } else {
            mConnectBtn.setEnabled(true);
            mFileBtn.setEnabled(true);
            mUploadBtn.setEnabled(false);
        }
    }

    // Gets error message for failed upload.
    private void handleFwProgrammingError(final String errMsg) {
        (new Handler(getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                mStrCompletionError = errMsg;
                enableAll();
                mProgressViev.setText(errMsg);
            }
        });
    }

    // Program "thread": simply calls the required method to program new FW.
    // Programming generates events that, when handled, do the progress indication and error detection.
    private void programThread() {
        mUpdateCompleteWasOK = true;
        try {
            if (mIsApplication)
                mApi.programApplicationFile(mFullFilePath);
            else
                mApi.programBootloaderFile(mFullFilePath);
            mStrCompletionError = null;
        } catch (Exception ex) {
            // Error(s) will be handled elsewhere.
        }

        try {
            visibleLog("programThread(): BOOT");
            mApi.moduleBoot(false);
            visibleLog("programThread(): BOOT CALLED");
            mStrCompletionError = null;
        } catch (Exception ex) {
            // TODO
            mStrCompletionError = ex.getMessage();
            visibleLog("programThread(), FATAL: " + mStrCompletionError);
        }
    }

    // Start a thread that call the necessary update method.
    private void beginUpload() {
        mUpdateComplete = false;
        mCurrentProgress = -1;
        (new Thread(new Runnable() {
            @Override
            public void run() {
                programThread();
            }
        })).start();
    }

    // Choose a file to upload.
    private void tryGetFile() {
        Intent intent;
        Intent chooser;

        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        chooser = Intent.createChooser(intent, "Select file");

        try {
            mSelectingFile = true;
            mFullFilePath = null;
            startActivityForResult(chooser, REQ_FILE_OPEN);
        } catch (Exception ex) {
            String strErr = ex.getMessage();
            shortToast(this, "Error:\n" + strErr);
        }
    }

    // Starts the device scan activity.
    private void scanForDevice() {
        NurDeviceListActivity.startDeviceRequest(MainActivity.this);//, NurDeviceListActivity.REQ_BLE_DEVICES, DEVICE_SEARCH_TIMEOUT_MS, false);
    }

    // Tries to get the FW version.
    private void tryVersion() {
        try {
            mVersionStr = mApi.getReaderInfo().swVersion;
        } catch (Exception ex) {
            mVersionStr = null;
        }
    }

    private void showConnecting() {
        String strConnection;
        strConnection = "Connecting: " + mDeviceAddress;

        mProgressViev.setText(strConnection);
    }

    private void showConnection() {
        String strConnection;
        strConnection = "Connected: " + mDeviceAddress;
        mProgressViev.setText(strConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == NurDeviceListActivity.REQUEST_SELECT_DEVICE && resultCode == NurDeviceListActivity.RESULT_OK && data != null) {

            disableAll();
            mDeviceAddress = null;

            try {
                NurDeviceSpec spec = new NurDeviceSpec(data.getStringExtra(NurDeviceListActivity.SPECSTR));
                mDeviceAddress = spec.getAddress();

                if (mAcTr != null) {
                    System.out.println("Dispose transport");
                    mAcTr.dispose();
                }

                mAcTr = NurDeviceSpec.createAutoConnectTransport(this, mApi, spec);
                mAcTr.setAddress(spec.getAddress());
                showConnecting();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (requestCode == REQ_FILE_OPEN) {
            if (data != null) {
                String fullPath;
                String strFileText = "Selected file:\nL2 ";

                fullPath = getActualFileName(data.getDataString());

                if (fullPath == null || !inspectSelectedFwFile(fullPath))
                {
                    mFullFilePath = null;
                    mFileView.setText(R.string.text_file_na);
                    longToast(this, "Selected file check failed!");
                }
                else {

                    if (mIsApplication)
                        strFileText += "application";
                    else
                        strFileText += "bootloader";
                    strFileText += (", version: " + mFwFileType.getVersion());

                    mFullFilePath = fullPath;

                    mFileView.setText(strFileText);
                }
            }
        }
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

    private void handleConnectDisconnect() {
        mFileBtn.setEnabled(false);
        mUploadBtn.setEnabled(false);

        if (mApi.isConnected()) {

            mDeviceAddress = null;
            if (mAcTr != null) {
                mAcTr.dispose();
                mAcTr = null;
            }

            mConnectBtn.setText(R.string.text_search_devices);
            mConnectBtn.setEnabled(true);
        } else {
            scanForDevice();
        }
    }

    // Return true if the modulöe is detected to be in the application mode.
    private boolean isAppMode() throws Exception {
        return mApi.getMode().equalsIgnoreCase("A");
    }

    private void setModeTextByMode() {
        String modeString;

        try {
            if (isAppMode())
                modeString = getString(R.string.text_curmode_app);
            else
                modeString = getString(R.string.text_curmode_loader);
        } catch (Exception ex) {
            shortToast(this, "Could not get mode...");
            mModeView.setText(R.string.text_mode_error);
            return;
        }

        if (mVersionStr != null)
            modeString += (": " + mVersionStr);

        mModeView.setText(modeString);
    }

    // Buzzer or whatever in the "raw" directory.
    private void postFailureSound() {
        (new Handler(getMainLooper())).postDelayed(new Runnable() {
            @Override
            public void run () {
                try {
                    Uri notification = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.buzzer_x);
                    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                    r.play();
                }
                catch (Exception e) {
                }
            }
        }, 50);
    }

    @Override
    public void logEvent(int logLevel, String logMessage) {
        // Log message from the API.
        Log.d(TAG, logMessage);
    }

    @Override
    public void IOChangeEvent(NurEventIOChange eventIOChange) {
        // Event from I/O sampo or other board with external I/O
    }

    @Override
    public void bootEvent(String bootString) {
        // "APP", "LOADER"
        handleBootEvent();
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
        if (mStartProgramOnBoot) {
            handleBootEvent();
            return;
        }
        // Generic connected event
        Log.e(TAG, "CONNECT");
        tryVersion();
        mFileBtn.setEnabled(true);
        mUploadBtn.setEnabled(mFullFilePath != null);
        mConnectBtn.setText(R.string.text_disconnect);
        mConnectBtn.setEnabled(true);
        setModeTextByMode();
        showConnection();
    }

    @Override
    public void debugMessageEvent(String dbgMessage) {
        // A device running debug build has sent a debug message.
    }

    @Override
    public void deviceSearchEvent(NurEventDeviceInfo eventDeviceInfo) {
        // Device search response as an event.
    }

    @Override
    public void disconnectedEvent() {
        if (mStartProgramOnBoot)
            return;

        // General disconnection notification.
        Log.e(TAG, "DISCONNECT");
        mVersionStr = null;
        mFileBtn.setEnabled(false);
        mUploadBtn.setEnabled(false);
        mModeView.setText(R.string.text_not_connected);
        mConnectBtn.setText(R.string.text_search_devices);
        if (!mProgramFailed)
            mProgressViev.setText(R.string.text_idle);
        mProgramFailed = false;
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

    void visibleLog(String vMsg)
    {
        Log.e(TAG, vMsg);
    }

    @Override
    public void programmingProgressEvent(NurEventProgrammingProgress eventProgramming) {
        // Information about software update progress.
        boolean done = false;
        String msg;
        int error, completedPage, totalPages, progress;

        error = eventProgramming.error;
        completedPage = eventProgramming.currentPage + 1; // From 0...
        totalPages = eventProgramming.totalPages;
        progress = (completedPage * 100) / totalPages;

        if (error == 0) {
            if (completedPage == 0) // Actual is -1, start notification.
            {
                mUploadProgress.setProgress(0);
                return;
            }

            if (completedPage == totalPages) {
                visibleLog("About to complete");
                mProgressViev.setText("Ready, wait for boot...");
                mUploadProgress.setProgress(mUploadProgress.getMax());
                mUpdateComplete = true;
                done = true;
            }
            else {
                msg = "Page: " + completedPage + " / " + totalPages;
                mProgressViev.setText(msg);
                if (mCurrentProgress != progress) {
                    mUploadProgress.setProgress(progress);
                    mCurrentProgress = progress;
                }
            }
        }
        else
        {
            postFailureSound();
            // TODO
            String strError;
            mUpdateCompleteWasOK = false;
            strError = NurApiErrors.getErrorMessage(error);
            handleFwProgrammingError("Program error:\n" + strError);
            Log.e(TAG, "Upload completion error: " + strError);

            mUploadProgress.setProgress(0);
            mProgramFailed = true;
            done = true;
            if (mAcTr != null) {
                mAcTr.dispose();
                mAcTr = null;
            }
        }

        if (done)
            enableButtons(true);
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

    private boolean inspectSelectedFwFile(String fileName)
    {
        boolean checkOK = false;
        mFwFileType = null;

        try
        {
            mFwFileType = mApi.checkNurFwBinaryFile(fileName);

            if (mFwFileType.fwType == NurApi.NUR_BINTYPE_L2APP)
                mIsApplication = true;
            else
                mIsApplication = false;

            checkOK = true;
        }
        catch (Exception ex)
        {
            // TODO
            Log.e(TAG, "File check error: " + ex.getMessage());
        }

        return checkOK;
    }

    private String []splitByChar(String stringToSplit, char separator, boolean removeEmpty)
    {
        String expression;
        ArrayList<String> strList = new ArrayList<>();
        String []arr;
        String tmp;

        expression = String.format("\\%c", separator);
        arr = stringToSplit.split(expression);

        for (String s : arr)
        {
            tmp = s.trim();
            if (removeEmpty && tmp.isEmpty())
                continue;
            strList.add(tmp);
        }

        return strList.toArray(new String[0]);
    }

    public static void shortToast(Context ctx, String message)
    {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    public static void longToast(Context ctx, String message)
    {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    public static String yesNo(boolean yes)
    {
        return yes ? "YES" : "NO";
    }
}
