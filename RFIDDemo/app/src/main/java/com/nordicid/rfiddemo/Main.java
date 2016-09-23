package com.nordicid.rfiddemo;

import java.util.Timer;
import java.util.TimerTask;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubAppList;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.*;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

public class Main extends AppTemplate {

	// Authentication app requirements.
	public static final int AUTH_REQUIRED_VERSION = 0x050500 + ('A' & 0xFF);
	public static final String AUTH_REQUIRED_VERSTRING = "5.5-A";

	/** Requesting file for key reading. */
	public static final int REQ_FILE_OPEN = 4242;

	Timer timer;
	TimerTask timerTask;
	final Handler timerHandler = new Handler();

	private NurApiAutoConnectTransport mAcTr;

	public static final String KEYFILE_PREFNAME = "TAM1_KEYFILE";
	public static final String KEYNUMBER_PREFNAME = "TAM1_KEYNUMBER";

	private static Main gInstance;

    public void toggleScreenRotation(boolean enable){
        setRequestedOrientation((enable) ? ActivityInfo.SCREEN_ORIENTATION_SENSOR : ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    }

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
				timerHandler.post(new Runnable() {
					@Override
					public void run() {
						updateStatus();
					}
				});
			}
		};
	}

	public static Main getInstance()
	{
		return gInstance;
	}

	public void handleKeyFile()
	{
		Intent intent;
		Intent chooser;

		intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");

		chooser = Intent.createChooser(intent, "Select file");

		try {
			startActivityForResult(chooser, REQ_FILE_OPEN);
		} catch (Exception ex) {
			String strErr = ex.getMessage();
			Toast.makeText(this, "Error:\n" + strErr, Toast.LENGTH_SHORT).show();
		}
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

	public void saveKeyFilename(String fileName)
	{
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		editor.putString(KEYFILE_PREFNAME, fileName);

		editor.commit();
	}

	public String getKeyFileName()
	{
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		String keyFileName = pref.getString(KEYFILE_PREFNAME, "");

		return keyFileName;
	}

	public void saveUsedKeyNumber(int keyNumber)
	{
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();

		editor.putInt(KEYNUMBER_PREFNAME, keyNumber);

		editor.commit();
	}

	public int getUsedKeyNumber()
	{
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		int keyNumber = pref.getInt(KEYNUMBER_PREFNAME, -1);

		return keyNumber;
	}

	void loadSettings() {
		SharedPreferences pref = getSharedPreferences("DemoApp", Context.MODE_PRIVATE);
		String type = pref.getString("connType", "");

        /* Get rotation setting enable / disable rotation sensors */
        toggleScreenRotation(pref.getBoolean("Srotation",false));
        Beeper.setEnabled(pref.getBoolean("Sounds",true));

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

		Beeper.init();

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

	// Visible app choices / not.
	public void syncViewContents()
	{
		super.onResume();
	}

	private boolean mAuthAppAdded = false;

	@Override
	public void onCreateSubApps(SubAppList subAppList) {
		gInstance = this;
		NurApi theApi = getNurApi();
		
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
		subAppList.addSubApp(new BarcodeApp());

		/* Authentication application. */
		subAppList.addSubApp(new AuthenticationAppTabbed());

		/* Test mode application. */
		subAppList.addSubApp(new TestModeApp());

        /* Reader settings application. */
        subAppList.addSubApp(new SettingsAppTabbed());

		theApi.setLogLevel(NurApi.LOG_ERROR);
		
		setAppListener(new NurApiListener() {
			@Override
			public void disconnectedEvent() {

				if (exitingApplication())
					return;

				updateStatus();
				Toast.makeText(Main.this, getString(R.string.reader_disconnected), Toast.LENGTH_SHORT).show();

				getSubAppList().getApp("Barcode").setIsVisibleInMenu(false);
			}
			@Override
			public void connectedEvent() {
				updateStatus();
				Toast.makeText(Main.this, getString(R.string.reader_connected), Toast.LENGTH_SHORT).show();

				// Show barcode app only for accessory devices
				NurAccessoryExtension ext = new NurAccessoryExtension(getNurApi());
				getSubAppList().getApp("Barcode").setIsVisibleInMenu(ext.isSupported());
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

		((TextView)findViewById(R.id.app_statustext)).setOnClickListener(mStatusBarOnClick);
	}

	int testmodeClickCount = 0;
	long testmodeClickTime = 0;

	View.OnClickListener mStatusBarOnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (testmodeClickCount < 10) {
				if (testmodeClickTime != 0 && SystemClock.uptimeMillis() - testmodeClickTime > 5000) {
					testmodeClickCount = 0;
				}
				testmodeClickTime = SystemClock.uptimeMillis();
				testmodeClickCount++;

				if (testmodeClickCount == 10) {
					Toast.makeText(Main.this, "Test Mode enabled", Toast.LENGTH_SHORT).show();
					getSubAppList().getApp("Test Mode").setIsVisibleInMenu(true);
				}
			}
		}
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		((TextView)findViewById(R.id.app_statustext)).setOnClickListener(mStatusBarOnClick);
	}

	@Override
	public void onCreateDrawerItems(Drawer drawer) {
		drawer.addTitle("Connection");
        drawer.addTitle("About");
		drawer.addTitle("Contact");
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

	// Parse the URI the get the actual file name.
	private String getActualFileName(String strUri) {
		String strFileName = null;
		Uri uri;
		String scheme;

		uri = Uri.parse(strUri);
		scheme = uri.getScheme();

		if (scheme.equalsIgnoreCase("content")) {
			String primStr;
			primStr = uri.getLastPathSegment().replace("primary:", "");
			strFileName = Environment.getExternalStorageDirectory() + "/" + primStr;
		}

		return strFileName;
	}
	
	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

			case REQ_FILE_OPEN:
			{
				if (data != null) {
					String fullPath;

					fullPath = getActualFileName(data.getDataString());

					if (fullPath == null)
						Toast.makeText(Main.this, "No file selected.", Toast.LENGTH_SHORT).show();
					else {
						saveKeyFilename(fullPath);

						SettingsAppAuthTab authTab = SettingsAppAuthTab.getInstance();

						if (authTab != null)
							authTab.updateViews();
					}
				}
			}
			break;

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

    void handleContactClick() {
        final View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        AlertDialog.Builder builder = new Builder(this);
        builder.setView(dialogLayout);
        builder.show();
    }

	@Override
	public void onDrawerItemClick(AdapterView<?> parent, View view, int position, long id) 
	{
		switch (position) {
            case 0:
                handleConnectionClick();
                break;
            case 1:
                handleAboutClick();
                break;
            case 2:
                handleContactClick();
                break;
            default:
                break;
		}
	}
}
