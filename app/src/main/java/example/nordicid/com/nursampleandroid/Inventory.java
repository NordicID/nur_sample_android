package example.nordicid.com.nursampleandroid;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.nordicid.nurapi.*;
import com.nordicid.tdt.*;

import java.util.Arrays;

import static com.nordicid.nurapi.NurApi.BANK_EPC;
import static com.nordicid.nurapi.NurApi.BANK_TID;
import static com.nordicid.nurapi.NurApi.BANK_USER;
import static com.nordicid.nurapi.NurApi.MAX_EPC_LENGTH;

public class Inventory extends Activity {

    public static final String TAG = "NUR_SAMPLE";

    //Handles of these will be fetch from MainActivity
    private NurApi mNurApi;
    private static AccessoryExtension mAccExt;

    //UI
    private TextView mResultTextView;
    private TextView mStatusTextView;
    private TextView mEPCTextView;
    private ToggleButton mInvStreamButton;

    //These values will be shown in the UI
    private String mUiStatusMsg;
    private String mUiResultMsg;
    private String mUiEpcMsg;
    private int mUiStatusColor;
    private int mUiResultColor;
    private int mUiEpcColor;

    //Need to keep track when state change on trigger button
    private boolean mTriggerDown;

    //This demo (Inventory stream) just counts different tags found
    private int mTagsAddedCounter;

    //====== Global variables for ScanSingleTag thread operation ======
    //This counter add by one when single tag found after inventory. Reset to zero if multiple tags found.
    int mSingleTagFoundCount;

    //This is true while searching single tag operation ongoing.
    boolean mSingleTagDoTask;

    //This counts scan rounds and when reaching 15 it's time to stop.
    int mSingleTagRoundCount;

    //Temporary storing current TX level because single tag will be search using low TX level
    int mSingleTempTxLevel;

    //This variable hold last tag epc for making sure same tag found 3 times in row.
    static String mTagUnderReview;

    //===================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        //Get NurApi and Accessory handles from MainActivity
        mNurApi = MainActivity.GetNurApi();
        mAccExt = MainActivity.GetAccessoryExtensionApi();

        //Set event listener for this activity
        mNurApi.setListener(mNurApiEventListener);

        mResultTextView = (TextView)findViewById(R.id.text_result);
        mStatusTextView = (TextView)findViewById(R.id.text_status);
        mEPCTextView = (TextView)findViewById(R.id.text_epc);

        Button mScanSingleButton = (Button) findViewById(R.id.buttonScanSingleTag);
        mInvStreamButton = (ToggleButton)findViewById(R.id.toggleButtonInvStream);

        //UI of Info text. Need to change based on reader type connected.
        TextView mInvStreamHdr = (TextView)findViewById(R.id.textViewInvStrHdr);
        TextView mInvStreamTxt1 = (TextView)findViewById(R.id.textViewInvStr1);
        TextView mInvStreamTxt2 = (TextView)findViewById(R.id.textViewInvStr2);

        TextView mSingleTagHdr = (TextView)findViewById(R.id.textViewSingleHdr);
        TextView mSingleTagTxt1 = (TextView)findViewById(R.id.textViewSingle1);
        TextView mSingleTagTxt2 = (TextView)findViewById(R.id.textViewSingle2);
        TextView mSingleTagTxt3 = (TextView)findViewById(R.id.textViewSingle3);
        TextView mSingleTagTxt4 = (TextView)findViewById(R.id.textViewSingle4);

        mTriggerDown = false;
        mSingleTagDoTask = false;
        mTagsAddedCounter = 0;
        mTagUnderReview = "";

