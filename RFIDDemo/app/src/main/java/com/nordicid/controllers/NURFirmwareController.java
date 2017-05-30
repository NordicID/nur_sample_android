package com.nordicid.controllers;

import android.graphics.Interpolator;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.nordicid.helpers.UpdateContainer;
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

public class NURFirmwareController extends UpdateController {

    private static final String TAG = "NUR_FW_CONTROLLER";
    private NurApi mApi = null;
    private boolean mStartProgramOnBoot = false;
    private boolean mUpdateComplete = false;
    private boolean mUpdateCompleteWasOK = true;
    private boolean mIsApplication = false;
    private String mStrCompletionError = "";
    private NurBinFileType mFwFileType = null;
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

    public NURFirmwareController(NurApi api, String appSource, String bldrSource) {
        mAppUpdateSource = appSource;
        mBldrUpdateSource = bldrSource;
        mApi = api;
        mNurListener = new NurApiListener() {
            @Override
            public void programmingProgressEvent(NurEventProgrammingProgress eventProgramming) {
                int error;
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

    public boolean inspectSelectedFwFile(String fileName, String desiredModuleType, int fileType) {
        mFwFileType = null;
        try {
            Log.e("MODULE_TYPE",desiredModuleType);
            mFwFileType = mApi.checkNurFwBinaryFile(fileName,desiredModuleType);
            mIsApplication = (mFwFileType.fwType == NurApi.NUR_BINTYPE_L2APP);
            return mFwFileType.fwType == fileType;
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "File check error: " + ex.getMessage());
        }
        return false;
    }

    private void programThread() {
        mUpdateCompleteWasOK = true;
        try {
            if (mIsApplication)
                mApi.programApplicationFile(mFilePath);
            else
                mApi.programBootloaderFile(mFilePath);
        } catch (Exception ex) {
            ex.printStackTrace();
            if (mFirmwareControllerListener != null)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mFirmwareControllerListener.onUpdateInterrupted("Failed to program the module");
                    }
                });

        }
        try {
            Log.e(TAG, "programThread(): BOOT");
            mUpdateComplete = true;
            mApi.moduleBoot(false);
            Log.e(TAG, "programThread(): update complete");
        } catch (Exception ex) {
            ex.printStackTrace();
            mUpdateComplete = false;
            if (mFirmwareControllerListener != null)
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mFirmwareControllerListener.onUpdateInterrupted("Unexpected error while booting module");
                    }
                });
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
        if (mApi.isConnected() && mFilePath != null) {
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

        Log.e(TAG, "handleBootEvent(): mStartProgramOnBoot = " + mStartProgramOnBoot + "; mUpdateComplete = " + mUpdateComplete);

        if (mStartProgramOnBoot) {
            mStartProgramOnBoot = false;
            beginUpload();
        }
        else if (mUpdateComplete) {
            if (mFirmwareControllerListener != null)
                mFirmwareControllerListener.onUpdateComplete(mUpdateCompleteWasOK);
        }
    }

    public boolean startUpdate(){
        prepareUpload();
        return true;
    }

    public void abortUpdate(){}

    public void pauseUpdate(){}

    public void resumeUpdate(){}

    /**
     * Compares remote and current versions
     * @param currentVersion
     * @param remoteVersion
     * @return true if remote is newer false if not
     */
    public boolean checkVersion(String currentVersion, String remoteVersion){
        /** Major.Minor-Build **/
        String[] currentSplits = currentVersion.split("-|\\.");
        String[] remoteSplits = remoteVersion.split("-|\\.");
        try{
            if(!currentSplits[0].equals(remoteSplits[0])){
                return ((Integer.parseInt(currentSplits[0]) - Integer.parseInt(remoteSplits[0])) < 0);
            }
            if(!currentSplits[1].equals(remoteSplits[1])){
                return ((Integer.parseInt(currentSplits[1]) - Integer.parseInt(remoteSplits[1])) < 0);
            }
            if(currentSplits[2].charAt(0) != (remoteSplits[2].charAt(0))){
                return (currentSplits[2].charAt(0) - remoteSplits[2].charAt(0) < 0);
            }
        } catch (Exception e){
            // ok
        }
        return  false;
    }
}