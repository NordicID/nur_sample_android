package com.nordicid.rfiddemo;

import java.util.ArrayList;

import com.nordicid.apptemplate.SubAppTabbed;
import com.nordicid.controllers.InventoryController;
import com.nordicid.controllers.InventoryController.InventoryControllerListener;
import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.*;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/*The host that hostes InventoryAppReadingTab and InventoryAppFoundTab cause
 * by default SubApp defines the main/host fragment to retain its information.
 * 
 */
public class InventoryAppTabbed extends SubAppTabbed {

	private Button mStartStopInventory;
	
	private InventoryAppReadingTab mReadingTab;
	private InventoryAppFoundTab mFoundTab;

	private InventoryController mInventoryController;
	
	long mLastUpdateTagCount = 0;

	Handler mHandler;

	private static InventoryAppTabbed gInstance = null;
	public static InventoryAppTabbed getInstance()
	{
		return gInstance;
	}
	
	public InventoryController getInventoryController()
	{		
		return mInventoryController;	
	}

	@Override
	public NurApiListener getNurApiListener()
	{		
		return mInventoryController.getNurApiListener();	
	}
	
	public InventoryAppTabbed() {
		super();
		gInstance = this;
		mHandler = new Handler(Looper.getMainLooper());
		mInventoryController = new InventoryController(getNurApi());
	}

	@Override
	public void onVisibility(boolean val) {
	}

	@Override
	protected int onGetFragments(ArrayList<Fragment> fragments, ArrayList<String> fragmentNames) throws Exception
	{
		//create instances to fragments and pager.
		mReadingTab = new InventoryAppReadingTab();
		mFoundTab = new InventoryAppFoundTab();
		
		fragmentNames.add("Reading");
		fragments.add(mReadingTab);

		fragmentNames.add("Found");
		fragments.add(mFoundTab);

		return R.id.pager;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	}
	
	//When inventory is running keep screen on
	private void keepScreenOn(boolean value) {
		mView.setKeepScreenOn(value);
	}

	private View mView;
	
	Runnable mTimeUpdate = new Runnable() {
		@Override
		public void run() {
			mReadingTab.updateStats(mInventoryController);
			
			if (mLastUpdateTagCount != mInventoryController.getTagStorage().size())
				mFoundTab.mFoundTagsListViewAdapter.notifyDataSetChanged();
			
			mLastUpdateTagCount = mInventoryController.getTagStorage().size();
			
			if (mInventoryController.isInventoryRunning())
				mHandler.postDelayed(mTimeUpdate, 250);				
		}
	};
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		mView = view;
		
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
			public void readerConnected() {
				mStartStopInventory.setText(getString(R.string.start));
			}

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
		
		//adds button bar button. Buttonbar will be visible in every tab.
		addButtonBarButton(getString(R.string.clear), new OnClickListener() {
			@Override
			public void onClick(View v) {
				clearReadings();
			}
		});

		super.onViewCreated(view, savedInstanceState);
	}
	
	public void stopInventory() {
		mInventoryController.stopInventory();
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
	
	public void clearReadings() {
		mInventoryController.clearInventoryReadings();
		mFoundTab.mFoundTagsListViewAdapter.notifyDataSetChanged();
        mLastUpdateTagCount = 0;
		mReadingTab.updateStats(mInventoryController);
	}

	//main layout
	@Override
	public int getLayout() {
		return R.layout.app_inventory_tabbed;
	}

	@Override
	public String getAppName() {
		return "Inventory";
	}

	@Override
	public int getTileIcon() {
		return R.drawable.ic_inventory;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (mInventoryController.isInventoryRunning()) {
			stopInventory();
		}
	}
	
	@Override
	public void onStop() {
		super.onStop();
		if (mInventoryController.isInventoryRunning()) {
			stopInventory();
		}
	}

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
}
