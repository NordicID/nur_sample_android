package example.nordicid.com.nursampleandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

import java.util.ArrayList;
import java.util.HashMap;

public class WriteTag extends Activity {

    //Handles of these will be fetch from MainActivity
    private static NurApi mNurApi;
    private static NurAccessoryExtension mAccessoryApi;

    //In here found tags stored
    private NurTagStorage mTagStorage = new NurTagStorage();

    //ListView and adapter for showing tags found
    private ListView mTagsListView;
    private SimpleAdapter mTagsListViewAdapter;
    private ArrayList<HashMap<String, String>> mListViewAdapterData = new ArrayList<HashMap<String,String>>();

    //UI
    private Button mRefreshButton;
    private TextView mWritingStatus;
    private boolean newEpcOK;

    //Selected EPC (from list)
    private String mSelectedEPC;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        //Get NurApi and Accessory handles from MainActivity
        mNurApi = MainActivity.GetNurApi();
        mAccessoryApi = MainActivity.GetNurAccessory();

        //Set event listener for this activity
        mNurApi.setListener(mNurApiEventListener);

        mWritingStatus=(TextView)findViewById(R.id.textWriteStatus);
        mRefreshButton = (Button) findViewById(R.id.buttonRefreshTagList);
        mTagsListView = (ListView)findViewById(R.id.tags_listview);

        //sets the adapter for listview
        mTagsListViewAdapter = new SimpleAdapter(this, mListViewAdapterData, R.layout.taglist_row, new String[] {"epc","rssi"}, new int[] {R.id.tagText,R.id.rssiText});
        mTagsListView.setAdapter(mTagsListViewAdapter);
        mTagsListView.setCacheColorHint(0);

