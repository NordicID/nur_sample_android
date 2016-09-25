package com.nordicid.rfiddemo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nordicid.controllers.InventoryController;

public class InventoryAppReadingTab extends Fragment {

    private TextView mInventoryCountTextView;
    private TextView mInventoryAvgTagPerSecond;
    private TextView mInventoryTagsInTime;
    private TextView mInventoryTagsPerSecond;
    private TextView mInventoryMaxTagsPerSecond;
    private InventoryAppTabbed inventoryAppTabbed = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_inventory_reading, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        mInventoryCountTextView = (TextView) view.findViewById(R.id.num_of_tags_textview);
        mInventoryAvgTagPerSecond = (TextView) view.findViewById(R.id.average_tags_per_second_textview);
        mInventoryTagsInTime = (TextView) view.findViewById(R.id.tags_in_time_textview);
        mInventoryMaxTagsPerSecond = (TextView) view.findViewById(R.id.max_tags_per_second);
        mInventoryTagsPerSecond = (TextView) view.findViewById(R.id.tags_per_second_textview);

        inventoryAppTabbed = InventoryAppTabbed.getInstance();

        updateStats(inventoryAppTabbed.getInventoryController());
	}

    public void updateStats(InventoryController invCtl)
    {
        InventoryController.Stats stats = invCtl.getStats();

        mInventoryTagsInTime.setText(String.format("%.1f", stats.getTagsFoundInTimeSecs()));
        mInventoryTagsPerSecond.setText(String.format("%.1f", stats.getTagsPerSec()));
        mInventoryCountTextView.setText(Long.toString(invCtl.getTagStorage().size()));
        mInventoryMaxTagsPerSecond.setText(String.format("%.1f", stats.getMaxTagsPerSec()));
        mInventoryAvgTagPerSecond.setText(String.format("%.1f", stats.getAvgTagsPerSec()));
    }
}