        if(MainActivity.IsAccessorySupported())
        {
            //Accessories are available so we can start/stop inventory from reader button
            mInvStreamHdr.setText("InventoryStream (Trigger button)");
            mInvStreamTxt1.setText("1. Press and keep trigger button down. (Inventory started)");
            mInvStreamTxt2.setText("2. Release trigger to stop");

            //Single tag info changes
            mSingleTagHdr.setText("Single tag (Unpair button)");
            mSingleTagTxt2.setText("Press Unpair button for starting single tag read.");

            //No need buttons on UI
            mInvStreamButton.setVisibility(View.GONE);
            mScanSingleButton.setVisibility((View.GONE));
        }
        else
        {
            //No accessory. This is probably fixed reader. Inventory must start from button on UI
            mInvStreamHdr.setText("InventoryStream");
            mInvStreamTxt1.setText("Turn inventory stream on/off from button below.");
            mInvStreamTxt2.setText("");

            //Single tag info changes
            mSingleTagHdr.setText("Single tag");
            mSingleTagTxt2.setText("Press button below for starting single tag read.");

            mInvStreamButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        StartInventoryStream();
                    } else {
                        StopInventoryStream();
                    }
                    showOnUI();
                }
            });

            mScanSingleButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    ScanSingleTagThread();
                }
            });

        }
    }

    /**
     * Show UI items
     */
    private void showOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mResultTextView.setText(mUiResultMsg);
                mResultTextView.setTextColor(mUiResultColor);
                mStatusTextView.setText(mUiStatusMsg);
                mStatusTextView.setTextColor(mUiStatusColor);
                mEPCTextView.setText(mUiEpcMsg);
                mEPCTextView.setTextColor(mUiEpcColor);
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Inventory onStart ");
        mUiEpcMsg="EPC";
        mUiResultMsg = "Result";
        mUiStatusMsg = "Waiting button press...";
        mUiStatusColor = Color.BLACK;
        mUiResultColor = Color.BLACK;
        mUiEpcColor = Color.BLACK;
        showOnUI();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Inventory onStop ");
        //Make sure going out from ScanSingleTagThread
        mSingleTagDoTask=false;
    }


    public void writeEpcByTID(byte[] tidBuffer, int tidBufferLength, int newEpcBufferLength, byte[] newEpcBuffer) throws Exception
    {
        byte [] wrBuf = new byte[MAX_EPC_LENGTH + 2];
        int paddedEpcBufferLen;
        int pc;
        paddedEpcBufferLen = ((newEpcBufferLength * 8) + 15)/16*2;
        Arrays.fill(wrBuf,(byte) 0);

        //Set epc length in words
        pc = (paddedEpcBufferLen/2) << 11;
        wrBuf[0] = (byte)(pc>>8);
        wrBuf[1] = (byte)pc;

        //Add Epc
        System.arraycopy(newEpcBuffer, 0, wrBuf, 2, newEpcBufferLength );
        mNurApi.writeTag(BANK_TID, 0, tidBufferLength * 8, tidBuffer, BANK_EPC, 1, paddedEpcBufferLen + 2,wrBuf);
    }

    /**
     * ScanSingleTagThread function making inventory until single tag found from antenna field or time out.
     */
    private void ScanSingleTagThread()
    {
        Log.i(TAG, "ScanSingleTagThread");
        if(mSingleTagDoTask  || mTriggerDown) return; //Already running tasks so let's not disturb that operation.

        Thread sstThread = new Thread(new Runnable() {
            @Override
            public void run() {

                //Store current TX level of RFID reader
                try {
                    mSingleTempTxLevel = mNurApi.getSetupTxLevel();
                    //Log.i(TAG, "Current TxLevel = " + mSingleTempTxLevel);
                    //Set rather low TX power. You need to get close to tag for successful reading
                    mNurApi.setSetupTxLevel(NurApi.TXLEVEL_9); //This is attenuation level as dBm from max level 27dBm
                }
                catch (Exception ex)
                {
                    mUiStatusMsg = ex.getMessage();
                    mUiStatusColor = Color.RED;
                    showOnUI();
                    return;
                }


                mUiResultMsg = ".";
                mUiResultColor = Color.BLUE;
                mUiEpcMsg="";
                mUiEpcColor = Color.BLACK;

                mSingleTagDoTask = true;
                mSingleTagRoundCount = 0;
                mSingleTagFoundCount = 0;

                long time_start = System.currentTimeMillis();

                while (mSingleTagDoTask)
                {
                    mUiStatusMsg="Scan single tag (round:" + String.valueOf(mSingleTagRoundCount) + ")";
                    showOnUI();
                    try {
                        mNurApi.clearIdBuffer(); //Clear buffer from existing tags
                        //Do the inventory with small rounds and Q values. We looking for single tag..
                        //mNurApi.setIRConfig(NurApi.IRTYPE_EPCDATA, BANK_TID, 0, 6);
                        NurRespInventory resp = mNurApi.inventory(2, 4, 0); //Rounds=2, Q=4, Session=0

                        mSingleTagRoundCount++;
                        if (resp.numTagsFound > 1) {
                            mUiResultMsg = String.valueOf("Too many tags seen");
                            mUiResultColor=Color.RED;
                            mSingleTagFoundCount = 0;
                        } else if (resp.numTagsFound == 1) {
                            NurTag tag = mNurApi.fetchTagAt(true, 0); //Get tag information from pos 0

                            //We looking for same tag in antenna field seen 3 times in row. isSameTag function make sure it is.
                            if (isSameTag(tag.getEpcString())) mSingleTagFoundCount++;
                            else mSingleTagFoundCount = 1; //It was not. Start all over.

                            mSingleTagFoundCount=3;
                            if (mSingleTagFoundCount == 3) {
                                //Single tag found multiple times (3) in row so let's accept.
                                try {
                                    //Check if tag is GS1 coded. Exception fired if not and plain EPC shown.
                                    //This is TDT (TagDataTranslation) library feature.
                                    EPCTagEngine engine = new EPCTagEngine(tag.getEpcString());
                                    //Looks like it is GS1 coded, show pure Identity URI
                                    String gs = engine.buildPureIdentityURI();
                                    mUiResultMsg = "GS1 coded tag!";
                                    mUiEpcMsg = gs;
                                } catch (Exception ex) {
                                    //Not GS1 coded, show EPC only
                                    mUiResultMsg = "Single Tag found!";
                                    mUiEpcMsg = "EPC=" + tag.getEpcString() + " TID=" + NurApi.byteArrayToHexString(tag.getIrData());
                                }

                                /*
                                Reading TID BANK
                                EPC is known at this point, let's use that for reading first 32-bit TID bank using readTagByEpc()
                                Change the 'rdAddress' and'readByteCount' params for your purposes. Make sure values are in word boundaries (2,4,6,8...)
                                */

                                try {
                                    byte[] tidBank1 = mNurApi.readTagByEpc(tag.getEpc(), tag.getEpc().length, BANK_TID, 0, 4);
                                    mUiEpcMsg += "\nTID:" + NurApi.byteArrayToHexString(tidBank1);
                                    //SAMPLE WRITING EPC SINGULATED BY TID
                                    //final byte newEpc[] = new byte[] { (byte)0xaa, (byte)0xbb,(byte)0xcc,(byte)0xdd };
                                    //writeEpcByTID(tidBank1,tidBank1.length, newEpc.length, newEpc);
                                }
                                catch (NurApiException e)
                                {
                                    mUiEpcMsg += "\nTID:" + e.getMessage();
                                }

                                /*
                                Reading USER BANK
                                Tag may have USER memory or not.
                                This sample (trying) read first 32-bit
                                Change the 'rdAddress' and'readByteCount' params for your purposes. Make sure values are in word boundaries (2,4,6,8...)
                                 */
                                /*
                                try
                                {
                                    byte [] usrBank1 = mNurApi.readTagByEpc(tag.getEpc(),tag.getEpc().length,NurApi.BANK_USER,0, 4);
                                    mUiEpcMsg += "\nUSER:"+ NurApi.byteArrayToHexString(usrBank1);

                                    //SAMPLE WRITING USER SINGULATED BY EPC
                                    //final byte newUser[] = new byte[] { (byte)0x12, (byte)0x34,(byte)0x56,(byte)0x78, (byte)0x90,(byte)0x78 };
                                    //byte arr[] = NurApi.hexStringToByteArray("12345697");
                                    //mNurApi.writeTagByEpc(tag.getEpc(),tag.getEpc().length,BANK_USER,0,arr.length,arr);
                                    //mNurApi.writeTag(BANK_USER,0,arr.length,arr);


                                }
                                catch (NurApiException e)
                                {
                                    if(e.error == 4110)
                                        mUiEpcMsg += "\n(No USER memory)";
                                    else mUiEpcMsg += "\nUSER:" + e.getMessage(); //Another error. Show it.
                                }
                                */

                                //Set nice 'success' color to result text
                                mUiResultColor = Color.rgb(0, 128, 0);
                                //give good beep for user on device if available
                                if(MainActivity.IsAccessorySupported())
                                    mAccExt.beepAsync(500);
                                //..and on phone
                                Beeper.beep(Beeper.BEEP_300MS);
                                //We are done here
                                mSingleTagDoTask = false;
                                mUiStatusMsg = "Waiting button press...";
                            } else {
                                String dots = ".";
                                for (int x = 0; x < mSingleTagFoundCount; x++)
                                    dots += ".";

                                mUiResultMsg=dots;
                                mUiResultColor =  Color.BLUE;
                            }
                        }

                        //We try scan max 7000 millisec
                        if(System.currentTimeMillis() >= time_start+7000) {
                            //Give up.
                            mUiResultMsg = "No single tag found";
                            mUiResultColor = Color.RED;
                            mUiStatusMsg = "Waiting button press...";
                            //give some kind of "timeout beeps" for user
                            if(MainActivity.IsAccessorySupported())
                                mAccExt.beepAsync(300);
                            else
                                Beeper.beep(Beeper.BEEP_300MS);

                            mSingleTagDoTask = false;
                        }

                    } catch (Exception ex) {
                        mUiStatusMsg=ex.getMessage();
                        mUiStatusColor = Color.RED;
                    }
                }

                //Original TX level back
                try {
                    mNurApi.setSetupTxLevel(mSingleTempTxLevel);
                }
                catch (Exception ex)
                {
                    mUiStatusMsg = ex.getMessage();
                    mUiStatusColor = Color.RED;
                }

                showOnUI();
                Beeper.beep(Beeper.BEEP_100MS);
            }

        });

        sstThread.start();
    }

    /**
     * Start inventory streaming. After this you can receive InventoryStream events.
     * Inventory stream is active around 20 sec then stopped automatically. Event received about the state of streaming so you can start it immediately again.
     */
    private void StartInventoryStream()
    {
        if(mSingleTagDoTask  || mTriggerDown) {
            mInvStreamButton.setChecked(false);
            return; //Already running tasks so let's not disturb that operation.
        }

        try {
            mNurApi.clearIdBuffer(); //This command clears all tag data currently stored into the module’s memory as well as the API's internal storage.
            mNurApi.startInventoryStream(); //Kick inventory stream on. Now inventoryStreamEvent handler offers inventory results.
            mTriggerDown = true; //Flag to indicate inventory stream running
            mTagsAddedCounter = 0;
            mUiResultMsg = "Tags:" + String.valueOf(mTagsAddedCounter);
            mUiStatusMsg = "Inventory streaming...";
        }
        catch (Exception ex)
        {
            mUiResultMsg = ex.getMessage();
        }
    }

    /**
     * Stop streaming.
     */
    private void StopInventoryStream()
    {
        try {
            if (mNurApi.isInventoryStreamRunning())
                mNurApi.stopInventoryStream();
            mTriggerDown = false;
            mUiStatusMsg = "Waiting button press...";
            mUiStatusColor = Color.BLACK;
        }
        catch (Exception ex)
        {
            mUiResultMsg = ex.getMessage();
            mUiResultColor = Color.RED;
        }
    }

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
        try {
            Log.i(TAG, "IO src=" + Integer.toString(event.source) + " dir=" + Integer.toString(event.direction));

            if (event.source == 100) {
                //Trigger down.
                if(mSingleTagDoTask) return; //Only if Single scan not running
                if(event.direction == 1)
                    StartInventoryStream(); //Start Inventory streaming.
                else if (event.direction == 0)
                    StopInventoryStream(); //Trigger released. Stop streaming if running.
            }
            else if(event.source == 101)
            {
                //Power button pressed or released.
            }
            else if(event.source == 102)
            {
                //Unpair button pressed or released.
                //Single scan operation start when Unpair button push down but no need to keep down during operation.
                if(event.direction == 1) {
                    //Unpair button pressed. if SingleScan already running so nothing to do.
                    ScanSingleTagThread(); //Do the single scan. No need to keep unpair button down.
                }
            }
            else if(event.source == ACC_SENSOR_SOURCE.ToFSensor.getNumVal()) {
                //ToF sensor of EXA21 has triggered GPIO event
                // Direction goes 0->1 when sensors reads less than Range Lo filter (unit: mm)
                // Direction goes 1->0 when sensors reads more than Range Hi filter (unit: mm)
                if(event.direction == 1) {
                    //There are something front of EXA21 ToF sensor, let's start inventory
                    StartInventoryStream();
                }
                else {
                    //Nothing seen at front of ToF sensor. It's time to stop inventory.
                    StopInventoryStream();
                }
            }
        }
        catch (Exception ex)
        {
            mUiResultMsg = ex.getMessage();
        }

        showOnUI();
    }

    /**
     * Check is tag same as previous one
     * @param epc
     * @return
     */
    static boolean isSameTag(String epc)
    {
            if(epc.compareTo(mTagUnderReview) == 0)
                return true;
            else //set new
                mTagUnderReview = epc;

            return false;
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
        public void inventoryStreamEvent(NurEventInventory event) {

            try {
                if (event.stopped) {
                    //InventoryStreaming is not active for ever. It automatically stopped after ~20 sec but it can be started again immediately if needed.
                    //check if need to restart streaming
                    if (mTriggerDown)
                        mNurApi.startInventoryStream(); //Trigger button still down so start it again.

                } else {

                    if(event.tagsAdded>0) {
                        //At least one new tag found
                        if(MainActivity.IsAccessorySupported())
                            mAccExt.beepAsync(20); //Beep on device
                        else
                            Beeper.beep(Beeper.BEEP_40MS); //Cannot beep on device so we beep on phone

                        NurTagStorage tagStorage = mNurApi.getStorage(); //Storage contains all tags found

                        //Iterate just received tags based on event.tagsAdded
                        for(int x=mTagsAddedCounter;x<mTagsAddedCounter+event.tagsAdded;x++) {
                            //Real application should handle all tags iterated here.
                            //But this just show how to get tag from storage.
                            String epcString;
                            NurTag tag = tagStorage.get(x);
                            epcString = NurApi.byteArrayToHexString(tag.getEpc());
                            //showing just EPC of last tag
                            mUiEpcMsg = epcString;
                        }

                        //Finally show count of tags found
                        mTagsAddedCounter += event.tagsAdded;
                        mUiResultMsg = "Tags:" + String.valueOf(mTagsAddedCounter);
                        mUiResultColor = Color.rgb(0, 128, 0);
                        showOnUI(); //Show results on UI
                    }
                }
            }
            catch (Exception ex)
            {
                //mStatusTextView.setText(ex.getMessage());
               // mUiStatusColor = Color.RED;
                //showOnUI();
            }
        }
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
