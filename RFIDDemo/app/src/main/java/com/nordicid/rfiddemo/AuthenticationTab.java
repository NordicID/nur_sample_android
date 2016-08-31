package com.nordicid.rfiddemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AuthenticationTab extends Fragment {

	private AuthenticationAppTabbed mParent;
	
	private TextView mTagsProcessed;
	private TextView mTagsOk;
	private TextView mTagsFailed;

	public AuthenticationTab(AuthenticationAppTabbed parent) {
		mParent = parent;
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
	}
	
	long lastTagCount = 0;
}

