package example.nordicid.com.nursampleandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.nurapi.ACC_SENSOR_FEATURE;
import com.nordicid.nurapi.ACC_SENSOR_FILTER_FLAG;
import com.nordicid.nurapi.ACC_SENSOR_MODE_FLAG;
import com.nordicid.nurapi.AccSensorChanged;
import com.nordicid.nurapi.AccSensorConfig;
import com.nordicid.nurapi.AccSensorEventListener;
import com.nordicid.nurapi.AccSensorFilter;
import com.nordicid.nurapi.AccSensorRangeData;
import com.nordicid.nurapi.AccessoryExtension;
import com.nordicid.nurapi.NurApi;
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

import java.util.ArrayList;
import java.util.HashMap;

public class Sensor extends Activity {

    public static final String TAG = "NUR_SAMPLE";

    //Handle of NurApi will be fetch from MainActivity
    private static NurApi mNurApi;
    //Handle of AccessoryExtension will be fetch from MainActivity
    private static AccessoryExtension mAccExt;

    ArrayList<AccSensorConfig> sensorList;
    private static int selectedSensor = -1;

    private static CheckBox mCheckModeGpio;
    private static CheckBox mCheckModeStream;
    private static CheckBox mCheckRangeFilter;
    private static CheckBox mCheckTimeFilter;

    private static EditText mEditRangeLo;
    private static EditText mEditRangeHi;
    private static EditText mEditTimeLo;
    private static EditText mEditTimeHi;

    private static Button mButtonApplyMode;
    private static Button mButtonApplyFilter;

    private static TextView mRangeValue;
    private static Button mButtonGetRangeValue;
    private static ProgressBar mProgressRange;

    private static EditText mEditIOEvent;

    //Range Data var
    private static int mRangeData = 0;

