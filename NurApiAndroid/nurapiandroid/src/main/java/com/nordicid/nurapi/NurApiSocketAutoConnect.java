/* 
  Copyright 2016- Nordic ID
  NORDIC ID SOFTWARE DISCLAIMER

  You are about to use Nordic ID Demo Software ("Software"). 
  It is explicitly stated that Nordic ID does not give any kind of warranties, 
  expressed or implied, for this Software. Software is provided "as is" and with 
  all faults. Under no circumstances is Nordic ID liable for any direct, special, 
  incidental or indirect damages or for any economic consequential damages to you 
  or to any third party.

  The use of this software indicates your complete and unconditional understanding 
  of the terms of this disclaimer. 
  
  IF YOU DO NOT AGREE OF THE TERMS OF THIS DISCLAIMER, DO NOT USE THE SOFTWARE.  
*/
package com.nordicid.nurapi;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
//import android.util.Log;
import android.os.Handler;
import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;

public class NurApiSocketAutoConnect implements NurApiAutoConnectTransport
{
	static final String TAG = "NurApiSocketAutoConnect";

	private NurApi mApi = null;
	private Context mContext = null;
	private String mAddress = ""; // Spec addr: ip:port
	private String mHost = "";
	private int mPort = 0;
	private boolean mInvalidAddress = false;

	public NurApiSocketAutoConnect(Context c, NurApi na)
	{
		this.mContext = c;
		this.mApi = na;
	}

	private void disconnect() 
	{
		if (mAutoConnRunning) {
			mAutoConnRunning = false;
			try {
				mAutoConnThread.interrupt();
				mAutoConnThread.join(10000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mAutoConnThread = null;
		}
	}
	
	@Override
	public void onResume() 
	{
		if (mApi.isConnected())
			return;

		this.setAddress(getAddress());
	}
	
	@Override
	public void onDestroy() 
	{
		
	}

	@Override
	public void setAddress(String addr)
	{
		if (addr == mAddress) {
			Log.d(TAG, "setAddress address is same! " + addr);
			if (!mAutoConnRunning) {
				startAutoConnectThread();
			}
			return;
		}

		Log.d(TAG, "setAddress " + addr);

		mInvalidAddress = false;
		if (addr.toLowerCase().equals("disabled"))
		{
			Log.d(TAG, "setAddress DISABLED");
			disconnect();
			mAddress = "Disabled";
			return;
		}

		try {
			URI uri = new URI("my://" + addr);
			String host = uri.getHost();
			int port = uri.getPort();

			if (uri.getHost() == null || uri.getPort() == -1) {
				mInvalidAddress = true;
			} else {
				mHost = host;
				mPort = port;
			}
		} catch (URISyntaxException e) {
			mInvalidAddress = true;
		}

		if (mInvalidAddress) {
			Log.d(TAG, "setAddress INVALID");
			return;
		}

		mAddress = addr;
		Log.d(TAG, "setAddress host=" + mHost);
		Log.d(TAG, "setAddress port=" + mPort);

		try {
			mApi.setTransport(null);
		} catch (Exception e) {
			e.printStackTrace();
		}

		startAutoConnectThread();
	}

	void startAutoConnectThread()
	{
		mAutoConnRunning = true;
		mAutoConnThread = new Thread(mAutoConnRunnable);
		mAutoConnThread.start();
	}

	Runnable mAutoConnRunnable = new Runnable() {
		@Override
		public void run()
		{
			Log.d(TAG, "Auto connect thread started");

			NurApiSocketTransport tr = new NurApiSocketTransport(mHost, mPort);
			try {
				while (mAutoConnRunning)
				{
					if (tr.isConnected()) {
						Thread.sleep(5000);
						continue;
					}

					Log.d(TAG, "Trying to connect");
					try {
						mApi.setTransport(tr);
						mApi.connect();
					} catch (Exception ex) {
						Log.d(TAG, "FAILED");
						Thread.sleep(5000);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Disconnect
			try {
				mApi.setTransport(null);
			} catch (Exception ex) { }

			Log.d(TAG, "Auto connect thread exit");
		}
	};

	boolean mAutoConnRunning = false;
	Thread mAutoConnThread = null;

	@Override
	public void onPause() {
	}

	@Override
	public void onStop() {
		disconnect();
	}

	@Override
	public void dispose() {
		onStop();
	}

	@Override
	public String getType() {
		return "TCP";
	}

	@Override
	public String getAddress() {
		return mAddress;
	}

	@Override
	public String getDetails() {
		if (mInvalidAddress)
			return "Invalid Address";
		return "";
	}
}
