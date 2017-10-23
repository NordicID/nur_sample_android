package com.nordicid.rfiddemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.controllers.BthDFUController;
import com.nordicid.controllers.NURFirmwareController;
import com.nordicid.controllers.UpdateController;
import com.nordicid.helpers.UpdateContainer;
import com.nordicid.helpers.UpdateContainerListAdapter;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurDeviceScanner;
import com.nordicid.nurapi.NurDeviceSpec;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nordicid.apptemplate.AppTemplate.getAppTemplate;
import com.nordicid.nurapi.BleScanner;
import com.nordicid.nurapi.BleScanner.BleScannerListener;

public class SettingsAppUpdatesTab extends android.support.v4.app.Fragment implements View.OnClickListener, BleScannerListener {

    static final int REQ_FILE_OPEN = 42;
    SettingsAppTabbed mOwner;
    View mView;
    NurApi mApi;
    /* stores device application address */
    String mApplicationModeAddress;
    NurAccessoryExtension mExt;
    RadioGroup mUpdateSelector;
    TextView mUpdateStatus;
    TextView mUpdateProgress;
    TextView mFileName;
    TextView mUIBlockedTV;
    Button mFilePicker;
    Button mStartUpdate;
    Button mRemoteFilePicker;
    ProgressBar mProgressBar;
    ProgressBar mSpinner;
    RadioButton mNURFWRadio;
    RadioButton mDFURadio;
    RadioButton mDFUBLDRRadio;
    RadioButton mNURBLDRRadio;

    Handler mHandler;

    /** Used to search for DFU targets **/
    String mDFUTargetAdd = "";
    boolean mDFUTargetAddFound = false;
    List<BluetoothDevice> mDfuExaFound = new ArrayList<BluetoothDevice>();

