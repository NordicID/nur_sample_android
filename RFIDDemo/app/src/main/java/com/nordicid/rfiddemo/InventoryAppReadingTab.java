package com.nordicid.rfiddemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class InventoryAppReadingTab extends Fragment {

	private InventoryAppTabbed mParent;
	
	private TextView mInventoryCountTextView;
	private TextView mInventoryTotalTime;
	private TextView mInventoryTagsInTime;

	public InventoryAppReadingTab(InventoryAppTabbed parent) {
		mParent = parent;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_inventory_reading, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mInventoryCountTextView = (TextView) view.findViewById(R.id.num_of_tags_textview);
		mInventoryCountTextView.setText("0");
		
		mInventoryTotalTime = (TextView) view.findViewById(R.id.tags_total_time_textview);
		mInventoryTotalTime.setText("0");
		
		mInventoryTagsInTime = (TextView) view.findViewById(R.id.tags_in_time_textview);
		mInventoryTagsInTime.setText("0");
	}
	
	long lastTagCount = 0;

	public void updateNumTags(long numTags) {
		if (lastTagCount != numTags) {
			mInventoryTagsInTime.setText(String.format("%.1f", mParent.getInventoryController().getElapsedSecs()));
			lastTagCount = numTags;
		}		
		if (numTags < 0)
			numTags = 0;
		mInventoryCountTextView.setText(Long.toString(numTags));
		mInventoryTotalTime.setText(String.format("%.1f", mParent.getInventoryController().getElapsedSecs()));
	}	
	
}
