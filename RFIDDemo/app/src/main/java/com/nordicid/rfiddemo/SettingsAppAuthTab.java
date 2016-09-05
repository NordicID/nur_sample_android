package com.nordicid.rfiddemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nordicid.tagauth.ISO29167_10;

public class SettingsAppAuthTab extends Fragment 
{
	SettingsAppTabbed mOwner;
	
	private Button mKeySelBtn;
	private Button mKeyClearBtn;
	private Button mKeyNumBtn;

	private TextView mKeyFileText;
	private TextView mKeyNumText;
	
	private Main mMainInstance;

	boolean mSelectingFile = false;
	
	// For file selection "callback".
	private static SettingsAppAuthTab gInstance;
	public static SettingsAppAuthTab getInstance()
	{
		return gInstance;
	}

	public SettingsAppAuthTab() {
		gInstance = this;
		mOwner = SettingsAppTabbed.getInstance();
		mMainInstance = Main.getInstance();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_settings_auth, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mKeySelBtn = (Button)view.findViewById(R.id.btn_keyload);
		mKeyClearBtn = (Button)view.findViewById(R.id.btn_keyfileclear);
		mKeyNumBtn = (Button)view.findViewById(R.id.btn_keynumsel);

		mKeySelBtn.setOnClickListener(mBtnHandler);
		mKeyClearBtn.setOnClickListener(mBtnHandler);
		mKeyNumBtn.setOnClickListener(mBtnHandler);

		mKeyFileText = (TextView) view.findViewById(R.id.text_cur_keyfile);
		mKeyNumText = (TextView) view.findViewById(R.id.text_cur_keynum);
	}
	
	OnClickListener mBtnHandler = (new OnClickListener() {
			@Override
			public void onClick(View v) {
				int id = v.getId();
				if (id == R.id.btn_keyload)
					handleKeyFile2();
				else if (id == R.id.btn_keyfileclear)
					handleKeyClear();
				else if (id == R.id.btn_keynumsel)
					handleKeyNum();
			}
		});


	void handleKeyFile2()
	{
		mMainInstance.handleKeyFile();
	}

	void handleKeyFile()
	{
		Intent intent;
		Intent chooser;

		intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
		intent.addCategory(Intent.CATEGORY_OPENABLE);
		intent.setType("*/*");

		chooser = Intent.createChooser(intent, "Select file");

		try {
			mSelectingFile = true;
			startActivityForResult(chooser, Main.REQ_FILE_OPEN);
		} catch (Exception ex) {
			String strErr = ex.getMessage();
			Toast.makeText(getActivity(), "Error:\n" + strErr, Toast.LENGTH_SHORT).show();
			mSelectingFile = false;
		}
	}
	
	void handleKeyClear()
	{
		mMainInstance.saveKeyFilename("");
		updateViews();
	}

	void keySelected(int keyNumber)
	{
		mMainInstance.saveUsedKeyNumber(keyNumber);
	}

	void handleKeyNum()
	{
		// TODO
		Context ctx = getActivity();

		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setTitle("Enter key number to use (0...255)");

		// Set up the input
		final EditText input = new EditText(ctx);
		// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		builder.setView(input);

		// Set up the buttons
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int keyNumber = Integer.parseInt(input.getText().toString());

				if (keyNumber < 0 || keyNumber > ISO29167_10.LAST_TAM_KEYNUMBER)
					Toast.makeText(getActivity(), "Invalid key number\nRange is 0..." + ISO29167_10.LAST_TAM_KEYNUMBER, Toast.LENGTH_SHORT).show();
				else {
					keySelected(keyNumber);
				}
				updateViews();
			}
		});
		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				updateViews();
			}
		});

		builder.show();
	}

	public void updateViews()
	{
		String keyFileName;
		int keyNumber;

		keyFileName = mMainInstance.getKeyFileName();

		if (keyFileName.isEmpty())
			mKeyFileText.setText("(none)");
		else
			mKeyFileText.setText(keyFileName);

		keyNumber = mMainInstance.getUsedKeyNumber();
		mKeyNumText.setText("Key number to use: " + keyNumber);
	}

	@Override
	public void onResume() {
		super.onResume();
		updateViews();
	}
}
