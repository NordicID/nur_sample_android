package com.nordicid.controllers;

import com.nordicid.nurapi.AntennaMapping;
import com.nordicid.nurapi.NurApi;

import android.util.Log;

public class TraceAntennaSelector 
{
    public final int ANT_UNKNOWN = 0;
    public final int ANT_CD = 1;
    public final int ANT_CIRCULAR = 2;
    public final int ANT_PROXIMITY = 3;
    
    int mCurrentAnt = ANT_UNKNOWN;

	AvgBuffer mSignalAvg = new AvgBuffer(3, 0);
	NurApi mApi;

	int mBackupSelectedAntenna;
	int mBackupAntennaMask;
	int mBackupTxLevel;
	
	int mCrossDipoleAntMask;
	int mCircularAntMask;
	int mProximityAntMask;
	
	boolean mIsProximity;
	
	static public int getPhysicalAntennaMask(AntennaMapping []map, String ant)
	{
		int mask = 0;
		for (int n=0; n<map.length; n++)
		{
			if (map[n].name.startsWith(ant))
				mask |= (1 << map[n].antennaId);
		}
		return mask;
	}
	
	public void begin(NurApi api) throws Exception
	{
		mApi = api;

		mCurrentAnt = ANT_UNKNOWN;
		
		//mSignalAvg.Reset();
		mSignalAvg.clear();

		mBackupAntennaMask = mApi.getSetupAntennaMaskEx();
		mBackupSelectedAntenna = mApi.getSetupSelectedAntenna();
		mBackupTxLevel = mApi.getSetupTxLevel();

		mApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);
		
		AntennaMapping []map = mApi.getAntennaMapping();
		
		mCrossDipoleAntMask = getPhysicalAntennaMask(map, "CrossDipole");
		mCircularAntMask = getPhysicalAntennaMask(map, "Circular");
		mProximityAntMask = getPhysicalAntennaMask(map, "Proximity");
		
		mApi.setSetupTxLevel(0);
		
		selectCrossDipoleAntenna();
	}
	
	public void stop() throws Exception
	{
		mApi.setSetupAntennaMaskEx(mBackupAntennaMask);
		mApi.setSetupSelectedAntenna(mBackupSelectedAntenna);
		mApi.setSetupTxLevel(mBackupTxLevel);
	}
	
	public int getSignalStrength()
	{
		return (int)mSignalAvg.getAvgValue();
	}
	
	public int getCurrentAntenna()
	{
		return mCurrentAnt;
	}
	
	void selectCrossDipoleAntenna() throws Exception
	{
		if (mCrossDipoleAntMask != 0 && mCurrentAnt != ANT_CD)
		{
			mApi.setSetupAntennaMaskEx(mCrossDipoleAntMask);
			mCurrentAnt = ANT_CD;
			Log.d("TRACE", "ANT_CD");
		}
	}
	
	void selectCircularAntenna() throws Exception
	{
		if (mCircularAntMask != 0 && mCurrentAnt != ANT_CIRCULAR)
		{
			mApi.setSetupAntennaMaskEx(mCircularAntMask);
			mCurrentAnt = ANT_CIRCULAR;
			Log.d("TRACE", "ANT_CIRCULAR");
		}
	}	
	
	void selectProximityAntenna() throws Exception
	{
		if (mProximityAntMask != 0 && mCurrentAnt != ANT_PROXIMITY)
		{
			mApi.setSetupAntennaMaskEx(mProximityAntMask);
			mCurrentAnt = ANT_PROXIMITY;
			Log.d("TRACE", "ANT_PROXIMITY");
		}
	}
	
	public int adjust(int locateSignal) throws Exception
	{
		if (mCurrentAnt != ANT_PROXIMITY)
		{
			// rescale 0-100 to 0-95 as proximity makes up last 5%
			locateSignal = (int)(locateSignal * 0.95f);
		} else {
			// rescale 0-70 to 95-100
			locateSignal = 95 + (int)((float)locateSignal / 14);
            if (locateSignal > 100) locateSignal = 100;
		}
		//Log.d("TRACE", "locateSignal " + locateSignal);

		mSignalAvg.add(locateSignal);
		int avgSignal = (int)mSignalAvg.getAvgValue();

		if (locateSignal == 0)
		{
			selectCrossDipoleAntenna();
			return avgSignal;
		}
		
		switch (mCurrentAnt)
		{
		case ANT_CD:
			// If we get over 40% switch to Circular.
            // It is faster since there is only one antenna to do inventory on,
            // but The crossdipole has slightly better range.
			if (locateSignal > 40)
			{
				selectCircularAntenna();
			}
			break;

		case ANT_CIRCULAR:
            // If we get under 35% switch to CrossDP
            if (locateSignal < 35)
            {
                selectCrossDipoleAntenna();
            }
            // If Circular gets over 95% we have ran out of sensitivity on that
            // antenna and it the proximity antenna is now useful.
            else if (locateSignal >= 95)
            {
            	selectProximityAntenna();
            }
            break;
            
		case ANT_PROXIMITY:
			// Set Circular back on for the next pass
			if (locateSignal < 97)
				selectCircularAntenna();
			break;
			
		default:
			break;
		}
		
		return avgSignal;
	}
}
