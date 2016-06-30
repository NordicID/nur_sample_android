package com.nordicid.rfiddemo;

import java.util.Timer;
import java.util.TimerTask;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubAppList;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiAutoConnectTransport;
import com.nordicid.nurapi.NurApiBLEAutoConnect;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurApiUsbAutoConnect;
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
import com.nordicid.nurapi.NurRespReaderInfo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends AppTemplate {
	
	Timer timer;
    TimerTask timerTask;
    final Handler timerHandler = new Handler();
	
	private NurApiAutoConnectTransport mAcTr;
	
	public void startTimer() {
		
		stopTimer();
		
		//set a new Timer
		timer = new Timer();
		
		//initialize the TimerTask's job
		initializeTimerTask();
		
		//schedule the timer, after the first 10000ms the TimerTask will run every 10000ms
		timer.schedule(timerTask, 10000, 10000); //
	}

	public void stopTimer() {
		//stop the timer, if it's not already null
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	public void initializeTimerTask() {
		
		timerTask = new TimerTask() {
			@Override
			public void run() {
				//use a handler to run a toast that shows the current timestamp
				timerHandler.post(new Runnable() {
					@Override
					public void run() {
						updateStatus();
					}
				});
			}
		};
	}
	
	void saveSettings()
	{
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		if (mAcTr == null) {
			editor.putString("connType", "");
			editor.putString("connAddr", "");
		} else {
			editor.putString("connType", mAcTr.getType());
			editor.putString("connAddr", mAcTr.getAddress());
		}
		editor.commit();
		
		updateStatus();
	}
	
	void loadSettings()
	{
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		String type = pref.getString("connType", "");
		if (type.equals("BLE")) {
			mAcTr = new NurApiBLEAutoConnect(this, getNurApi());
		}
		else if (type.equals("USB")) {
			mAcTr = new NurApiUsbAutoConnect(this, getNurApi());
		}
		else {
			mAcTr = null;
		}
		if (mAcTr != null) {
			mAcTr.setAddress(pref.getString("connAddr", ""));
		}
		
		updateStatus();
	}
	
	void updateStatus()
	{
		String str;
		if (mAcTr != null) {
			
			str = mAcTr.getType() + " " + mAcTr.getAddress();
			if (getNurApi().isConnected())
				str += " CONNECTED";
			else
				str += " DISCONNECTED";
			
			String details = mAcTr.getDetails();
			if (!details.equals(""))
				str +="; " + details;
			
		} else {
			str = "No connection defined";
		}
		setStatusText(str);
	}
	
	@Override
	protected void onPause() {
		super.onPause();	
		
		stopTimer();
		
		if (mAcTr != null) {
			mAcTr.onPause();
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if (mAcTr == null)
			loadSettings();
		
		if (mAcTr != null) {			
			mAcTr.onResume();
		}
		
		startTimer();
	}
	
	@Override
	protected void onStop() {
		super.onStop();		
		
		if (mAcTr != null) {
			mAcTr.onStop();
		}		
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mAcTr != null) {
			mAcTr.onDestroy();
		}		
	}

	@Override
	public void onCreateSubApps(SubAppList subAppList) {
		
		NurApi theApi = getNurApi();

		/* Reader settings application. */
		//subAppList.addSubApp(new SettingsApp(this, this, theApi));
		subAppList.addSubApp(new SettingsAppTabbed(this, this, theApi));
		
		if (AppTemplate.LARGE_SCREEN) 
		{
			subAppList.addSubApp(new InventoryApp(this, this, theApi));
		} else {
			subAppList.addSubApp(new InventoryAppTabbed(this, this, theApi));
		}
		
		/* Tag trace application. */
		subAppList.addSubApp(new TraceApp(this, this, theApi));
		
		/* Tag write application. */
		subAppList.addSubApp(new WriteApp(this, this, theApi));

		/* Barcode application. */
		subAppList.addSubApp(new BarcodeApp(this, this, theApi));
		
		theApi.setLogLevel(NurApi.LOG_ERROR);// | NurApi.LOG_USER | NurApi.LOG_VERBOSE);
		
		setAppListener(new NurApiListener() {
			@Override
			public void disconnectedEvent() { 
				updateStatus();
				Toast.makeText(Main.this, getString(R.string.reader_disconnected), Toast.LENGTH_SHORT).show();
			}
			@Override
			public void connectedEvent() { 
				updateStatus(); 
				Toast.makeText(Main.this, getString(R.string.reader_connected), Toast.LENGTH_SHORT).show();
			}

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
			public void inventoryExtendedStreamEvent(NurEventInventory event) { }
			@Override
			public void frequencyHopEvent(NurEventFrequencyHop event) { }
			@Override
			public void epcEnumEvent(NurEventEpcEnum event) { }
			@Override
			public void deviceSearchEvent(NurEventDeviceInfo event) { }
			@Override
			public void debugMessageEvent(String event) { }
			@Override
			public void clientDisconnectedEvent(NurEventClientInfo event) { }
			@Override
			public void clientConnectedEvent(NurEventClientInfo event) { }
			@Override
			public void bootEvent(String event) { }
			@Override
			public void autotuneEvent(NurEventAutotune event) { }
			@Override
			public void IOChangeEvent(NurEventIOChange event) { }
			@Override
			public void tagTrackingScanEvent(NurEventTagTrackingData event) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void tagTrackingChangeEvent(NurEventTagTrackingChange event) {
				// TODO Auto-generated method stub
				
			}
		});
	}

	@Override
	public void onCreateDrawerItems(Drawer drawer) {
		drawer.addTitle("About");
		drawer.addTitle("Connection");
	}

	void handleAboutClick()
	{
		final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_about, null);
		AlertDialog.Builder builder = new Builder(this);
		builder.setView(dialogLayout);
		
		final TextView readerAttachedTextView = (TextView) dialogLayout.findViewById(R.id.reader_attached_is);

		if (getNurApi().isConnected()) {
			
			readerAttachedTextView.setText(getString(R.string.attached_reader_info));
			
			try {
				NurRespReaderInfo readerInfo = getNurApi().getReaderInfo();
				
				final TextView modelTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_model);
				modelTextView.setText(getString(R.string.about_dialog_model) + " " + readerInfo.name);
				modelTextView.setVisibility(View.VISIBLE);
				
				final TextView serialTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_serial);
				serialTextView.setText(getString(R.string.about_dialog_serial) + " " + readerInfo.serial);
				serialTextView.setVisibility(View.VISIBLE);
				
				final TextView firmwareTextView = (TextView) dialogLayout.findViewById(R.id.reader_info_firmware);
				firmwareTextView.setText(getString(R.string.about_dialog_firmware) + " " + readerInfo.swVersion);
				firmwareTextView.setVisibility(View.VISIBLE);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			readerAttachedTextView.setText(getString(R.string.no_reader_attached));
		}

		builder.show();
	}
	
	//NurApiBLETransport bleTr;
	
	private static final int REQUEST_SELECT_DEVICE = 1;
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case REQUEST_SELECT_DEVICE:
        	//When the DeviceListActivity return, with the selected device address
            if (resultCode == Activity.RESULT_OK && data != null) {
                final String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                //mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);
               
                try {
                	if (mAcTr != null) {
	    				System.out.println("Dispose transport");            	
    					mAcTr.dispose();
    				}

					System.out.println("New BLE transport: " + deviceAddress);
					mAcTr = new NurApiBLEAutoConnect(Main.this, getNurApi());
        			mAcTr.setAddress(deviceAddress);
        			saveSettings();		
                	
        		} catch (Exception e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
            }
            break;
        }
	}
	
	void handleConnectionClick()
	{
		AlertDialog.Builder builder = new Builder(this);
		
		final CharSequence[] items = { "USB", "BLE" };

		builder.setTitle("Select connection")
        .setItems(items, new DialogInterface.OnClickListener() {
            	@Override
				public void onClick(DialogInterface dialog, int which) {
            		switch (which)
            		{
            		case 0:
            			{
            				if (mAcTr != null)
            					mAcTr.dispose();
            				
            				mAcTr = new NurApiUsbAutoConnect(Main.this, getNurApi());
            				mAcTr.setAddress("USB");
            				saveSettings();
            			}
            			break;
            		case 1:
            			{
            				Intent newIntent = new Intent(Main.this, DeviceListActivity.class);
            				startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
            			}
            			break;
            		}
            	}
        });
		builder.show();
	}

	@Override
	public void onDrawerItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		switch (position) {
		case 0:
			handleAboutClick();
			break;
		case 1:
			handleConnectionClick();
			break;
		default:
			break;
		}
	}
}
