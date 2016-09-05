package com.nordicid.controllers;

import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.NurApi;
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
import com.nordicid.nurapi.NurTag;
import com.nordicid.nurapi.NurTagStorage;

public class InventoryController {

	private int mInventoryRounds = 0;
	
	private long mInventoryStartTime;
	
	private boolean mInventoryRunning = false;
	
	private NurApi mApi;
	private InventoryControllerListener mInventoryListener;
	
	private NurApiListener mThisClassListener = null;

	public NurApiListener getNurApiListener()
	{		
		return mThisClassListener;
	}

	public double getElapsedSecs()
	{		
		if (mInventoryStartTime == 0)
			return 0;
		return (double)(System.currentTimeMillis() - mInventoryStartTime) / 1000.0;
	}

	public InventoryController(NurApi na) {
		mApi = na;
		
		mThisClassListener = new NurApiListener() {	
			@Override 
			public void inventoryStreamEvent(NurEventInventory event) {

				if (event.tagsAdded > 0) {
					handleInventoryTags();
				}

				if (event.stopped && mInventoryRunning) {
					
					try {
						System.out.println("InventoryController: inventory stream restart...");
						mApi.startInventoryStream();
					}
					catch (Exception err) {
						err.printStackTrace();
					}
				}

				mInventoryRounds += event.roundsDone;
			}
			
			@Override
			public void connectedEvent() {
				if (mInventoryListener != null) {
					mInventoryListener.readerConnected();
				}
			}
			
			@Override
			public void disconnectedEvent() {
				if (mInventoryListener != null) {
					mInventoryListener.readerDisconnected();
					stopInventory();
				}
			}

			@Override public void IOChangeEvent(NurEventIOChange event) {
				// Handle BLE trigger
				if (event.source == NurAccessoryExtension.TRIGGER_SOURCE && event.direction == 0)
				{
					if (isInventoryRunning())
						stopInventory();
					else
						startContinuousInventory();
				}
			}
			@Override public void bootEvent(String arg0) {}
			@Override public void clientConnectedEvent(NurEventClientInfo arg0) {}
			@Override public void clientDisconnectedEvent(NurEventClientInfo arg0) {}
			@Override public void deviceSearchEvent(NurEventDeviceInfo arg0) {}
			@Override public void frequencyHopEvent(NurEventFrequencyHop arg0) {}
			@Override public void inventoryExtendedStreamEvent(NurEventInventory arg0) {}
			@Override public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) {}
			@Override public void programmingProgressEvent(NurEventProgrammingProgress arg0) {}
			@Override public void traceTagEvent(NurEventTraceTag arg0) { }
			@Override public void triggeredReadEvent(NurEventTriggeredRead arg0) {}
			@Override public void logEvent(int arg0, String arg1) {}
			@Override public void debugMessageEvent(String arg0) {}
			@Override public void epcEnumEvent(NurEventEpcEnum event) {}
			@Override public void autotuneEvent(NurEventAutotune event) { }
			@Override public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
			@Override public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }			
		};
	}

	public boolean startContinuousInventory() {
		
		if (mApi.isConnected()) {
			try {
				// Do not use OPFLAGS_INVSTREAM_ZEROS
				mApi.setSetupOpFlags(mApi.getSetupOpFlags() & ~NurApi.OPFLAGS_INVSTREAM_ZEROS);
				mApi.startInventoryStream();
				mInventoryStartTime = System.currentTimeMillis();
				mInventoryRunning = true;
				mInventoryListener.inventoryStateChanged();
			} catch (Exception err) {
				err.printStackTrace();
				return false;
			}
						
			return true;
		}
		else {
			return false;
		}
	}
	
	public boolean isInventoryRunning() {
		return mInventoryRunning;
	}
	
	public boolean stopInventory() {
		try {
			mInventoryRunning = false;
			if (mApi.isConnected()) {
				mApi.stopInventoryStream();
			}
			mInventoryListener.inventoryStateChanged();
		} catch (Exception err) {	
			err.printStackTrace();
			return false;
		}
		
		return true;
	}
	
	private void handleInventoryTags() {
		
		synchronized (mApi.getStorage()) {
			
			NurTagStorage tagStorage = mApi.getStorage();
			
			for (int i = 0; i < tagStorage.size(); i++) {
				
				NurTag tag = tagStorage.get(i);		
				mInventoryListener.tagFound(tag, mInventoryRounds);
			}
			
			tagStorage.clear();
		}
	}
	
	public void setListener(InventoryControllerListener l) {
		mInventoryListener = l;
	}
	
	public void clearInventoryReadings() {
		mInventoryStartTime = 0;
		mApi.getStorage().clear();
		
		if (isInventoryRunning())
			mInventoryStartTime = System.currentTimeMillis();
	}
	
	public interface InventoryControllerListener {
		public void tagFound(NurTag tag, int roundsDone);
		public void readerDisconnected();
		public void readerConnected();
		public void inventoryStateChanged();
	}
}
