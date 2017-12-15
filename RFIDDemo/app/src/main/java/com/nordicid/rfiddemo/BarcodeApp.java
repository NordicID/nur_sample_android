package com.nordicid.rfiddemo;

import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Object;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.nordicid.apptemplate.SubApp;
import com.nordicid.nuraccessory.AccessoryBarcodeResult;
import com.nordicid.nuraccessory.AccessoryBarcodeResultListener;
import com.nordicid.nuraccessory.NurAccessoryConfig;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiErrors;
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

import android.content.Context;
import android.content.ContentResolver;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.database.Cursor;
import android.util.Log;


public class BarcodeApp extends SubApp {

	private NurApiListener mThisClassListener = null;
	private AccessoryBarcodeResultListener mResultListener;

	private NurAccessoryExtension mAccessoryExt;
	private boolean mIsBle = false;
	private NurAccessoryConfig mBleCfg = null;

	private EditText mEditText;
	private Button mTriggerBtn = null;
	private Button mSendCfgBtn = null;


	@Override
	public NurApiListener getNurApiListener() {
		return mThisClassListener;
	}

	public BarcodeApp() {
		super();

		mAccessoryExt = getAppTemplate().getAccessoryApi();

		mResultListener = new AccessoryBarcodeResultListener() {
			@Override
			public void onBarcodeResult(AccessoryBarcodeResult result) {

				if (!mIsActive)
					return;

				getAppTemplate().setEnableBattUpdate(true);

				if (result.status == NurApiErrors.NO_TAG) {
					mText = "No barcode found";
					Beeper.beep(Beeper.FAIL);
				}
				else if (result.status == NurApiErrors.NOT_READY) {
					if (!mCancelRequested)
						mIgnoreNextTrigger = true;
					mCancelRequested = false;
					mText = "Cancelled";
				}
				else if (result.status == NurApiErrors.HW_MISMATCH) {
					mText = "No hardware found";
					Beeper.beep(Beeper.FAIL);
				}
				else if (result.status != NurApiErrors.NUR_SUCCESS) {
					mText = "Error: " + result.status;
					Beeper.beep(Beeper.FAIL);
				}
				else {
					mText = result.strBarcode;
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Activity.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Barcode", result.strBarcode);
                    clipboard.setPrimaryClip(clip);
					Beeper.beep(Beeper.BEEP_100MS);
				}
				updateText();
				mIsActive = false;
				ChangeTriggerText(false);
			}
		};

		mThisClassListener = new NurApiListener() {
			@Override
			public void connectedEvent() {
				testBleReader();
			}

			@Override
			public void disconnectedEvent() { }
			@Override
			public void logEvent(int level, String txt) { }
			@Override
			public void bootEvent(String event) { }
			@Override
			public void inventoryStreamEvent(NurEventInventory event) { }

			@Override
			public void IOChangeEvent(NurEventIOChange event) {
				if (event.source == NurAccessoryExtension.TRIGGER_SOURCE)
					bleTrigger(event.direction);
			}

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
		return "Barcode";
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_barcode;
	}

	@Override
	public int getLayout() {
		return R.layout.app_barcode;
	}

	@Override
	public void onVisibility(boolean val) {
		if (val && getNurApi().isConnected()) {
			testBleReader();
		}
		else if (!val)
		{
			if (mIsActive) {
				mIsActive = false;
				try {
					mAccessoryExt.cancelBarcodeAsync();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			getAppTemplate().setEnableBattUpdate(true);
		}
	}

	private void testBleReader() {
		mIsBle = getAppTemplate().getAccessorySupported();
		mBleCfg = null;
		if (mIsBle) {
			try {
				mBleCfg = mAccessoryExt.getConfig();
			} catch (Exception e) {
			}
		}
	}

	String mText = "";

	void updateText() {
		getAppTemplate().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mEditText.setText(mText);
			}
		});
	}

