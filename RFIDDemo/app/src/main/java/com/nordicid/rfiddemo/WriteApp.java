package com.nordicid.rfiddemo;

import java.util.HashMap;

import com.nordicid.apptemplate.SubApp;
import com.nordicid.controllers.InventoryController;
import com.nordicid.controllers.TagWriteController;
import com.nordicid.controllers.TagWriteController.WriteTagControllerListener;
import com.nordicid.nurapi.NurApi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

public class WriteApp extends SubApp {

	private ListView mTagsListView;
	private RelativeLayout mEmptyView;
	
	private SimpleAdapter mTagsListViewAdapter;

	private boolean newEpcOK;
	
	private TagWriteController mTagWriteController;
	private InventoryController mInventoryController;

    private Button mRefreshButton = null;
	
	public WriteApp() {
		super();
		mTagWriteController = new TagWriteController(getNurApi());
		mInventoryController = new InventoryController(getNurApi());
	}

	@Override
	public String getAppName() {
		return "Write";
	}
	
	@Override
	public int getTileIcon() {
		return R.drawable.ic_write;
	}
	
	
	@Override
	public int getLayout() {
		return R.layout.app_write;
	}

	@Override
	public void onViewCreated(View view, Bundle  savedInstanceState) {
		
		mTagWriteController.setListener(new WriteTagControllerListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void readerDisconnected() {
			}

			@Override
			public void readerConnected() {
			}
			
		});
		
		mTagsListView = (ListView) view.findViewById(R.id.tags_listview);
		mEmptyView = (RelativeLayout) view.findViewById(R.id.listview_empty);
		
		//sets the adapter for listview
		mTagsListViewAdapter = new SimpleAdapter(getActivity(), mInventoryController.getListViewAdapterData(), R.layout.taglist_row, new String[] {"epc"}, new int[] {R.id.tagText});
		mTagsListView.setAdapter(mTagsListViewAdapter);
		mTagsListView.setEmptyView(mEmptyView);
		mTagsListView.setCacheColorHint(0);
		mTagsListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				@SuppressWarnings("unchecked")
				final HashMap<String, String> mSelectedTag = (HashMap<String,String>) mTagsListView.getItemAtPosition(position);
				showWriteDialog(mSelectedTag);
				
			}
		});

        mRefreshButton = addButtonBarButton(getString(R.string.refresh_list), new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					if (!mInventoryController.doSingleInventory()) {
						Toast.makeText(getActivity(), getString(R.string.reader_connection_error), Toast.LENGTH_SHORT).show();
					}
					else if (mInventoryController.getTagStorage().size() == 0) {
						Toast.makeText(getActivity(), "No tags found", Toast.LENGTH_SHORT).show();
					}

				} catch (Exception e) {
					Toast.makeText(getActivity(), getString(R.string.reader_error), Toast.LENGTH_SHORT).show();
				}
				mTagsListViewAdapter.notifyDataSetChanged();
			}
		});
	}
	
	//builds and shows the dialog
	private void showWriteDialog(HashMap<String,String> tag) {
		
		View dialogLayout = getLayoutInflater(null).inflate(R.layout.dialog_write,null);
		final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		
		builder.setView(dialogLayout);
		
		String currentEpc = tag.get("epc");
		
		final TextView currentEpcTextView = (TextView) dialogLayout.findViewById(R.id.write_dialog_current);
		currentEpcTextView.setText(currentEpc);
		
		final EditText newEpcEditText = (EditText) dialogLayout.findViewById(R.id.write_dialog_new);
		newEpcEditText.setText(currentEpc);
		
		newEpcOK = true;
		
		newEpcEditText.addTextChangedListener(new TextWatcher() {

			@Override public void afterTextChanged(Editable s) {
				String tmp = newEpcEditText.getText().toString().replaceAll("[^a-fA-F_0-9]", "");
				
				if (!tmp.equals(newEpcEditText.getText().toString())) {
					newEpcEditText.setText(tmp);
					newEpcEditText.setSelection(newEpcEditText.getText().length());
				}
				
				if (newEpcEditText.getText().toString().length() == 0 || (newEpcEditText.getText().toString().length() % 4) != 0) {
					newEpcEditText.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
					newEpcOK = false;
				}
				else {
					newEpcEditText.setBackgroundColor(getResources().getColor(android.R.color.white));
					newEpcOK = true;
				}
			}
			
			@Override public void beforeTextChanged(CharSequence s, int start,int count, int after) {}
			
			@Override public void onTextChanged(CharSequence s, int start,int before, int count) {
				
			}
			
		});
		
		final AlertDialog dialog = builder.create();
		
		final Button cancelButton = (Button) dialogLayout.findViewById(R.id.dialog_cancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				dialog.dismiss();
			}
			
		});

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mRefreshButton.performClick();
            }
        });
		
		final Button writeButton = (Button) dialogLayout.findViewById(R.id.dialog_write);
		writeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (newEpcOK) {
                    try {
                        byte[] newEpc = NurApi.hexStringToByteArray(newEpcEditText.getText().toString());
                        byte[] currentEpc = NurApi.hexStringToByteArray(currentEpcTextView.getText().toString());
						//Log.e("0","oldLen="+String.valueOf(newEpc.length) + " NewLen=" + String.valueOf(currentEpc.length));
                        boolean succeeded = mTagWriteController.writeTagByEpc(currentEpc, currentEpc.length, newEpc.length, newEpc);

                        if (succeeded) {
                            Toast.makeText(getActivity(), "Tag write succeeded", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            Beeper.beep(Beeper.BEEP_100MS);
                        } else {
                            String error = mTagWriteController.getLastWriteError();
                            String errorMsg;
                            if(error.contains("select"))
                                errorMsg = "Could not find tag";
                            else if(error.contains("unspecified"))
                                errorMsg = "Could not write to tag";
                            else if(error.contains("Partial"))
                                errorMsg = "Data partially written !";
                            else
                                errorMsg = error;
                            Toast.makeText(getActivity(), "Tag write failed miserably!\n"+errorMsg, Toast.LENGTH_SHORT).show();
                            Beeper.beep(Beeper.FAIL);
                        }

                    } catch (Exception e) {
                        Toast.makeText(getActivity(), getString(R.string.reader_error), Toast.LENGTH_SHORT).show();
                    }
				}
			}
			
		});
		
		dialog.show();
	}
	
}
