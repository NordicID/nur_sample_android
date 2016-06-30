package com.nordicid.controllers;

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
import com.nordicid.nurapi.NurRespReadData;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class TraceTagController {

	private NurApi mApi;
	private TracedTagInfo mTracedTagInfo = new TracedTagInfo();
	private TraceTagListener mTraceListener;
	private TraceAntennaSelector mTraceAntSelector = new TraceAntennaSelector();

	private long mLastUpdateTime = 0;
	private long mUpdateInterVal = 100;

	private boolean mEnableBeep = true;

	private Handler mHandler;
	
	private NurApiListener mThisClassListener = new NurApiListener() {
		@Override
		public void traceTagEvent(NurEventTraceTag event) {
			handleTraceTag(event);
		}

		@Override
		public void IOChangeEvent(NurEventIOChange arg0) { }

		@Override
		public void bootEvent(String arg0) { }

		@Override
		public void clientConnectedEvent(NurEventClientInfo arg0) { }

		@Override
		public void clientDisconnectedEvent(NurEventClientInfo arg0) { }

		@Override
		public void deviceSearchEvent(NurEventDeviceInfo arg0) { }

		@Override
		public void connectedEvent() {
			if (mTraceListener != null) {
				mTraceListener.readerConnected();
			}
		}

		@Override
		public void disconnectedEvent() {
			System.out.println("Trace controller: the reader is disconnected.");
			
			try {
				mApi.stopTraceTag();
			} catch (Exception err) {
				err.printStackTrace();
			}

			if (mTraceListener != null) {
				mTraceListener.readerDisconnected();
			}
		}

		@Override
		public void frequencyHopEvent(NurEventFrequencyHop arg0) { }
		@Override
		public void inventoryExtendedStreamEvent(NurEventInventory event) { }
		@Override
		public void inventoryStreamEvent(NurEventInventory event) { } 
		@Override
		public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) { }
		@Override
		public void programmingProgressEvent(NurEventProgrammingProgress arg0) { }
		@Override
		public void triggeredReadEvent(NurEventTriggeredRead arg0) { }
		@Override
		public void logEvent(int arg0, String arg1) { }
		@Override
		public void debugMessageEvent(String arg0) { }
		@Override
		public void epcEnumEvent(NurEventEpcEnum event) { }
		@Override
		public void autotuneEvent(NurEventAutotune event) { }
		@Override
		public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
		@Override
		public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
	};

	public TraceTagController(NurApi na) {
		mHandler = new Handler(Looper.getMainLooper());
		mApi = na;
	}

	public NurApiListener getNurApiListener() {
		return mThisClassListener;
	}
	
	private void handleTraceTag(NurEventTraceTag event)
	{
		int signalStrength = 0;
		try {
			signalStrength = mTraceAntSelector.adjust(event.scaledRssi);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mTracedTagInfo.scaledRssi = signalStrength;

		final long currentTime = System.currentTimeMillis();

		if (currentTime - mLastUpdateTime > mUpdateInterVal) 
		{
			mHandler.removeCallbacksAndMessages(null);
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mTraceListener.traceTagEvent(mTracedTagInfo);
					mLastUpdateTime = currentTime;
				}
			});
		}
	}

	private NurEventTraceTag doTracePass()
    {
		NurEventTraceTag ret = new NurEventTraceTag();
		ret.antennaId = 0;
    	ret.rssi = -127;
    	ret.scaledRssi = 0;
    	
    	//Log.d("TRACE", "doTracePass() +");
    	
        for (int i = 0; i < 3 && mTraceRunning; i++)
        {
            try
            {
            	NurRespReadData data = mApi.traceTagByEpc(mTracedTagInfo.epc, mTracedTagInfo.epc.length, NurApi.TRACETAG_NO_EPC);
            	ret.antennaId = data.antennaID;
            	ret.rssi = data.rssi;
            	ret.scaledRssi = data.scaledRssi;
            	break;
            }
            catch (Exception ex)
            {
                // retry
            }
        }
    	//Log.d("TRACE", "doTracePass() - " + ret.scaledRssi);
        return ret;
    }
	
	boolean mTraceRunning = false;
		
	Runnable mBeeperThreadRunnable = new Runnable() {
		@Override
		public void run() 
		{	
			ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 80);
			
			while (mTraceRunning)
			{
				int avgStrength = mTraceAntSelector.getSignalStrength();
				int sleepTime = 1000;
				int beepDuration = 100;
				int toneType = ToneGenerator.TONE_DTMF_0;
				
				if (avgStrength > 0)
                {
					toneType = ToneGenerator.TONE_DTMF_4 + (avgStrength / 10); //ToneGenerator.TONE_DTMF_9;
                    sleepTime = 160 - avgStrength;
                    beepDuration = 50;
                }
				
				if (mEnableBeep)
					toneGen.startTone(toneType, beepDuration);
				
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}; 
	Thread mBeeperThread;

	Runnable mTraceThreadRunnable = new Runnable() {
		@Override
		public void run() {
			
			try {
				mTraceAntSelector.begin(mApi);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			while (mTraceRunning)
			{
				NurEventTraceTag ev = doTracePass();
				if (mTraceRunning)
					handleTraceTag(ev);
			}
			
			try {
				mTraceAntSelector.stop();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	Thread mTraceThread;

	public boolean startTagTrace(byte[] epc) 
	{
		if (isTracingTag())
			return false;
		
		if (mApi.isConnected() && epc.length > 0) {
			try {
				mTracedTagInfo.epc = epc;				
				
				mTraceRunning = true;
				mTraceThread = new Thread(mTraceThreadRunnable);
				mTraceThread.start();
				
				mBeeperThread = new Thread(mBeeperThreadRunnable);
				mBeeperThread.start();
			} catch (Exception err) {
				err.printStackTrace();
				return false;
			}

			return true;
		}
		
		return false;		
	}

	public boolean stopTagTrace() {
		if (!isTracingTag())
			return false;
		
		try {
			mTraceRunning = false;
			if (mTraceThread != null)
			{
				mTraceThread.join(5000);
				mTraceThread = null;
			}
			if (mBeeperThread != null)
			{
				mBeeperThread.join(5000);
				mBeeperThread = null;
			}
						
		} catch (Exception err) {
			err.printStackTrace();
			return false;
		}

		return true;
	}

	public boolean isTracingTag() {
		return mTraceRunning;
	}

	public void setEnableBeep(boolean val) {
		mEnableBeep = val;
	}

	public void setUpdateInterval(int interval) {
		mUpdateInterVal = interval;
	}

	public void setListener(TraceTagListener l) {
		mTraceListener = l;
	}

	public interface TraceTagListener {
		public void traceTagEvent(TracedTagInfo data);
		public void readerDisconnected();
		public void readerConnected();
	}

	public class TracedTagInfo {		
		public int scaledRssi;
		public byte[] epc;
	}
}
