package example.nordicid.com.nursampleandroid;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

/**
 * Barcode activity is used by devices equipped with accessories and Imager
 */
public class Barcode extends Activity {

    public static final String TAG = "NUR_SAMPLE";

    private static NurApi mNurApi;
    private static NurAccessoryExtension mAccessoryApi;

    //UI
    private TextView mResultTextView;
    private TextView mStatusTextView;

    private String mUiResultText;
    private String mUiStatusText;

    //Long or Short length of toast text to be shown in UI once
    private String mToastLong;
    private String mToastShort;

    //Need to keep track when barcode mScanning or mAiming ongoing in case it cancelled by trigger press or program leave
    private boolean mScanning;
    private boolean mAiming;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode);

        //Get NurApi and Accessory handles from MainActivity
        mNurApi = MainActivity.GetNurApi();
        mAccessoryApi = MainActivity.GetNurAccessory();

        //Set event listener for this activity
        mNurApi.setListener(mNurApiEventListener);

        mAccessoryApi.registerBarcodeResultListener(mResultListener);

        mResultTextView = (TextView)findViewById(R.id.text_result);
        mStatusTextView = (TextView)findViewById(R.id.text_status);

        mUiResultText = "result";
        mUiStatusText = "Waiting trigger...";
        mToastLong="Welcome to Barcode scan sample";
        mToastShort="";
        showOnUI();

        mScanning = false;
        mAiming = false;
    }

    /**
     * Update content of some global variables to UI items
     */
    private void showOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResultTextView.setText(mUiResultText);
                mStatusTextView.setText(mUiStatusText);

                //Show Toast if any
                if(mToastLong.length()>0)
                {
                    Toast.makeText(getApplicationContext() ,mToastLong ,Toast.LENGTH_LONG).show();
                    mToastLong="";
                }

                if(mToastShort.length()>0)
                {
                    Toast.makeText(getApplicationContext(), mToastShort,Toast.LENGTH_SHORT).show();
                    mToastShort="";
                }
            }
        });
    }

    /**
     * Barcode result handling
     */
    private AccessoryBarcodeResultListener mResultListener = new AccessoryBarcodeResultListener()
    {
        @Override
        public void onBarcodeResult(AccessoryBarcodeResult result) {

            Log.i(TAG, "BarcodeResult " + result.strBarcode);

            if (result.status == NurApiErrors.NO_TAG) {
                mUiStatusText="No barcode found";
                Beeper.beep(Beeper.FAIL);
                mScanning=false;
            }
            else if (result.status == NurApiErrors.NOT_READY) {
                mUiStatusText = "Cancelled";
            }
            else if (result.status == NurApiErrors.HW_MISMATCH) {
                //This reader does'nt support imager.
                mUiStatusText = "No hardware found";
                Beeper.beep(Beeper.FAIL);
                mScanning=false;
            }
            else if (result.status != NurApiErrors.NUR_SUCCESS) {
                mUiStatusText = "Error: " + result.status;
                Beeper.beep(Beeper.FAIL);
                mScanning=false;
            }
            else {
                //Barcode scan success. Show result on the screen.
                mUiResultText = result.strBarcode;
                mUiStatusText = "Waiting trigger...";
                Beeper.beep(Beeper.BEEP_100MS); //Beep on phone

                try {
                    mAccessoryApi.beepAsync(100); //Beep on device
                    if (mAccessoryApi.getConfig().hasVibrator()) {
                        //Device has vibra so vibrate.
                        mAccessoryApi.vibrate(200);
                    }
                }
                catch (Exception ex)
                {
                    mToastLong = "Error! " + ex.getMessage();
                }

                mScanning=false;
            }

            showOnUI();
        }
    };

    /**
     *  Handle I/O events from reader.
     *  When user press button on reader, event fired and handled in this function.
     *  NurEventIOChange offers 'source' and 'direction' to determine which button changes state.
     *  source 100 = Trigger button
     *  source 101 = Power button
     *  source 102 = Unpair button
     *  direction 0 = Button released
     *  direction 1 = Button pressed down
     */
    private void HandleIOEvent(NurEventIOChange event)
    {
        Log.i(TAG, "BarcodeKey " + event.source + " Dir=" + event.direction);

        try {
            if (event.source == 100 && event.direction == 1) {
                if(mScanning) {
                    //There is mScanning ongoing so we need just abort it
                    mAccessoryApi.cancelBarcodeAsync();
                    Log.i(TAG, "Cancelling..");
                }
                else {
                    mAiming = true;
                    mAccessoryApi.imagerAIM(mAiming);
                    mUiStatusText = "Aiming...";
                }
            } else if (event.source == 100 && event.direction == 0) {
                if(mScanning)
                {
                    mScanning=false;
                    return;
                }
                //Trigger released. Stop aiming and start mScanning
                mAiming = false;
                mAccessoryApi.imagerAIM(mAiming);
                mAccessoryApi.readBarcodeAsync(5000); //5 sec timeout
                mUiStatusText = "Scanning barcode...";
                mScanning = true;
            }
            else if(event.source == 101)
            {
                if(event.direction == 0)
                    mToastShort="Power button released";
                else mToastShort="Power button pressed";
            }
            else if(event.source == 102)
            {
                if(event.direction == 0)
                    mToastShort="Unpair button released";
                else mToastShort="Unpair button pressed";
            }
        }
        catch (Exception ex)
        {
            //Show error on status field
            mUiStatusText = ex.getMessage();
        }

        showOnUI();
    }

    /**
     * Barcode activity stopping.. Shutdown scanning or aiming operations if any.
     */
    @Override
    protected void onStop() {
        super.onStop();

        try {
            if (mScanning) {
                //There is mScanning ongoing so we need just abort it
                mAccessoryApi.cancelBarcodeAsync();
            }

            if (mAiming) {
                mAiming = false;
                mAccessoryApi.imagerAIM(mAiming);
            }
        }
        catch (Exception ex) {
            Log.i(TAG, "Exception barcode onStop:" + ex.getMessage());
        }
    }

    /**
     * NurApi event handlers.
     * NOTE: All NurApi events are called from NurApi thread, thus direct UI updates are not allowed.
     * If you need to access UI controls, you can use runOnUiThread(Runnable) or Handler.
     */
    private NurApiListener mNurApiEventListener = new NurApiListener()
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
            finish(); //Device disconnected. Exit from this activity
        }
        @Override
        public void deviceSearchEvent(NurEventDeviceInfo event) { }
        @Override
        public void debugMessageEvent(String event) { }
        @Override
        public void connectedEvent() { }
        @Override
        public void clientDisconnectedEvent(NurEventClientInfo event) { }
        @Override
        public void clientConnectedEvent(NurEventClientInfo event) { }
        @Override
        public void bootEvent(String event) {}
        @Override
        public void IOChangeEvent(NurEventIOChange event) {
             HandleIOEvent(event);
        }
        @Override
        public void autotuneEvent(NurEventAutotune event) { }
        @Override
        public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
        //@Override
        public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
    };
}