	void ChangeTriggerText(final boolean state)
	{
		getAppTemplate().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(mTriggerBtn != null)
					mTriggerBtn.setText(getString((state) ? R.string.stop : R.string.start));
			}
		});
	}

	boolean mIsActive = false;

	private void handleTrigger() {
		if (!getNurApi().isConnected()) {
			Toast.makeText(getActivity(), "Reader not connected", Toast.LENGTH_SHORT).show();
			return;
		} else if (!mIsBle) {
			Toast.makeText(getActivity(), "Reader not supported", Toast.LENGTH_SHORT).show();
			return;
		} else if (mBleCfg != null && (mBleCfg.getHidBarCode() || mBleCfg.getHidRFID())) {
			Toast.makeText(getActivity(), "Invalid reader config. Disable HID mode in settings", Toast.LENGTH_SHORT)
					.show();
			return;
		}

		if (mIsActive) {
			try {
				mAccessoryExt.imagerAIM(false);
				mAccessoryExt.cancelBarcodeAsync();
				mCancelRequested = true;
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			mText = "Cancelled";
			Log.e("0","TRG =" + mText);
			ChangeTriggerText(false);
			updateText();
			mIsActive = false;
			return;
		}

		mEditText.setText("Scan barcode..");
		mText = "";

		try {
			getAppTemplate().setEnableBattUpdate(false);
			mAccessoryExt.imagerAIM(false);
			mAccessoryExt.readBarcodeAsync(5000);
			ChangeTriggerText(true);
			mIsActive = true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			mEditText.setText("Could not start scanner!");

			mText = "";
		}
	}

	private void handleSetConfiguration()
	{
		Intent intent;
		Intent filePicker;

		intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("text/plain");

		filePicker = Intent.createChooser(intent, getResources().getString(R.string.file_picker));

		try {
			Main.getInstance().setDoNotDisconnectOnStop(true);

			startActivityForResult(filePicker, 42);
		} catch (Exception ex) {

			String strErr = ex.getMessage();
			Toast.makeText(getActivity(), "Error:\n" + strErr, Toast.LENGTH_SHORT).show();
			Main.getInstance().setDoNotDisconnectOnStop(false);
		}

	}

	boolean mIgnoreNextTrigger = false;
	boolean mCancelRequested = false;

	private void bleTrigger(int dir)
	{
		boolean aim=false;

		Log.e("0","TRG dir=" + String.valueOf(dir));

		if (dir == 0)
		{
			if (!mIgnoreNextTrigger)
				handleTrigger();
			mIgnoreNextTrigger = false;
		}
		else if(dir == 1)
		{
			if(mIsActive) aim = false;
			else aim = true;

			try
			{
				mAccessoryExt.imagerAIM(aim);
			}
			catch (Exception e)
			{
				mEditText.setText("Could not set aimer!");
				mText = "";
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 42 && resultCode == Activity.RESULT_OK)
		{
			super.onActivityResult(requestCode, resultCode, data);
			if (data != null)
			{
				Uri uri = data.getData();
				handleFileSelection(uri);
			}
		}
	}

	private void handleFileSelection(Uri uri)
	{
		BufferedReader br;
		int succes_cnt=0;
		int nack_cnt = 0;

		try {
			br = new BufferedReader(new InputStreamReader(getActivity().getContentResolver().openInputStream(uri)));

			String line = null;
			while ((line = br.readLine()) != null)
			{
				Log.e("0","IMG cfg=" + line);
				try
				{
					byte arr[] = mAccessoryExt.imagerCmd(line, 0);
					if(arr == null)
					{
						//mEditText.setText("Config failed! (invalid config string)");
						//Toast.makeText(getActivity(), "Config line not valid", Toast.LENGTH_SHORT).show();
						//Not valid config line. Take next
						continue;
					}
					if(arr[0] == 21)
					{
						nack_cnt++;
					}
					else if(arr[0] == 6)
					{
						//mEditText.setText("Config success!");
						succes_cnt++;
					}
					/*
					else
					{
						mEditText.setText("Config failed! " + String.valueOf(arr[0]));

						for (int x = 0; x < arr.length; x++) {
							Log.e("0", "Line [" + String.valueOf(x) + "]=" + String.valueOf(arr[x]));
						}
					}
					*/
				}
				catch (Exception e)
				{
					Log.e("0","IMG err=" + e.getMessage());
					mEditText.setText(e.getMessage());
					//break;
				}


			}
			br.close();

			if(succes_cnt>0)
			{
				//Save codes to Imager flash(Opticon)
				line = "@MENU_OPTO@ZZ@Z2@ZZ@OTPO_UNEM@";
				try {
					byte rsp[] = mAccessoryExt.imagerCmd(line, 0);
					if (rsp == null) {
						mEditText.setText("Saving configuration failed! (no response)");
					} else if (rsp[0] == 21) {
						mEditText.setText("Saving configuration failed! (nack)");
					} else if (rsp[0] == 6) {
						if(nack_cnt==0)
							mEditText.setText("Config success!");
						else mEditText.setText("Config success: (" + String.valueOf(succes_cnt)+" rows ) failed: ("+ String.valueOf(nack_cnt)+" rows)");
					} else {
						mEditText.setText("Saving configuration failed! " + String.valueOf(rsp[0]));
						/*
						for (int x = 0; x < arr.length; x++) {
							Log.e("0", "Line [" + String.valueOf(x) + "]=" + String.valueOf(arr[x]));
						}
						*/
					}
				}
				catch (Exception ex)
				{
					Log.e("0","IMG saveerr=" + ex.getMessage());
					mEditText.setText(ex.getMessage());
				}

			}
			else
			{
				if(nack_cnt>0)
					mEditText.setText("Config failed! (Check your config string)");
				else
					mEditText.setText("Valid config string not found!");
				//Toast.makeText(getActivity(), "Config line not valid", Toast.LENGTH_SHORT).show();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		mEditText = (EditText) view.findViewById(R.id.result_text);

		mAccessoryExt.registerBarcodeResultListener(mResultListener);
		mTriggerBtn = addButtonBarButton(getString(R.string.start), new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleTrigger();
			}
		});

		mSendCfgBtn = addButtonBarButton(getString(R.string.set_cfg_file), new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleSetConfiguration();
			}
		});

	}



}