        //List item selected
        mTagsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                @SuppressWarnings("unchecked")
                final HashMap<String, String> mSelectedTag = (HashMap<String,String>) mTagsListView.getItemAtPosition(position);
                showWriteDialog(mSelectedTag);
            }
        });

        //Refresh list Button OnClick handler
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Refresh tag list
                try {
                    doSingleInventory(); //Make single round inventory.
                }
                catch (Exception ex)
                {
                    //Something fails..
                    Toast.makeText(WriteTag.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    /**
     * Perform Write operation.
     * Current EPC used for singulating correct tag.
     * When found, new EPC will be written.
     * return empty string if success. Otherwise error message.
     */
    private String writeTagByEpc(byte[] epcBuffer, int epcBufferLength, int newEpcBufferLength, byte[] newEpcBuffer) {

        String ret = "";
        int savedTxLevel = 0;
        int savedAntMask = 0;

        try {
            // Make sure antenna autoswitch is enabled
            if (mNurApi.getSetupSelectedAntenna() != NurApi.ANTENNAID_AUTOSELECT)
                mNurApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

            //Let's use more TX power for write operation. Just making sure operation succeed.
            //Save current Tx level and Antenna settings
            savedTxLevel = mNurApi.getSetupTxLevel();
            savedAntMask = mNurApi.getSetupAntennaMaskEx();

            // Attempt to use circular antenna
            AntennaMapping []map = mNurApi.getAntennaMapping();
            String ant="Circular";
            int circMask = 0;
            for (int n=0; n<map.length; n++)
            {
                if (map[n].name.startsWith(ant))
                    circMask |= (1 << map[n].antennaId);
            }

            if (circMask != 0)
                mNurApi.setSetupAntennaMaskEx(circMask); //Found from this dev so let's use Circular

            // Set full TX power
            mNurApi.setSetupTxLevel(0);

        } catch (Exception e) {
            Toast.makeText(WriteTag.this, e.getMessage(), Toast.LENGTH_LONG).show();
            ret = e.getMessage();
        }

        if (ret.isEmpty())
        {
            try {
                //Do write operation
                mNurApi.writeEpcByEpc(epcBuffer, epcBufferLength, newEpcBufferLength, newEpcBuffer);
            }
            catch (Exception e)
            {
                //Something fails..
                ret = e.getMessage();
            }
        }

        // Restore settings
        try {
            mNurApi.setSetupTxLevel(savedTxLevel);
            mNurApi.setSetupAntennaMaskEx(savedAntMask);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }


    /**
     * Builds and shows the dialog of writing new epc
     */
    private void showWriteDialog(HashMap<String,String> tag) {

        View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_write,null);
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setView(dialogLayout);
        mSelectedEPC = tag.get("epc");

        final TextView currentEpcTextView = (TextView) dialogLayout.findViewById(R.id.write_dialog_current);
        currentEpcTextView.setText("Write new EPC");

        final EditText newEpcEditText = (EditText) dialogLayout.findViewById(R.id.write_dialog_new);
        newEpcEditText.setText(mSelectedEPC);

        newEpcOK = true;

        newEpcEditText.addTextChangedListener(new TextWatcher() {

            @Override public void afterTextChanged(Editable s) {
                String tmp = newEpcEditText.getText().toString().replaceAll("[^a-fA-F_0-9]", "");

                if (!tmp.equals(newEpcEditText.getText().toString())) {
                    newEpcEditText.setText(tmp);
                    newEpcEditText.setSelection(newEpcEditText.getText().length());
                }

                //Length of EPC must be correct. This shows different field color if length is not correct
                if (newEpcEditText.getText().toString().length() == 0 || (newEpcEditText.getText().toString().length() % 4) != 0) {
                    newEpcEditText.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                    newEpcOK = false;
                }
                else {
                    //All ok
                    newEpcEditText.setBackgroundColor(getResources().getColor(android.R.color.white));
                    newEpcOK = true;
                }
            }

            @Override public void beforeTextChanged(CharSequence s, int start,int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start,int before, int count) {}
        });

        final AlertDialog dialog = builder.create();

        final Button cancelButton = (Button) dialogLayout.findViewById(R.id.dialog_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }

        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) { }
        });

        final Button writeButton = (Button) dialogLayout.findViewById(R.id.dialog_write);
        writeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (newEpcOK) {
                    try {
                        //We have now current and new EPC and their lengths. Let's Write new EPC..
                        new WriteOperation().execute(newEpcEditText.getText().toString(),mSelectedEPC);
                        dialog.dismiss();
                    }
                    catch (Exception e) {
                        Toast.makeText(WriteTag.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

        });

        dialog.show();
    }

    /**
     * WriteOperation performing in background using AsyncTask. It's also easy to publish results on the UI thread.
     */
    private class WriteOperation extends AsyncTask<String, String,String> {

        @Override
        protected String doInBackground(String... params) {

            String writeResult=""; //If write success this remain empty string. otherwise error message
            try {
                byte[] newEpc = NurApi.hexStringToByteArray(params[0]);
                byte[] currentEpcBytes = NurApi.hexStringToByteArray(params[1]);
                //We have
                writeResult = writeTagByEpc(currentEpcBytes, currentEpcBytes.length, newEpc.length, newEpc);
                if (writeResult.isEmpty()) {
                    Beeper.beep(Beeper.BEEP_300MS);
                } else {
                    Beeper.beep(Beeper.FAIL);
                }

            }
            catch (Exception e)
            {
                writeResult = e.getMessage();
                Beeper.beep(Beeper.FAIL);
            }

            return writeResult; //result to onPostExecute
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.isEmpty()) {
                mWritingStatus.setTextColor(Color.rgb(0,160,0));
                mWritingStatus.setText("Write success");
                try { //Refresh list
                    doSingleInventory();
                } catch (Exception e) {
                }
            }
            else
            {
                mWritingStatus.setTextColor(Color.rgb(160,0,0));
                mWritingStatus.setText(result); //Show error
            }
        }

        @Override
        protected void onPreExecute() {
            mWritingStatus.setTextColor(Color.rgb(0,0,160));
            mWritingStatus.setText("Writing...");
        }

        @Override
        protected void onProgressUpdate(String... values) {}
    }

    /**
     * New tags will be added to our existing tag storage.
     * List view adapter will be updated for new tags
     */
    private void handleInventoryResult()
    {
        synchronized (mNurApi.getStorage())
        {
            HashMap<String, String> tmp;
            NurTagStorage tagStorage = mNurApi.getStorage();

            // Add tags tp internal tag storage
            for (int i = 0; i < tagStorage.size(); i++) {

                NurTag tag = tagStorage.get(i);

                if (mTagStorage.addTag(tag))
                {
                    // Add new
                    tmp = new HashMap<String, String>();
                    tmp.put("epc", tag.getEpcString());
                    tmp.put("rssi", Integer.toString(tag.getRssi()));
                    tag.setUserdata(tmp);
                    mListViewAdapterData.add(tmp);
                    mTagsListViewAdapter.notifyDataSetChanged();
                }
            }

            // Clear NurApi tag storage
            tagStorage.clear();
            Beeper.beep(Beeper.BEEP_40MS);
        }
    }

    /**
     * Clear tag storages from NUR and from our internal tag storage
     * Also Listview cleared
     */
    private void clearInventoryReadings() {
        mNurApi.getStorage().clear();
        mTagStorage.clear();
        mListViewAdapterData.clear();
        mTagsListViewAdapter.notifyDataSetChanged();
    }

    /**
     * Perform inventory to seek tags near
     */
    private boolean doSingleInventory() throws Exception {

        if (!mNurApi.isConnected())
            return false;

        // Make sure antenna autoswitch is enabled
        if (mNurApi.getSetupSelectedAntenna() != NurApi.ANTENNAID_AUTOSELECT)
            mNurApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

        // Clear old readings
        clearInventoryReadings();

        try {
            // Perform inventory
            mNurApi.inventory();
            // Fetch tags from NUR
            mNurApi.fetchTags();
        }
        catch (NurApiException ex)
        {
            // Did not get any tags
            if (ex.error == NurApiErrors.NO_TAG)
                return true;

            throw ex;
        }
        // Handle inventoried tags
        handleInventoryResult();
        return true;
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
        public void IOChangeEvent(NurEventIOChange event) { }
        @Override
        public void autotuneEvent(NurEventAutotune event) { }
        @Override
        public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
        //@Override
        public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
    };
}
