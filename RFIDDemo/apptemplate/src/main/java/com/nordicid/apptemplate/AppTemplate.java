package com.nordicid.apptemplate;

import java.util.ArrayList;

import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurApiUiThreadRunner;
import com.nordicid.nurapi.NurEventAutotune;
import com.nordicid.nurapi.NurEventClientInfo;
import com.nordicid.nurapi.NurEventDeviceInfo;
import com.nordicid.nurapi.NurEventEpcEnum;
import com.nordicid.nurapi.NurEventFrequencyHop;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurEventInventory;
import com.nordicid.nurapi.NurEventNxpAlarm;
import com.nordicid.nurapi.NurEventProgrammingProgress;
import com.nordicid.nurapi.NurEventTagTrackingChange;
import com.nordicid.nurapi.NurEventTagTrackingData;
import com.nordicid.nurapi.NurEventTraceTag;
import com.nordicid.nurapi.NurEventTriggeredRead;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Extend this class to your MainActivity to use the template.
 * 
 * If not you do not use manifestmerger remember to add
 * android:configChanges="orientation|screenSize|keyboardHidden" to your
 * Manifest inside <activity>
 * 
 * @author Nordic ID
 * 
 */

public class AppTemplate extends FragmentActivity {
	
	private SubAppList mSubAppList;
	
	private DrawerLayout mDrawerLayout;
	private FrameLayout mMenuContainer;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
	private Menu mMenu;
	private MenuItem mCloseButton;
	private Drawer mDrawer;
	
	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;
	
	private NurApi mApi;
	
	private boolean mConfigurationChanged = false;
	private boolean mNoConfigChangeCheck = false;

	private static AppTemplate gInstance = null;
	public static AppTemplate getAppTemplate()
	{
		return gInstance;
	}

	public boolean isRecentConfigurationChange()
	{
		boolean rc = mConfigurationChanged;
		mConfigurationChanged = false;
		return rc;
	}
	
	public void setAppListener(NurApiListener l)
	{
		mAppListener = l;
	}
	
	/* Which sub-application is listening. */
	private NurApiListener mAppListener = null;	
	private NurApiListener mCurrentListener = null;	
	private NurApiListener mNurApiListener = new NurApiListener() {		
		@Override
		public void triggeredReadEvent(NurEventTriggeredRead event) { }
		
		@Override
		public void traceTagEvent(NurEventTraceTag event) {
			if (mCurrentListener != null)
				mCurrentListener.traceTagEvent(event);					
		}
		
		@Override
		public void programmingProgressEvent(NurEventProgrammingProgress event) { }
		@Override
		public void nxpEasAlarmEvent(NurEventNxpAlarm event) { }
		
		@Override
		public void logEvent(int level, String txt) {
			String pref = "ERROR: ";
			if (level == NurApi.LOG_DATA || txt == null || txt == "")
				return;	// Just to avoid spam.
			
			if (level == NurApi.LOG_USER)
				pref = "USER: ";
			else if (level == NurApi.LOG_VERBOSE)
				pref = "VERB: ";
			
			System.out.println(pref + txt);
		}
		
		@Override
		public void inventoryStreamEvent(NurEventInventory event) {
			if (mCurrentListener != null)
				mCurrentListener.inventoryStreamEvent(event);
		}
		
		@Override
		public void inventoryExtendedStreamEvent(NurEventInventory event) {
			if (mCurrentListener != null)
				mCurrentListener.inventoryExtendedStreamEvent(event);
		}
		
		@Override
		public void frequencyHopEvent(NurEventFrequencyHop event) { }
		
		@Override
		public void epcEnumEvent(NurEventEpcEnum event) { }
		
		@Override
		public void disconnectedEvent() {
			if (mAppListener != null)
				mAppListener.disconnectedEvent();
			if (mCurrentListener != null)
				mCurrentListener.disconnectedEvent();		
		}
		
		@Override
		public void deviceSearchEvent(NurEventDeviceInfo event) { }
		
		@Override
		public void debugMessageEvent(String event) { }
		
		@Override
		public void connectedEvent() {
			if (mAppListener != null)
				mAppListener.connectedEvent();
			if (mCurrentListener != null)
				mCurrentListener.connectedEvent();
		}
		
		@Override
		public void clientDisconnectedEvent(NurEventClientInfo event) { }
		@Override
		public void clientConnectedEvent(NurEventClientInfo event) { }
		
		@Override
		public void bootEvent(String event) {
			//Toast.makeText(AppTemplate.this, "BOOT " + event, Toast.LENGTH_SHORT).show();
			if (mCurrentListener != null)
				mCurrentListener.bootEvent(event);			
		}
		
		@Override
		public void IOChangeEvent(NurEventIOChange event) {
			//Toast.makeText(AppTemplate.this, "IOCHG " + event.source + "; " + event.direction, Toast.LENGTH_SHORT).show();
			if (mCurrentListener != null)
				mCurrentListener.IOChangeEvent(event);
		}

		@Override
		public void autotuneEvent(NurEventAutotune event) { }
		@Override
		public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
		@Override
		public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
	};

