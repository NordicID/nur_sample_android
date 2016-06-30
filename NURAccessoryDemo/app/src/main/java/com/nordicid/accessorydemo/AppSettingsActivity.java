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

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Spinner;

public class AppSettingsActivity extends AppCompatActivity {
    /** Some helper functionalities. */
    private Helpers mHelpers;

    /** What to report back. */
    private int mFinalActivityResult = ApplicationConstants.RESULT_CANCELED;

    /** Change detection. */
    private boolean mRememberDevice = false;
    /** Change detection. */
    private int mSearchIndex = -1;
    /** Change detection. */
    private int mBarcodeIndex = 0;

    /** Forget current device if any. */
    private Button mForgetButton;
    /** Apply changes. */
    private Button mApplyBtn;
    /** Device search timeout. */
    private Spinner mSearchTmoSpinner;
    /** Barcode scan timeout. */
    private Spinner mBarcodeTmpSpinner;
    /** Whether to remember the selected device or not. */
    private CheckBox mDeviceRememberChk;

    /** Adapter for the drop down list selections. */
    private ArrayAdapter<CharSequence> mTimeSettingAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Helpers.lockToPortrait(this);

        mHelpers = new Helpers(this);

        setContentView(R.layout.activity_app_settings);

        mDeviceRememberChk = (CheckBox)findViewById(R.id.chk_remember_device);
        mForgetButton = (Button)findViewById(R.id.btn_forget_device);
        mApplyBtn = (Button)findViewById(R.id.btn_apply_app_settings);

        mSearchTmoSpinner = (Spinner)findViewById(R.id.spin_search_timeout);
        mBarcodeTmpSpinner = (Spinner)findViewById(R.id.spin_barcode_scan_timeout);

        mTimeSettingAdapter = ArrayAdapter.createFromResource(this, R.array.arr_timeouts, android.R.layout.simple_spinner_item);
        mTimeSettingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSearchTmoSpinner.setAdapter(mTimeSettingAdapter);
        mBarcodeTmpSpinner.setAdapter(mTimeSettingAdapter);

        // Set controls according to current values.
        populateUpdateControls();

        // Simply check for changes and enable apply button if needed.
        final AdapterView.OnItemSelectedListener onItemSelectionChangeHandler = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long longId) {
                mApplyBtn.setEnabled(checkForChanges());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        // Handle spinners.
        mSearchTmoSpinner.setOnItemSelectedListener(onItemSelectionChangeHandler);
        mBarcodeTmpSpinner.setOnItemSelectedListener(onItemSelectionChangeHandler);

        mDeviceRememberChk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mApplyBtn.setEnabled(checkForChanges());
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Tha main activity may want to disconnect/forget the currently connected device.
        setResult(mFinalActivityResult);
        finish();
    }

    public void handleButtonClick(View v)
    {
        int id;
        id = v.getId();

        if (id == R.id.btn_apply_app_settings)
            applySelections();

        if (id == R.id.btn_forget_device)
            forgetCurrentDevice();
    }

    // Make the application to forhet the currently connected device.
    private void forgetCurrentDevice()
    {
        mFinalActivityResult = ApplicationConstants.RESULT_OK_FORGET_DEVICE;
        DataBroker.getInstance().removeAutoConnectDevice();
        mForgetButton.setText(R.string.text_forget_device);
        mForgetButton.setEnabled(false);
    }

    // Simply checks if any of the original / current settings changed.
    private boolean checkForChanges()
    {
        int tmoSearch, tmoBarcode;
        boolean remember;

        remember = mDeviceRememberChk.isChecked();
        tmoSearch = mSearchTmoSpinner.getSelectedItemPosition();
        tmoBarcode = mBarcodeTmpSpinner.getSelectedItemPosition();

        if (remember != mRememberDevice || tmoSearch != mSearchIndex || tmoBarcode != mBarcodeIndex)
            return true;

        return false;
    }

    // "Default" to 5 seconds.
    private int selectionToTimeout(int selection)
    {
        if (selection == 0)
            return ApplicationConstants.SEC_4_MILLIS;

        if (selection == 2)
            return ApplicationConstants.SEC_6_MILLIS;

        if (selection == 3)
            return ApplicationConstants.SEC_7_MILLIS;

        return ApplicationConstants.SEC_5_MILLIS;
    }

    // "Default" to selection 1 (=5 seconds).
    private int timeoutToSelection(int timeout)
    {
        if (timeout == ApplicationConstants.SEC_4_MILLIS)
            return 0;
        if (timeout == ApplicationConstants.SEC_6_MILLIS)
            return 2;
        if (timeout == ApplicationConstants.SEC_7_MILLIS)
            return 3;

        return 1;
    }

    private void applySelections()
    {
        DataBroker.setDeviceSearchTimeout(selectionToTimeout(mSearchTmoSpinner.getSelectedItemPosition()));
        DataBroker.setBarcodeScanTimeout (selectionToTimeout(mBarcodeTmpSpinner.getSelectedItemPosition()));
        DataBroker.rememberDevice(mDeviceRememberChk.isChecked());
        DataBroker.getInstance().savePreferences();

        mApplyBtn.setEnabled(false);
    }

    /**
     * Populate and update controls with current settings.
     */
    private void populateUpdateControls()
    {
        mRememberDevice = DataBroker.rememberDevice();
        mSearchIndex = timeoutToSelection(DataBroker.getDeviceSearchTimeout());
        mBarcodeIndex = timeoutToSelection(DataBroker.getBarcodeScanTimeout());

        mSearchTmoSpinner.setSelection(mSearchIndex);
        mBarcodeTmpSpinner.setSelection(mBarcodeIndex);
        mDeviceRememberChk.setChecked(mRememberDevice);

        BLEDeviceDescription devDesc = DataBroker.getInstance().getAutoconnectDevice();
        if (devDesc != null) {
            mForgetButton.setEnabled(true);
            mForgetButton.setText("Forget: " + devDesc.getAddress());
        }
        else
        {
            mForgetButton.setText(R.string.text_forget_device);
            mForgetButton.setEnabled(false);
        }
    }
}
