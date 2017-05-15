package com.nordicid.controllers;

import android.util.Log;

import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiErrors;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurBinFileType;
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

public class NURFirmwareController {

    private static final String TAG = "NUR_FW_CONTROLLER";
    private NurApi mApi = null;
    private boolean mStartProgramOnBoot = false;
    private boolean mUpdateComplete = false;
    private boolean mUpdateCompleteWasOK = true;
    private boolean mIsApplication = false;
    private String mStrCompletionError = "";
    private NurBinFileType mFwFileType = null;
    private String mFullFilePath = null;
    private NurApiListener mNurListener = null;
    private FirmWareControllerListener mFirmwareControllerListener = null;

    public interface FirmWareControllerListener {
        void onProgrammingEvent(NurEventProgrammingProgress nurEventProgrammingProgress);
        void onUpdateComplete(boolean status);
        void onUpdateStarted();
        void onDeviceConnected();
        void onDeviceDisconnected();
        void onUpdateInterrupted(String error);
    }

    public boolean isFileSet() {
        return mFullFilePath != null;
    }

    public NURFirmwareController(NurApi api) {
        mApi = api;
        mNurListener = new NurApiListener() {
            @Override
            public void programmingProgressEvent(NurEventProgrammingProgress eventProgramming) {
                int error;
                Log.d("PROGRESSEVENTCONTROLLER", "got progress event");
                if (mFirmwareControllerListener != null)
                    mFirmwareControllerListener.onProgrammingEvent(eventProgramming);
                error = eventProgramming.error;
                if (error != 0) {
                    mUpdateCompleteWasOK = false;
                    mStrCompletionError = NurApiErrors.getErrorMessage(error);
                    Log.e(TAG, "Upload completion error: " + error);
                }
            }

            @Override
            public void bootEvent(String s) {
                handleBootEvent();
            }

            @Override
            public void connectedEvent() {
                if (mFirmwareControllerListener != null)
                    mFirmwareControllerListener.onDeviceConnected();
                if (mStartProgramOnBoot || mUpdateComplete)
                    handleBootEvent();
            }

            @Override
            public void disconnectedEvent() {
                if (mFirmwareControllerListener != null)
                    mFirmwareControllerListener.onDeviceDisconnected();
                if (mStartProgramOnBoot)
                    return;
            }

            @Override
            public void logEvent(int i, String arg0) {}
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
            public void debugMessageEvent(String arg0) {}
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
            public void tagTrackingChangeEvent(NurEventTagTrackingChange arg0) {}
        };
    }

    public NurApiListener getNurApiListener() {
        return mNurListener;
    }

    public void registerListener(FirmWareControllerListener l) {
        mFirmwareControllerListener = l;
    }

    public void unregisterListener() { mFirmwareControllerListener = null;  }

    public void setFilePath(String fullFilePath) {
        mFullFilePath = fullFilePath;
    }

    public String getFilePath() {
        return mFullFilePath;
    }

    public boolean isApplication() {
        return mIsApplication;
    }

    public String getFileVersion() {
        if(mFwFileType != null)
            return mFwFileType.getVersion();
        return null;
    }

    public String getCompletionError() {
        return mStrCompletionError;
    }

    private boolean isAppMode() throws Exception {
        return mApi.getMode().equalsIgnoreCase("A");
    }

    public boolean inspectSelectedFwFile(String fileName, String desiredModuleType) {
        boolean checkOK = false;
        mFwFileType = null;
        try {
            Log.e("MODULE",desiredModuleType);
            mFwFileType = mApi.checkNurFwBinaryFile(fileName,desiredModuleType);
            mIsApplication = (mFwFileType.fwType == NurApi.NUR_BINTYPE_L2APP);
            checkOK = true;
        } catch (Exception ex) {
            Log.e(TAG, "File check error: " + ex.getMessage());
        }
        return checkOK;
    }

    private void programThread() {
        mUpdateCompleteWasOK = true;
        try {
            if (mIsApplication)
                mApi.programApplicationFile(mFullFilePath);
            else
                mApi.programBootloaderFile(mFullFilePath);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (mFirmwareControllerListener != null)
                mFirmwareControllerListener.onUpdateInterrupted(ex.getMessage());
        }
        try {
            Log.e(TAG, "programThread(): BOOT");
            mApi.moduleBoot(false);
            mUpdateComplete = true;
        } catch (Exception ex) {
            ex.printStackTrace();
            if (mFirmwareControllerListener != null)
                mFirmwareControllerListener.onUpdateInterrupted(ex.getMessage());
        }
    }

    private void beginUpload() {
        (new Thread(new Runnable() {
            @Override
            public void run() {
                programThread();
            }
        })).start();
    }

    public void prepareUpload() {
        if (mApi.isConnected() && mFullFilePath != null) {
            mStartProgramOnBoot = true;
            mUpdateComplete = false;
            try {
                if (mFirmwareControllerListener != null)
                    mFirmwareControllerListener.onUpdateStarted();
                if (isAppMode())
                    mApi.moduleBoot(false);
                else
                    handleBootEvent();
            } catch (Exception ex) {
                mStartProgramOnBoot = false;
                ex.printStackTrace();
                if (mFirmwareControllerListener != null)
                    mFirmwareControllerListener.onUpdateInterrupted(ex.getMessage());
            }
        }
    }

    private void handleBootEvent() {
        if (mStartProgramOnBoot) {
            mStartProgramOnBoot = false;
            beginUpload();
        }
        if (mUpdateComplete) {
            if (mFirmwareControllerListener != null)
                mFirmwareControllerListener.onUpdateComplete(mUpdateCompleteWasOK);
        }
    }
}