package example.nordicid.com.nursampleandroid;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.nurapi.*;

import java.util.ArrayList;
import java.util.HashMap;

import example.nordicid.com.nursampleandroid.TraceTagController.TraceTagListener;
import example.nordicid.com.nursampleandroid.TraceTagController.TracedTagInfo;

public class Trace extends Activity {

    //Handle of NurApi will be fetch from MainActivity
    private static NurApi mNurApi;

    //Selected EPC to Trace
    static String mSelectedEpc;

    //In here found tags stored
    private NurTagStorage mTagStorage = new NurTagStorage();

    //UI
    private Button mRefreshButton;
    private TextView mTraceableEpcEditText;
    private EditText mPctText;
    private ProgressBar mProgressBar;

    //Controller of Trace tag
    private TraceTagController mTraceController;

    //ListView and adapter for showing tags found
    private ListView mTagsListView;
    private SimpleAdapter mTagsListViewAdapter;
    private ArrayList<HashMap<String, String>> mListViewAdapterData = new ArrayList<HashMap<String,String>>();

    //ProgressBar animation
    ObjectAnimator animation = null;
    int mLastVal = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trace);

        //Get NurApi handle from MainActivity
        mNurApi = MainActivity.GetNurApi();

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mPctText = (EditText) findViewById(R.id.pct_text);
        mTraceableEpcEditText = (TextView) findViewById(R.id.locate_epc_edittext);
        mTraceController = new TraceTagController(mNurApi);

        // Do not save EditText state
        mTraceableEpcEditText.setSaveEnabled(false);

        ViewGroup.LayoutParams lp = mProgressBar.getLayoutParams();
        WindowManager wm = (WindowManager) Trace.this.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        lp.width = size.x / 3;
        lp.height = size.x / 3;
        mProgressBar.setLayoutParams(lp);
        mPctText.setLayoutParams(lp);

        mTraceController.setListener(new TraceTagListener() {
            @Override
            public void traceTagEvent(TracedTagInfo data) {
                int scaledRssi = data.scaledRssi;
                ShowProgressBar(scaledRssi);
                mLastVal = scaledRssi;
            }

            @Override
            public void readerDisconnected() {
                stopTrace();
            }

            @Override
            public void readerConnected() { }

            @Override
            public void IOChangeEvent(NurEventIOChange event) { }

        });

        mRefreshButton = (Button) findViewById(R.id.locate_button);
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

                try {
                    if(mNurApi.isTraceTagRunning())
                    {
                        //Another tag selected for tracing. Stop existing tracing and start with this new one.
                        stopTrace();
                    }
                }
                catch (Exception ex)
                {
                    Toast.makeText(Trace.this,ex.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                //Start tag tracing using selected tag
                try {
                    mSelectedEpc = mSelectedTag.get("epc");
                    mTraceableEpcEditText.setText("Trace tag: " + mSelectedEpc);
                    //Set tag to be traced..
                    mTraceController.setTagTrace(mSelectedEpc);
                    //..and start tracing..
                    startTrace();
                }
                catch (Exception ex)
                {
                    Toast.makeText(Trace.this,ex.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        });

        //Refresh list Button OnClick handler
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Refresh tag list or stop tracing
                try {
                    if(mTraceController.isTracingTag())
                    {
                        //Need to stop tag tracing
                        stopTrace();
                        ShowProgressBar(0);
                        mTraceableEpcEditText.setText("");
                        return;
                    }

                    clearInventoryReadings(); //Clear all from old stuff
                    doSingleInventory(); //Make single round inventory.
                }
                catch (Exception ex)
                {
                    //Something fails..
                    Toast.makeText(Trace.this, ex.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Update Progress bar. Show nice animation..
     */
    private void ShowProgressBar(int scaledRssi)
    {
        mProgressBar.setProgress(scaledRssi);
        mPctText.setText(scaledRssi + "%");

        if (animation != null) {
            mLastVal = (int) animation.getAnimatedValue();
            animation.cancel();
        } else {
            animation = ObjectAnimator.ofInt(mProgressBar, "progress", mLastVal, scaledRssi);
            animation.setInterpolator(new LinearInterpolator());
        }

        animation.setIntValues(mLastVal, scaledRssi);
        animation.setDuration(300);
        animation.start();
    }

    /**
     * Start tracing. EPC has been selected from list.
     */
    private void startTrace() {

        try {
            if (!mTraceController.isTracingTag())
            {
                if (mSelectedEpc == null || mSelectedEpc.length() == 0)
                    Toast.makeText(this, "Select EPC to locate from list", Toast.LENGTH_SHORT).show();
                else if (!mNurApi.isConnected())
                    Toast.makeText(this, "Reader not connected", Toast.LENGTH_SHORT).show();
                else  if (mTraceController.startTagTrace(mSelectedEpc))
                    mRefreshButton.setText("STOP");
                else
                    Toast.makeText(this, "Invalid EPC", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception ex) {
            Toast.makeText(this, "Reader error", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Stop tracing.
     */
    private void stopTrace() {
        if (mTraceController.isTracingTag())
        {
            mTraceController.stopTagTrace();
            mRefreshButton.setText("Refresh");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mTraceController.isTracingTag()) {
            stopTrace();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTraceController.isTracingTag()) {
            stopTrace();
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
}
