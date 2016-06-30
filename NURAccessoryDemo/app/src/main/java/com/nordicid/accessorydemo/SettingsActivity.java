/*
  Copyright 2016- Nordic ID
  NORDIC ID DEMO SOFTWARE DISCLAIMER

  You are about to use Nordic ID Demo Software ("Software").
  It is explicitly stated that Nordic ID does not give any kind of warranties,
  expressed or implied, for this Software. Software is provided "as is" and with
  all faults. Under no circumstances is Nordic ID liable for any direct, special,
  incidental or indirect damages or for any economic consequential damages to you
  or to any third party.

  The use of this software indicates your complete and unconditional understanding
  of the terms of this disclaimer.

  IF YOU DO NOT AGREE OF THE TERMS OF THIS DISCLAIMER, DO NOT USE THE SOFTWARE.
*/

package com.nordicid.accessorydemo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = "AccessoryDemo_SETTINGS";
    private boolean mInitializing = true;

    private NurApi mApi = DataBroker.getInstance().getNurApi();;
    private NurAccessoryExtension mExtension = DataBroker.getInstance().getAccessoryExtension();
    private Helpers mHelpers = new Helpers(this);

    private boolean mExtensionSupported = false;

    /** Flag set for the setup handled here. */
    private static final int SETUP_FLAGSET = NurApi.SETUP_TXLEVEL | NurApi.SETUP_INVQ | NurApi.SETUP_INVSESSION | NurApi.SETUP_INVROUNDS | NurApi.SETUP_INVTARGET | NurApi.SETUP_ANTMASKEX;

    /** Transmission level selection. */
    private Spinner mTxLevel;
    /** Q-parameter selection. */
    private Spinner mQ;
    /** Inventory session selection. */
    private Spinner mSession;
    /** Number of internal rounds during the inventory. */
    private Spinner mRounds;
    /** Inventory target. */
    private Spinner mTarget;

    /** Button to handle antenna selection. */
    private Button mAntennaBtn;

    /** */
    private CheckBox mSetNameCheck;
    /** */
    private EditText mNameEdit;
    /** */
    private CheckBox mRFIDHIDCheck;
    /** */
    private CheckBox mBarcodeHIDCheck;

    /** If checked the nthe setup applied is also made permanent (stored to the NUR module). */
    private CheckBox mStoreCheck;

    /** The setup: read at the start and applied when OK.*/
    private NurSetup mSetupToApply = null;

    /** */
    private NurAccessoryConfig mAccessoryConfigToApply;

    private boolean mIsOneWattReader = true;
    private int mTxHighLevel = ApplicationConstants.TX_LEVEL_HIGH_1W;
    private int mTxMediumLevel = ApplicationConstants.TX_LEVEL_MEDIUM_1W;
    private int mTxLowLevel = ApplicationConstants.TX_LEVEL_LOW_1W;
    private ArrayAdapter<CharSequence> mTxLevelAdapter;

    private AlertDialog.Builder mAntennaDialog = null;
    private boolean []mCheckedAntennas = null;

    // Antennas
    private ArrayList<String> mAntennaMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        mExtension = DataBroker.getAccessoryExtension();

        mTxLevel = (Spinner) findViewById(R.id.spin_tx_level);
        mQ = (Spinner) findViewById(R.id.spin_inventory_q);
        mSession = (Spinner) findViewById(R.id.spin_inventory_session);
        mRounds = (Spinner) findViewById(R.id.spin_inventory_rounds);
        mTarget = (Spinner) findViewById(R.id.spin_inventory_target);
        mStoreCheck = (CheckBox) findViewById(R.id.chk_store_setup);

        mAntennaBtn = (Button)findViewById(R.id.btn_antennas);

        mSetNameCheck = (CheckBox)findViewById(R.id.chk_name_edit);
        mNameEdit = (EditText)findViewById(R.id.edit_name);

        mRFIDHIDCheck = (CheckBox) findViewById(R.id.chk_rfid_hid);
        mBarcodeHIDCheck = (CheckBox) findViewById(R.id.chk_barcode_hid);

        final View.OnClickListener onCheckClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemsChanged();
            }
        };

        mSetNameCheck.setOnClickListener(onCheckClick);
        mRFIDHIDCheck.setOnClickListener(onCheckClick);
        mBarcodeHIDCheck.setOnClickListener(onCheckClick);

        (new Handler(getMainLooper())).post(new Runnable() {
            @Override
            public void run() {
                valuesToControls();
            }
        });

        (new Handler(getMainLooper())).postDelayed(new Runnable() {
            @Override
            public void run() {
                mInitializing = false;
            }
        }, 150);
    }


    private void valuesToControls()
    {
        int txLevel = ApplicationConstants.TX_LEVEL_MEDIUM_0W5;
        int q = 5;
        int session = 0;
        int rounds = 0;
        int target = NurApi.INVTARGET_A;
        String strAccessoryName = "N / A";

        try {
            mSetupToApply = mApi.getModuleSetup(SETUP_FLAGSET);
            txLevel = mSetupToApply.txLevel;
            q = mSetupToApply.inventoryQ;
            session = mSetupToApply.inventorySession;
            rounds = mSetupToApply.inventoryRounds;
            target = mSetupToApply.inventoryTarget;
        }
        catch (Exception ex)
        {
            // TODO
        }

        try
        {
            NurRespDevCaps dc;
            dc = mApi.getDeviceCaps();

            if (dc.maxTxmW == 500)
            {
                mTxHighLevel = ApplicationConstants.TX_LEVEL_HIGH_0W5;
                mTxMediumLevel = ApplicationConstants.TX_LEVEL_MEDIUM_0W5;
                mTxLowLevel = ApplicationConstants.TX_LEVEL_LOW_0W5;
                mIsOneWattReader = false;
            }
        }
        catch (Exception ex)
        {
            // TODO
        }

        if (mExtension != null && mExtension.isSupported())
        {
            try
            {
                boolean rfidEnabled, barcodeEnabled;

                mAccessoryConfigToApply = mExtension.getConfig();
                mExtensionSupported = true;
                strAccessoryName = mAccessoryConfigToApply.name;

                rfidEnabled = mAccessoryConfigToApply.getHidRFID();
                barcodeEnabled = mAccessoryConfigToApply.getHidBarCode();
                mRFIDHIDCheck.setChecked(rfidEnabled);
                mBarcodeHIDCheck.setChecked(barcodeEnabled);
            }
            catch (Exception ex)
            {

            }
        }


        mTxLevel.setSelection(txLevelToSelection(txLevel));
        mQ.setSelection(qToSelection(q));
        mSession.setSelection(session);
        mRounds.setSelection(roundsSelectionSetting(rounds));
        mTarget.setSelection(target);

        if (mExtensionSupported)
        {
            try {
                boolean rfidEnabled, barcodeEnabled;
                rfidEnabled = mAccessoryConfigToApply.getHidRFID();
                barcodeEnabled = mAccessoryConfigToApply.getHidBarCode();
                mRFIDHIDCheck.setChecked(rfidEnabled);
                mBarcodeHIDCheck.setChecked(barcodeEnabled);
            }
            catch (Exception ex)
            {
                mExtensionSupported = false;
            }
        }

        if (mIsOneWattReader)
            mTxLevelAdapter = ArrayAdapter.createFromResource(this, R.array.arr_tx_levels_1W, android.R.layout.simple_spinner_item);
        else
            mTxLevelAdapter = ArrayAdapter.createFromResource(this, R.array.arr_tx_levels_500mW, android.R.layout.simple_spinner_item);

        mTxLevelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTxLevel.setAdapter(mTxLevelAdapter);

        final AdapterView.OnItemSelectedListener onItemSelectionChangeHandler = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long longId) {
                itemsChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        mTxLevel.setOnItemSelectedListener(onItemSelectionChangeHandler);
        mQ.setOnItemSelectedListener(onItemSelectionChangeHandler);
        mSession.setOnItemSelectedListener(onItemSelectionChangeHandler);
        mRounds.setOnItemSelectedListener(onItemSelectionChangeHandler);
        mTarget.setOnItemSelectedListener(onItemSelectionChangeHandler);

        mNameEdit.setText(strAccessoryName);
        mNameEdit.setEnabled(false);
        mSetNameCheck.setChecked(false);
        mSetNameCheck.setEnabled(mExtensionSupported);

        mRFIDHIDCheck.setEnabled(mExtensionSupported);
        mBarcodeHIDCheck.setEnabled(mExtensionSupported);

        tryAntennaMapping();
    }

    private void tryAntennaMapping()
    {
        mAntennaBtn.setEnabled(false);
        try
        {
            int i;
            AntennaMapping []antennaMap;
            antennaMap = mApi.getAntennaMapping();

            mAntennaMap = new ArrayList<String>();
            mCheckedAntennas = new boolean[antennaMap.length];

            i = 0;
            for (AntennaMapping mapEntry : antennaMap) {
                mAntennaMap.add(mapEntry.name);
            }
        }
        catch (Exception ex)
        {
            // TODO
            mHelpers.shortToast("Antenna map error");
            return;
        }

        // Disable antenna selection in case there is only one antenna available.
        if (mAntennaMap.size() == 1)
        {
            // TODO
            return;
        }

        antennaMaskToCheckedItems(mSetupToApply.antennaMaskEx);

        mAntennaDialog = new AlertDialog.Builder(this);

        final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                if (which == DialogInterface.BUTTON_POSITIVE)
                {
                    tryInterpretAntennaSelections();
                }

            }
        };

        DialogInterface.OnMultiChoiceClickListener multiChoiseListener = new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {

            }
        };

        mAntennaDialog.setNegativeButton("Cancel", dialogClickListener);
        mAntennaDialog.setPositiveButton("OK", dialogClickListener);

        final CharSequence []antennaSeq = mAntennaMap.toArray(new CharSequence[mAntennaMap.size()]);
        mAntennaDialog.setMultiChoiceItems(antennaSeq, mCheckedAntennas, multiChoiseListener);
        mAntennaDialog.setTitle("Selected antennas");
        mAntennaDialog.create();

        mAntennaBtn.setEnabled(true);
    }

    private void antennaMaskToCheckedItems(int antennaMaskEx)
    {
        int checkMaskEx = NurApi.ANTENNAMASK_1;
        int i, n;

        n = mCheckedAntennas.length;
        for (i=0;i<n;i++)
        {
            if ((antennaMaskEx & checkMaskEx) != 0)
                mCheckedAntennas[i] = true;
            else
                mCheckedAntennas[i] = false;
            checkMaskEx <<= 1;
        }
    }

    private void tryInterpretAntennaSelections()
    {
        int antennaMaskEx = 0;
        int antennaCheckedMask = NurApi.ANTENNAMASK_1;

        for (boolean checked : mCheckedAntennas)
        {
            if (checked)
                antennaMaskEx |= antennaCheckedMask;
            antennaCheckedMask <<= 1;
        }

        if (antennaMaskEx != 0)
            mSetupToApply.antennaMaskEx = antennaMaskEx;
        else
            mHelpers.shortToast("Invalid antenna selections, not stored");
    }

    private void itemsChanged()
    {
        int pos;

        if (mInitializing)
            return;

        pos = mTxLevel.getSelectedItemPosition();
        mSetupToApply.txLevel = selectionToTxLevel(pos);

        pos = mQ.getSelectedItemPosition();
        mSetupToApply.inventoryQ = selectionToQ(pos);

        mSetupToApply.inventorySession = mSession.getSelectedItemPosition();
        mSetupToApply.inventoryRounds = roundsSelectionSetting(mRounds.getSelectedItemPosition());
        mSetupToApply.inventoryTarget = mTarget.getSelectedItemPosition();

        mNameEdit.setEnabled(mSetNameCheck.isChecked());

        if (mExtensionSupported) {
            mAccessoryConfigToApply.setHidBarcode(mBarcodeHIDCheck.isChecked());
            mAccessoryConfigToApply.setHidRFID(mRFIDHIDCheck.isChecked());
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        cancelResult();
    }

    private int txLevelToSelection(int level)
    {
        if (level == mTxHighLevel)
            return 2;
        else if (level == mTxLowLevel)
            return 0;

        // Middle selection.
        return 1;
    }

    private int selectionToTxLevel(int selection)
    {
        // 0 low, 1 med, 2 high
        if (selection == 0)
            return mTxLowLevel;
        if (selection == 2)
            return mTxHighLevel;

        return mTxMediumLevel;
    }

    // <item>Automatic</item>
    // <item>Use Q = 4</item>
    // <item>Use Q = 5</item>
    // <item>Use Q = 6</item>
    private int qToSelection(int q)
    {
        switch (q)
        {
            case 0: return 0;
            case 4: return 1;
            case 6: return 3;
            default: break;
        }

        // Q = 5
        return 2;
    }

    private int selectionToQ(int selection)
    {
        switch (selection)
        {
            case 0: return 0;
            case 1: return 4;
            case 3: return 6;
            default: break;
        }

        return 5;
    }

    private int roundsSelectionSetting(int roundsOrSelection)
    {
        if (roundsOrSelection > 4)
            return 4;
        return roundsOrSelection;
    }

    // 3, 5, 7
    private int millisToSelection(int seconds)
    {
        if (seconds == ApplicationConstants.SEC_4_MILLIS)
            return 0;
        if (seconds == ApplicationConstants.SEC_7_MILLIS)
            return 2;

        return 1;
    }

    private int selectedSecondsToMillis(int selection)
    {
        if (selection == 0)
            return ApplicationConstants.SEC_4_MILLIS;

        if (selection == 1)
            return ApplicationConstants.SEC_5_MILLIS;

        return ApplicationConstants.SEC_7_MILLIS;
    }

    private void applySelections()
    {
        int reqResult = ApplicationConstants.RESULT_OK;
        boolean store, storeFailed;
        String nameToSet = "";
        boolean setName = false;
        boolean bleRestartRequired = false;
        Intent resultIntent = new Intent();
        boolean warnAppClose = false;

        if (mExtensionSupported && mSetNameCheck.isChecked()) {
            nameToSet = mNameEdit.getText().toString();

            if (nameToSet.trim().isEmpty())
            {
                mHelpers.shortToast("Name cannot be empty.");
                return;
            }

            else
                setName = true;
        }

        store = mStoreCheck.isChecked();
        storeFailed = false;

        try {
            mApi.setModuleSetup(mSetupToApply, SETUP_FLAGSET);
        }
        catch (Exception ex)
        {
            mHelpers.shortToast("Failed to apply module setup!");
            reqResult = ApplicationConstants.RESULT_ERROR;
        }

        if (mExtensionSupported)
        {
            try {
                if (setName) {
                    // Name change?
                    if (mAccessoryConfigToApply.name.compareTo(nameToSet) != 0)
                        bleRestartRequired = true;
                    mAccessoryConfigToApply.name = nameToSet;
                }

                mExtension.setConfig(mAccessoryConfigToApply);

                // If HID for barcode or RFID is selected, then tell that
                // // this application needs to be closed in order to make use of them.
                warnAppClose = (mAccessoryConfigToApply.getHidBarCode() || mAccessoryConfigToApply.getHidRFID());
            }
            catch (Exception ex)
            {
                if (ex.getClass() == NurApiException.class)
                    mHelpers.shortToast("Accessory configuration set error " + ((NurApiException)ex).error);
                else
                    mHelpers.shortToast("Failed to apply accessory configuration!");

                reqResult = ApplicationConstants.RESULT_ERROR;
            }
        }

        if (reqResult == ApplicationConstants.RESULT_OK && store)
        {
            try {
                mApi.storeSetup(NurApi.STORE_ALL);
            }
            catch (Exception ex)
            {
                // No error as the settings were applied anyway, just note.
                storeFailed = true;
            }
        }

        if (reqResult == ApplicationConstants.RESULT_OK) {
            if (store && storeFailed)
                mHelpers.shortToast("Settings applied but storing failed");
            else if (store)
                mHelpers.shortToast("Settings applied and stored OK.");
            else {
                if (warnAppClose)
                    mHelpers.longToast("Settings applied OK.\nWarning: in order to make use of HID, this application needs to be closed.");
                else
                    mHelpers.shortToast("Settings applied OK.");
            }

            if (bleRestartRequired) {
                resultIntent.putExtra(ApplicationConstants.BLE_DEVICE_NEW_NAME, nameToSet);
                reqResult = ApplicationConstants.RESULT_OK_BLE_RESTART;    // Also state that the name needs to be applied
            }
        }

        setResult(reqResult, resultIntent);
        finish();
    }

    private void handleAntennaSelection()
    {
        antennaMaskToCheckedItems(mSetupToApply.antennaMaskEx);
        mAntennaDialog.show();
    }

    private void cancelResult()
    {
        setResult(ApplicationConstants.RESULT_CANCELED);
        mHelpers.shortToast("No settings were applied");
        finish();
    }

    public void handleButton(View v)
    {
        int id = v.getId();

        if (id == R.id.btn_antennas)
            handleAntennaSelection();
        else if (id == R.id.btn_settings_cancel)
            cancelResult();
        else if (id == R.id.btn_settings_ok)
            applySelections();
    }
}
