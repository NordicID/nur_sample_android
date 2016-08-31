package com.nordicid.rfiddemo;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubAppTabbed;
import com.nordicid.controllers.AuthenticationController;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiListener;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class AuthenticationAppTabbed extends SubAppTabbed {

	/** Requesting file for uploading. */
	private static final int REQ_FILE_OPEN = 1;

	private Button mStartStopBtn;
	private Button mReadKeysBtn;
	private Button mClearBtn;

	private AuthenticationTab mAuthTab;
	private AuthenticationAppFoundTab mOkTagsTab;
	private AuthenticationAppFoundTab mFailedTagsTab;

	// private NurApiListener mThisClassListener = null;

	private Handler mHandler = null;
	private AuthenticationController mAuthController = null;
	private AuthenticationController.AuthenticationControllerListener mAuthListener = null;

	boolean mIsActive = false;
	private View mView;

	@Override
	public NurApiListener getNurApiListener()
	{
		if (mAuthController != null)
			mAuthController.getNurApiListener();
		return null;
	}

	public AuthenticationAppTabbed(Context c, AppTemplate t, NurApi na) {
		super(c, t, na);

		mHandler = new Handler(Looper.getMainLooper());
		mAuthController = new AuthenticationController(na);

		mAuthListener = new AuthenticationController.AuthenticationControllerListener() {
			@Override
			public void onNewOkTags(ArrayList<byte[]> okTagList) {

			}

			@Override
			public void onNewFailedTags(ArrayList<byte[]> okTagList) {

			}

			@Override
			public void readerDisconnected() {

			}

			@Override
			public void readerConnected() {

			}

			@Override
			public void authenticationStateChanged(boolean executing) {

			}
		};

		mAuthController.setListener(mAuthListener);
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
		mView.setKeepScreenOn(value);
	}

	private void handleStartStop() {

	}
	
	private void handleKeyRead() {

	}

	private void handleStorageClear() {

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

	private void tryGetFile() {
		Intent intent;
		Intent chooser;

		// mSelectingFile =  true;

		intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");

		chooser = Intent.createChooser(intent, "Select file");

		try {
			// mSelectingFile = true;
			startActivityForResult(chooser, REQ_FILE_OPEN);
		} catch (Exception ex) {
			String strErr = ex.getMessage();
			// shortToast("Error:\n" + strErr);
			// mSelectingFile = false;
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_FILE_OPEN) {
			if (data != null) {
				String fullPath;

				fullPath = getActualFileName(data.getDataString());

				if (fullPath == null)
					Toast.makeText(getActivity(), "No file selected.", Toast.LENGTH_SHORT).show();
				else if (!mAuthController.readKeysFromFile(fullPath)){
					Toast.makeText(getActivity(), "Error when reading the keys.", Toast.LENGTH_SHORT).show();
				}
				else {
					Toast.makeText(getActivity(), "Total of " + mAuthController.getTotalKeyCount() + " keys were read.", Toast.LENGTH_SHORT).show();
				}
			}
		}
	}

	@Override
	protected int onGetFragments(ArrayList<Fragment> fragments, ArrayList<String> fragmentNames) throws Exception {
		//create instances to fragments and pager.
		mAuthTab = new AuthenticationTab(this);
		mOkTagsTab = new AuthenticationAppFoundTab(this);
		mFailedTagsTab = new AuthenticationAppFoundTab(this);

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
		mStartStopBtn = addButtonBarButton("Start", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleStartStop();
			}
		});
		mReadKeysBtn = addButtonBarButton("Read keys", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleKeyRead();
			}
		});

		mClearBtn  = addButtonBarButton("Clear tags", new OnClickListener() {
			@Override
			public void onClick(View v) {
				handleStorageClear();
			}
		});

		super.onViewCreated(view, savedInstanceState);
	}

	private void stopAuthentication()
	{
		if (mAuthController != null)
			mAuthController.stopAuthentication();
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
