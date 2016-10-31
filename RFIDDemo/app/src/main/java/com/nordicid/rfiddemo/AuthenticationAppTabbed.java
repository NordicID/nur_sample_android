package com.nordicid.rfiddemo;

import com.nordicid.apptemplate.SubAppTabbed;
import com.nordicid.controllers.AuthenticationController;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurTag;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

public class AuthenticationAppTabbed extends SubAppTabbed {

	private static boolean mSetupQueryPosted = false;

	private Button mStartStopBtn;
	private Button mSetupBtn;
	private Handler mHandler;

	// Main view: processed , OK and failed tags; key set to use text.
	private AuthenticationTab mAuthTab;
	// List for OK and filed tags; use for locating
	private AuthenticationAppFoundTab mOkTagsTab;
	private AuthenticationAppFoundTab mFailedTagsTab;

	// controller does the actual authentication.
	private AuthenticationController mAuthController = null;
	private AuthenticationController.AuthenticationControllerListener mAuthListener = null;

	// This view
	private View mView;

	// Instance of this sub app.
	private static AuthenticationAppTabbed gInstance;
	public static AuthenticationAppTabbed getInstance() { return gInstance; }

	// Tag hashes for adapters.
	private final ArrayList<HashMap<String,String>> mOkTagHash = new ArrayList<HashMap<String,String>>();
	private final ArrayList<HashMap<String,String>> mFailedTagHash= new ArrayList<HashMap<String,String>>();

	@Override
	public NurApiListener getNurApiListener()
	{
		if (mAuthController != null)
			return mAuthController.getNurApiListener();
		return null;
	}

	public AuthenticationAppTabbed() {
		super();

		mHandler = new Handler(Looper.getMainLooper());
		gInstance = this;
		mAuthController = new AuthenticationController(getNurApi());

		mAuthListener = new AuthenticationController.AuthenticationControllerListener() {
			@Override
			public void processedCountChanged(int newCount)
			{
				mAuthTab.updateTotalCount(newCount);
			}

			@Override
			public void onNewOkTag(NurTag newTag) {
				tagFound(newTag, mOkTagHash, true);
				mAuthTab.updateOkCount(mOkTagHash.size());
				mOkTagsTab.updateAll();
			}

			@Override
			public void onNewFailedTag(NurTag newTag) {
				tagFound(newTag, mFailedTagHash, false);
				mAuthTab.updateFailCount(mFailedTagHash.size());
				mFailedTagsTab.updateAll();

			}

			@Override
			public void readerDisconnected() {
				handleReaderDisconnect();
			}

			@Override
			public void readerConnected() {
				handleReaderConnect();
			}

			@Override
			public void authenticationStateChanged(boolean executing, boolean errorOccurred) {
				// Worker thread may call this - in case there was an error that cause the stop.
				threadHandleStateChange(executing, errorOccurred);
			}

			@Override
			public void resetAll()
			{
				mOkTagHash.clear();
				mFailedTagHash.clear();
				mAuthTab.updateTotalCount(0);
				mAuthTab.updateOkCount(0);
				mAuthTab.updateFailCount(0);
			}
		};
	}

