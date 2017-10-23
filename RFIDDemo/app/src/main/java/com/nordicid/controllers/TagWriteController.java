package com.nordicid.controllers;

import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiException;
import com.nordicid.nurapi.NurApiListener;
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
import com.nordicid.nurapi.NurRespInventory;
import com.nordicid.nurapi.NurTag;
import com.nordicid.nurapi.NurTagStorage;
import android.util.Log;

public class TagWriteController {
	
	private NurApi mApi;
	private WriteTagControllerListener mWriteListener;
	private NurApiListener mThisClassListener = null;
	
	public TagWriteController(NurApi an) {
		mApi = an;
		setApi();
	}
	
	public NurApiListener getNurApiListener()
	{
		return mThisClassListener;
	}
	
	private void setApi() {
		mThisClassListener = new NurApiListener() {			
			@Override 
			public void connectedEvent() {
				if (mWriteListener != null) {
					mWriteListener.readerConnected();
				}
			}
			
			@Override 
			public void disconnectedEvent() {
				if (mWriteListener != null) {
					mWriteListener.readerDisconnected();
				}
			}

			@Override public void IOChangeEvent(NurEventIOChange arg0) {}
			@Override public void bootEvent(String arg0) {}
			@Override public void clientConnectedEvent(NurEventClientInfo arg0) {}
			@Override public void clientDisconnectedEvent(NurEventClientInfo arg0) {}
			@Override public void deviceSearchEvent(NurEventDeviceInfo arg0) {}
			@Override public void frequencyHopEvent(NurEventFrequencyHop arg0) {}
			@Override public void inventoryExtendedStreamEvent(NurEventInventory event) { }
			@Override public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) {}
			@Override public void programmingProgressEvent(NurEventProgrammingProgress arg0) {}
			@Override public void traceTagEvent(NurEventTraceTag arg0)  { }
			@Override public void triggeredReadEvent(NurEventTriggeredRead arg0) {}
			@Override public void logEvent(int arg0, String arg1) {}
			@Override public void debugMessageEvent(String arg0) {}
			@Override public void inventoryStreamEvent(NurEventInventory event) { }
			@Override public void epcEnumEvent(NurEventEpcEnum event) { }
			@Override public void autotuneEvent(NurEventAutotune event) { }
			@Override public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
			@Override public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }			
		};
	}

	String mLastWriteError = "";

	public String getLastWriteError()
	{
		return mLastWriteError;
	}

	public boolean writeTagByEpc(byte[] epcBuffer, int epcBufferLength, 
			int newEpcBufferLength, byte[] newEpcBuffer) {
		
		boolean ret = true;
		int savedTxLevel = 0;
		int savedAntMask = 0;
		
		try {
			// Make sure antenna autoswitch is enabled
			if (mApi.getSetupSelectedAntenna() != NurApi.ANTENNAID_AUTOSELECT)
				mApi.setSetupSelectedAntenna(NurApi.ANTENNAID_AUTOSELECT);

			savedTxLevel = mApi.getSetupTxLevel();
			savedAntMask = mApi.getSetupAntennaMaskEx();
			
			// Attempt to use circular antenna
			int circMask = TraceAntennaSelector.getPhysicalAntennaMask(mApi.getAntennaMapping(), "Circular");
			if (circMask != 0)
				mApi.setSetupAntennaMaskEx(circMask);
			
			// Full power
			mApi.setSetupTxLevel(0);
			
		} catch (Exception e) {
			e.printStackTrace();
			ret = false;
		}

		if (ret)
		{
			try {
				/*
				// Write tag
				for(int x=0;x<newEpcBufferLength;x++)
				{
					Log.e("0","X=" + String.valueOf(x) + "=" + String.valueOf(newEpcBuffer[x]));
				}
				*/
				//Log.e("0","newLen="+String.valueOf(newEpcBufferLength) + " oldLen=" + String.valueOf(epcBufferLength));
				mApi.writeEpcByEpc(epcBuffer, epcBufferLength, newEpcBufferLength, newEpcBuffer);
				//Just testing reading by epc
				/*
				byte [] arr = mApi.readTagByEpc(epcBuffer,epcBufferLength,NurApi.BANK_USER,0,64);
				Log.e("0","Length=" + String.valueOf(arr.length));

				for(int x=0;x<arr.length;x++)
				{
					Log.e("0","mem=" + String.valueOf(x) + "=" + String.valueOf(arr[x]));
				}
				*/

				ret = true;
			}
			catch (Exception err)
			{
				mLastWriteError = err.getMessage();
				Log.e("0","READERR=" + mLastWriteError);
				ret = false;
			}
		}
		
		// Restore
		try {
			mApi.setSetupTxLevel(savedTxLevel);
			mApi.setSetupAntennaMaskEx(savedAntMask);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return ret;
	}
	
	public void setListener(WriteTagControllerListener l) {
		mWriteListener = l;
	}
	
	public interface WriteTagControllerListener {
		public void readerDisconnected();
		public void readerConnected();
	}
	
}