	private void changeSubAppListener()
	{
		SubApp tmpSubApp = mSubAppList.getCurrentOpenSubApp();

		if (tmpSubApp != null) 
			mCurrentListener = tmpSubApp.getNurApiListener();		
		else 
			mCurrentListener = null;
	}

	/**
	 * if back button pressed once. @see doubleOnBackPressedExit
	 */
	private boolean backPressedOnce;
	private boolean applicationPaused = true;
	private boolean showMenuAnimation;
	
	/**
	 * Indicates if the current device has a large screen
	 */
	public static boolean LARGE_SCREEN;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		gInstance = this;

		super.onCreate(savedInstanceState);
		//int screenLayout = 0;
		//int layoutMask = 0;
		
		mApi = new NurApi();
		
		mApi.setUiThreadRunner(new NurApiUiThreadRunner() {
			
			@Override
			public void runOnUiThread(Runnable r) {
				AppTemplate.this.runOnUiThread(r);
			}
		});
		
		mApi.setListener(mNurApiListener);
		
		//Application theme and typeface will be changed
		setTheme(getApplicationTheme());
		TypefaceOverrider.setDefaultFont(getApplicationContext(), "MONOSPACE", getPahtToTypeface());

		setContentView(R.layout.main);

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
		mDrawer = new Drawer();
		
		//Set the drawer and add items to it
		setDrawer(true);

		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		LARGE_SCREEN = ((getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_LARGE);

		mSubAppList = new SubAppList();
		
		//Adds the users SubApps to SubAppList
		onCreateSubApps(mSubAppList);
		
		if (mSubAppList.getApps().size() == 0) 
		{
			Toast.makeText(this, "No subapps found", Toast.LENGTH_SHORT).show();
		} 
		else 
		{
			mMenuContainer = (FrameLayout) findViewById(R.id.menu_container);
			
			//Activity is created show menu animation
			showMenuAnimation = true;
			
			//If application started in landscape, configure the layout
			if (mMenuContainer != null) {
				mNoConfigChangeCheck = true;
				onConfigurationChanged(getResources().getConfiguration());	
			}
		}
	}
	
	public void setStatusText(String text)
	{
		TextView t = (TextView) findViewById(R.id.app_statustext);
		t.setText(text.toUpperCase());
	}
	
	@Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

	public int getTemplateTheme() {
		return R.style.TemplateDefaultTheme;
	}
	
	/**
	 * Creates the action / title bar. Used internally.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		getMenuInflater().inflate(R.menu.actionbar_menu, menu);
		
		mMenu = menu;
		mCloseButton = mMenu.findItem(R.id.actionbar_close_button);
		
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Gets the applications root view.
	 * @return rootView
	 */
	protected View getRootView() {
		View rootView = getWindow().getDecorView().getRootView();
		return rootView;
	}
	
