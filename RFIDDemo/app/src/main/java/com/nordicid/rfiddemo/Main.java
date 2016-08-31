package com.nordicid.rfiddemo;

import java.util.Timer;
import java.util.TimerTask;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubApp;
import com.nordicid.apptemplate.SubAppList;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.*;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends AppTemplate {

	public static final int AUTH_REQUIRED_VERSION = 0x050500 + ('A' & 0xFF);
	public static final String AUTH_REQUIRED_VERSTRING = "5.5-A";
	Timer timer;
	TimerTask timerTask;
	final Handler timerHandler = new Handler();

	private NurApiAutoConnectTransport mAcTr;

	// These are used to toggle the visibility of the barcode app - not all readers support the accessory extension.
	private SubAppList localAppList;
	private SubApp localBarcodeApp;
	private SubApp localAuthApp;

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

	void saveSettings() {
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

	void loadSettings() {
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		String type = pref.getString("connType", "");
		if (type.equals("BLE")) {
			mAcTr = new NurApiBLEAutoConnect(this, getNurApi());
		} else if (type.equals("USB")) {
			mAcTr = new NurApiUsbAutoConnect(this, getNurApi());
		} else {
			mAcTr = null;
		}
		if (mAcTr != null) {
			mAcTr.setAddress(pref.getString("connAddr", ""));
		}

		updateStatus();
	}

	void updateStatus() {
		String str;
		if (mAcTr != null) {

			str = mAcTr.getType() + " " + mAcTr.getAddress();
			if (getNurApi().isConnected())
				str += " CONNECTED";
			else
				str += " DISCONNECTED";

			String details = mAcTr.getDetails();
			if (!details.equals(""))
				str += "; " + details;

		} else {
			str = "No connection defined";
		}
		setStatusText(str);

		// getWindow().getDecorView().invalidate();
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

	private int getFwIntVersion(NurApi api)
	{
		int iVersion = 0;
		mDetectedFw = "";

		if (api != null && api.isConnected()) {
			try {
				NurRespReaderInfo ri;
				ri = api.getReaderInfo();

				mDetectedFw = ri.swVersion;

				iVersion = ri.swVerMajor & 0xFF;
				iVersion <<= 8;
				iVersion |= (ri.swVerMinor & 0xFF);
				iVersion <<= 8;
				iVersion |= (ri.swVerDev & 0xFF);

			} catch (Exception ex) {

			}
		}

		return iVersion;
	}

	private String mDetectedFw = "";
	// Test sub-apps that depend on FW version or accessory presence here.
	private void testAddConditionalApps() {
		boolean okToAdd = false;
		mAuthAppAdded = false;

		if (localAppList == null) {
			syncViewContents();
			return;
		}

		try {
			okToAdd = (new NurAccessoryExtension(getNurApi())).isSupported();
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		if (okToAdd)
			localAppList.addSubApp(localBarcodeApp);

		if (getFwIntVersion(getNurApi()) >= AUTH_REQUIRED_VERSION) {
			localAppList.addSubApp(localAuthApp);
			mAuthAppAdded = true;
		}

		syncViewContents();
	}

	void removeConditionalApps()
	{
		// Remove sub-app via AppTemplate : checks if the application is currently exiting
		// thus eliminating any possible conflicts when destroying.
		removeSubApp(localBarcodeApp);
		removeSubApp(localAuthApp);
		mAuthAppAdded = false;
	}

	// Visible app choices / not.
	public void syncViewContents()
	{
		super.onResume();
	}

	private boolean mAuthAppAdded = false;

	@Override
	public void onCreateSubApps(SubAppList subAppList) {

		NurApi theApi = getNurApi();

		/* Reader settings application. */
		subAppList.addSubApp(new SettingsAppTabbed());
		
		if (AppTemplate.LARGE_SCREEN) 
		{
			subAppList.addSubApp(new InventoryApp());
		} else {
			subAppList.addSubApp(new InventoryAppTabbed());
		}
		
		/* Tag trace application. */
		subAppList.addSubApp(new TraceApp());
		
		/* Tag write application. */
		subAppList.addSubApp(new WriteApp());

		/* Barcode application. */
		// Add this later if accessory extension is present.
		localBarcodeApp = new BarcodeApp();
		/* Authentication application. */
		// Add this later if module's FW version supports version 2 commands.
		localAuthApp = new AuthenticationAppTabbed();

		localAppList = subAppList;
		theApi.setLogLevel(NurApi.LOG_ERROR);
		
		setAppListener(new NurApiListener() {
			@Override
			public void disconnectedEvent() {
				removeConditionalApps();

				if (exitingApplication())
					return;

				updateStatus();
				Toast.makeText(Main.this, getString(R.string.reader_disconnected), Toast.LENGTH_SHORT).show();
			}
			@Override
			public void connectedEvent() {
				String msg = getString(R.string.reader_connected);
				int toastLength = Toast.LENGTH_SHORT;

				testAddConditionalApps();

				if (!mAuthAppAdded && !mDetectedFw.isEmpty())
				{
					msg += ("\nAuthentication app not added.\nFW = " + mDetectedFw + "\nMinimum required=" + AUTH_REQUIRED_VERSTRING);
					toastLength = Toast.LENGTH_LONG;
				}

				updateStatus();

				Toast.makeText(Main.this, msg, toastLength).show();
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
			public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
			@Override
			public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
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
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

			case NurDeviceListActivity.REQUEST_SELECT_DEVICE: {
				if (data == null || (resultCode != NurDeviceListActivity.RESULT_BLE && resultCode != NurDeviceListActivity.RESULT_USB))
					return;

				if (data != null) {
					String deviceAddress, transportType;

					try {
						deviceAddress = data.getStringExtra(NurDeviceListActivity.DEVICE_ADDRESS);
						if (resultCode == NurDeviceListActivity.RESULT_BLE)
							transportType = NurDeviceSpec.BLE_TYPESTR;
						else
							transportType = NurDeviceSpec.USB_TYPESTR;

						if (mAcTr != null) {
							System.out.println("Dispose transport");
							mAcTr.dispose();
						}

						mAcTr = NurDeviceSpec.getAutoConnectTransport(this, transportType, getNurApi());
						mAcTr.setAddress(deviceAddress);
						saveSettings();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			break;
		}
	}

	void handleConnectionClick()
	{
		if (mAcTr != null)
			mAcTr.dispose();

		// Request for "all devices", not filtering "nordicid_*".
		NurDeviceListActivity.startDeviceRequest(this);
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
