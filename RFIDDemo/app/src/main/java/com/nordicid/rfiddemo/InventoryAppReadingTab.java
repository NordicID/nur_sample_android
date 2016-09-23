package com.nordicid.rfiddemo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class InventoryAppReadingTab extends Fragment {

    private TextView mInventoryCountTextView;
    private TextView mInventoryAvgTagPerSecond;
    private TextView mInventoryTagsInTime;
    private TextView mInventoryTagsPerSecond;
    private TextView mInventoryMaxTagsPerSecond;
    private Handler tagsPerSecondHandler = null;
    private InventoryAppTabbed inventoryAppTabbed = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        if(tagsPerSecondHandler == null) {
            tagsPerSecondHandler = new Handler();
            tagsPerSecondHandler.postDelayed(tagsPerSecondRunnable, 1000);
        }
		return inflater.inflate(R.layout.tab_inventory_reading, container, false);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

        mInventoryCountTextView = (TextView) view.findViewById(R.id.num_of_tags_textview);
        mInventoryCountTextView.setText("0");

        mInventoryAvgTagPerSecond = (TextView) view.findViewById(R.id.average_tags_per_second_textview);
        mInventoryAvgTagPerSecond.setText("-");

        mInventoryTagsInTime = (TextView) view.findViewById(R.id.tags_in_time_textview);
        mInventoryTagsInTime.setText("0");

        mInventoryMaxTagsPerSecond = (TextView) view.findViewById(R.id.max_tags_per_second);
        mInventoryMaxTagsPerSecond.setText("-");

        mInventoryTagsPerSecond = (TextView) view.findViewById(R.id.tags_per_second_textview);
        mInventoryTagsPerSecond.setText("-");

        inventoryAppTabbed = InventoryAppTabbed.getInstance();
	}

    Runnable tagsPerSecondRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                if(inventoryAppTabbed.getInventoryController().isInventoryRunning()){
                    inventoryAppTabbed.tagsPerSecond = inventoryAppTabbed.getInventoryController().getReadTagsCount();
                    inventoryAppTabbed.mTagsPerSecondSum += inventoryAppTabbed.tagsPerSecond;
                    inventoryAppTabbed.mTagsPerSecondCounter++;
                    inventoryAppTabbed.averageTagsPerSecond = inventoryAppTabbed.mTagsPerSecondSum/inventoryAppTabbed.mTagsPerSecondCounter;
                    if(inventoryAppTabbed.tagsPerSecond > inventoryAppTabbed.maxTagsPerSecond)
                        inventoryAppTabbed.maxTagsPerSecond = inventoryAppTabbed.tagsPerSecond;
                    inventoryAppTabbed.getInventoryController().clearReadTagBuffer();
                }
            }
            catch (Exception e) {
                // TODO: handle exception
            }
            finally{
                //also call the same runnable to call it at regular interval
                tagsPerSecondHandler.postDelayed(this, 1000);
            }
        }
    };

    public void updateNumTags(long numTags) {
        if (numTags < 0)
            numTags = 0;
        if (inventoryAppTabbed.lastTagCount != numTags) {
            mInventoryTagsInTime.setText(String.format("%.1f", InventoryAppTabbed.getInstance().getInventoryController().getElapsedSecs()));
            inventoryAppTabbed.lastTagCount = numTags;
        }
        mInventoryCountTextView.setText(Long.toString(numTags));
        mInventoryTagsPerSecond.setText(String.format("%d", inventoryAppTabbed.tagsPerSecond));
        mInventoryMaxTagsPerSecond.setText(String.format("%d", inventoryAppTabbed.maxTagsPerSecond));
        mInventoryAvgTagPerSecond.setText(String.format("%d", inventoryAppTabbed.averageTagsPerSecond));

    }
}
