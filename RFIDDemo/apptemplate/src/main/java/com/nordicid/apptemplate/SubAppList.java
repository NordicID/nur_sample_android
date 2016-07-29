package com.nordicid.apptemplate;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
	private ArrayList<SubApp> mSubApps = new ArrayList<SubApp>();
	private ArrayList<String> mSubAppNames = new ArrayList<String>();
	private GridView mGridView;
	private AppTemplate appTemplate;
	private int currentOpenSubApp = -1;
	
	public SubAppList(AppTemplate t) {
		appTemplate = t;
	}
	
	public SubAppList() { }
	
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
		
		
		mGridView.setAdapter(new ImageAdapter(this));
		mGridView.setOnItemClickListener(this);
		mGridView.setHorizontalSpacing(mGridView.getVerticalSpacing());
		
		return view;
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {	
		appTemplate.setApp(i, null);
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
		mSubAppNames.add(app.getAppName());
	}

	public void removeSubApp(SubApp app) {
		try {
			mSubAppNames.remove(app.getAppName());
			mSubApps.remove(app);
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Gets the ArrayList of SubApps
	 * @return ArrayList of SubApp
	 */
	public ArrayList<SubApp> getApps() {
		return mSubApps;
	}
	
	/**
	 * Gets all SubApp names
	 * @return ArrayList of SubApp names
	 */
	public ArrayList<String> getSubAppNames() {
		return mSubAppNames;
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
			SubApp currentApp = getApp(index);
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
	 * Gets SubApp object from ArrayList with index
	 * @param i Index of and SubApp
	 * @return SubApp
	 */
	public SubApp getApp(int i) {
		return mSubApps.get(i);
	}
	
	/**
	 * Gets SubApp from ArrayList of SubApps with its name.
	 * @param name of the SubApp
	 * @return SubApp or null
	 */
	
	public SubApp getApp(String name) {
		int index = mSubAppNames.indexOf(name);
		
		if (index != -1) {
			return mSubApps.get(index);
		}
		
		return null;
	}
	
	/**
	 * Gets the SubApps name
	 * @param i index
	 * @return Name of the SubApp
	 */
	public String getAppName(int i) {
		return mSubAppNames.get(i);
	}
	
}
