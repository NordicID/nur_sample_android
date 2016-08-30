package com.nordicid.rfiddemo;

import java.util.ArrayList;
import java.util.HashMap;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubApp;
import com.nordicid.controllers.InventoryController;
import com.nordicid.controllers.InventoryController.InventoryControllerListener;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurTag;
import com.nordicid.nurapi.NurTagStorage;

import android.app.AlertDialog;
import android.content.Context;
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
	private TextView mInventoryTotalTime;
	private TextView mInventoryTagsInTime;
	private ListView mFoundTagsListView;
	private SimpleAdapter mFoundTagsListViewAdapter;
	private Button mStartStopInventory;
	private View mView;
	
	private int mNumTags = 0;
	long mLastUpdateTagCount = 0;
	
	Handler mHandler;
	
	private NurTagStorage mTagStorage = new NurTagStorage();
	public static ArrayList<HashMap<String, String>> FOUND_TAGS = new ArrayList<HashMap<String,String>>(); 

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
			updateNumTags(mNumTags);
			
			if (mLastUpdateTagCount != mNumTags)
				mFoundTagsListViewAdapter.notifyDataSetChanged();
			
			mLastUpdateTagCount = mNumTags;
			
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
			public void tagFound(NurTag tag, int roundsDone) {
				
				HashMap<String, String> tmp;
				
				if (mTagStorage.addTag(tag)) {
					
					tmp = new HashMap<String, String>();
					tmp.put("epc", tag.getEpcString());
					tmp.put("rssi", Integer.toString(tag.getRssi()));
					tmp.put("timestamp", Integer.toString(tag.getTimestamp()));
					tmp.put("freq", Integer.toString(tag.getFreq())+" kHz Ch: "+Integer.toString(tag.getChannel()));
					tmp.put("found", "1");
					tmp.put("foundpercent", "100");
					tag.setUserdata(tmp);

					FOUND_TAGS.add(tmp);
					mFoundTagsListViewAdapter.notifyDataSetChanged();
					
					mNumTags++;
					mInventoryCountTextView.setText(Integer.toString(mNumTags));
				}
				else {
					
					tag = mTagStorage.getTag(tag.getEpc());
					
					tmp = (HashMap<String, String>) tag.getUserdata();
					tmp.put("rssi", Integer.toString(tag.getRssi()));
					tmp.put("timestamp", Integer.toString(tag.getTimestamp()));
					tmp.put("freq", Integer.toString(tag.getFreq())+" kHz (Ch: "+Integer.toString(tag.getChannel())+")");
					tmp.put("found", Integer.toString(tag.getUpdateCount()));
					tmp.put("foundpercent", Integer.toString((int) (((double) tag.getUpdateCount()) / (double) roundsDone * 100)));
				}
			}

			@Override
			public void readerDisconnected() {
				mStartStopInventory.setText(getString(R.string.start));
			}

			@Override
			public void readerConnected() {
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
			
		}); 
	} 
	
	long lastTagCount = 0;

	public void updateNumTags(int numTags) {
		if (lastTagCount != numTags) {
			mInventoryTagsInTime.setText(String.format("%.1f", mInventoryController.getElapsedSecs()));
			lastTagCount = numTags;
		}		
		if (numTags < 0)
			numTags = 0;
		mInventoryCountTextView.setText(Integer.toString(numTags));
		mInventoryTotalTime.setText(String.format("%.1f", mInventoryController.getElapsedSecs()));
	}
	
	private void clearReadings() {

		mTagStorage.clear();
		InventoryApp.FOUND_TAGS.clear();
		mFoundTagsListViewAdapter.notifyDataSetChanged();
		mNumTags = 0;	
		mLastUpdateTagCount = -1;
		updateNumTags(-1);
		mInventoryController.clearInventoryReadings();		
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
			
		//Clear button and alertdialog
		addButtonBarButton(getString(R.string.clear), new OnClickListener(){
			@Override
			public void onClick(View v) {
				clearReadings();
			}
		});
		
		//statistics UI
		mInventoryCountTextView = (TextView) mView.findViewById(R.id.num_of_tags_textview);
		mInventoryTotalTime = (TextView) view.findViewById(R.id.tags_total_time_textview);
		mInventoryTotalTime.setText("0");
		mInventoryTagsInTime = (TextView) view.findViewById(R.id.tags_in_time_textview);
		mInventoryTagsInTime.setText("0");
		mFoundTagsListView = (ListView) mView.findViewById(R.id.tags_listview);
	
		//sets simple adapter for foundtags list
		mFoundTagsListViewAdapter = new SimpleAdapter(
											getActivity(), 
											FOUND_TAGS, 
											R.layout.taglist_row,
											new String[] { "epc" },
											new int[] { R.id.tagText });
		
		//empty view when no tags in list
		mFoundTagsListView.setEmptyView(view.findViewById(R.id.no_tags));
		mFoundTagsListView.setAdapter(mFoundTagsListViewAdapter);
		mFoundTagsListView.setCacheColorHint(0);
		mFoundTagsListView.setOnItemClickListener(new OnItemClickListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				
				//if tag clicked
				HashMap<String, String> selectedTagData =(HashMap<String, String>) mFoundTagsListView.getItemAtPosition(position);
				showTagDialog(selectedTagData);
			}
			
		});
		
	}
	
	public void startInventory() {
		if (!mInventoryController.startContinuousInventory()) {
			Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
		}
	}
	
	public void stopInventory() {
		mInventoryController.stopInventory();
	}
	
	private void showTagDialog(final HashMap<String, String> tagData) {
		//shows dialog and the clicked tags information
		View tagDialogLayout = getLayoutInflater(null).inflate(R.layout.dialog_tagdata, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setView(tagDialogLayout);
		
		final TextView epcTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_epc);
		epcTextView.setText(getString(R.string.dialog_epc)+" "+tagData.get("epc"));
		
		final TextView rssiTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_rssi);
		rssiTextView.setText(getString(R.string.dialog_rssi)+" "+tagData.get("rssi"));
		
		final TextView timestampTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_timestamp);
		timestampTextView.setText(getString(R.string.dialog_timestamp)+" "+tagData.get("timestamp"));
		
		final TextView fregTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_freq);
		fregTextView.setText(getString(R.string.dialog_freg)+" "+tagData.get("freq"));
		
		final TextView foundTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_found);
		foundTextView.setText(getString(R.string.dialog_found)+" "+tagData.get("found"));
		
		final TextView foundPercentTextView = (TextView) tagDialogLayout.findViewById(R.id.selected_tag_foundpercent);
		foundPercentTextView.setText(getString(R.string.dialog_found_precent)+" "+tagData.get("foundpercent"));
		
		final AlertDialog dialog = builder.create();
		
		//close button made in "Android L" style. See the layout
		final Button closeDialog = (Button) tagDialogLayout.findViewById(R.id.selected_tag_close_button);
		closeDialog.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
	
		final Button locateTag = (Button) tagDialogLayout.findViewById(R.id.selected_tag_locate_button);
		locateTag.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();
				b.putString("epc", tagData.get("epc"));
				dialog.dismiss();
				getAppTemplate().setApp("Locate", b);
			}
		});
		
		dialog.show();
	}

	//if back pressed when fragment is active
	@Override
	public boolean onFragmentBackPressed() {
		
		//if inventory running close it and return true
		if (mInventoryController.isInventoryRunning()) {
			mInventoryController.stopInventory();
			mStartStopInventory.setText(getString(R.string.start));
			return true;
		}
		else {
			//if not return false and AppTemplate closes the app
			return false;
		}
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
