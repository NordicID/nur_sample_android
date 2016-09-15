package com.nordicid.rfiddemo;

import com.nordicid.apptemplate.AppTemplate;
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.Toast;

public class BarcodeApp extends SubApp {

	private NurApiListener mThisClassListener = null;
	private AccessoryBarcodeResultListener mResultListener;

	private NurAccessoryExtension mAccessoryExt;
	private boolean mIsBle = false;
	private NurAccessoryConfig mBleCfg = null;

	private EditText mEditText;

	@Override
	public NurApiListener getNurApiListener() {
		return mThisClassListener;
	}

	public BarcodeApp() {
		super();

		mAccessoryExt = new NurAccessoryExtension(getNurApi());

		mResultListener = new AccessoryBarcodeResultListener() {
			@Override
			public void onBarcodeResult(AccessoryBarcodeResult result) {
				if (result.status == NurApiErrors.NO_TAG) {
					mText = "No barcode found";
				}
				else if (result.status != NurApiErrors.NUR_SUCCESS) {
					mText = "Error: " + result.status;
				}
				else {
					mText = result.strBarcode;
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Barcode", result.strBarcode);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getActivity(), "Barcode copied to clipboard", Toast.LENGTH_SHORT).show();
					try {
						// Beep on success
						mAccessoryExt.beepAsync(200);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				updateText();
				mIsActive = false;
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
	}

	private void testBleReader() {
		mIsBle = mAccessoryExt.isSupported();
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
				mAccessoryExt.cancelBarcodeAsync();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			mText = "Cancelled";
			updateText();
			mIsActive = false;
			return;
		}

		mEditText.setText("Scan barcode..");
		mText = "";

		try {
			mAccessoryExt.readBarcodeAsync(5000);
			mIsActive = true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

			mEditText.setText("Could not start scanner!");
			mText = "";
		}
	}

	private void bleTrigger(int dir) {
		if (dir == 0) {
			handleTrigger();
		}
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mEditText = (EditText) view.findViewById(R.id.result_text);

		mAccessoryExt.registerBarcodeResultListener(mResultListener);
		addButtonBarButton("Trigger", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleTrigger();
			}
		});
	}
}