    //ListView and adapter for showing sensors found
    private ListView mSensorsListView;
    private SimpleAdapter mSensorListViewAdapter;
    private ArrayList<HashMap<String, String>> mSensorListViewAdapterData = new ArrayList<HashMap<String,String>>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);

        //Get NurApi- and Accessory extension handles from MainActivity
        mNurApi = MainActivity.GetNurApi();
        mAccExt = MainActivity.GetAccessoryExtensionApi();

        //Set event listener for this activity. Mainly for receiving IOEvents
        mNurApi.setListener(mNurApiEventListener);

        //Register sensor event listener. Then we are able to receive events from accessory sensors.
        mAccExt.registerSensorEventListener(mSensorResult);

        //Checkboxes and edittext field will be updated when sensor selected from list
        mCheckModeGpio = (CheckBox)findViewById(R.id.checkBoxModeGpio);
        mCheckModeStream = (CheckBox)findViewById(R.id.checkBoxModeStream);

        mCheckRangeFilter = (CheckBox)findViewById(R.id.checkBoxFilterRange);
        mCheckTimeFilter = (CheckBox)findViewById(R.id.checkBoxFilterTime);

        mEditRangeLo = (EditText)findViewById(R.id.editTextRangeLo);
        mEditRangeHi = (EditText)findViewById(R.id.editTextRangeHi);
        mEditTimeLo = (EditText)findViewById(R.id.editTextTimeLo);
        mEditTimeHi = (EditText)findViewById(R.id.editTextTimeHi);

        mButtonApplyMode = (Button)findViewById(R.id.buttonApplyMode);
        mButtonApplyFilter = (Button)findViewById(R.id.buttonApplyFilter);

        mRangeValue = (TextView)findViewById(R.id.textViewRange);
        mButtonGetRangeValue = (Button)findViewById(R.id.buttonReadRange);
        mProgressRange = (ProgressBar)findViewById(R.id.progressBarRange);
        mProgressRange.setMax(300); //300mm MAX (may changed..)

        //IOEvents are shown in this editext
        mEditIOEvent = (EditText)findViewById(R.id.editIOEvent);

        mSensorsListView = (ListView)findViewById(R.id.sensors_listview);
        //sets the adapter for listview
        mSensorListViewAdapter = new SimpleAdapter(this, mSensorListViewAdapterData, R.layout.sensorlist_row, new String[] {"sensor"}, new int[] {R.id.sensorRow});
        mSensorsListView.setAdapter(mSensorListViewAdapter);

        //List item (sensor) selected
        mSensorsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedSensor = (int)id;
                view.setSelected(true);
                Log.i(TAG,"Selected:" + selectedSensor + " Sensors=" + sensorList.size());
                UpdateSensorControls();
            }
        });

        mEditIOEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditIOEvent.getText().clear();
            }
        });

        try {
            HashMap<String, String> tmp = new HashMap<String, String>();

            sensorList = mAccExt.accSensorEnumerate();
            //iterate sensor list. sensorList is null if no sensors available
            for(int x=0;x<sensorList.size();x++) {
                AccSensorConfig cfg = sensorList.get(x);
                //Features of sensor telling us what we can do with it.
                String featureTxt="Features:";
                ACC_SENSOR_FEATURE featureFlags[] = cfg.getFeatureFlags();

                for(int i=0;i<featureFlags.length;i++) {
                    featureTxt+=featureFlags[i].toString() + "/";
                }

                String row = cfg.source + " (" + featureTxt + ")";

                tmp.put("sensor", row);
                mSensorListViewAdapterData.add(tmp);
            }
        } catch (Exception e) {
            Log.e(TAG,"Error enumerating sensors:" + e.getMessage());
        }

        mButtonApplyMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Apply sensor mode

                if(selectedSensor == -1) {

                    //passkeyInput();
                    return;
                }

                AccSensorConfig cfg = sensorList.get(selectedSensor);

                if(mCheckModeStream.isChecked()) {
                    if(cfg.hasFeature(ACC_SENSOR_FEATURE.StreamValue) == false) {
                        Toast.makeText(getApplicationContext(), "Sensor doesn't support streaming values", Toast.LENGTH_LONG).show();
                        mCheckModeStream.setChecked(false);
                        return;
                    }
                    cfg.setModeFlag(ACC_SENSOR_MODE_FLAG.Stream);
                } else cfg.clearModeFlag(ACC_SENSOR_MODE_FLAG.Stream);

                if(mCheckModeGpio.isChecked()) {
                    cfg.setModeFlag(ACC_SENSOR_MODE_FLAG.Gpio);
                } else cfg.clearModeFlag(ACC_SENSOR_MODE_FLAG.Gpio);

                //Finally, set config.
                try {
                    mAccExt.accSensorSetConfig(cfg);
                    Toast.makeText(getApplicationContext(), "Mode set successfully", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "ERROR changing mode:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        mButtonApplyFilter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if(selectedSensor == -1) {

                        try {
                            String txt = mAccExt.getBLEPasskey();
                            Toast.makeText(getApplicationContext(), "Passkey=" + txt, Toast.LENGTH_LONG).show();
                        }
                        catch (Exception e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                        }

                        return;
                    }
                    AccSensorConfig cfg = sensorList.get(selectedSensor);
                    AccSensorFilter filter = new AccSensorFilter();

                    if(mCheckRangeFilter.isChecked())
                        filter.setFilterFlag(ACC_SENSOR_FILTER_FLAG.Range);
                    else filter.clearFilterFlag(ACC_SENSOR_FILTER_FLAG.Range);

                    if(mCheckTimeFilter.isChecked())
                        filter.setFilterFlag(ACC_SENSOR_FILTER_FLAG.Time);
                    else filter.clearFilterFlag(ACC_SENSOR_FILTER_FLAG.Time);

                    filter.rangeThreshold.lo = Integer.parseInt(mEditRangeLo.getText().toString());
                    filter.rangeThreshold.hi = Integer.parseInt(mEditRangeHi.getText().toString());
                    filter.timeThreshold.lo = Integer.parseInt(mEditTimeLo.getText().toString());
                    filter.timeThreshold.hi = Integer.parseInt(mEditTimeHi.getText().toString());

                    mAccExt.accSensorSetFilter(cfg.source, filter);
                    Toast.makeText(getApplicationContext(), "Filter set successfully", Toast.LENGTH_LONG).show();

                }
                catch (Exception e) {
                    Log.e(TAG,e.getMessage());
                    Toast.makeText(getApplicationContext(), "ERROR changing filter:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

        mButtonGetRangeValue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(selectedSensor == -1) {

                    try {
                        mAccExt.clearBLEPasskey();
                    }
                    catch (Exception e) {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }

                    return; //No sensor selected
                }
                //For reading sensor range value, we need to know source of selected sensor and cast response to AccSensorRangeData
                try {
                    AccSensorRangeData rangeData = (AccSensorRangeData)mAccExt.accSensorGetValue(sensorList.get(selectedSensor).source);
                    mRangeData = rangeData.range;
                    showOnUI();
                }
                catch (Exception e) {
                    Log.e(TAG,e.getMessage());
                    Toast.makeText(getApplicationContext(), "ERROR reading range value:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        });

        mSensorListViewAdapter.notifyDataSetChanged();

        if(sensorList != null) {
            if(sensorList.size() == 0)
                Toast.makeText(getApplicationContext(), "No Sensors", Toast.LENGTH_LONG).show();
            else
            {
                Toast.makeText(getApplicationContext(), "Select sensor from list", Toast.LENGTH_LONG).show();
                Logger(sensorList.size() + " sensors available");
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "No Sensors available", Toast.LENGTH_LONG).show();
            Logger("NO SENSORS AVAILABLE");
            finish();
        }

        try {
            Log.i(TAG,"DeviceType=" + mAccExt.getConfig().getDeviceType());

        }catch (Exception e) {}

    }

    /**
     * Passkey set input test for EXA21 BLE. Do not use!
     */
    private void passkeyInput()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("passkey");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_CLASS_NUMBER);
        try {
            input.setText(mAccExt.getBLEPasskey());
        }catch (Exception e) {}
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    mAccExt.setBLEPasskey(input.getText().toString());
                    String txt = mAccExt.getBLEPasskey();
                    Toast.makeText(getApplicationContext(), "Passkey=" + txt, Toast.LENGTH_LONG).show();
                }
                catch (Exception e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void UpdateSensorControls() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(selectedSensor==-1) return;

                    AccSensorConfig cfg = sensorList.get(selectedSensor);
                    AccSensorFilter filter = mAccExt.accSensorGetFilter(sensorList.get(selectedSensor).source);

                    //Set Mode checkboxes
                    ACC_SENSOR_MODE_FLAG[] modeFlags = cfg.getModeFlags();
                    //Clear mode checkboxes and set check again if set
                    mCheckModeGpio.setChecked(false);
                    mCheckModeStream.setChecked(false);
                    if(modeFlags != null) {
                        for (int i = 0; i < modeFlags.length; i++) {
                            if (modeFlags[i] == ACC_SENSOR_MODE_FLAG.Gpio)
                                mCheckModeGpio.setChecked(true);
                            else if (modeFlags[i] == ACC_SENSOR_MODE_FLAG.Stream)
                                mCheckModeStream.setChecked(true);
                        }
                    }

                    //Set filter checkboxes
                    ACC_SENSOR_FILTER_FLAG[] filterFlags = filter.getFilterFlags();
                    //Clear filter checkboxes and set check again if set
                    mCheckRangeFilter.setChecked(false);
                    mCheckTimeFilter.setChecked(false);
                    if(filterFlags != null) {
                        for (int i = 0; i < filterFlags.length; i++) {
                            if (filterFlags[i] == ACC_SENSOR_FILTER_FLAG.Range)
                                mCheckRangeFilter.setChecked(true);
                            else if (filterFlags[i] == ACC_SENSOR_FILTER_FLAG.Time)
                                mCheckTimeFilter.setChecked(true);
                        }
                    }

                    //Fill edit fields of filters
                    mEditTimeLo.setText(Integer.toString(filter.timeThreshold.lo));
                    mEditTimeHi.setText(Integer.toString(filter.timeThreshold.hi));

                    mEditRangeLo.setText(Integer.toString(filter.rangeThreshold.lo));
                    mEditRangeHi.setText(Integer.toString(filter.rangeThreshold.hi));

                } catch (Exception e) {
                    Log.e(TAG, "Error updating controls:" + e.getMessage());
                    Toast.makeText(getApplicationContext(), "Error updating controls:" + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    /**
     * Show UI items
     */
    private void showOnUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mProgressRange.setProgress(mRangeData);
                mRangeValue.setText(mRangeData +" mm");
            }
        });
    }

    private void Logger(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mEditIOEvent.append("> " + msg);
                Log.w(TAG, msg);
            }
        });
    }

    private AccSensorEventListener mSensorResult = new AccSensorEventListener() {
        @Override
        public void onSensorChanged(AccSensorChanged accSensorChanged) {
            Logger("onSensorChanged=" + accSensorChanged.source + " Removed=" + accSensorChanged.removed+"\n");
        }

        @Override
        public void onRangeData(AccSensorRangeData accSensorRangeData) {
            mRangeData = accSensorRangeData.range;
            showOnUI();
        }
    };

    //NurApi event handlers. We are interested IOChangeEvent as sensor can throw that event
    private NurApiListener mNurApiEventListener = new NurApiListener() {
        @Override public void logEvent(int i, String s) { }
        @Override public void connectedEvent() { }
        @Override public void disconnectedEvent() { }
        @Override public void bootEvent(String s) { }
        @Override public void inventoryStreamEvent(NurEventInventory nurEventInventory) { }
        @Override public void IOChangeEvent(NurEventIOChange nurEventIOChange) {
            String ioEventString = "IOEvent Dir=" + nurEventIOChange.direction + " Sensor=" + nurEventIOChange.sensor + " Source=" + nurEventIOChange.source+"\n";
            Logger(ioEventString);
        }
        @Override public void traceTagEvent(NurEventTraceTag nurEventTraceTag) { }
        @Override public void triggeredReadEvent(NurEventTriggeredRead nurEventTriggeredRead) { }
        @Override public void frequencyHopEvent(NurEventFrequencyHop nurEventFrequencyHop) { }
        @Override public void debugMessageEvent(String s) { }
        @Override public void inventoryExtendedStreamEvent(NurEventInventory nurEventInventory) { }
        @Override public void programmingProgressEvent(NurEventProgrammingProgress nurEventProgrammingProgress) { }
        @Override public void deviceSearchEvent(NurEventDeviceInfo nurEventDeviceInfo) { }
        @Override public void clientConnectedEvent(NurEventClientInfo nurEventClientInfo) { }
        @Override public void clientDisconnectedEvent(NurEventClientInfo nurEventClientInfo) { }
        @Override public void nxpEasAlarmEvent(NurEventNxpAlarm nurEventNxpAlarm) { }
        @Override public void epcEnumEvent(NurEventEpcEnum nurEventEpcEnum) { }
        @Override public void autotuneEvent(NurEventAutotune nurEventAutotune) { }
        @Override public void tagTrackingScanEvent(NurEventTagTrackingData nurEventTagTrackingData) { }
        @Override public void tagTrackingChangeEvent(NurEventTagTrackingChange nurEventTagTrackingChange) {

        }
    };



}
