package com.nordicid.rfiddemo;

import java.util.HashMap;

import com.nordicid.apptemplate.AppTemplate;
import com.nordicid.apptemplate.SubApp;
import com.nordicid.controllers.TraceTagController;
import com.nordicid.controllers.TraceTagController.TraceTagListener;
import com.nordicid.controllers.TraceTagController.TracedTagInfo;
import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiListener;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class TraceApp extends SubApp {
	
	private SimpleAdapter mFoundTagsListViewAdapter;
	
	private String mLocatableEpc;
	
	private Button mStartStopLocating;
	private EditText mLocatableEpcEditText;
	private EditText mPctText;
	
	private ProgressBar mProgressBar;
	private ListView mFoundTagsListView;
	private RelativeLayout mEmptyListViewNotice;
	
	private TraceTagController mTraceController;
	
	public TraceApp() {
		super();
		mTraceController = new TraceTagController(getNurApi());
	}
	
	@Override
	public int getTileIcon() {
		return R.drawable.ic_inventory;
	}
	
	@Override
	public String getAppName() {
		return "Locate";
	}
	
	@Override
	public int getLayout() {
		return R.layout.app_locate;
	}
	
	@Override
	public void onVisibility(boolean val)
	{
		if (!val)
		{
			stopTrace();
		}
	}
	
	ObjectAnimator animation = null;
	int mLastVal = 0;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mTraceController.setListener(new TraceTagListener() {			
			@Override
			public void traceTagEvent(TracedTagInfo data) {
				mProgressBar.setProgress(data.scaledRssi);
				mPctText.setText(data.scaledRssi + "%");
				
				if (animation != null)
				{
					animation.cancel();
				}
				
				animation = ObjectAnimator.ofInt(mProgressBar, "progress", mLastVal, data.scaledRssi);
				animation.setDuration(200);
				animation.setInterpolator(new LinearInterpolator());
				animation.start();
				
				mLastVal = data.scaledRssi;
			}

			@Override
			public void readerDisconnected() {
				stopTrace();
			}

			@Override
			public void readerConnected() {
			}
		});
		
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		mStartStopLocating = addButtonBarButton(getString(R.string.start), new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mTraceController.isTracingTag()) {
					stopTrace();
				}
				else {
					startTrace();
				}
			}			
		});
		
		mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		mFoundTagsListView = (ListView) view.findViewById(R.id.tags_listview);
		mEmptyListViewNotice = (RelativeLayout) view.findViewById(R.id.listview_empty);
		mLocatableEpcEditText = (EditText) view.findViewById(R.id.locate_epc_edittext);
		mPctText = (EditText) view.findViewById(R.id.pct_text);
		
		mLocatableEpcEditText.addTextChangedListener(new TextWatcher() {

			@Override public void afterTextChanged(Editable s) {
				String tmp = mLocatableEpcEditText.getText().toString().replaceAll("[^a-fA-F_0-9]", "");
				
				if (!tmp.equals(mLocatableEpcEditText.getText().toString())) {
					mLocatableEpcEditText.setText(tmp);
					mLocatableEpcEditText.setSelection(mLocatableEpcEditText.getText().length());
				}
				
				if (!(mLocatableEpcEditText.getText().toString().length() > 0) && mLocatableEpcEditText.getText().toString().length() % 4 != 0) {
					mLocatableEpcEditText.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
				}
				else {
					mLocatableEpcEditText.setBackgroundColor(getResources().getColor(android.R.color.white));
				}
			}
			
			@Override public void beforeTextChanged(CharSequence s, int start,int count, int after) {}
			@Override public void onTextChanged(CharSequence s, int start,int before, int count) {}
			
		});
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			LayoutParams lp = mProgressBar.getLayoutParams();
			WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			lp.width = size.x / 2;
			lp.height = size.x / 2;
			mProgressBar.setLayoutParams(lp);
			mPctText.setLayoutParams(lp);
		}
		else {
			LayoutParams lp = mProgressBar.getLayoutParams();
			WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
			Display display = wm.getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			lp.width = size.y / 2;
			lp.height = size.y / 2;
			mProgressBar.setLayoutParams(lp);
			mPctText.setLayoutParams(lp);
		}
		
		mFoundTagsListViewAdapter = new SimpleAdapter(getActivity(), InventoryApp.FOUND_TAGS, R.layout.taglist_row, new String[] {"epc"}, new int[] {R.id.tagText});
		mFoundTagsListView.setEmptyView(mEmptyListViewNotice);
		mFoundTagsListView.setAdapter(mFoundTagsListViewAdapter);
		mFoundTagsListView.setCacheColorHint(0);
		
		mFoundTagsListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				@SuppressWarnings("unchecked")
				HashMap<String,String> selectedTagData = (HashMap<String, String>) mFoundTagsListView.getItemAtPosition(position);
				String epc = selectedTagData.get("epc");
				mLocatableEpcEditText.setText(epc);
				mLocatableEpc = epc;
			}			
		});
		
		mFoundTagsListViewAdapter.notifyDataSetChanged();
		
		if (getArguments() != null) {
			Bundle b = getArguments();
			
			if (b.getString("epc") != null) {
				mLocatableEpc = b.getString("epc");
				mLocatableEpcEditText.setText(mLocatableEpc);
			}

		}
	}
	
	private void startTrace() {
		//mTraceController.startTagTrace(mLocatableEpc.getBytes());		
		try {
			if (!mTraceController.isTracingTag())
			{
				mTraceController.startTagTrace(NurApi.hexStringToByteArray(mLocatableEpc));
				mStartStopLocating.setText(getString(R.string.stop));
				Toast.makeText(getActivity(), getString(R.string.location_started), Toast.LENGTH_SHORT).show();
			}
		}
		catch (Exception ex) { 
			
		}
	}
	
	private void stopTrace() {
		if (mTraceController.isTracingTag())
		{
			mTraceController.stopTagTrace();
			mStartStopLocating.setText(getString(R.string.start));
			Toast.makeText(getActivity(), getString(R.string.location_stopped), Toast.LENGTH_SHORT).show();
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public boolean onFragmentBackPressed() {
		
		if (mTraceController.isTracingTag()) {
			
			try {
				stopTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		
		if (mTraceController.isTracingTag()) {
			try {
				stopTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public NurApiListener getNurApiListener()
	{
		return mTraceController.getNurApiListener();
	}
}
