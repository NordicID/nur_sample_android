package com.nordicid.rfiddemo;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.Toast;

public class AuthenticationAppFoundTab extends Fragment {

	public static final String DATA_TAG_EPC = "epc";
	public static final String DATA_TAG_AUTH_OK = "authenticated";

	private AuthenticationAppTabbed mOwner;
	public SimpleAdapter mTagLVAdapter;
	private ListView mTagList;

	private boolean mListingOkTags = false;

	private ArrayList<HashMap<String, String>> mTagSourceHash;

	public AuthenticationAppFoundTab() {
		mOwner = AuthenticationAppTabbed.getInstance();
	}

	private Handler mHandler;

	public void setTagHashSource(ArrayList<HashMap<String, String>> newSrc, boolean okTags)
	{
		mTagSourceHash = newSrc;
		mListingOkTags = okTags;
		mHandler = new Handler(Looper.getMainLooper());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_inventory_taglist, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mTagList = (ListView) view.findViewById(R.id.tags_listview);
		
		mTagLVAdapter = new SimpleAdapter(
				getActivity(),
				mTagSourceHash,
				R.layout.taglist_row, 
				new String[] {"epc"}, 
				new int[] {R.id.tagText});


		mTagList.setEmptyView(view.findViewById(R.id.no_tags));
		mTagList.setAdapter(mTagLVAdapter);
		mTagList.setCacheColorHint(0);
		mTagList.setOnItemClickListener(new OnItemClickListener() {
		
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			
				@SuppressWarnings("unchecked")
				final HashMap<String, String> selectedTagData = (HashMap<String, String>) mTagList.getItemAtPosition(position);
				showTagDialog(selectedTagData);
			}
		
		});

		mTagLVAdapter.notifyDataSetChanged();
		
	}

	public void updateAll()
	{
		if (mTagLVAdapter == null)	// Case: not created yet??
		return;

		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mTagLVAdapter.notifyDataSetChanged();
			}
		});
	}
	
	private void showTagDialog(final HashMap<String, String> tagData) {
		
		View tagDialogLayout = getLayoutInflater(null).inflate(R.layout.dialog_tagdata_auth, null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setView(tagDialogLayout);
		
		final TextView epcTextView = (TextView) tagDialogLayout.findViewById(R.id.auth_tag_epc);
		epcTextView.setText(getString(R.string.dialog_epc)+" "+tagData.get(DATA_TAG_EPC));

		final TextView authStatusTextView = (TextView) tagDialogLayout.findViewById(R.id.auth_tag_auth_ok);
		if (tagData.get(DATA_TAG_AUTH_OK).equalsIgnoreCase("yes")) {
			authStatusTextView.setText(getString(R.string.dialog_auth_ok));
			authStatusTextView.setTextColor(Color.GREEN);
		}
		else {
			authStatusTextView.setText(getString(R.string.dialog_auth_fail));
			authStatusTextView.setTextColor(Color.RED);
		}
		
		final AlertDialog dialog = builder.create();


		final Button tam2Button = (Button) tagDialogLayout.findViewById(R.id.auth_tag_tam2_button);
		tam2Button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(getActivity(), getString(R.string.text_not_implemented), Toast.LENGTH_SHORT).show();
				dialog.dismiss();
			}
		});

		final Button closeDialog = (Button) tagDialogLayout.findViewById(R.id.auth_tag_close_button);
		closeDialog.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
			
		});
	
		final Button locateTag = (Button) tagDialogLayout.findViewById(R.id.auth_tag_locate_button);
		locateTag.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Bundle b = new Bundle();

				dialog.dismiss();

				TraceApp.setStartParams(tagData.get(DATA_TAG_EPC), true);
				mOwner.getAppTemplate().setApp("Locate");
			}			
		});

		dialog.show();
	}
}