    boolean mUpdateRunning = false;
    /** true if NUR FW update is selected **/
    boolean mNURFWUpdate = true;
    /** true if NUR | DFU APP update is selected **/
    boolean mAppUpdate = true;
    boolean mUpdateRetry = false;
    /** holds NUR MODULE type **/
    private String mModuleType;
    /** holds currently selected updateController **/
    UpdateController mCurrentController;
    BthDFUController mDFUController;
    NURFirmwareController mNURAPPController;
    /** Listeners **/
    NurApiListener mNURControllerListener = null;
    BthDFUController.BthFirmwareControllerListener mDFUUpdateListener = new BthDFUController.BthFirmwareControllerListener() {
        @Override
        public void onDeviceConnected(String address) {
            setStatus(R.color.StatusGreen,R.string.update_connected);
        }

        @Override
        public void onDeviceConnecting(String address) {
            setStatus(R.color.StatusOrange,R.string.update_connecting);
        }

        @Override
        public void onDeviceDisconnected(String address) {
            setStatus(R.color.StatusRed,R.string.update_disconnected);
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            setStatus(R.color.StatusGreen,R.string.update_started);
            setProgress(percent);
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            handleUpdateFinished();
            mUpdateProgress.setText(getString(R.string.update_aborted));
        }

        @Override
        public void onUpdateError(String deviceAddress, int error, int errorType, String message) {
            mUpdateProgress.setText("Update error: " + message + " Code:" + error);
            Log.e("NIDDFUUpdate","Update failed: " + error + " " + errorType + " " + message);
            handleUpdateFinished();
            AlertDialog.Builder alert = createAlertDialog("Update failed. Please restart your device and try again.");
            if(!mUpdateRetry)
                alert.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mUpdateRetry = true;
                        handleDFUUpdateStart();
                    }
                });
            alert.setNegativeButton("Dismiss",null);
            alert.show();
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            setStatus(R.color.StatusOrange,R.string.update_validating_fw);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            setStatus(R.color.StatusOrange,R.string.update_starting);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            setStatus(R.color.StatusGreen,R.string.update_started);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            setStatus(R.color.StatusOrange,R.string.update_starting);
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            setStatus(R.color.StatusOrange,R.string.update_disconnecting);
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            setStatus(R.color.StatusGreen,R.string.update_complete);
            setProgress(mProgressBar.getMax());
            handleUpdateFinished();
            createAlertDialog(getString(R.string.dfu_finished)).show();
        }
    };

    NURFirmwareController.FirmWareControllerListener mNURFWListener = new NURFirmwareController.FirmWareControllerListener() {
        @Override
        public void onProgrammingEvent(NurEventProgrammingProgress nurEventProgrammingProgress) {
            int error, completedPage, totalPages, progress;
            if(!mUpdateRunning)
                return;
            error = nurEventProgrammingProgress.error;
            completedPage = nurEventProgrammingProgress.currentPage + 1; // From 0...
            totalPages = nurEventProgrammingProgress.totalPages;
            progress = (completedPage * 100) / totalPages;
            if (error == 0) {
                if (completedPage == 0) {
                    setProgress(0);
                    return;
                }
                if (completedPage == totalPages) {
                    setStatus(R.color.StatusOrange,R.string.ready_wait_boot);
                    setProgress(mProgressBar.getMax());
                } else {
                    setProgressText(progress + "%");
                    setProgress(progress);
                }
            }
        }

        @Override
        public void onUpdateComplete(boolean status) {
            if(!mUpdateRunning)
                return;
            if (status)
                setStatus(R.color.StatusGreen,R.string.update_complete);
            else
                setStatus(R.color.StatusGreen,mNURAPPController.getCompletionError());
            setProgress(mProgressBar.getMax());
            handleUpdateFinished();
            createAlertDialog("Update " + ((status) ? "succesful" : "failed. Please try again.")).show();
        }

        @Override
        public void onUpdateStarted() {
            Main.getInstance().setProgrammingFlag(true);
            getAppTemplate().setEnableBattUpdate(false);
            keepScreenOn(true);
            mUpdateRunning = true;
            disableAll();
            setStatus(R.color.StatusGreen,R.string.update_started);
        }

        @Override
        public void onDeviceConnected() {
            enableAll();
        }

        @Override
        public void onDeviceDisconnected() {
            disableAll();
        }

        @Override
        public void onUpdateInterrupted(String error) {
            setStatus(R.color.StatusRed,R.string.update_aborted);
            handleUpdateFinished();
        }
    };

    NurApiListener mNURApiListener = new NurApiListener() {
        @Override
        public void connectedEvent() { enableAll();
            handleDFUSupported();
            if(mUpdateRunning && mNURControllerListener != null){
                mNURControllerListener.connectedEvent();
            }
        }
        @Override
        public void disconnectedEvent() {
            if(mUpdateRunning && mNURControllerListener != null){
                mNURControllerListener.disconnectedEvent();
            }
        }
        @Override
        public void bootEvent(String s) {
            if(mUpdateRunning && mNURControllerListener != null){
                mNURControllerListener.bootEvent(s);
            }
        }
        @Override
        public void programmingProgressEvent(NurEventProgrammingProgress arg0) {
            if(mUpdateRunning && mNURControllerListener != null){
                mNURControllerListener.programmingProgressEvent(arg0);
            }
        }
        @Override
        public void logEvent(int i, String s) {}
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

    @Override
    public void onBleDeviceFound(final BluetoothDevice device, final String name, final int rssi)
    {
        if (device.getAddress().equalsIgnoreCase(mDFUTargetAdd)){
            Log.i("DEVICESCAN", "Found target device " + device.getAddress());
            mDFUTargetAddFound = true;
            mDfuExaFound.add(device);
            BleScanner.getInstance().unregisterListener(this);
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run()
                {
                    dfuScanFinished();
                }
            }, 3000);
        }
        else if (name.equalsIgnoreCase("dfuexa") || name.equalsIgnoreCase("dfunid")) {
            mDfuExaFound.add(device);
        }
    }

    public SettingsAppUpdatesTab(){
        mHandler = new Handler();

        mOwner = SettingsAppTabbed.getInstance();
        mApi = mOwner.getNurApi();
        mExt = getAppTemplate().getAccessoryApi();
        /** get controllers from Main **/
        mNURAPPController = Main.getInstance().getNURUpdateController();
        mDFUController = Main.getInstance().getDFUUpdateController();
        mCurrentController = mNURAPPController;

        /** Listeners **/
        mNURControllerListener = mNURAPPController.getNurApiListener();
        mDFUController.setBthFwControllerListener(mDFUUpdateListener);

    }

    private void handleDFUSupported(){
        if(Main.getInstance().getAccessorySupported()){
            //Check bootloader versio
            if(mDFUController.isBldrUpdateAvailable())
                mDFUBLDRRadio.setEnabled(true);
            else mDFUBLDRRadio.setEnabled(false);

            mDFURadio.setEnabled(true);
        }
    }

    private AlertDialog.Builder createAlertDialog(String text){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        TextView message = new TextView(getActivity());
        builder.setTitle("Update application");
        message.setText(text);
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        message.setGravity(Gravity.CENTER);
        builder.setView(message).setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        return builder;
    }

    private void setStatus(int colorId,int textId){
        mUpdateStatus.setTextColor(mOwner.getResources().getColor(colorId));
        mUpdateStatus.setText(textId);
    }

    private void setStatus(int colorId,String text){
        mUpdateStatus.setTextColor(Color.RED);
        mUpdateStatus.setText(text);
    }

    private void handleUpdateFinished(){
        mUpdateRunning = false;
        mDFUTargetAddFound = false;
        mDfuExaFound.clear();
        setProgress(0);
        setStatus(R.color.StatusRed,R.string.update_state_idle);
        mSpinner.setVisibility(View.GONE);
        keepScreenOn(false);
        enableAll();
        if(mNURFWUpdate){
            Main.getInstance().setProgrammingFlag(false);
            getAppTemplate().setEnableBattUpdate(true);
        } else {
            Main.getInstance().loadSettings();
            //Main.getInstance().getNurAutoConnect().setAddress(mApplicationModeAddress);
        }
        toggleLockUI(false);
        mUIBlockedTV.setVisibility(View.GONE);
    }

    private void setProgress(int progress){
        mProgressBar.setProgress(progress);
        mUpdateProgress.setText(progress + "%");
    }

    private void dfuScanFinished()
    {
        Log.i("DEVICESCAN", "dfuScanFinished; mUpdateRunning " + mUpdateRunning + "; mDfuExaFound " + mDfuExaFound.size());

        if (!mUpdateRunning && mDfuExaFound.size() > 0)
        {
            if (!mDFUTargetAddFound && mDfuExaFound.size() > 1) {
                // TODO: Pop up selection list for mDfuExaFound
            }
            Log.e("DEVICESCAN", "Starting DFU controller");
            mUpdateRunning = true;
            keepScreenOn(true);
            disableAll();

            final String dfuTargAddr = mDfuExaFound.get(0).getAddress();
            setStatus(R.color.StatusOrange,R.string.update_starting);

            mDFUController.setTargetAddress(dfuTargAddr);
            mDFUController.startUpdate();
        }
        else if (mDfuExaFound.size() == 0)
        {
            mUpdateProgress.setText("DFU Device not found");
            Log.e("NIDDFUUpdate","DFU Device not found");
            handleUpdateFinished();
        }
    }

    private boolean handleDFUUpdateStart(){
        try {
            /* skip this if retrying device already in DFU mode */
            if(!mUpdateRetry) {
                Main.getInstance().getAccessoryApi().restartBLEModuleToDFU();
                Thread.sleep(500);
                Main.getInstance().disposeTrasport();
                Thread.sleep(500);
            }
            mDFUTargetAddFound = false;
            mDfuExaFound.clear();
            mDFUTargetAdd = mDFUController.getDfuTargetAddress(mApplicationModeAddress);
            mDFUController.setTargetAddress(mDFUTargetAdd);
            setStatus(R.color.StatusOrange,R.string.looking_for_device);

            BleScanner.getInstance().registerScanListener(this);

            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run()
                {
                    dfuScanFinished();
                }
            }, 10000);

            return true;

        }  catch (Exception e) {
            e.printStackTrace();
            Log.e("UPDATE APP",e.getMessage());
        }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tab_settings_updates, container, false);
        mView = view;
        mUpdateSelector = (RadioGroup) view.findViewById(R.id.radioUpdateSelect);
        mUpdateStatus = (TextView) view.findViewById(R.id.text_update_status);
        mUpdateProgress = (TextView) view.findViewById(R.id.text_update_progress);
        mFileName = (TextView) view.findViewById(R.id.text_filename);
        mUIBlockedTV = (TextView) view.findViewById(R.id.ui_blocked_tv);
        mFilePicker = (Button) view.findViewById(R.id.btn_select_file);
        mFilePicker.setOnClickListener(this);
        mRemoteFilePicker = (Button) view.findViewById(R.id.btn_download_file);
        mRemoteFilePicker.setOnClickListener(this);
        mStartUpdate = (Button) view.findViewById(R.id.btn_start_update);
        mStartUpdate.setOnClickListener(this);
        mProgressBar = (ProgressBar) view.findViewById(R.id.update_progressbar);
        mProgressBar.setMax(100);
        mSpinner = (ProgressBar) view.findViewById(R.id.progress_spinner);
        mNURFWRadio = (RadioButton) view.findViewById(R.id.radio_nur_update);
        mDFURadio = (RadioButton) view.findViewById(R.id.radio_dfu_update);
        mDFUBLDRRadio = (RadioButton) view.findViewById(R.id.radio_dfu_bldr_update);
        mNURBLDRRadio = (RadioButton) view.findViewById(R.id.radio_nur_bldr_update);

        /** Radio group listener **/
        mUpdateSelector.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                /** Thanks to google. this gets triggered whenever you start / leave / resume this fragment
                 *  Any other solution ?
                 **/
                if(!mNURFWRadio.isPressed() && !mDFURadio.isPressed() && !mNURBLDRRadio.isPressed() && !mDFUBLDRRadio.isPressed())
                    return;
                mNURAPPController.setFilePath(null);
                mDFUController.setFilePath(null);
                mFileName.setText(getString(R.string.not_available));
                enableAll();
                switch(checkedId) {
                    case R.id.radio_dfu_bldr_update:
                    case R.id.radio_dfu_update:
                        mNURFWUpdate = false;
                        mAppUpdate = checkedId == R.id.radio_dfu_update;
                        mCurrentController = mDFUController;
                        break;
                    case R.id.radio_nur_bldr_update:
                    case R.id.radio_nur_update:
                        mNURFWUpdate = true;
                        mAppUpdate = checkedId == R.id.radio_nur_update;
                        mCurrentController = mNURAPPController;
                        break;
                    default:
                        // what ?
                        break;
                }
            }
        });
        return view;
    }

    @Override
    public void onClick(View view){
        try {
            mModuleType = mApi.getFwInfo().get("MODULE");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        switch (view.getId()){
            case R.id.btn_select_file:
                Intent intent;
                Intent filePicker;
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                //intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                if(mNURFWUpdate)
                    intent.setType("application/octet-stream");
                else
                    intent.setType("application/zip");
                filePicker = Intent.createChooser(intent, getResources().getString(R.string.file_picker));
                try {
                    Main.getInstance().setDoNotDisconnectOnStop(true);
                    startActivityForResult(filePicker, REQ_FILE_OPEN);
                } catch (Exception ex) {
                    String strErr = ex.getMessage();
                    Toast.makeText(mOwner.getActivity(), "Error:\n" + strErr, Toast.LENGTH_SHORT).show();
                    Main.getInstance().setDoNotDisconnectOnStop(false);
                }
                break;
            case R.id.btn_download_file:
                final Dialog dialog = new Dialog(Main.getInstance());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.updates_list_view);
                final ListView updatesList = (ListView) dialog.findViewById(R.id.updates_list_view);
                /* click listener
                 *  Selected files are handled here
                 */
                updatesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        UpdateContainer selectedUpdate = (UpdateContainer) updatesList.getItemAtPosition(position);
                        mCurrentController.grabUpdateFile(selectedUpdate);
                        dialog.cancel();
                        handleFileSelection(new File(Main.getInstance().getFilesDir().getPath(),selectedUpdate.name).getPath());
                    }
                });
                /* */
                ListAdapter customAdapter = new UpdateContainerListAdapter(Main.getInstance(),R.layout.update_container_list_item, ( mAppUpdate ) ? mCurrentController.fetchApplicationUpdates() : mCurrentController.fetchBldrUpdates());
                updatesList.setAdapter(customAdapter);
                dialog.show();
                break;
            case R.id.btn_start_update:
                mUpdateRetry = false;
                mSpinner.setVisibility(View.VISIBLE);
                mUIBlockedTV.setVisibility(View.VISIBLE);
                toggleLockUI(true);
                if(mNURFWUpdate){
                    /* NUR FW update selected */
                    if (mApi.isConnected()) {
                        setStatus(R.color.StatusOrange,R.string.update_starting);
                        mCurrentController.startUpdate();
                    } else
                        Toast.makeText(mOwner.getActivity(), R.string.device_not_ready, Toast.LENGTH_SHORT).show();
                } else {
                    /* DFU FW update selected */
                    /* Disconnect device
                     *  Save Application mode address to restore autoConnect
                     *  convert address to DFU mode and start
                     */
                    mApplicationModeAddress = Main.getInstance().getNurAutoConnect().getAddress();
                    if (mApplicationModeAddress == null || mApplicationModeAddress.isEmpty()) {
                        Toast.makeText(mOwner.getActivity(), R.string.no_bluetooth_device, Toast.LENGTH_SHORT).show();
                        break;
                    }
                    if(!handleDFUUpdateStart()) {
                        Toast.makeText(Main.getInstance(),"Failed to restart device update cancelled",Toast.LENGTH_SHORT).show();
                        handleUpdateFinished();
                    }
                }
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE_OPEN && resultCode == Activity.RESULT_OK) {
            super.onActivityResult(requestCode, resultCode, data);
            if (data != null) {
                final String fullPath = getFileName(data.getDataString());
                if (fullPath == null) {
                    mFileName.setText(R.string.not_available);
                    Toast.makeText(mOwner.getActivity(), R.string.file_check_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                handleFileSelection(fullPath);
            }
        }
    }

    private void handleFileSelection(String fullPath){
        String path = fullPath;
        if(mNURFWUpdate)
            if(!mNURAPPController.inspectSelectedFwFile(fullPath, mModuleType ,mAppUpdate ? NurApi.NUR_BINTYPE_L2APP : NurApi.NUR_BINTYPE_L2LOADER)){
                path =null;
                Toast.makeText(mOwner.getActivity(), getString(R.string.update_invalid_file), Toast.LENGTH_SHORT).show();
                //return;
            }
        mCurrentController.setFilePath(path);
        setTextFileName();
        enableAll();
    }

    private String getFileName(String strUri) {
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

    private void setTextFileName(){
        mOwner.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if(mNURFWUpdate) {
                    if(mCurrentController.isFileSet()){
                        String version = mNURAPPController.getFileVersion();
                        if (version != null)
                            mFileName.setText("L2 " + ((mNURAPPController.isApplication()) ? "application" : "bootloader") + (", version: " + version));
                        else
                            mFileName.setText(R.string.not_available);
                    } else {
                        mFileName.setText(R.string.not_available);
                    }
                } else {
                    String path = mDFUController.getFilePath();
                    if(path != null && !path.isEmpty()){
                        mFileName.setText(path.substring(path.lastIndexOf('/') + 1));
                    } else {
                        mFileName.setText(R.string.not_available);
                    }
                }
            }
        });
    }

    private void setProgressText(final String text){
        mOwner.getActivity().runOnUiThread(new Runnable() {
            public void run() {
                mUpdateProgress.setText(text);
            }
        });
    }

    private void toggleLockUI(boolean lock){
        if(lock)
            Main.getInstance().getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        else
            Main.getInstance().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void keepScreenOn(boolean value) {
        mView.setKeepScreenOn(value);
    }

    private void disableAll() {
        enableButtons(false);
    }

    private void enableAll() { enableButtons(true); }

    public void enableButtons(boolean enable) {
        if(mUpdateRunning){
            mFilePicker.setEnabled(false);
            mStartUpdate.setEnabled(false);
            mRemoteFilePicker.setEnabled(false);
            return;
        }
        if (mApi.isConnected()) {
            mRemoteFilePicker.setEnabled(enable);
            mFilePicker.setEnabled(enable);
            mStartUpdate.setEnabled(enable && ( mNURAPPController.isFileSet() || mDFUController.isFileSet()));
        } else {
            mRemoteFilePicker.setEnabled(false);
            mFilePicker.setEnabled(false);
            mStartUpdate.setEnabled(false);
        }
    }

    private void setTextDeviceConnection(){
        if(mUpdateRunning){
            setStatus(R.color.StatusGreen,R.string.update_started);
        } else {
            setStatus(R.color.StatusRed,R.string.update_state_idle);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        handleDFUSupported();
        mNURAPPController.registerListener(mNURFWListener);
        mDFUController.registerListener();
        enableAll();
        setTextFileName();
        setTextDeviceConnection();
        mOwner.getAppTemplate().switchNurApiListener(mNURApiListener);
        if(mUpdateRunning){
            mSpinner.setVisibility(View.VISIBLE);
            mUIBlockedTV.setVisibility(View.VISIBLE);
            toggleLockUI(true);
        } else {
            mSpinner.setVisibility(View.GONE);
            mUIBlockedTV.setVisibility(View.GONE);
            toggleLockUI(false);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        mNURAPPController.unregisterListener();
        mDFUController.unregisterListener();
        mOwner.getAppTemplate().restoreListener();
    }
}