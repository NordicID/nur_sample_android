package com.nordicid.apptemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.nordicid.nurapi.NurApi;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class SubAppTabbed extends SubApp {

	protected static final Field sChildFragmentManagerField; //For a workaround if nested fragments used.

	private SubAppTabbedPagerAdapter mPagerAdapter;
	protected ViewPager mPager;
	
	protected int mPagerID = -1;
	protected ArrayList<Fragment> mFragments = new ArrayList<Fragment>();
	protected ArrayList<String> mFragmentNames = new ArrayList<String>();

	public SubAppTabbed() {
		super();
	}
	
	protected int onGetFragments(ArrayList<Fragment> fragments, ArrayList<String> fragmentNames) throws Exception
	{
		throw new Exception("Not implemented");
	}

	protected String onGetPreferredTab()
	{
		return "";
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		try {
			if (mPagerID == -1)
			{
				mPagerID = onGetFragments(mFragments, mFragmentNames);	
			}
			mPagerAdapter = new SubAppTabbedPagerAdapter(getChildFragmentManager());
			mPager = (ViewPager) view.findViewById(mPagerID);
			mPager.setAdapter(mPagerAdapter);

			String preferredTab;
			preferredTab = onGetPreferredTab();
			if(!preferredTab.isEmpty())
			{
				int tabIndex;
				tabIndex = mFragmentNames.indexOf(preferredTab);

				if (tabIndex >= 0) {
					mPager.setCurrentItem(tabIndex);
				}
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		super.onViewCreated(view, savedInstanceState);
	}
	
	//Custom pager adapter class
	public class SubAppTabbedPagerAdapter extends FragmentStatePagerAdapter {

		public SubAppTabbedPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		//which fragment should be visible
		@Override
		public Fragment getItem(int position) {
			if (position < mFragments.size())
				return mFragments.get(position);
			return null;
		}
		
		//titles
		@Override
		public CharSequence getPageTitle(int position) {
			if (position < mFragmentNames.size())
				return mFragmentNames.get(position);
			return null;
		}

		@Override
		public int getCount() {
			return mFragmentNames.size();
		}

		
		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {
			/*Does nothing else that prevents crash. "Restore" the subapps 
			 state in the "host fragment". SubApps only draws the UI  */	
		}

	}
	
	/////Workaround when using nested fragments/////
	static {
        Field f = null;
        try {
            f = Fragment.class.getDeclaredField("mChildFragmentManager");
            f.setAccessible(true);
        } catch (NoSuchFieldException e) {}
        sChildFragmentManagerField = f;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (sChildFragmentManagerField != null) {
            try {
                sChildFragmentManagerField.set(this, null);
            } catch (Exception e) {}
        }
    }
    /////////////////////////////////////////////////
}
