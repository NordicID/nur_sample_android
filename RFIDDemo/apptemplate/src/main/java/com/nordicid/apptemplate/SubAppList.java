package com.nordicid.apptemplate;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
/**
 * @author Nordic ID
 * 
 * Builds and manages the menu.
 */
public class SubAppList extends Fragment implements AdapterView.OnItemClickListener {
	public static final String TAG = "SubAppList";
	private ArrayList<SubApp> mSubApps = new ArrayList<SubApp>();
	private GridView mGridView;
	private AppTemplate appTemplate;
	private int currentOpenSubApp = -1;

	private ArrayList<SubApp> mVisibleSubApps = new ArrayList<SubApp>();

	public SubAppList() {
		appTemplate = AppTemplate.getAppTemplate();
	}

    public void updateSubAppsVisibility()
	{
		mVisibleSubApps.clear();
		for (int n=0; n<mSubApps.size(); n++){
			if (mSubApps.get(n).getIsVisibleInMenu())
				mVisibleSubApps.add(mSubApps.get(n));
		}

		if (mImageAdapter != null)
			mImageAdapter.notifyDataSetChanged();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
		Bundle savedInstanceState) {
	
		View view = inflater.inflate(R.layout.fragment_subapplist,
				container, false);
		
		mGridView = (GridView) view.findViewById(R.id.gridView);
		
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		
		int screenHeight = size.y;
		int screenWidth = size.x;
		
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
			
			int horizontalMargin = screenWidth / 17;
			int verticalMargin = screenHeight / 25;
			
			mGridView.setPadding(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
			mGridView.setColumnWidth((screenWidth - (horizontalMargin * 2)) / 2);
			
		}
		else {
			
			int verticalMargin = getResources().getConfiguration().screenWidthDp / 35;
			int horizontalMargin = getResources().getConfiguration().screenHeightDp / 45;
			
			mGridView.setPadding(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin);
		}

		mImageAdapter = new ImageAdapter(this);
		mGridView.setAdapter(mImageAdapter);
		mGridView.setOnItemClickListener(this);
		mGridView.setHorizontalSpacing(mGridView.getVerticalSpacing());
		
		return view;
	}

	ImageAdapter mImageAdapter;

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {	
		appTemplate.setApp(i);
		view.setSelected(true);
	}

	/**
	 * Gets the current GridView (menu)
	 * @return view of menu
	 */
	public GridView getGridView() {
		return mGridView;
	}
	
	/**
	 * Gets the SubAppLists Context
	 * @return Activity's context
	 */
	public Context getContext() {
		return getActivity();
	}
	
	/**
	 * Adds SubApp to the ArrayList of SubApp objects
	 * @param app SubApp
	 */
	public void addSubApp(SubApp app) {
		mSubApps.add(app);
		updateSubAppsVisibility();
	}

	public void removeSubApp(SubApp app) {
		try {
			mSubApps.remove(app);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		updateSubAppsVisibility();
	}
	
	/**
	 * Gets the ArrayList of SubApps
	 * @return ArrayList of SubApp
	 */
	public ArrayList<SubApp> getAllApps() {
		return mSubApps;
	}

	public ArrayList<SubApp> getVisibleApps() {
		return mVisibleSubApps;
	}

	public SubApp getVisibleApp(int i) {
		return mVisibleSubApps.get(i);
	}

	public int getVisibleSubAppIndex(String name) {
		for (int n=0; n<mVisibleSubApps.size(); n++){
			if (mVisibleSubApps.get(n).getAppName().equals(name))
				return n;
		}
		return -1;
	}

	public int getSubAppIndex(String name) {
		for (int n=0; n<mSubApps.size(); n++){
			String app = mSubApps.get(n).getAppName();
			if (app == name) {
				return n;
			}
		}
		return -1;
	}

	/**
	 * Gets the current open SubApps index.
	 * Returns -1 if no SubApp open.
	 * @return index of an current open SubApp
	 */
	public int getCurrentOpenSubAppIndex() {
		return currentOpenSubApp;
	}
	
	/**
	 * Gets the current open SubApp.
	 * Returns null if no SubApp open.
	 * @return current open SubApp or null
	 */
	public SubApp getCurrentOpenSubApp() {
		
		int index = getCurrentOpenSubAppIndex();
		
		if (index != -1) {
			SubApp currentApp = getVisibleApp(index);
			return currentApp;
		}
		
		return null;
	}
	
	/**
	 * Sets open SubApps index. Used Internally.
	 * @param i SubApps index
	 */
	public void setCurrentOpenSubApp(int i) {
		SubApp cur = getCurrentOpenSubApp();
		if (cur != null)
			cur.onVisibility(false);
		
		currentOpenSubApp = i;
		cur = getCurrentOpenSubApp();
		if (cur != null)
			cur.onVisibility(true);
	}

	/**
	 * Gets SubApp from ArrayList of SubApps with its name.
	 * @param name of the SubApp
	 * @return SubApp or null
	 */
	public SubApp getApp(String name) {
		int index = getSubAppIndex(name);
		
		if (index != -1) {
			return mSubApps.get(index);
		}
		
		return null;
	}
}
