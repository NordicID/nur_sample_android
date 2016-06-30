package com.nordicid.accessorydemo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nordicid.nurapi.NurApi;

public class QueryNewEPCActivity extends AppCompatActivity {

    private TextView mCurrentEpcText;
    private EditText mNewEPCEdit;
    private Button mOkBtn;

    private String mEpcDataResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String epcStr = "(null)";
        Helpers.lockToPortrait(this);

        Intent params = getIntent();

        if (params != null)
        {
            try
            {
                epcStr = params.getStringExtra(ApplicationConstants.SELECTED_EPC_STRING);
            }
            catch (Exception ex)
            {
                epcStr = "FAIL";
            }
        }

        setContentView(R.layout.activity_query_new_epc);

        mCurrentEpcText = (TextView) findViewById(R.id.text_current_epc);
        mNewEPCEdit =  (EditText) findViewById(R.id.edit_new_epc);
        mOkBtn = (Button) findViewById(R.id.btn_epc_write_ok);

        mCurrentEpcText.setText("Current EPC:\n" + epcStr);
        mNewEPCEdit.setText(epcStr);

        // check valid EPC data on the fly.
        mNewEPCEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkValidData();
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkValidData();
            }
        });

        setResult(ApplicationConstants.RESULT_CANCELED);
    }

    public void handleEpcDataDialogButton(View v)
    {
        int id = v.getId();

        if (id == R.id.btn_epc_write_ok)
            okReturnNewData();
        else {
            setResult(ApplicationConstants.RESULT_CANCELED);
            finish();
        }
    }

    private void okReturnNewData()
    {
        // Return new EPC data as string; its contents has been validated.
        Intent returnIntent = new Intent();
        returnIntent.putExtra(ApplicationConstants.NEW_EPC_DATA_STRING, mNewEPCEdit.getText().toString());
        setResult(ApplicationConstants.RESULT_OK, returnIntent);
        finish();
    }

    void checkValidData()
    {
        boolean valid = false;

        try
        {
            String strNewEpc = mNewEPCEdit.getText().toString();
            byte []testData;

            testData = NurApi.hexStringToByteArray(strNewEpc);

            if (testData != null && testData.length >=2 && (testData.length % 2) == 0)
                valid = true;

        }
        catch (Exception ex) { }

        mOkBtn.setEnabled(valid);
    }
}
