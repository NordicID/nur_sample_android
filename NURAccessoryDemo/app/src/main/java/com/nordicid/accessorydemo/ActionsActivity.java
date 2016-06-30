package com.nordicid.accessorydemo;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

import java.util.Random;

public class ActionsActivity extends AppCompatActivity implements ActionBar.TabListener, NurApiListener, AccessoryBarcodeResultListener {
    private ActionBar mActionBar;
    private NurApi mApi;
    private NurAccessoryExtension mAccessoryExtension;

    private boolean mInventoryWasStopped = false;
    private boolean mSearching = false;
    private boolean mBarcodeScanActive = false;

    private int mTagSelectionIndex = -1;
    private NurTagStorage mMainStorage = null;
    private TagEPCAdapter mTagViewAdapter = null;
    private ListView mTagView = null;
    private AdapterView.OnItemClickListener mTagItemClickListener;

    private TextView mCountView = null;

    private byte [] mCurrentlySelectedEPC = null;
    private byte [] mNewEPCData = null;
    private boolean mSGTINFilter = false;

    private int mCurrentTab = 0;

    Button mCountControlBtn;
    Button mClearBtn;
    Button mFilterBtn;
    Button mSessionBtn;

    TraceTagController mTraceController;
    ProgressBar mSearchIndicator;
    TextView mSearchRssiText;
    TextView mSearchEPCTExt;
    Button mSearchControlBtn;

    Button mBarcodeControlBtn;
    TextView mBarcodeResult;

    private Random mEpcGen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Helpers.lockToPortrait(this);

        setContentView(R.layout.activity_actions);

        mActionBar = getSupportActionBar();
        mApi = DataBroker.getInstance().getNurApi();
        mTraceController = new TraceTagController(mApi);

        mMainStorage = new NurTagStorage();
        mAccessoryExtension = DataBroker.getAccessoryExtension();
        mAccessoryExtension.registerBarcodeResultListener(this);

        mEpcGen = new Random(System.currentTimeMillis());