	private void threadHandleStateChange(final boolean executing, final boolean errorOccurred)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				if (isVisible()) {
					keepScreenOn(executing);
					if (mStartStopBtn != null)
						mStartStopBtn.setText(executing ? "Stop" : "Start");

					if (errorOccurred)
						Toast.makeText(getActivity(), "Authentication stopped due to an error", Toast.LENGTH_SHORT).show();
				}
			}
		});
	}

	private void handleReaderConnect()
	{
		if (!isVisible())
			return;

		boolean keysWereOk = false;

		mStartStopBtn.setEnabled(true);
		mStartStopBtn.setText("Start");

		try {
			if (mAuthController.getTotalKeyCount() > 0) {
				// This would cause an exception...
				mAuthController.getAuthKeyNumber();
				// ...if not, then enable the start/stop button.
				mStartStopBtn.setEnabled(true);
				keysWereOk = true;
			}
		}
		catch (Exception ex) { }

		if (!keysWereOk)
			postSetupQuery();
	}

	private void handleReaderDisconnect()
	{
		if (!isVisible())
			return;

		mStartStopBtn.setEnabled(false);
		mStartStopBtn.setText("Start");
		// Ask again next time.
		mSetupQueryPosted = false;
	}

	/**
	 * A tag was authenticated or failed to authenticate.
	 * @param tag Tag to process/add.
	 * @param target The target hash where the tag is added.
	 * @param authenticationOk True if the tag authenticated OK.
     */
	private void tagFound(NurTag tag, ArrayList<HashMap<String, String>> target, boolean authenticationOk) {

		HashMap<String, String> tmp;

		tmp = new HashMap<String, String>();
		tmp.put(AuthenticationAppFoundTab.DATA_TAG_EPC, tag.getEpcString());

		if (authenticationOk)
			tmp.put(AuthenticationAppFoundTab.DATA_TAG_AUTH_OK, "YES");
		else
			tmp.put(AuthenticationAppFoundTab.DATA_TAG_AUTH_OK, "NO");
		tag.setUserdata(tmp);

		target.add(tmp);
	}

	@Override
	public String getAppName() {
		return "Authentication";
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_tagauth;
	}

	@Override
	public int getLayout() {
		return R.layout.app_tagauth_tabbed;
	}

	@Override
	public void onVisibility(boolean val) {

	}

	private void keepScreenOn(boolean value) {
		if (mView != null)
			mView.setKeepScreenOn(value);
	}

	private void handleStartStop() {
		if (mAuthController.isAuthenticationRunning()) {
			mAuthController.stopAuthentication();
			mSetupBtn.setEnabled(true);
		}
		else {
			if (!getNurApi().isConnected())
			{
				Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
				return;
			}
			try
			{
				mAuthController.setAuthKeyNumber(Main.getInstance().getUsedKeyNumber());
			}
			catch (Exception ex)
			{
				Toast.makeText(getActivity(), "Authentication key number set error", Toast.LENGTH_SHORT).show();
				return;
			}

			if (!mAuthController.startTAM1Authentication())
				Toast.makeText(getActivity(), "Authentication start error", Toast.LENGTH_SHORT).show();
			else
				mSetupBtn.setEnabled(false);
		}
	}

	private void handleStorageClear() {
		if (mAuthController.isAuthenticationRunning())
			return;

		mAuthController.clearAllTags();
		mOkTagHash.clear();
		mFailedTagHash.clear();
		mOkTagsTab.updateAll();
		mFailedTagsTab.updateAll();
	}

	@Override
	protected int onGetFragments(ArrayList<Fragment> fragments, ArrayList<String> fragmentNames) throws Exception {
		//create instances to fragments and pager.
		mAuthTab = new AuthenticationTab();

		mOkTagsTab = new AuthenticationAppFoundTab();
		mOkTagsTab.setTagHashSource(mOkTagHash, true);

		mFailedTagsTab = new AuthenticationAppFoundTab();
		mFailedTagsTab.setTagHashSource(mFailedTagHash, false);

		fragmentNames.add(getString(R.string.text_authenticating));
		fragments.add(mAuthTab);

		fragmentNames.add(getString(R.string.text_tags_auth_ok));
		fragments.add(mOkTagsTab);

		fragmentNames.add(getString(R.string.text_tags_auth_failed));
		fragments.add(mFailedTagsTab);

		return R.id.auth_pager;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mView = view;

		mAuthController.setListener(mAuthListener);

		mStartStopBtn = addButtonBarButton("Start", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleStartStop();
			}
		});
		addButtonBarButton("Clear tags", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleStorageClear();
			}
		});

		mSetupBtn = addButtonBarButton("Setup", new OnClickListener() {
			@Override
			public void onClick(View v) {
				goToSetup();
			}
		});

		mStartStopBtn.setEnabled(getNurApi().isConnected());

		tryKeySetup();

		super.onViewCreated(view, savedInstanceState);
	}

	private void stopAuthentication()
	{
		if (mAuthController != null)
			mAuthController.stopAuthentication();
	}

	// Set up keys from a file.
	// Format of the file can be found in the AuthenticationController.java.
	private void tryKeySetup()
	{
		String keyFileName;
		int keyNumber = -1;
		int keyCount = 0;
		boolean ok = false;

		mStartStopBtn.setEnabled(false);
		keyFileName = Main.getInstance().getKeyFileName();
		keyNumber = Main.getInstance().getUsedKeyNumber();

		try
		{
			int result;
			result = mAuthController.readKeysFromFile(keyFileName);
			if (result == AuthenticationController.KEYS_OK) {
				keyCount = mAuthController.getTotalKeyCount();
				mAuthController.setAuthKeyNumber(keyNumber);
				ok = true;
			}
		}
		catch (Exception ex) { }

		if (!ok)
			postSetupQuery();
		else {
			// getString(R.string.text_key_set_loaded, keyCount, keyNumber);
			// Toast.makeText(getActivity(), "Loaded " + keyCount + " keys, using set " + keyNumber, Toast.LENGTH_SHORT).show();
			Toast.makeText(getActivity(), String.format(getString(R.string.text_key_set_loaded), keyCount, keyNumber), Toast.LENGTH_SHORT).show();
		}

		mStartStopBtn.setEnabled(ok);
	}

	void postSetupQuery()
	{
		if (mSetupQueryPosted)
			return;

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				querySetupStart();
			}
		});
	}

	private void querySetupStart()
	{
		AlertDialog.Builder builder;
		builder = new AlertDialog.Builder(getActivity())
				.setTitle(R.string.text_auth_keys_missing)
				.setMessage(R.string.text_auth_key_setup_query)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mSetupQueryPosted = true;
						goToSetup();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mSetupQueryPosted = true;
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert);

		builder.show();
	}

	private void goToSetup()
	{
		if (mAuthController.isAuthenticationRunning())
			return;

		SettingsAppTabbed.setPreferredTab("Authentication");
		getAppTemplate().setApp("Settings");
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		stopAuthentication();
	}

	@Override
	public void onStop() {
		super.onStop();
		stopAuthentication();
	}
}
