package com.nordicid.apptemplate;

import com.nordicid.nurapi.*;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;


/**
 * @author Nordic ID
 * 
 * Extend this class in your "subapp class" to build
 * a subapp
 */

public class SubApp extends Fragment {
	private LinearLayout mButtonBar;
	private int mButtonBarButtonCount;
	private boolean mVisible = true;
	
	/*private int[] default_animations = {
			R.anim.default_enter_app,
			R.anim.default_exit_app
		};*/
	
	public Context getContext()
	{
		return AppTemplate.getAppTemplate();
	}
	public AppTemplate getAppTemplate()
	{
		return AppTemplate.getAppTemplate();
	}
	public NurApi getNurApi()
	{
		return AppTemplate.getAppTemplate().getNurApi();
	}
	
	public SubApp() {
		super();
	}

	/**
	 * Get menu visibility of the subapp.
	 *
	 * @return boolean true/false
	 */
	public boolean getIsVisibleInMenu() { return mVisible; }

	/**
	 * Set menu visibility of the subapp.
	 *
	 * @param val true/false
	 */
	public void setIsVisibleInMenu(boolean val) {
		mVisible = val;
		getAppTemplate().getSubAppList().updateSubAppsVisibility();
	}

	/**
	 * Gets SubApp name. Override this method 
	 * in your subapp class to return
	 * desirable name (Default: "No name").
	 * 
	 * @return String name
	 */
	public String getAppName() {
		return "No name";
	}
	
	/**
	 * Method to get the subapps icon for the subapplist.
	 * Default/missing icon: Question mark 
	 * 
	 * @return Drawable R.drawable.question
	 */
	public int getTileIcon() {
		return R.drawable.question;
	}
	
	/**
	 * Method to get subapps layout.
	 * Default: blank layout with a notice text
	 * 
	 * @return int R.layout.xx
	 */
	public int getLayout() {
		return R.layout.default_subapp;
	}
	
	/**
	 * Gets the subapps enter/exit animations. Default animation 
	 * effect: fade.
	 * @return int[] of animations [R.anim.xx,R.anim.xx]
	 */
	/*public int[] getAnimations() {
		return default_animations;
	}*/
	
	public void onVisibility(boolean val)
	{
		// nop
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		View view = inflater.inflate(getLayout(), container, false);
		mButtonBar = (LinearLayout) getActivity().getWindow().getDecorView().findViewById(R.id.app_button_bar);
		mButtonBarButtonCount = 0;
		
		getActivity().getActionBar().setTitle(getAppName());
		
		return view;
	}
	
	/**
	 * Sets button bars visibility. 
	 * (View.VISIBLE/GONE/INVISIBLE)
	 * @param viewInt
	 */
	public void setButtonBarVisibility(int viewInt) {
		mButtonBar.setVisibility(viewInt);
	}
	
	/**
	 * Adds a button bar button and returns the created Button.
	 * @param text
	 * @param onClickListener
	 * @return Button
	 */
	public Button addButtonBarButton(String text, OnClickListener onClickListener) {
		
		mButtonBarButtonCount += 1;
		mButtonBar.setWeightSum(mButtonBarButtonCount);
		
		Button b = (Button) LayoutInflater.from(getActivity()).inflate(R.layout.button_bar_button, null);
		b.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 1f));
		b.setText(text);
		b.setOnClickListener(onClickListener);
		mButtonBar.addView(b);

		return b;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		
		if (mButtonBarButtonCount > 0) {
			mButtonBar.removeAllViews();
		}
	}
	
	/**
	 *  Handles onBackPressed in a fragment. 
	 *  Return true to stop default activity's onBackPressed and return false
	 *  to continue with default activity's onBackPressed.
	 * @return boolean
	 */
	public boolean onFragmentBackPressed() {
		return false;
	}
	
	/*@Override
	public void onDetach() {
		super.onDetach();
		
		//Workaround for known bug with v4 support library and fragmentTabHost
		try {
			Field childFragmentManager = Fragment.class.getDeclaredField("mChildFragmentManager");
			childFragmentManager.setAccessible(true);
			childFragmentManager.set(this, null);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}*/
	
	/*
	 * NUR API listener stuff.
	 * Override these as necessary in various sub-applications.
	 */

	public NurApiListener getNurApiListener()
	{
		return null;	
	}	
}
