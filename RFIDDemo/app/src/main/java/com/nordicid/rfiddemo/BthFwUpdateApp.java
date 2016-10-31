package com.nordicid.rfiddemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.apptemplate.SubApp;
import com.nordicid.controllers.BthDFUController;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurEventAutotune;
import com.nordicid.nurapi.NurEventClientInfo;
import com.nordicid.nurapi.NurEventDeviceInfo;
import com.nordicid.nurapi.NurEventEpcEnum;
import com.nordicid.nurapi.NurEventFrequencyHop;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurEventInventory;
import com.nordicid.nurapi.NurEventNxpAlarm;
import com.nordicid.nurapi.NurEventProgrammingProgress;
import com.nordicid.nurapi.NurEventTagTrackingChange;
import com.nordicid.nurapi.NurEventTagTrackingData;
import com.nordicid.nurapi.NurEventTraceTag;
import com.nordicid.nurapi.NurEventTriggeredRead;

public class BthFwUpdateApp extends SubApp implements View.OnClickListener {

    private static final int REQ_FILE_OPEN = 1234;
    private BthDFUController mFWController = null;
    private Button mSelectFileBtn = null;
    private Button mUpdateFWBtn = null;
    private TextView mFileNameTV = null;
    private TextView mProgressTV = null;
    private TextView mStatusTV = null;
    private ProgressBar mProgressBar = null;
    private ProgressBar mSpinner = null;
    private Main mainRef = null;
    private View mView;
    private Handler mUIHandler = null;
    private NurApiListener mNurApiListener = null;
    /**
     * used to retain device address before going to DFU mode
     * restore this value after all exit points of dfu process
     **/
    private String mApplicationModeAddress = null;
    private boolean mUpdateRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mView = view;
        mSelectFileBtn = (Button) mView.findViewById(R.id.btn_file_picker);
        mUpdateFWBtn = (Button) mView.findViewById(R.id.btn_upload_fw);
        mSelectFileBtn.setOnClickListener(this);
        mUpdateFWBtn.setOnClickListener(this);

        mFileNameTV = (TextView) mView.findViewById(R.id.txt_file_name);
        mProgressTV = (TextView) mView.findViewById(R.id.text_view_progress_value);
        mStatusTV = (TextView) mView.findViewById(R.id.text_connection_status);

