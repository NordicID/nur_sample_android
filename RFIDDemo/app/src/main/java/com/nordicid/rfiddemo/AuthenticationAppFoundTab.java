package com.nordicid.rfiddemo;

import java.util.HashMap;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class AuthenticationAppFoundTab extends Fragment {

	private AuthenticationAppTabbed mOwner;
	public SimpleAdapter mFoundTagsListViewAdapter;
	private ListView mInventoryTagList;
	private AuthenticationAppTabbed mParent;
	
	public AuthenticationAppFoundTab() {
		mOwner = AuthenticationAppTabbed.getInstance();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_inventory_taglist, container, false);
	}
	
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mInventoryTagList = (ListView) view.findViewById(R.id.tags_listview);
		
		mFoundTagsListViewAdapter = new SimpleAdapter(
				getActivity(),
				InventoryApp.FOUND_TAGS,
				R.layout.taglist_row, 
				new String[] {"epc"}, 
				new int[] {R.id.tagText});


		mInventoryTagList.setEmptyView(view.findViewById(R.id.no_tags));
		mInventoryTagList.setAdapter(mFoundTagsListViewAdapter);
		mInventoryTagList.setCacheColorHint(0);
		mInventoryTagList.setOnItemClickListener(new OnItemClickListener() {
		
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
				@SuppressWarnings("unchecked")
				final HashMap<String, String> selectedTagData = (HashMap<String, String>) mInventoryTagList.getItemAtPosition(position);
				showTagDialog(selectedTagData);
			}
		
		});

		mFoundTagsListViewAdapter.notifyDataSetChanged();
		
	}
	
	private void showTagDialog(final HashMap<String, String> tagData) {
		
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
				mParent.getAppTemplate().setApp("Locate", b);
			}			
		});
		
		dialog.show();
	}
}