	/**
	 * Method that is executed when SubAppList created. Use 
	 * this method to add SubApps to SubAppList.
	 *
	 * @see com.nordicid.apptemplate.SubAppList#addSubApp(SubApp)
	 */
	protected void onCreateSubApps(SubAppList subAppList) { }

	protected void removeSubApp(SubApp toRemove)
	{
		if (mExitingApplication)
			return;	// Don't go poking around when already exiting.

		mSubAppList.removeSubApp(toRemove);
	}

	/**
	 * Gets all the SubApp objects from
	 * SubAppList in a ArrayList.
	 * @return ArrayList of SubApps
	 * @see SubApp
	 */
	public ArrayList<SubApp> getSubApps() {
		return mSubAppList.getApps();
	}
	
	/**
	 * Gets the SubApp with index
	 * @param i Index of SubApp in ArrayList of SubApps
	 * @return SubApp
	 * @see SubApp
	 */
	public SubApp getApp(int i) {
		return mSubAppList.getApp(i);
	}

	/**
	 * Returns SubAppList
	 * @return SubAppList
	 * @see com.nordicid.apptemplate.SubAppList
	 */
	public SubAppList getSubAppList() {
		return mSubAppList;
	}
	
	/**
	 * Returns ArrayList of SubApps
	 * @see SubApp
	 * @return ArrayList
	 */
	protected ArrayList<SubApp> getApps() {
		return mSubAppList.getApps();
	}
	