        mTagItemClickListener = new AdapterView.OnItemClickListener() {
            // final AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleTagItemSelection(position);
            }
        };

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Actions related to inventory/count, search etc.
        mActionBar.addTab(mActionBar.newTab().setText("Count").setTabListener(this));
        mActionBar.addTab(mActionBar.newTab().setText("Search").setTabListener(this));
        mActionBar.addTab(mActionBar.newTab().setText("Barcode").setTabListener(this));

        mApi.setListener(this);

        setResult(ApplicationConstants.RESULT_OK);

        mTraceController.setListener(new TraceTagController.TraceTagListener() {
            @Override
            public void traceTagEvent(TraceTagController.TracedTagInfo data) {
                handleNewTraceLevel(data.scaledRssi);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mApi.setListener(this);
    }

    private void panicExit()
    {
        // Disconnected during activity.
        setResult(ApplicationConstants.RESULT_UNEXPECTED_DISCONNECT);

        if (mBarcodeScanActive)
            stopBarcodeScan(false);
        try
        {
            mApi.stopAllContinuousCommands();
        }
        catch (Exception ex) { }

    }

    public void handleCountTabButton(View v)
    {
        int id = v.getId();

        switch (id)
        {
            case R.id.btn_start_inventory:
                // testTagAddition();
                controlInventory();
                break;
            case R.id.btn_clear_tags:
                clearTags();
                break;

            case R.id.btn_epc_filter:
                handleFilterSelection();
                break;

            case R.id.btn_count_session:
                handleSessionSelection();
                break;
        }
    }

    public void handleSearchTabButton(View v)
    {
        int id = v.getId();

        switch (id)
        {
            case R.id.btn_tag_search_control:
                controlTagSearch();
                break;
        }
    }

    public void handleActionsBarcodeTabButton(View v)
    {
        int id = v.getId();

        if (id == R.id.btn_barcode_control)
        {
            if (mBarcodeScanActive)
            {
                stopBarcodeScan(true);
            }
            else {
                beginBarcodeScan();
            }
        }
    }

    private boolean inventoryStreamRunning() {
        return mApi.isInventoryStreamRunning() || mApi.isInventoryExtendedStreamRunning();
    }

    void controlInventory()
    {
        if (inventoryStreamRunning())
            stopInventory();
        else
            startInventory();
    }

    void controlTagSearch()
    {
        if (mSearching)
            stopTagSearch();
        else
            startTagSearch();
    }

    void stopTagSearch()
    {
        try
        {
            mTraceController.stopTagTrace();
        }
        catch (Exception ex) { }
        mSearching = false;

        mSearchControlBtn.setText(R.string.text_start_tag_search);
    }

    void startTagSearch()
    {
        mSearching = false;
        clearScaledRssiAverage();

        // Enable all antennas

        try
        {
            // Each bit is an antenna. enable all at this point.
            // In ACD there are 4 antenna = 4 bits = 0x0F (or with these constants)
            mApi.setSetupAntennaMaskEx(NurApi.ANTENNAMASK_1 | NurApi.ANTENNAMASK_2 | NurApi.ANTENNAMASK_3 | NurApi.ANTENNAMASK_4);
            // Also enable automatic use by setting the selection to "automatic" at this point.
            mApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);
        }
        catch (Exception ex)
        {
            Helpers.shortToast(this, "Fatal error when enabling antennas:\n" + ex.getMessage());
            return;
        }

        try
        {
            // Begin search/locate so that the stream does not send back the EPC contents;
            // only the RSSI information is sent (if any).
            // mApi.traceTagByEpc(mCurrentlySelectedEPC, mCurrentlySelectedEPC.length, NurApi.TRACETAG_START_CONTINUOUS | NurApi.TRACETAG_NO_EPC);
            mTraceController.startTagTrace(mCurrentlySelectedEPC);
            mSearching = true;
        }
        catch (Exception ex)
        {
            Helpers.shortToast(this, "Tag search start error:\n" + ex.getMessage());
        }

        if (mSearching)
            mSearchControlBtn.setText(R.string.text_stop_tag_search);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ApplicationConstants.REQ_NEW_EPC_DATA && resultCode == ApplicationConstants.RESULT_OK && data != null)
        {
            boolean ok = false;
            // Get data
            try {
                mNewEPCData = NurApi.hexStringToByteArray(data.getStringExtra(ApplicationConstants.NEW_EPC_DATA_STRING));
                // Try to write
                ok = true;
            }
            catch (Exception ex)
            {
                Helpers.shortToast(this, "Cannot interpret data, error:\n" + ex.getMessage());
            }

            if (ok)
                writeNewEPC();
        }
        else if (requestCode == ApplicationConstants.REQ_NEW_EPC_DATA && resultCode == ApplicationConstants.RESULT_CANCELED)
            Helpers.shortToast(this, "Write canceled!");
    }

    void queryNewEpcData()
    {
        String epcText = NurApi.byteArrayToHexString(mCurrentlySelectedEPC);
        Intent epcDataQueryIntent = new Intent(this, QueryNewEPCActivity.class);

        epcDataQueryIntent.putExtra(ApplicationConstants.SELECTED_EPC_STRING, epcText);

        startActivityForResult(epcDataQueryIntent, ApplicationConstants.REQ_NEW_EPC_DATA);
    }

    /**
     * EPC write in two possible ways:
     *  - write "EPC by EPC" with the built in API function.
     *    This also takes care of the PC-bits' writing if necessary in order to make the actual EPC appearance during the inventory correct
     *  - write new EPC by singulating with current one, but writing with BlockWrite operation.
     *  - note: EPC is assumed to be open i.e. no password is used
     */
    void writeNewEPC()
    {
        // "Write EPC by EPC".
        // Get the selected tag and use its internal write function.
        // SEE: writeEpcByEpc
        int antennaId =0;
        int circularAntenna =0;
        int txLevel = 0;
        boolean restoreSetup = false;
        boolean removeFromAPIStorage = false;
        NurTag tagToWrite;
        tagToWrite = mMainStorage.get(mTagSelectionIndex);
        boolean circularOk = false;
        String strResult = "";
        // Save antenna & TX level:

        try
        {
            antennaId = mApi.getSetupSelectedAntenna();
            circularAntenna = getPhysicalAntennaMask("Circular");
            circularOk = (circularAntenna != 0);

            if (circularOk)
                mApi.setSetupSelectedAntenna(circularAntenna);

            txLevel = mApi.getSetupTxLevel();
            mApi.setSetupTxLevel(0);    // Full
            restoreSetup = true;
        }
        catch (Exception ex)
        {
            // TODO: handle error
        }

        try {

            // Tag shall use the API...
            tagToWrite.setAPI(mApi);
            // ...then simply:
            tagToWrite.writeEpc(mNewEPCData);
            //  --> see alternative: writeEpcByEpc (with API function)

            strResult = "New EPC written OK, \"Circular\" OK = " + Helpers.yesNo(circularOk);

            // NOTE: the EPC in the list is not valid after this anymore.
            // So, in case no change:
            if (!Helpers.byteArrayCompare(tagToWrite.getEpc(), mNewEPCData)) {
                mMainStorage.remove(mTagSelectionIndex);
                mTagViewAdapter.notifyDataSetChanged();
                mCountView.setText("Tags: " + String.valueOf(mMainStorage.size()));
                removeFromAPIStorage = true;
            }
        } catch (Exception ex) {
            strResult = "Tag write error:\n" + ex.getMessage();
        }

        Helpers.shortToast(this, strResult);

        // Tag's EPC was change?
        if (removeFromAPIStorage)
        {
            try
            {
                mApi.getStorage().removeTag(mCurrentlySelectedEPC);
            }
            catch (Exception ex)
            {
                // TODO: handle error
            }
        }

        if (restoreSetup) {
            try {
                mApi.setSetupSelectedAntenna(antennaId);
                mApi.setSetupTxLevel(txLevel);    // Full
            } catch (Exception ex) {
                // TODO: handle error
            }
        }

        mCurrentlySelectedEPC = null;
        mTagSelectionIndex = -1;
    }

    /**
     * EXAMPLE: here is how to write with API's "write EPC by EPC" command.
     */
    void writeEpcByEpc()
    {
        try {
            mApi.writeEpcByEpc(mCurrentlySelectedEPC, mCurrentlySelectedEPC.length, mNewEPCData.length, mNewEPCData);
            // Using password 0xACDCABBA when the EPC memory is locked to "secured" state would be like:
            // mApi.writeEpcByEpc(0xACDCABBA, mCurrentlySelectedEPC, mCurrentlySelectedEPC.length, mNewEPCData.length,mNewEPCData);
        }
        catch (Exception ex)
        {
            /* Handle error. */
        }
    }

    void inventoryRelatedButtonUpdate(boolean start)
    {
        int textId;
        if (start)
            textId = R.string.text_stop_tag_count;
        else
            textId = R.string.text_start_tag_count;

        mCountControlBtn.setText(textId);
        mSessionBtn.setEnabled(!start);
        mFilterBtn.setEnabled(!start);
        mClearBtn.setEnabled(!start);
    }

    int getPhysicalAntennaMask(String ant)
    {
        AntennaMapping []map;
        try
        {
            map = mApi.getAntennaMapping();
        }
        catch (Exception ex)
        {
            return  0;
        }

        int mask = 0;
        for (int n=0; n<map.length; n++)
        {
            if (map[n].name.startsWith(ant))
                mask |= (1 << map[n].antennaId);
        }
        return mask;
    }

    void startInventory()
    {
        try
        {
            int crossDipoleAntMask = getPhysicalAntennaMask("CrossDipole");
            if (crossDipoleAntMask != 0)
                mApi.setSetupAntennaMaskEx(crossDipoleAntMask);

            mInventoryWasStopped = false;

            if (mSGTINFilter) {
                NurInventoryExtended exParams = new NurInventoryExtended();
                NurInventoryExtendedFilter []filters= new NurInventoryExtendedFilter [] { new NurInventoryExtendedFilter() };
                exParams.Q = mApi.getSetupInventoryQ();
                exParams.inventorySelState = NurApi.INVSELSTATE_ALL;
                exParams.inventoryTarget = mApi.getSetupInventoryTarget();
                exParams.rounds = mApi.getSetupInventoryRounds();
                exParams.session = mApi.getSetupInventorySession();
                exParams.transitTime = 0;

                filters[0].action = NurApi.FILTER_ACTION_0;
                filters[0].address = 32;    // Bit address of EPC
                filters[0].bank = NurApi.BANK_EPC;
                filters[0].maskBitLength = 8;   // One byte
                // SGTIN-96 header byte value.
                filters[0].maskdata = new byte [] { 0x30 } ;
                filters[0].targetSession = exParams.session;
                filters[0].truncate = false;    // NOTE: currently has no effect.
                mApi.startInventoryExtendedStream(exParams, filters);
            }
            else
                mApi.startInventoryStream();

            inventoryRelatedButtonUpdate(true);
        }
        catch (Exception ex)
        {
            inventoryRelatedButtonUpdate(false);
            Helpers.longToast(this, "Serious inventory error: " + ex.getMessage());
        }
    }

    void stopInventory()
    {
        mInventoryWasStopped = true;
        try
        {
            // Calling this will use the Q, session and rounds value already set to the module.
            mApi.stopAllContinuousCommands();
        }
        catch (Exception ex) { }

        inventoryRelatedButtonUpdate(false);
    }

    void handleFilterSelection()
    {
        AlertDialog.Builder filterDlg = new AlertDialog.Builder(this);
        filterDlg.setTitle("SGTIN filter");
        CharSequence []items = new CharSequence[] { "Count SGTIN-96 only", "Count all tags" };

        DialogInterface.OnClickListener sgtinSelectionListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int countSGTIN96Selection)
            {
                if (countSGTIN96Selection == 0)
                    mSGTINFilter = true;
                else if (countSGTIN96Selection == 1)
                    mSGTINFilter = false;
            }
        };

        filterDlg.setItems(items, sgtinSelectionListener);
        filterDlg.create().show();
    }

    void handleSessionSelection()
    {
        final Context ctx = this;
        AlertDialog.Builder sessionDlg = new AlertDialog.Builder(this);
        CharSequence []items = new CharSequence[] { "Session 0", "Session 1", "Session 2", "Session 3", "Use current" };

        try {
            items[4] = items[4] + ": " + mApi.getSetupInventorySession();
        }
        catch (Exception ex)
        {
            Helpers.shortToast(ctx, "Error: could not get current session...");
            return;
        }

        sessionDlg.setTitle("Select inventory session");

        DialogInterface.OnClickListener sessionClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int sessionSelection)
            {
                if (sessionSelection <= 3) {
                    try
                    {
                        mApi.setSetupInventorySession(sessionSelection);
                        Helpers.shortToast(ctx, "Session is now " + sessionSelection);
                    }
                    catch (Exception ex)
                    {
                        Helpers.shortToast(ctx, "Error settings session to " + sessionSelection);
                    }
                }
            }
        };

        sessionDlg.setItems(items, sessionClickListener);
        sessionDlg.create().show();
    }

    void clearTags()
    {
        synchronized (mMainStorage) {
            mMainStorage.clear();

            try {
                mApi.clearIdBuffer();
            }
            catch (Exception ex) { }

            mCurrentlySelectedEPC = null;
            mTagSelectionIndex = -1;
            mTagViewAdapter.notifyDataSetChanged();
            mCountView.setText("Tags: 0");
        }
    }

    void assignCountViewControls()
    {
        mTagView = (ListView)findViewById(R.id.tag_epc_rssi_view);
        mCountView = (TextView)findViewById(R.id.tag_count_view);

        if (mTagViewAdapter == null)
            mTagViewAdapter = new TagEPCAdapter(ActionsActivity.this, mMainStorage);
        mTagView.setAdapter(mTagViewAdapter);
        mTagView.setOnItemClickListener(mTagItemClickListener);

        mCountView.setText("Tags: " + String.valueOf(mMainStorage.size()));

        mCountControlBtn = (Button)findViewById(R.id.btn_start_inventory);
        mClearBtn = (Button)findViewById(R.id.btn_clear_tags);
        mFilterBtn = (Button)findViewById(R.id.btn_epc_filter);
        mSessionBtn = (Button)findViewById(R.id.btn_count_session);
    }

    void assignSearchViewControls()
    {
        boolean enSearch = false;
        // Progress bar
        mSearchIndicator = (ProgressBar)findViewById(R.id.pbar_tag_rssi);
        mSearchIndicator.setVisibility(ProgressBar.GONE);

        // mSearchIndicator.setIndeterminate(false);
        // RSSI text
        mSearchRssiText = (TextView) findViewById(R.id.text_search_rssi);
        // EPC text
        mSearchEPCTExt = (TextView)findViewById(R.id.text_search_epc);
        // Button
        mSearchControlBtn  = (Button)findViewById(R.id.btn_tag_search_control);

        mSearchControlBtn.setText(R.string.text_start_tag_search);
        if (mCurrentlySelectedEPC == null) {
            mSearchEPCTExt.setText("No selected EPC");
        }
        else
        {
            mSearchIndicator.setVisibility(ProgressBar.VISIBLE);
            mSearchEPCTExt.setText(NurApi.byteArrayToHexString(mCurrentlySelectedEPC));
            enSearch = true;
        }

        mSearchControlBtn.setEnabled(enSearch);

        ViewGroup.LayoutParams lp = mSearchIndicator.getLayoutParams();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        lp.width = size.x / 2;
        // Note: weight
        lp.height = (7 * size.y / 10)  / 2;
        mSearchIndicator.setLayoutParams(lp);
        mSearchRssiText.setLayoutParams(lp);

    }

    void assignBarcodeViewControls()
    {
        mBarcodeControlBtn = (Button) findViewById(R.id.btn_barcode_control);
        mBarcodeResult = (TextView) findViewById(R.id.text_barcode_result);
        mAccessoryExtension.registerBarcodeResultListener(this);
    }

    void tagOperationQuery(final int selectedIndex)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        CharSequence []items =  new CharSequence[] { "Search tag", "Write new EPC", "Cancel" };
        final Context ctx = this;

        builder.setTitle("Select tag operation");
        mTagSelectionIndex = -1;

        builder.setItems(items,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0: // Search
                                mTagSelectionIndex = selectedIndex;
                                try {
                                    mCurrentlySelectedEPC = mMainStorage.get(selectedIndex).getEpc();
                                    mActionBar.setSelectedNavigationItem(1);
                                    startTagSearch();
                                }
                                catch (Exception ex)
                                {
                                    Helpers.shortToast(ctx, "Fatal error: " + ex.getMessage());
                                }
                                break;
                            case 1: // EPC write
                                try {
                                    mCurrentlySelectedEPC = mMainStorage.get(selectedIndex).getEpc();
                                    mTagSelectionIndex = selectedIndex;
                                    // Helpers.shortToast(ctx, "Operation:\n" + selectedOpMsg);
                                    // Continue with write.
                                    // TODO:
                                    queryNewEpcData();   // Activity result will start the write when ready.
                                }
                                catch (Exception ex)
                                {
                                    Helpers.shortToast(ctx, "Fatal error: " + ex.getMessage());
                                }

                                break;

                            default: // Cancel
                                break;
                        }
                    }
                });

        builder.create().show();
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
        //Called when a tab is selected
        int tabPosition = tab.getPosition();
        mCurrentTab = tabPosition;

        switch (tabPosition) {
            case 0:
                setContentView(R.layout.actions_tab_count);
                assignCountViewControls();
                mTagViewAdapter.notifyDataSetChanged();
                break;
            case 1:
                setContentView(R.layout.actions_tab_search);
                assignSearchViewControls();
                break;
            case 2:
                setContentView(R.layout.actions_tab_barcode);
                assignBarcodeViewControls();
                break;
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // Inventory / count tab.
        int tabUnselected = tab.getPosition();
                //mBarcodeScanActive
        switch (tabUnselected)
        {
            case 0:
                stopInventory();
                break;
            case 1:
                stopTagSearch();
                break;
            case 2:
                if (mBarcodeScanActive)
                    stopBarcodeScan(true);
                break;
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        int tabPosition = tab.getPosition();
        switch (tabPosition) {
            case 0:
                setContentView(R.layout.actions_tab_count);
                assignCountViewControls();
                mTagViewAdapter.notifyDataSetChanged();
                break;
            case 1:
                setContentView(R.layout.actions_tab_search);
                assignSearchViewControls();
                break;
            case 2:
                setContentView(R.layout.actions_tab_barcode);
                assignBarcodeViewControls();
                break;
        }
    }

    void handleNewTags(int nrOfNewTags, boolean streamWasStopped)
    {
        if (nrOfNewTags > 0) {
            synchronized (mMainStorage) {
                NurTagStorage st;
                int current;

                st = mApi.getStorage();

                current = mMainStorage.size();

                for (; current < st.size(); current++) {
                    mMainStorage.addTag(st.get(current));
                    mTagViewAdapter.notifyDataSetChanged();
                }
            }
        }

        mCountView.setText("Tags: " + String.valueOf(mMainStorage.size()));

        if (streamWasStopped) {
            // Automatic stop will occur in about 20 seconds: auto continue.
            if (!mInventoryWasStopped) {
                startInventory();
                Helpers.shortToast(this, "Stream restart.");
            }
            else
                inventoryRelatedButtonUpdate(false);
        }
    }

    String getTagDetails(NurTag tag)
    {
        String details = "";
        details = tag.getEpcString();
        details += ("\nFrom antenna: " + tag.getAntennaId());
        details += ("\nRSSI: " + tag.getRssi());
        details += ("\nScaled RSSI: " + tag.getRssi());

        return details;
    }

    void handleTagItemSelection(int position)
    {
        if (!inventoryStreamRunning() && position < mMainStorage.size())
        {
            tagOperationQuery(position);
        }
    }

    int handleOperationSelection()
    {
        int op = ApplicationConstants.RESULT_CANCELED;

        return op;
    }

    private static final int N_RSSI_AVG_VALUES = 4;
    private int mRssiWrp = 0;
    private int mRssiRdp = 1;
    private int mRssiSum = 0;

    int [] mScaledRssiAvgValues = new int[N_RSSI_AVG_VALUES];

    void clearScaledRssiAverage()
    {
        int i;
        mRssiWrp = 0;
        mRssiRdp = 1;
        mRssiSum = 0;

        for (i = 0; i<mScaledRssiAvgValues.length;i++)
            mScaledRssiAvgValues[i] = 0;
    }

    int getScaledRssiAverage(int newScaledValue)
    {
        mRssiSum -= mScaledRssiAvgValues[mRssiRdp++];
        mRssiSum += newScaledValue;
        mScaledRssiAvgValues[mRssiWrp++] = newScaledValue;

        if (mRssiRdp == N_RSSI_AVG_VALUES)
            mRssiRdp = 0;

        if (mRssiWrp == N_RSSI_AVG_VALUES)
            mRssiWrp = 0;

        return (mRssiSum / N_RSSI_AVG_VALUES);
    }
    /**
     * Handle tag trace / search notification.
     * @param eventScaledRssi RSSI  as 0...100%.
     */
    void handleNewTraceLevel(int eventScaledRssi)
    {
        mSearchIndicator.setProgress(eventScaledRssi);
        mSearchRssiText.setText(eventScaledRssi + "%");
    }

    /**
     * Begin barcode scan. This prevents other communication with the module for the time being.
     */
    private void beginBarcodeScan()
    {
        int timeout = DataBroker.getInstance().getBarcodeScanTimeout();
        try
        {
            mAccessoryExtension.readBarcodeAsync(timeout);
            mBarcodeScanActive = true;
            mBarcodeControlBtn.setText(R.string.text_barcode_cancel);
        }
        catch (Exception ex)
        {
            Helpers.okDialog(this, "Barcode scan error", ex.getMessage());
            mBarcodeScanActive = false;
        }
    }

    /**
     * End the barcode scan.
     *
     * @param cancel If true the nthe scan is also canceled (force cancel).
     */
    private void stopBarcodeScan(boolean cancel)
    {
        if (cancel)
        {
            try
            {
                mAccessoryExtension.cancelBarcodeAsync();
            }
            catch (Exception ex)
            {
                // TODO
            }
        }

        mBarcodeScanActive = false;
        mBarcodeControlBtn.setText(R.string.text_barcode_scan);
    }

    void handleOperationPerTab() {
        if (mCurrentTab == 0) {
            if (inventoryStreamRunning())
                stopInventory();
            else
                startInventory();
        }
        else if (mCurrentTab == 1) {
            if (mSearching)
                stopTagSearch();
            else if (mCurrentlySelectedEPC != null)
                startTagSearch();
        }
        else if (mCurrentTab == 2) {
            if (mBarcodeScanActive)
                stopBarcodeScan(true);
            else
                beginBarcodeScan();
        }
    }

    // NUR API events.
    @Override
    public void logEvent(int logLevel, String logMessage) {
        // Log message from the API.
    }

    @Override
    public void IOChangeEvent(NurEventIOChange eventIOChange) {
        // Event from I/O sampo or other board with external I/O
        // With accessory this event is also generated with the value of 100.
        if (eventIOChange.source == 100 && eventIOChange.direction == 0)
            handleOperationPerTab();
    }

    @Override
    public void bootEvent(String bootString) {
        // "APP" or "LOADER"
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
        // Generic connected event
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
        panicExit();
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
        handleNewTags(eventInventory.tagsAdded, eventInventory.stopped);
    }

    @Override
    public void inventoryStreamEvent(NurEventInventory eventInventory) {
        // Inventory stream event.
        handleNewTags(eventInventory.tagsAdded, eventInventory.stopped);
        // mCountView.setText(String.valueOf(mRoundCount) + " / " + String.valueOf(eventInventory.tagsAdded));
        // mRoundCount++;
    }

    @Override
    public void nxpEasAlarmEvent(NurEventNxpAlarm eventNxpAlarm) {
        // Event triggered by an NXP that has its EAS alarm bit set.
    }

    @Override
    public void programmingProgressEvent(NurEventProgrammingProgress eventProgramming) {
        // Information about software update progress.
    }

    @Override
    public void traceTagEvent(NurEventTraceTag eventTrace) {
        // Event triggered by currently traced tag.
        // handleNewTraceLevel(eventTrace.scaledRssi);
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

    /**
     * Handle the barcode result notification from the extension.
     *
     * @param result Class containing the asynchronous barcode scan result.
     *
     */
    @Override
    public void onBarcodeResult(AccessoryBarcodeResult result)
    {
        String strMessage = "Barcode error " + result.status;

        if (result.status == NurApiErrors.NUR_SUCCESS)
            strMessage = "Barcode result (" + result.status + "):\n\n" + result.strBarcode;
        else if (mAccessoryExtension.isNotSupportedError(result.status))
            strMessage = "Barcode reader not present";

        mBarcodeResult.setText(strMessage);
        stopBarcodeScan(false);
    }
}
