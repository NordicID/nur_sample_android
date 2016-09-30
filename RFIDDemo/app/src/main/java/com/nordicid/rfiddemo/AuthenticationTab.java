package com.nordicid.rfiddemo;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nordicid.tagauth.ISO29167_10;

public class AuthenticationTab extends Fragment {
	// private AuthenticationAppTabbed mOwner;
	
	private TextView mTagsProcessed;
	private TextView mTagsOk;
	private TextView mTagsFailed;
	private TextView mUsedKeySet;
	private Handler mHandler;

	public AuthenticationTab() {
		// mOwner = AuthenticationAppTabbed.getInstance();
		mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_authenticating, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mTagsProcessed = (TextView) view.findViewById(R.id.num_processed_tags);
		mTagsProcessed.setText("0");

		mTagsOk = (TextView) view.findViewById(R.id.tags_ok_textview);
		mTagsOk.setText("0");

		mTagsFailed = (TextView) view.findViewById(R.id.tags_failed_textview);
		mTagsFailed.setText("0");

		// text_used_key
		mUsedKeySet = (TextView) view.findViewById(R.id.text_used_key);

		updateUsedKey();
	}

	private void updateUsedKey()
	{
		int keyNum = Main.getInstance().getUsedKeyNumber();
		if (keyNum >= 0 && keyNum <= ISO29167_10.LAST_TAM_KEYNUMBER)
			mUsedKeySet.setText("Key set used: " + keyNum);
		else
			mUsedKeySet.setText("Key set used: N/A");
	}

	private void threadUpdateText(final TextView tv, final String text)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				tv.setText(text);
			}
		});
	}

	public void updateTotalCount(int newCount)
	{
		threadUpdateText(mTagsProcessed, Integer.toString(newCount));
	}

	public void updateOkCount(int newCount)
	{
		threadUpdateText(mTagsOk, Integer.toString(newCount));
	}

	public void updateFailCount(int newCount)
	{
		threadUpdateText(mTagsFailed, Integer.toString(newCount));
	}

	@Override
	public void onResume() {
		super.onResume();
		updateUsedKey();
	}
}

