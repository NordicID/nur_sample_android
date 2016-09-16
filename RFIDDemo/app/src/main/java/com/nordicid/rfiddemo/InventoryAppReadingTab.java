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
	private TextView mInventoryTotalTime;
	private TextView mInventoryTagsInTime;
    private TextView mInventoryTagsPerSecond;
    private long lastTagCount = 0;
    private double tagsPerSecond = 0;
    private Handler tagsPerSecondHandler;

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

        mInventoryTagsPerSecond = (TextView) view.findViewById(R.id.tags_per_second_textview);
        mInventoryTagsPerSecond.setText("0");

        tagsPerSecondHandler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{
                    if(InventoryAppTabbed.getInstance().getInventoryController().isInventoryRunning()){
                        tagsPerSecond = (lastTagCount - InventoryAppTabbed.getInstance().oldTagCount);
                        InventoryAppTabbed.getInstance().oldTagCount = lastTagCount;
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
        tagsPerSecondHandler.postDelayed(runnable, 1000);
	}

    public void updateNumTags(long numTags) {
        if (numTags < 0)
            numTags = 0;
        if (lastTagCount != numTags) {
            mInventoryTagsInTime.setText(String.format("%.1f", InventoryAppTabbed.getInstance().getInventoryController().getElapsedSecs()));
            lastTagCount = numTags;
        }
        mInventoryCountTextView.setText(Long.toString(numTags));
        mInventoryTagsPerSecond.setText(String.format("%.2f",tagsPerSecond));
        mInventoryTotalTime.setText(String.format("%.1f", InventoryAppTabbed.getInstance().getInventoryController().getElapsedSecs()));

    }

}
