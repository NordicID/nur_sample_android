package com.nordicid.rfiddemo;

import java.util.HashMap;

import com.nordicid.apptemplate.SubApp;
import com.nordicid.controllers.InventoryController;
import com.nordicid.controllers.InventoryController.InventoryControllerListener;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurTag;
import com.nordicid.nurapi.NurTagStorage;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class InventoryApp extends SubApp {

    private TextView mInventoryCountTextView;
    private TextView mInventoryTagsInTime;
    private TextView mInventoryMaxTagsPerSecond;
    private TextView mInventoryAvgTagPerSecond;
    private TextView mInventoryTagsPerSecond;
	private ListView mFoundTagsListView;
	private SimpleAdapter mFoundTagsListViewAdapter;
	private Button mStartStopInventory;
	private View mView;

	long mLastUpdateTagCount = 0;
	Handler mHandler;

	private InventoryController mInventoryController;

	@Override
	public NurApiListener getNurApiListener()
	{		
		return mInventoryController.getNurApiListener();	
	}

	public InventoryApp() {
		super();
		mHandler = new Handler(Looper.getMainLooper());
		mInventoryController = new InventoryController(getNurApi());
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_inventory;
	}
	
	@Override
	public String getAppName() {
		return "Inventory";
	}
	
	@Override
	public int getLayout() {
		return R.layout.app_inventory;
	}
	
	Runnable mTimeUpdate = new Runnable() {
		@Override
		public void run() {

			updateStats(mInventoryController);
			
			if (mLastUpdateTagCount != mInventoryController.getTagStorage().size())
				mFoundTagsListViewAdapter.notifyDataSetChanged();
			
			mLastUpdateTagCount = mInventoryController.getTagStorage().size();
			
			if (mInventoryController.isInventoryRunning())
				mHandler.postDelayed(mTimeUpdate, 250);				
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        mInventoryController.setListener(new InventoryControllerListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void tagFound(NurTag tag, boolean isNew) { }

			@Override
			public void inventoryRoundDone(NurTagStorage storage, int newTagsOffset, int newTagsAdded) { }

			@Override
			public void readerDisconnected() {
				mStartStopInventory.setText(getString(R.string.start));
			}

			@Override
			public void readerConnected() { }

			@Override
			public void inventoryStateChanged() {
				if (mInventoryController.isInventoryRunning()) {
					keepScreenOn(true);
					mStartStopInventory.setText(getString(R.string.stop));
					clearReadings();
					mHandler.postDelayed(mTimeUpdate, 250);
				}
				else {
					keepScreenOn(false);
					mStartStopInventory.setText(getString(R.string.start));
				}
			}

			@Override
			public void IOChangeEvent(NurEventIOChange event) {
				// Handle BLE trigger
				if (event.source == NurAccessoryExtension.TRIGGER_SOURCE && event.direction == 0)
				{
					if (mInventoryController.isInventoryRunning())
						stopInventory();
					else {
						startInventory();
					}
				}
			}

		});
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
	
	private void clearReadings() {
		mInventoryController.clearInventoryReadings();
		mFoundTagsListViewAdapter.notifyDataSetChanged();
		mLastUpdateTagCount = 0;
		updateStats(mInventoryController);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mView = view;
		
		//Start/stop button
		mStartStopInventory = addButtonBarButton(getString(R.string.start), new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mInventoryController.isInventoryRunning()) {
					stopInventory();
				}
				else {
					startInventory();
				}
			}
		});
		
		if (mInventoryController.isInventoryRunning()) {
			mStartStopInventory.setText(getString(R.string.stop));
		}
			
		// Clear button and alertdialog
		addButtonBarButton(getString(R.string.clear), new OnClickListener(){
			@Override
			public void onClick(View v) {
				clearReadings();
			}
		});
		
		// statistics UI
        mInventoryCountTextView = (TextView) mView.findViewById(R.id.num_of_tags_textview);
        mInventoryAvgTagPerSecond = (TextView) mView.findViewById(R.id.average_tags_per_second_textview);
        mInventoryTagsInTime = (TextView) mView.findViewById(R.id.tags_in_time_textview);
        mInventoryMaxTagsPerSecond = (TextView) mView.findViewById(R.id.max_tags_per_second);
        mInventoryTagsPerSecond = (TextView) mView.findViewById(R.id.tags_per_second_textview);

		mFoundTagsListView = (ListView) mView.findViewById(R.id.tags_listview);

		//sets simple adapter for foundtags list
		mFoundTagsListViewAdapter = new SimpleAdapter(
											getActivity(), 
											mInventoryController.getListViewAdapterData(),
											R.layout.taglist_row,
											new String[] { "epc" },
											new int[] { R.id.tagText });
		
		//empty view when no tags in list
		mFoundTagsListView.setEmptyView(mView.findViewById(R.id.no_tags));
		mFoundTagsListView.setAdapter(mFoundTagsListViewAdapter);
		mFoundTagsListView.setCacheColorHint(0);
		mFoundTagsListView.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				//if tag clicked
				HashMap<String, String> selectedTagData =(HashMap<String, String>) mFoundTagsListView.getItemAtPosition(position);
				mInventoryController.showTagDialog(getActivity(), selectedTagData);
			}
			
		});

		updateStats(mInventoryController);
	}
	
	public void startInventory() {
		try {
			if (!mInventoryController.startContinuousInventory()) {
				Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
			}
		} catch (Exception e)
		{
			Toast.makeText(getActivity(), getString(R.string.reader_error), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void stopInventory() {
		mInventoryController.stopInventory();
	}

	//if back pressed when fragment is active
	@Override
	public boolean onFragmentBackPressed() {
		
		// if inventory running, stop it and return true to indicate AppTemplate we've handled back press
		if (mInventoryController.isInventoryRunning()) {
			stopInventory();
			return true;
		}

		// Return false to let AppTemplate to handle back press
		return false;
	}
	
	//When inventory is running keep screen on
	private void keepScreenOn(boolean value) {
		mView.setKeepScreenOn(value);
	}
	
	//if app pauses stop the inventory.
	@Override
	public void onPause() {
		super.onPause();
		
		if (getAppTemplate().isRecentConfigurationChange() == false)
		{
			if (mInventoryController.isInventoryRunning()) {
				stopInventory();
			}
		}
	}
}