	/**
	 * Sets the layout for the new configuration that the devices
	 * has at the moment. Used internally.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		
		changeSubAppListener();
		
		if (mNoConfigChangeCheck == false)
			mConfigurationChanged = true;
		else 
			mNoConfigChangeCheck = false;
		
		mDrawerToggle.onConfigurationChanged(newConfig);
		
		setContentView(R.layout.main);
		
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {	
			
			mMenuContainer = (FrameLayout) findViewById(R.id.menu_container);
			
			if (mMenu != null) {
				mCloseButton.setVisible(false);
			}
			
		} else {

			mMenuContainer = null;

			if (mSubAppList.getCurrentOpenSubAppIndex() != -1) {
				mCloseButton.setVisible(true);
			}
		}
		
		if (!applicationPaused) {
			setFragments();
		}
		
		if (mSubAppList.getCurrentOpenSubAppIndex() == -1) {
			mSubAppList.setCurrentOpenSubApp(0);
		}

		setDrawer(false);
	}
	
	/**
	 * Handles back button presses. Current open SubApps onFragmentBackPressed 
	 * must return false to execute this method fully. If orientation is landscape 
	 * current open SubApp wont be closed, method will execute doubleOnBackPressExit 
	 * to exit.
	 * @see #doubleOnBackPressExit
	 * @see com.nordicid.apptemplate.AppTemplate
	 */
	@Override
	public void onBackPressed() {
		
		int currentAppIndex = mSubAppList.getCurrentOpenSubAppIndex();
		
		changeSubAppListener();
	
		if (currentAppIndex != -1) {
			
			SubApp app = mSubAppList.getApp(mSubAppList.getCurrentOpenSubAppIndex());
			
			if (app.onFragmentBackPressed()) {
				return;
			}
		}
		

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			
			if (mSubAppList.getCurrentOpenSubAppIndex() == -1) {
				doubleOnBackPressExit();
			}
			else {
				
				mFragmentTransaction = getSupportFragmentManager().beginTransaction();
				mFragmentTransaction.remove(mSubAppList).commit();
				getSupportFragmentManager().executePendingTransactions();
				
				mFragmentTransaction = getSupportFragmentManager().beginTransaction();
				mFragmentTransaction.replace(R.id.content, mSubAppList).commit();
				
				getActionBar().setTitle(R.string.app_name);
				mCloseButton.setVisible(false);
				mSubAppList.setCurrentOpenSubApp(-1);
				changeSubAppListener();
			}
		}
		else {
			doubleOnBackPressExit();
		}
	}

	private boolean mExitingApplication = false;

	// Return the "exiting now" status; may be required for not to
	// poke around the sub-apps too much at the wrong time.
	public boolean exitingApplication()
	{
		return mExitingApplication;
	}

	/**
	 * Method to exit the app if called twice in 2 seconds
	 */
	private void doubleOnBackPressExit() {
		if (backPressedOnce) {
			mExitingApplication = true;
			super.onBackPressed();
			//this.finish();
	        return;
	    }

	    backPressedOnce = true;
	    Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

	    new Handler().postDelayed(new Runnable() {
	        @Override
	        public void run() {
	            backPressedOnce = false;
				mExitingApplication = false;
	        }
	    }, 2000);
	}

	/**
	 * Method to open SubApp with index (apps index).
	 * If SubApp with index given not found, a Toast will be shown.
	 * @param index Index of the app in ArrayList of SubApps
	 * @param bundle Parameters for to be opened SubApp
	 */
	public void setApp(int index, Bundle bundle) {
		openSubApp(index, bundle);
		changeSubAppListener();
	}
	
	/**
	 * Method to open another SubApp with its name.
	 * If SubApp with given name not found, a Toast will be shown.
	 *
	 *  @param name of the SubApp
	 *  @param bundle PArameters for to be opened SubApp
	 *
	 *  @see SubAppList#getAppName(int)
	 */
	public void setApp(String name, Bundle bundle) {

		int index = mSubAppList.getSubAppNames().indexOf(name);
		
		if (index == -1) {
			Toast.makeText(this, "App with name \""+name+"\" not found", Toast.LENGTH_SHORT).show();
		}
		else {
			openSubApp(index, bundle);
		}
		
		changeSubAppListener();
	}
	
	/**
	 *  If SubApp to be open with the index not found, method shows a Toast. 
	 *  Used internally to open SubApps.
	 * @param i Index of an SubApp
	 * @param bundle Parameters.
	 */
	private void openSubApp(int i, Bundle bundle) {
 
		if (i != mSubAppList.getCurrentOpenSubAppIndex() && !mSubAppList.getApp(i).isVisible()) {
			
			if (i < mSubAppList.getApps().size()) {
				SubApp app = getApp(i);
				
				/*
				 * prevents crash if user navigates fast
				 * from subapp to another or starts the
				 * app in landscape and first taps the 
				 * first apps tile (that is already "open").
				 */
				if (bundle != null) {
					app.setArguments(bundle);
				}

				int[] animations = app.getAnimations();
			
				mFragmentManager = getSupportFragmentManager();
				mFragmentTransaction = mFragmentManager.beginTransaction();
				mFragmentTransaction.setCustomAnimations(animations[0], animations[1]);
				mFragmentTransaction.replace(R.id.content, app).commit();
	
				mSubAppList.setCurrentOpenSubApp(i);
				changeSubAppListener();
				
				if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
					mCloseButton.setVisible(true);
				}
			}
			else {
				Toast.makeText(this, "No subapp with index "+i+" found", Toast.LENGTH_SHORT).show();
			}
		}
		
	}
	
	/**
	 * gets the NurApi
	 * @return NurApi
	 */
	public NurApi getNurApi() {
		return mApi;
	}
	
	/**
	 *  Sets all the fragments
	 */
	private void setFragments() {
		SubApp app;
		int currentOpen = mSubAppList.getCurrentOpenSubAppIndex();
		int orientation = getResources().getConfiguration().orientation;

		mFragmentManager = getSupportFragmentManager();

		if (mSubAppList.isAdded()) {
			mFragmentTransaction = mFragmentManager.beginTransaction();
			mFragmentTransaction.remove(mSubAppList);
			mFragmentTransaction.commit();
			
			mFragmentManager.executePendingTransactions();
		}
		
		if (currentOpen != -1) {
			app = mSubAppList.getApp(currentOpen);
		}
		else {
			app = mSubAppList.getApp(0);
		}
		
		if (app.isAdded()) {
			mFragmentTransaction = mFragmentManager.beginTransaction();
			mFragmentTransaction.remove(app);
			mFragmentTransaction.commit();
			mFragmentManager.executePendingTransactions();
		}

		if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			if (currentOpen == -1) {
				mFragmentTransaction = mFragmentManager.beginTransaction();

				if (showMenuAnimation) {
					mFragmentTransaction.setCustomAnimations(R.anim.default_enter_menu, R.anim.default_exit_menu);
					showMenuAnimation = false;
				}

				mFragmentTransaction.add(R.id.content, mSubAppList);
				mFragmentTransaction.commit();
			}
			else {
				mFragmentTransaction = mFragmentManager.beginTransaction();
				mFragmentTransaction.add(R.id.content, app);
				mFragmentTransaction.commit();
				
				getActionBar().setTitle(R.string.app_name);
			}
		}
		else {

			mFragmentTransaction = mFragmentManager.beginTransaction();
			mFragmentTransaction.add(R.id.content, app);
			mFragmentTransaction.commit();

			setTitle(app.getAppName());

			mFragmentManager = getSupportFragmentManager();
			mFragmentTransaction = mFragmentManager.beginTransaction();

			if (showMenuAnimation) {
				mFragmentTransaction.setCustomAnimations(R.anim.default_enter_menu, R.anim.default_exit_menu);
				showMenuAnimation = false;
			}

			mFragmentTransaction.add(R.id.menu_container, mSubAppList);
			mFragmentTransaction.commit();
		}

		changeSubAppListener();
	}
	
	/**
	 * Sets the navigation drawer and adds the items to it
	 * if necessary
	 * 
	 * @param addItems
	 */
	private void setDrawer(boolean addItems) {
		
		if (addItems) {
			onCreateDrawerItems(mDrawer);
		}
					
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
	
				@Override
				public void onDrawerClosed(View drawerView) {
					invalidateOptionsMenu();
					super.onDrawerClosed(drawerView);
				}
	
				@Override
				public void onDrawerOpened(View drawerView) {
					invalidateOptionsMenu();
					super.onDrawerOpened(drawerView);
				}
		};
		
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setAdapter(new DrawerItemAdapter(this,mDrawer.getDrawerItemTitles()));
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

	}
	
	/**
	 * Gets desired theme that will be set to
	 * the application. Override this to return desired
	 * theme.
	 * 
	 * Default: R.style.TemplateDefaultTheme
	 * @return R.style.theme_name
	 */
	public int getApplicationTheme() {
		return R.style.TemplateDefaultTheme;
	}

	public void onCreateDrawerItems(Drawer drawer) {}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		else if (item.getItemId() == R.id.actionbar_close_button) {
			onBackPressed();
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResumeFragments() {
		super.onResumeFragments();
		setFragments();
		
		applicationPaused = false;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		applicationPaused = true;
	}
	
	@Override
	protected void onResume() {
		super.onResume();		
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		if (mApi != null) 
		{
			if (mApi.isConnected()) {
				try {				
					mApi.stopAllContinuousCommands();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (mApi != null) 
		{
			if (mApi.isConnected()) {
				try {
					mApi.stopAllContinuousCommands();
				} catch (Exception err) {
					err.printStackTrace();
				}
			}
		}
		
		mApi.dispose();
	}
	
	public void onDrawerItemClick(AdapterView<?> parent, View view, int position, long id) {}
	
	public String getPahtToTypeface() {
		return "fonts/RobotoCondensed-Regular.ttf";
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            onDrawerItemClick(parent, view, position, id);
            mDrawerLayout.closeDrawers();
        }
    }
	
	/**
	 * Handles the navigation drawers
	 */
	public class Drawer {
		
		private ArrayList<String> mDrawerItemTitles;
		
		public Drawer() {
			mDrawerItemTitles = new ArrayList<String>();
		}
		
		public void addTitle(String title) {
			mDrawerItemTitles.add(title);
		}
		
		public ArrayList<String> getDrawerItemTitles() {
			return mDrawerItemTitles;
		}

	}
}

