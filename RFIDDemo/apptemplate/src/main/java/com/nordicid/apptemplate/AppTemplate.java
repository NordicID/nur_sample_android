package com.nordicid.apptemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.nordicid.nuraccessory.NurAccessoryExtension;
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

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
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

	static final String TAG = "AppTemplate";

	private SubAppList mSubAppList;

	private final int APP_PERMISSION_REQ_CODE = 41;

	private DrawerLayout mDrawerLayout;
	private FrameLayout mMenuContainer;
	private ActionBarDrawerToggle mDrawerToggle;
	private ListView mDrawerList;
	private Menu mMenu;
	private MenuItem mCloseButton;
	private Drawer mDrawer;
	private TextView mBatteryStatus = null;
	private ImageView mBatteryIcon = null;
    private NurAccessoryExtension mAccessoryApi = null;
	private boolean mAccessorySupported = false;
	private boolean mEnableBattUpdate = true;
    protected boolean mProgrammingMode = false;

	private FragmentManager mFragmentManager;
	private FragmentTransaction mFragmentTransaction;
	
	protected NurApi mApi;
	
	private boolean mConfigurationChanged = false;
	private boolean mNoConfigChangeCheck = false;

	private static AppTemplate gInstance = null;
	public static AppTemplate getAppTemplate()
	{
		return gInstance;
	}

	public boolean isRecentConfigurationChange() {
		boolean rc = mConfigurationChanged;
		mConfigurationChanged = false;
		return rc;
	}

	public void setAppListener(NurApiListener l)
	{
		mAppListener = l;
	}

    public void setProgrammingFlag(boolean programming){
        mProgrammingMode = programming;
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
		public void programmingProgressEvent(NurEventProgrammingProgress event) {
            if (mCurrentListener != null)
                mCurrentListener.programmingProgressEvent(event);
        }
		@Override
		public void nxpEasAlarmEvent(NurEventNxpAlarm event) { }
		@Override
		public void logEvent(int level, String txt) { }
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
			if (exitingApplication())
				return;
			mAccessorySupported = false;
            // only do these things when reader in application mode
			if (!mProgrammingMode && mAppListener != null)
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
			try {
				mAccessorySupported = getAccessoryApi().isSupported();
			} catch (Exception e)
			{
				mAccessorySupported = false;
			}
			if (!mProgrammingMode && mAppListener != null)
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

	/** Swaping Listeners from settings fragment **/
    //FIXME is there a better way ?
	private NurApiListener oldListener = null;
	public void switchNurApiListener(NurApiListener newListener){
        oldListener = mCurrentListener;
        mCurrentListener = newListener;
    }
    public void restoreListener(){
        if(oldListener != null)
            mCurrentListener = oldListener;
        else
            mCurrentListener = mNurApiListener;
    }
	/** Testing only **/

	/**
	 * if back button pressed once. @see doubleOnBackPressedExit
	 */
	private boolean backPressedOnce;
	private boolean applicationPaused = true;
	private boolean showMenuAnimation;

	public boolean isApplicationPaused()
	{
		return applicationPaused;
	}
	
	/**
	 * Indicates if the current device has a large screen
	 */
	public static boolean LARGE_SCREEN;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		gInstance = this;
		/** Bluetooth Permission checks **/
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED  ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED  ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
				ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){

			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)  ||
					ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)  ||
					ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
					ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
					) {
				/** ? ? ? **/
			} else {
				ActivityCompat.requestPermissions(this, new String[]{
						Manifest.permission.ACCESS_COARSE_LOCATION,
						Manifest.permission.ACCESS_FINE_LOCATION,
						Manifest.permission.READ_EXTERNAL_STORAGE,
						Manifest.permission.WRITE_EXTERNAL_STORAGE},
						APP_PERMISSION_REQ_CODE);
			}
		}

		super.onCreate(savedInstanceState);
		//int screenLayout = 0;
		//int layoutMask = 0;
		
		mApi = new NurApi();
		//mApi.setLogLevel(mApi.getLogLevel() | NurApi.LOG_VERBOSE);
		mApi.setLogToStdout(true);

        mAccessoryApi = new NurAccessoryExtension(mApi);
		
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

        mBatteryStatus = (TextView) findViewById(R.id.battery_level);
		mBatteryIcon = (ImageView) findViewById(R.id.battery_icon);

		//setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		LARGE_SCREEN = ((getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_LARGE);
		mSubAppList = new SubAppList();
		
		//Adds the users SubApps to SubAppList
		onCreateSubApps(mSubAppList);
		
		if (mSubAppList.getAllApps().size() == 0)
		{
			Toast.makeText(this, "No subapps found", Toast.LENGTH_SHORT).show();
		} 
		else 
		{
			mMenuContainer = (FrameLayout) findViewById(R.id.menu_container);
			
			//Activity is created show menu animation
			showMenuAnimation = false;
			
			//If application started in landscape, configure the layout
			if (mMenuContainer != null) {
				mNoConfigChangeCheck = true;
				onConfigurationChanged(getResources().getConfiguration());	
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, final String permissions[], final int[] grantResults) {
		final List<String> missingPermissions = new ArrayList<>();
		switch (requestCode) {
			case APP_PERMISSION_REQ_CODE: {
				for (int i = 0; i < grantResults.length; i++) {
					if(grantResults[i] == PackageManager.PERMISSION_DENIED)
						missingPermissions.add(permissions[i]);
				}
				if (missingPermissions.size() > 0) {
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setMessage("Application will not work properly without all requested permissions. Do you want to grant them now ?")
							.setPositiveButton("Grant Permissions", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									ActivityCompat.requestPermissions(gInstance, missingPermissions.toArray(new String[0]), APP_PERMISSION_REQ_CODE);
								}
							})
							.setNegativeButton("No thanks", new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int id) {
									Toast.makeText(gInstance,"Missing permissions",Toast.LENGTH_SHORT).show();
								}
							});
					builder.create().show();
				}
			}
		}
	}

	public void setStatusText(String text)
	{
		TextView t = (TextView) findViewById(R.id.app_statustext);
		t.setText(text.toUpperCase());

        if(getNurApi().isConnected() && getAccessorySupported()) {
            try {
				if (mEnableBattUpdate)
	                setBatteryStatus(getAccessoryApi().getBatteryInfo().getPercentageString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            setBatteryStatus("");
        }
	}

    public void setBatteryStatus(String text) {
		if (text.length() == 0)
		{
			if (mBatteryStatus.getVisibility() != View.GONE) {
				mBatteryStatus.setVisibility(View.GONE);
				mBatteryIcon.setVisibility(View.GONE);
			}
		}
        else
		{
			if (mBatteryStatus.getVisibility() != View.VISIBLE) {
				mBatteryStatus.setVisibility(View.VISIBLE);
				mBatteryIcon.setVisibility(View.VISIBLE);
			}
			mBatteryStatus.setText(text);
		}
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
		if(mSubAppList.getCurrentOpenSubApp() != null && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
            mCloseButton.setVisible(true);
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

	/**
	 * Returns SubAppList
	 * @return SubAppList
	 * @see com.nordicid.apptemplate.SubAppList
	 */
	public SubAppList getSubAppList() {
		return mSubAppList;
	}

	/**
	 * Sets the layout for the new configuration that the devices
	 * has at the moment. Used internally.
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		Log.d(TAG, "onConfigurationChanged() newConfig.orientation " + newConfig.orientation);
		super.onConfigurationChanged(newConfig);

		if (mNoConfigChangeCheck == false)
			mConfigurationChanged = true;
		else 
			mNoConfigChangeCheck = false;

		setContentView(R.layout.main);

		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
		{
			mMenuContainer = (FrameLayout) findViewById(R.id.menu_container);
			if (mCloseButton != null)
				mCloseButton.setVisible(false);
		} else {
			mMenuContainer = null;
			if (mSubAppList.getCurrentOpenSubApp() != null) {
				if (mCloseButton != null)
					mCloseButton.setVisible(true);
			}
		}

		if (!applicationPaused) {
			setFragments(true);
		}
		if (mSubAppList.getCurrentOpenSubApp() == null) {
			mSubAppList.setCurrentOpenSubApp(mSubAppList.getVisibleApp(0));
		}
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        mBatteryStatus = (TextView) findViewById(R.id.battery_level);
		mBatteryIcon = (ImageView) findViewById(R.id.battery_icon);
		setDrawer(false);
        mDrawerToggle.onConfigurationChanged(newConfig);
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
		SubApp currentSubApp = mSubAppList.getCurrentOpenSubApp();
		
		if (currentSubApp != null) {
			// See if SubApp handled back button press
			if (currentSubApp.onFragmentBackPressed()) {
				return;
			}
		}

		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			if (currentSubApp == null) {
				doubleOnBackPressExit();
			}
			else {
				// Go back to main menu
				openSubApp(null);
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
			// Back pressed twice within 2sec -> shutdown everything

			// Stop generating NurApi events
			mApi.setListener(null);

			mExitingApplication = true;
			super.onBackPressed();
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
	 * Method to open another SubApp with its name.
	 * If SubApp with given name not found, a Toast will be shown.
	 *
	 *  @param name of the SubApp
	 */
	public void setApp(String name) {

        if (name == null) {
            openSubApp(null);
        }
        else {
            SubApp app = mSubAppList.getVisibleSubApp(name);

            if (app == null) {
                Toast.makeText(this, "App with name \"" + name + "\" not found", Toast.LENGTH_SHORT).show();
            } else {
                openSubApp(app);
            }
        }
    }

    //TODO gets visible / hidden apps
    public void setApp(String name,boolean visible) {

        if (name == null) {
            openSubApp(null);
        }
        else {
            SubApp app;
            if(visible)
                app = mSubAppList.getVisibleSubApp(name);
            else
                app = mSubAppList.getApp(name);
            if (app == null) {
                Toast.makeText(this, "App with name \"" + name + "\" not found", Toast.LENGTH_SHORT).show();
            } else {
                openSubApp(app);
            }
        }
    }
	
	/**
	 *  Used internally to open some SubApp
	 */
	private void openSubApp(SubApp app) {
		if (app == null)
		{
			// Go back to main menu
			mSubAppList.setCurrentOpenSubApp(null);
			setFragments(false);
		}
		else if (app != mSubAppList.getCurrentOpenSubApp() && !app.isVisible()) {
			mSubAppList.setCurrentOpenSubApp(app);
			setFragments(false);
		}
	}
	
	/**
	 * gets the NurApi
	 * @return NurApi
	 */
	public NurApi getNurApi() {
		return mApi;
	}

	public NurAccessoryExtension getAccessoryApi() { return mAccessoryApi; }
	public boolean getAccessorySupported() { return mAccessorySupported; }
	public void setEnableBattUpdate(boolean val) { mEnableBattUpdate = val; }

	private Fragment lastSetFragment = null;

	/**
	 *  Sets all the fragments
	 */
	private void setFragments(boolean configChange)
	{
		SubApp currentOpenSubApp = mSubAppList.getCurrentOpenSubApp();
		int orientation = getResources().getConfiguration().orientation;

		mFragmentManager = getSupportFragmentManager();

		// Always remove subapplist on config change
		if (configChange && mSubAppList.isAdded())
		{
			mFragmentTransaction = mFragmentManager.beginTransaction();
			mFragmentTransaction.remove(mSubAppList);
			mFragmentTransaction.commit();
			mFragmentManager.executePendingTransactions();
		}

		// Remove last set subapp
		if (lastSetFragment != null && lastSetFragment.isAdded()) {
			mFragmentTransaction = mFragmentManager.beginTransaction();
			mFragmentTransaction.remove(lastSetFragment);
			mFragmentTransaction.commit();
			mFragmentManager.executePendingTransactions();
		}

		if (orientation == Configuration.ORIENTATION_PORTRAIT)
		{
			if (currentOpenSubApp == null)
			{
				// No subapp selected, show subapplist
				if (!mSubAppList.isAdded()) {
					mFragmentTransaction = mFragmentManager.beginTransaction();
					mFragmentTransaction.replace(R.id.content, mSubAppList);
					mFragmentTransaction.commit();
				}

				if (mCloseButton != null)
					mCloseButton.setVisible(false);

				getActionBar().setTitle(R.string.app_name);
			}
			else
			{
				// Subapp selected, remove subapplist if needed first
				if (mSubAppList.isAdded()) {
					mFragmentTransaction = mFragmentManager.beginTransaction();
					mFragmentTransaction.remove(mSubAppList);
					mFragmentTransaction.commit();
					mFragmentManager.executePendingTransactions();
				}

				// Add subapp
				mFragmentTransaction = mFragmentManager.beginTransaction();
				mFragmentTransaction.replace(R.id.content, currentOpenSubApp);
				mFragmentTransaction.commit();
				lastSetFragment = currentOpenSubApp;

				getActionBar().setTitle(currentOpenSubApp.getAppName());

				if (mCloseButton != null)
					mCloseButton.setVisible(true);
			}
		}
		else
		{
			// Landscape

			if (mCloseButton != null)
				mCloseButton.setVisible(false);

			// Always show some subapp
			if (currentOpenSubApp == null) {
				currentOpenSubApp = mSubAppList.getVisibleApp(0);
			}

			// Add subapp
			mFragmentTransaction = mFragmentManager.beginTransaction();
			mFragmentTransaction.replace(R.id.content, currentOpenSubApp);
			mFragmentTransaction.commit();
			lastSetFragment = currentOpenSubApp;

			setTitle(currentOpenSubApp.getAppName());

			// If subapp not added, add it to sidebar
			if (!mSubAppList.isAdded()) {
				mFragmentTransaction = mFragmentManager.beginTransaction();
				mFragmentTransaction.replace(R.id.menu_container, mSubAppList);
				mFragmentTransaction.commit();
			}
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
					super.onDrawerClosed(drawerView);
                    invalidateOptionsMenu();
				}
	
				@Override
				public void onDrawerOpened(View drawerView) {
					super.onDrawerOpened(drawerView);
                    invalidateOptionsMenu();
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

		Log.d(TAG, "onResumeFragments() applicationPaused " + applicationPaused);

		super.onResumeFragments();
		setFragments(true);
		
		applicationPaused = false;
	}
	
	@Override
	protected void onPause() {
		Log.d(TAG, "onPause() applicationPaused " + applicationPaused);
		super.onPause();
		applicationPaused = true;
	}
	
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume() applicationPaused " + applicationPaused);
		super.onResume();
	}
	
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop() applicationPaused " + applicationPaused);
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
		Log.d(TAG, "onDestroy() applicationPaused " + applicationPaused);
		super.onDestroy();

		mExitingApplication = true;

		if (mApi != null) {
			mApi.dispose();
		}
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

