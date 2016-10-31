package com.nordicid.rfiddemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.apptemplate.SubApp;
import com.nordicid.controllers.NURFirmwareController;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurEventProgrammingProgress;


public class NurFwUpdateApp extends SubApp implements View.OnClickListener {

    private static final int REQ_FILE_OPEN = 3624;
    private NURFirmwareController mFWController = null;
    private Button mSelectFileBtn = null;
    private Button mUpdateFWBtn = null;
    private TextView mFileNameTV = null;
    private TextView mProgressTV = null;
    private ProgressBar mProgressBar = null;
    private ProgressBar mSpinner = null;
    private Main mainRef = null;
    private View mView;
    private boolean mUpdateRunning = false;
    private NURFirmwareController.FirmWareControllerListener mNurFWListener = null;

    @Override
    public NurApiListener getNurApiListener() {
        return mFWController.getNurApiListener();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view,savedInstanceState);
        mView = view;
        mSelectFileBtn = (Button) mView.findViewById(R.id.btn_file_picker);
        mUpdateFWBtn = (Button) mView.findViewById(R.id.btn_upload_fw);
        mSelectFileBtn.setOnClickListener(this);
        mUpdateFWBtn.setOnClickListener(this);
        mFileNameTV = (TextView) mView.findViewById(R.id.txt_file_name);
        mProgressTV = (TextView) mView.findViewById(R.id.text_view_progress_value);
        mSpinner = (ProgressBar) mView.findViewById(R.id.progress_spinner);
        mSpinner.setVisibility(View.GONE);
        mProgressBar = (ProgressBar) mView.findViewById(R.id.fw_upload_progressbar);
        mProgressBar.setProgress(0);
    }

    @Override
    public int getTileIcon() {
        return R.drawable.ic_settings;
    }

    @Override
    public String getAppName() {
        return "NUR Firmware Update";
    }

    @Override
    public int getLayout() {
        return R.layout.nur_firmware_update_app;
    }

    public NurFwUpdateApp() {
        super();
        mainRef = Main.getInstance();
        mFWController = new NURFirmwareController(getNurApi());
        mNurFWListener = new NURFirmwareController.FirmWareControllerListener() {
            @SuppressWarnings("unchecked")
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
                        mProgressBar.setProgress(0);
                        return;
                    }
                    if (completedPage == totalPages) {
                        setProgressText(getResources().getString(R.string.ready_wait_boot));
                        mProgressBar.setProgress(mProgressBar.getMax());
                    } else {
                        setProgressText(progress + "%");
                        mProgressBar.setProgress(progress);
                    }
                }
            }

            @Override
            public void onUpdateComplete(boolean status) {
                if(!mUpdateRunning)
                    return;
                if (status)
                    setProgressText(getResources().getString(R.string.update_complete_ok));
                else
                    setProgressText(mFWController.getCompletionError());
                handleUpdateFinished();
            }

            @Override
            public void onUpdateStarted(){
                mainRef.setProgrammingFlag(true);
                getAppTemplate().setEnableBattUpdate(false);
                keepScreenOn(true);
                mUpdateRunning = true;
                disableAll();
                mSpinner.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDeviceConnected(){
                enableAll();
            }

            @Override
            public void onDeviceDisconnected(){
                enableAll();
            }

            @Override
            public void onUpdateInterrupted(String error){
                setProgressText(getResources().getString(R.string.update_interrupted));
                handleUpdateFinished();
            }
        };
    }

    private void handleUpdateFinished(){
        mainRef.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                keepScreenOn(false);
                mainRef.setProgrammingFlag(false);
                getAppTemplate().setEnableBattUpdate(true);
                mUpdateRunning = false;
                mSpinner.setVisibility(View.GONE);
                mProgressBar.setProgress(0);
                enableAll();
            }
        });
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
                if (mainRef.getNurApi().isConnected()) {
                    setProgressText(getResources().getString(R.string.initiating_update));
                    mFWController.prepareUpload();
                } else
                    Toast.makeText(mainRef, R.string.device_not_ready, Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE_OPEN) {
            if (data != null) {
                String fullPath;
                fullPath = getActualFileName(data.getDataString());
                if (fullPath == null || !mFWController.inspectSelectedFwFile(fullPath)) {
                    mFileNameTV.setText(R.string.not_available);
                    Toast.makeText(mainRef, R.string.file_check_failed, Toast.LENGTH_SHORT).show();
                } else {
                    mFWController.setFilePath(fullPath);
                    setTextFileName();
                    enableAll();
                }
            }
        }
    }

    private void setFileNameView(){
        String version = mFWController.getFileVersion();
        if(version != null)
            mFileNameTV.setText("Selected file:\nL2 " + ((mFWController.isApplication()) ? "application" : "bootloader") + (", version: " + version));
        else
            mFileNameTV.setText(R.string.not_available);
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

    private void setTextFileName(){
        mainRef.runOnUiThread(new Runnable() {
            public void run() {
                if(mFWController.isFileSet()){
                    setFileNameView();
                } else {
                    mFileNameTV.setText(R.string.not_available);
                }
            }
        });
    }

    private void setProgressText(final String text){
        // TODO any other way ?
        Main.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                mProgressTV.setText(text);
            }
        });
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

    @Override
    public void onResume(){
        super.onResume();
        mFWController.registerListener(mNurFWListener);
        enableAll();
        setTextFileName();
    }

    @Override
    public void onPause(){
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