        mSpinner = (ProgressBar) mView.findViewById(R.id.progress_spinner);
        mSpinner.setVisibility(View.GONE);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.fw_upload_progressbar);
        mProgressBar.setProgress(0);
        mUIHandler = new Handler(Looper.getMainLooper());
        super.onViewCreated(view,savedInstanceState);
    }

    @Override
    public NurApiListener getNurApiListener() {
        return mNurApiListener;
    }

    @Override
    public int getTileIcon() {
        return R.drawable.ic_settings;
    }

    @Override
    public String getAppName() {
        return "BLE Firmware Update";
    }

    @Override
    public int getLayout() {
        return R.layout.bth_firmware_update_app;
    }

    public BthFwUpdateApp() {
        super();
        mainRef = Main.getInstance();
        mFWController = new BthDFUController(mainRef);
        /** DFU controller listener **/
        mFWController.setBthFwControllerListener(new BthDFUController.BthFirmwareControllerListener() {
            @Override
            public void onDeviceConnected(String address) {
                mStatusTV.setTextColor(Color.parseColor("#27ae60"));
                mStatusTV.setText("Connected to: " + address);
            }

            @Override
            public void onDeviceConnecting(String address) {
                mStatusTV.setTextColor(Color.parseColor("#e67e22"));
                mStatusTV.setText("Connecting to: " + address);
            }

            @Override
            public void onDeviceDisconnected(String address) {
                mStatusTV.setTextColor(Color.parseColor("#c0392b"));
                mStatusTV.setText("Disconnected");
            }

            @Override
            public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
                mProgressBar.setProgress(percent);
                mProgressTV.setText("Updating " + percent + "%");
            }

            @Override
            public void onDfuAborted(String deviceAddress) {
                handleUpdateFinish();
                mProgressTV.setText("DFU aborted by user.");
            }

            @Override
            public void onUpdateError(String deviceAddress, int error, int errorType, String message) {
                mProgressTV.setText("Update error: " + message + " Code:" + error);
                handleUpdateFinish();
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(mainRef);
                builder.setMessage("Update failed. Please restart your device and try again.").setPositiveButton("Ok", dialogClickListener).show();
            }

            @Override
            public void onFirmwareValidating(String deviceAddress) {
                mProgressTV.setText("Validating firmware");
            }

            @Override
            public void onDfuProcessStarting(String deviceAddress) {
                mProgressTV.setText("Update service starting");
            }

            @Override
            public void onDfuProcessStarted(String deviceAddress) {
                mProgressTV.setText("Update service started");
            }

            @Override
            public void onEnablingDfuMode(String deviceAddress) {
                mProgressTV.setText("Enabling service starting");
            }

            @Override
            public void onDeviceDisconnecting(String deviceAddress) {
                mStatusTV.setTextColor(Color.parseColor("#c0392b"));
                mStatusTV.setText("Disconnecting from: " + deviceAddress);
            }

            @Override
            public void onDfuCompleted(String deviceAddress) {
                mProgressTV.setText("Update Complete");
                mProgressBar.setProgress(mProgressBar.getMax());
                handleUpdateFinish();
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                };
                AlertDialog.Builder builder = new AlertDialog.Builder(mainRef);
                builder.setMessage("Update finished. you can now turn on your device normally.").setPositiveButton("Ok", dialogClickListener).show();
            }
        });
        /** NUR API listener **/
        mNurApiListener = new NurApiListener() {
            @Override
            public void connectedEvent() {
                enableAll();
            }
            @Override
            public void disconnectedEvent() {}
            @Override
            public void logEvent(int i, String s) {}
            @Override
            public void bootEvent(String s) {}
            @Override
            public void inventoryStreamEvent(NurEventInventory arg0) {}
            @Override
            public void IOChangeEvent(NurEventIOChange arg0) {}
            @Override
            public void traceTagEvent(NurEventTraceTag arg0) {}
            @Override
            public void triggeredReadEvent(NurEventTriggeredRead arg0) {}
            @Override
            public void frequencyHopEvent(NurEventFrequencyHop arg0) {}
            @Override
            public void debugMessageEvent(String s) {}
            @Override
            public void inventoryExtendedStreamEvent(NurEventInventory arg0) {}
            @Override
            public void programmingProgressEvent(NurEventProgrammingProgress arg0) {}
            @Override
            public void deviceSearchEvent(NurEventDeviceInfo arg0) {}
            @Override
            public void clientConnectedEvent(NurEventClientInfo arg0) {}
            @Override
            public void clientDisconnectedEvent(NurEventClientInfo arg0) {}
            @Override
            public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) {}
            @Override
            public void epcEnumEvent(NurEventEpcEnum arg0) {}
            @Override
            public void autotuneEvent(NurEventAutotune arg0) {}
            @Override
            public void tagTrackingScanEvent(NurEventTagTrackingData arg0) {}
            @Override
            public void tagTrackingChangeEvent(NurEventTagTrackingChange arg0s) {}
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_file_picker:
                Intent intent;
                Intent chooser;
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                chooser = Intent.createChooser(intent, getResources().getString(R.string.file_picker));
                try {
                    startActivityForResult(chooser, REQ_FILE_OPEN);
                } catch (Exception ex) {
                    String strErr = ex.getMessage();
                    Toast.makeText(mainRef, "Error:\n" + strErr, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_upload_fw:
                /** Disconnect device
                 *  Save Application mode address to restore autoConnect
                 *  convert address to DFU mode and start
                 **/
                mApplicationModeAddress = mainRef.getNurAutoConnect().getAddress();
                if (mApplicationModeAddress == null || mApplicationModeAddress.isEmpty()) {
                    Toast.makeText(mainRef, R.string.no_bluetooth_device, Toast.LENGTH_SHORT).show();
                    break;
                }
                //mainRef.getNurAutoConnect().setAddress("");
                if(handleUpdateStart()) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mFWController.StartDfu();
                        }
                    }, 4000);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE_OPEN) {
            if (data != null) {
                final String fullPath;
                fullPath = getActualFileName(data.getDataString());
                if (fullPath == null) {
                    mFileNameTV.setText(R.string.not_available);
                    Toast.makeText(mainRef, R.string.no_file_selected, Toast.LENGTH_SHORT).show();
                } else {
                    mFWController.setFilePath(fullPath);
                    // TODO any other way ?
                    mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            setTextFileName();
                            enableAll();
                        }
                    });
                }
            }
        }
    }

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

    private void disableAll() {
        enableButtons(false);
    }

    private void enableAll() {
        enableButtons(true);
    }

    private void enableButtons(boolean enable) {
        if(mUpdateRunning){
            mSelectFileBtn.setEnabled(false);
            mUpdateFWBtn.setEnabled(false);
            return;
        }
        if (mainRef.getNurApi().isConnected()) {
            mSelectFileBtn.setEnabled(enable);
            mUpdateFWBtn.setEnabled(enable && mFWController.isFileSet());
        } else {
            mSelectFileBtn.setEnabled(true);
            mUpdateFWBtn.setEnabled(false);
        }
    }

    private void handleUpdateFinish(){
        mUpdateRunning = false;
        mProgressBar.setProgress(0);
        mSpinner.setVisibility(View.GONE);
        mainRef.getNurAutoConnect().setAddress(mApplicationModeAddress);
        keepScreenOn(false);
        enableAll();
    }

    private boolean handleUpdateStart(){
        try {
            Main.getInstance().getAccessoryApi().restartBLEModuleToDFU();
        } catch (Exception e) {
            return false;
        }
        mUpdateRunning = true;
        mProgressTV.setText(R.string.initiating_update);
        keepScreenOn(true);
        mSpinner.setVisibility(View.VISIBLE);
        mFWController.setTargetAddress(mFWController.getDfuTargetAddress(mApplicationModeAddress));
        disableAll();
        return true;
    }

    private void setTextFileName(){
        String path = mFWController.getFilePath();
        if(path != null && !path.isEmpty()){
            mFileNameTV.setText(path.substring(path.lastIndexOf('/') + 1));
        } else {
            mFileNameTV.setText(R.string.not_available);
        }
    }

    private void setTextDeviceConnection(){
        if(mUpdateRunning){
            mStatusTV.setTextColor(Color.parseColor("#27ae60"));
            mStatusTV.setText("Connected to: " + mFWController.getTargerAddress());
        } else {
            mStatusTV.setTextColor(Color.parseColor("#c0392b"));
            mStatusTV.setText(R.string.state_disconnected);
        }
    }

    private void keepScreenOn(boolean value) {
        mView.setKeepScreenOn(value);
    }

    @Override
    public void onResume() {
        super.onResume();
        mFWController.registerListener();
        enableAll();
        setTextFileName();
        setTextDeviceConnection();
    }

    @Override
    public void onPause() {
        super.onPause();
        mFWController.unregisterListener();
    }

    @Override
    public boolean onFragmentBackPressed(){
        /** while update running back button and close button will be disabled **/
        if(!mUpdateRunning){
            return false;
        }
        return true;
    }

}
