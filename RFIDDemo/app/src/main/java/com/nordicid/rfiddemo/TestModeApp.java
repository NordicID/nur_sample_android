package com.nordicid.rfiddemo;

import com.nordicid.apptemplate.SubApp;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.NurApiListener;
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
import com.nordicid.nurapi.NurPacket;
import com.nordicid.nurapi.NurRespRegionInfo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class TestModeApp extends SubApp {

    private NurApiListener mThisClassListener = null;

    private NurAccessoryExtension mAccessoryExt;
    private boolean mIsBle = false;

    private Spinner mNurTestTypeSpinner;
    private Spinner mNurChannelSpinner;

    private Spinner mBleTestTypeSpinner;
    private Spinner mBleChannelSpinner;

    private Button mStartButton;
    private Button mStartNurButton;

    @Override
    public NurApiListener getNurApiListener() {
        return mThisClassListener;
    }

    public TestModeApp() {
        super();

        mAccessoryExt = getAppTemplate().getAccessoryApi();

        mThisClassListener = new NurApiListener() {
            @Override
            public void connectedEvent() {
                enableControls();
            }
            @Override
            public void disconnectedEvent() { enableControls(); }
            @Override
            public void logEvent(int level, String txt) { }
            @Override
            public void bootEvent(String event) { }
            @Override
            public void inventoryStreamEvent(NurEventInventory event) { }
            @Override
            public void IOChangeEvent(NurEventIOChange event) { }
            @Override
            public void traceTagEvent(NurEventTraceTag event) { }
            @Override
            public void triggeredReadEvent(NurEventTriggeredRead event) { }
            @Override
            public void frequencyHopEvent(NurEventFrequencyHop event) { }
            @Override
            public void debugMessageEvent(String event) { }
            @Override
            public void inventoryExtendedStreamEvent(NurEventInventory event) { }
            @Override
            public void programmingProgressEvent(NurEventProgrammingProgress event) { }
            @Override
            public void deviceSearchEvent(NurEventDeviceInfo event) { }
            @Override
            public void clientConnectedEvent(NurEventClientInfo event) { }
            @Override
            public void clientDisconnectedEvent(NurEventClientInfo event) { }
            @Override
            public void nxpEasAlarmEvent(NurEventNxpAlarm event) { }
            @Override
            public void epcEnumEvent(NurEventEpcEnum event) { }
            @Override
            public void autotuneEvent(NurEventAutotune event) { }
            @Override
            public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
            @Override
            public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
        };

        setIsVisibleInMenu(false);
    }

    @Override
    public String getAppName() {
        return "Test Mode";
    }

    @Override
    public int getTileIcon() {
        return R.drawable.ic_settings;
    }

    @Override
    public int getLayout() {
        return R.layout.app_testmode;
    }

    @Override
    public void onVisibility(boolean val) {
        if (val && mNurTestTypeSpinner != null) {
            enableControls();
        }
    }

    int []nurTestTypes = new int[] { 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, 0x88 };

    void startNurOnlyTest() {
        int nurTestType = 0x88;
        int nurTestCh = -1;
        if (mNurTestTypeSpinner.getSelectedItemPosition() > 0) {
            nurTestType = nurTestTypes[mNurTestTypeSpinner.getSelectedItemPosition() - 1];
            nurTestCh = mNurChannelSpinner.getSelectedItemPosition() - 1;
        }

        try {
            sendNurTestCommand(nurTestType, nurTestCh);
            if (mNurTestTypeSpinner.getSelectedItemPosition() > 0) {
                Toast.makeText(getContext(), "Started NUR test mode", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Stopped NUR test mode", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "NUR test cmd failed:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void startTest()
    {
        int nurTestValue = 0;
        int bleTestValue = 0;

        if (mNurTestTypeSpinner.getSelectedItemPosition() > 0)
        {
            nurTestValue = ((nurTestTypes[mNurTestTypeSpinner.getSelectedItemPosition() - 1] & 0xFF) << 8); // type
            nurTestValue |= (mNurChannelSpinner.getSelectedItemPosition() - 1) & 0xFF; // channel
        }

/*
        dtm_cmd_t      command_code = (command >> 14) & 0x03;
        dtm_freq_t     freq         = (command >> 8) & 0x3F;
        uint32_t       length       = (command >> 2) & 0x3F;
        dtm_pkt_type_t payload      = command & 0x03;

        #define LE_RESET                        0                                DTM command: Reset device.
        #define LE_RECEIVER_TEST                1                                DTM command: Start receive test.
        #define LE_TRANSMITTER_TEST             2                                DTM command: Start transmission test.
        #define LE_TEST_END                     3                                DTM command: End test and send packet report.

        #define CARRIER_TEST                    0                                Length=0 indicates a constant, unmodulated carrier until LE_TEST_END or LE_RESET

        #define DTM_PKT_PRBS9                   0x00                             Bit pattern PRBS9.
        #define DTM_PKT_0X0F                    0x01                             Bit pattern 11110000 (LSB is the leftmost bit).
        #define DTM_PKT_0X55                    0x02                             Bit pattern 10101010 (LSB is the leftmost bit).
        #define DTM_PKT_VENDORSPECIFIC          0x03                             Vendor specific. Nordic: Continuous carrier test, or configuration.

*/
        bleTestValue |= (mBleChannelSpinner.getSelectedItemPosition() << 8);

        switch (mBleTestTypeSpinner.getSelectedItemPosition())
        {
            case 0: //<item>RX Test</item>
                bleTestValue |= (1 << 14); // command_code = LE_RECEIVER_TEST
                break;
            case 1: // <item>TX Test PRBS9</item>
                bleTestValue |= (2 << 14); // command_code = LE_TRANSMITTER_TEST
                bleTestValue |= (37 << 2); // length = 37
                bleTestValue |= (0 << 0); // payload = DTM_PKT_PRBS9
                break;
            case 2: //<item>TX Test 00001111</item>
                bleTestValue |= (2 << 14); // command_code = LE_TRANSMITTER_TEST
                bleTestValue |= (8 << 2); // length = 8
                bleTestValue |= (1 << 0); // payload = DTM_PKT_0X0F
                break;
            case 3: //<item>TX Test 01010101</item>
                bleTestValue |= (2 << 14); // command_code = LE_TRANSMITTER_TEST
                bleTestValue |= (8 << 2); // length = 8
                bleTestValue |= (2 << 0); // payload = DTM_PKT_0X55
                break;
            case 4: //<item>Carrier On</item>
                bleTestValue |= (2 << 14); // command_code = LE_TRANSMITTER_TEST
                bleTestValue |= (0 << 2); // length = CARRIER_TEST
                bleTestValue |= (3 << 0); // payload = DTM_PKT_VENDORSPECIFIC
                break;
        }
        try {
            startBleTest((bleTestValue | nurTestValue<<16) & 0xFFFFFFFF);
            Toast.makeText(getContext(), "BLE test mode started. Hold BLE device power button to exit test mode.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not start BLE test mode:\n" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void startBleTest(int bleTestValue) throws Exception {

        Log.d("TM", "startBleTest() " + Integer.toHexString(bleTestValue& 0xFFFFFFFF));

        byte []param = new byte[5];
        param[0] = 10;
        NurPacket.PacketDword(param, 1, bleTestValue);

        getNurApi().customCmd(NurAccessoryExtension.NUR_CMD_ACC_EXT, param);
    }

    void sendNurTestCommand(int type, int ch) throws Exception {
        // NUR_CMD_CONTCARR = 0x61;

        Log.d("TM", "startNurTest() " + type + "; " + ch);

        byte []param;
        if (ch != -1)
        {
            param = new byte[2];
            param[0] = (byte)type;
            param[1] = (byte)ch;
        } else {
            param = new byte[1];
            param[0] = (byte)type;
        }
        getNurApi().customCmd(0x61, param);
    }

    private void enableControls() {

        mIsBle = getAppTemplate().getAccessorySupported();

        boolean enableBle = getNurApi().isConnected() && mIsBle;

        mBleTestTypeSpinner.setEnabled(enableBle);
        mBleChannelSpinner.setEnabled(enableBle);
        mStartButton.setEnabled(enableBle);

        mNurTestTypeSpinner.setEnabled(getNurApi().isConnected());
        mNurChannelSpinner.setEnabled(getNurApi().isConnected());
        mStartNurButton.setEnabled(getNurApi().isConnected());

        if (getNurApi().isConnected())
        {
            try {
                NurRespRegionInfo info = getNurApi().getRegionInfo();

                ArrayList<String> channels = new ArrayList<String>();
                channels.add("Middle Channel");

                if (info.regionId == 7)
                {
                    int []chs = new int[]{ 916800, 918000, 919200, 920400, 920600, 920800,
                            921000, 921200, 921400, 921600, 921800, 922000, 922200, 922400,
                            922600, 922800, 923000, 923200, 923400 };

                    for (int n = 0; n < chs.length; n++)
                    {
                        int freq = chs[n];
                        channels.add((n+1) + " (" + freq + " kHz)");
                    }
                } else {
                    for (int n = 0; n < info.channelCount; n++)
                    {
                        int freq = (int)(info.baseFrequency + (info.channelSpacing * n));
                        channels.add((n+1) + " (" + freq + " kHz)");
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, channels);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mNurChannelSpinner.setAdapter(adapter);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mNurTestTypeSpinner = (Spinner) view.findViewById(R.id.tm_nur_test_type);
        mNurChannelSpinner = (Spinner) view.findViewById(R.id.tm_nur_test_channel);
        mBleTestTypeSpinner = (Spinner) view.findViewById(R.id.tm_ble_test_type);
        mBleChannelSpinner = (Spinner) view.findViewById(R.id.tm_ble_test_channel);

        mStartButton = (Button) view.findViewById(R.id.tm_start_test);
        mStartNurButton = (Button) view.findViewById(R.id.tm_start_nur_test);

        ArrayAdapter<CharSequence> nurTestTypeSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.tm_nur_test_types, android.R.layout.simple_spinner_item);
        nurTestTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mNurTestTypeSpinner.setAdapter(nurTestTypeSpinnerAdapter);

        ArrayAdapter<CharSequence> bleTestTypeSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.tm_ble_test_types, android.R.layout.simple_spinner_item);
        bleTestTypeSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBleTestTypeSpinner.setAdapter(bleTestTypeSpinnerAdapter);

        ArrayList<String> bleChannels = new ArrayList<String>();
        for (int n = 0; n < 40; n++)
        {
            int freq = (int)(2402 + n * 2);
            bleChannels.add((n+1) + " (" + freq + " MHz)");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, bleChannels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBleChannelSpinner.setAdapter(adapter);

        enableControls();

        mStartButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                startTest();
            }
        });

        mStartNurButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                startNurOnlyTest();
            }
        });
    }
}
