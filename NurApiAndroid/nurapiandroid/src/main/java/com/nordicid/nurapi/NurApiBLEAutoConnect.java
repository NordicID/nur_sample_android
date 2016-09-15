/* 
  Copyright 2016- Nordic ID 
  NORDIC ID DEMO SOFTWARE DISCLAIMER

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

import com.nordicid.nuraccessory.NurAccessoryExtension;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * A BLE implementation of the automatically connecting transport.
 */
public class NurApiBLEAutoConnect implements UartServiceEvents, NurApiAutoConnectTransport
{
	// Dbg.
	public static final String TAG = "NurApiBLEAutoConnect";

	// The API instance given to this autoconnection instance.
	private NurApi mApi = null;

	// Context given to this autoconnect instance.
	private Context mContext = null;
	
	// The UART service required for this instance.
	private UartService mService = null;
	// Reader / accessory address.
	private String mAddr = "";
	
	boolean mServiceBound = false;

	// The physical transport used here.
	private NurApiBLETransport mTr = new NurApiBLETransport();

	// UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() 
    {
		@Override
        public void onServiceConnected(ComponentName className, IBinder rawBinder) 
		{
			if (mService == null) {
        		mService = ((UartService.LocalBinder) rawBinder).getService();
        		Log.d(TAG, "onServiceConnected NEW service");
			} else {
				Log.d(TAG, "onServiceConnected EXISTING service");
			}
    		
    		if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
    		else
    		{
    			mService.setEventListener(NurApiBLEAutoConnect.this, mContext);
    			if (!NurApiBLEAutoConnect.this.mAddr.equals("") && mService.getConnState() != UartService.STATE_CONNECTED)
    				mService.connect(NurApiBLEAutoConnect.this.mAddr);
    		}
        }

		@Override
        public void onServiceDisconnected(ComponentName classname) {
        		mService = null;
        }
    };

	/**
	 * Basic constructor.
	 *
	 * @param ctx Context: needed here because the is a service assignment.
	 * @param api The NUR API instance that this instance should "notify" about the connection changes.
     */
	public NurApiBLEAutoConnect(Context ctx, NurApi api)
	{		
		this.mContext = ctx;
		this.mApi = api;
	}

	/**
	 * Set address of the accessory / reader. This is the "connect" as well as the "disconnect" (when address is empty) method.
	 *
	 * @param addr Address of the accessory / reader as provided by e.g. the device search.
     */
	@Override
	public void setAddress(String addr)
	{
		if (addr == null)
			addr = "";
		mAddr = addr;
		if (mService != null) {
			mService.close();
			mService.connect(addr);
		} else {
			onResumeInternal();
		}
	}
	
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        final String action = intent.getAction();

	        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
	            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

	            switch (state) {
	            case BluetoothAdapter.STATE_OFF:
	            	onStopInternal();
					try {
		            	if (mApi.isConnected())
		            		mApi.disconnect();
					} catch (Exception e) {
						e.printStackTrace();
					}
	                break;
	            case BluetoothAdapter.STATE_TURNING_OFF:
	                break;
	            case BluetoothAdapter.STATE_ON:
	            	try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	            	onResumeInternal();
	                break;
	            case BluetoothAdapter.STATE_TURNING_ON:
	                break;
	            }
	        }
	    }
	};

	final Handler mConnStateHandler = new Handler();

	@Override
	public void onConnStateChanged() 
	{
		if (mService == null) {
			forceDisconnect();
			return;
		}

		Log.w(TAG, "onConnStateChanged " + mService.getConnState());
		
		if (mService.getConnState() == UartService.STATE_CONNECTED)
		{
            new Thread() {
                @Override
                public void run(){
                    try {
                        Thread.sleep(2000);
                        Log.w(TAG, "set transport null");
                        mApi.setTransport(null);

                        if (mService.getConnState() == UartService.STATE_CONNECTED) {
                            Log.w(TAG, "connect");
                            mTr.setService(mService);
                            mApi.setTransport(mTr);
                            mApi.connect();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                        if (!mApi.isConnected()) {
                            if (mServiceBound) {
                                Log.w(TAG, "reconnect on failure");
                                mService.close();
                                mService.connect(mAddr);
                            }
                        }
                    }
                }
            }.start();
			/*new Thread(new Runnable() {
				@Override
				public void run() {
					try {
                        Thread.sleep(2000);
						Log.w(TAG, "set transport null");
						mApi.setTransport(null);

						if (mService.getConnState() == UartService.STATE_CONNECTED) {
							Log.w(TAG, "connect");
							mTr.setService(mService);
							mApi.setTransport(mTr);
							mApi.connect();
						}

					} catch (Exception e) {
						e.printStackTrace();

						if (!mApi.isConnected()) {
							if (mServiceBound) {
								Log.w(TAG, "reconnect on failure");
								mService.close();
								mService.connect(mAddr);
							}
						}
					}
				}
			}).start();*/
		}
		else if (mService.getConnState() == UartService.STATE_DISCONNECTED)
		{
			forceDisconnect();
		}
		
		Log.w(TAG, "onConnStateChanged done " + mService.getConnState());
	}

	private void forceDisconnect() {
		try {
			Log.w(TAG, "disconnect");
			mApi.disconnect();
			mApi.setTransport(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		bleVersion = "";
		mTr.setService(null);
	}

	/**
	 * Upon data available: write received data to the RX ring buffer.
	 *
	 * @param data Data from the adapter.
     */
	@Override
	public void onDataAvailable(byte[] data)
	{
		mTr.writeRxBuffer(data);
	}

	/**
	 * On pause: NOP.
	 */
	@Override
	public void onPause() {
	}
	
	boolean mBtAdapterChangeEventRegistered = false;

	/**
	 * Handle resume internally.
	 */
	private void onResumeInternal()
	{
		if (mServiceBound && mService == null)
		{
			mServiceBound = false;
			mContext.stopService(new Intent(mContext, UartService.class));
			mContext.unbindService(mServiceConnection);
		}
		
		if (!mServiceBound)
		{
			Intent bindIntent = new Intent(mContext, UartService.class);
			if (!mContext.bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE))
			{
				mServiceBound = false;
				Log.e(TAG, "bindService() failed");
			} else {
				mServiceBound = true;
			}
		}
		
		if (!mAddr.trim().isEmpty() && mService != null)
		{
			if (mService.getConnState() != UartService.STATE_CONNECTED)
				mService.connect(mAddr);
		}
	}

	/**
	 * This method needs to be called on activity's on resume.
	 */
	@Override
	public void onResume() 
	{
		if (!mBtAdapterChangeEventRegistered)
		{
			mBtAdapterChangeEventRegistered = true;
			// Register for broadcasts on BluetoothAdapter state change
			Log.e(TAG, "**** REGISTER RECEIVER ***");
		    IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		    mContext.registerReceiver(mReceiver, filter);
		}
	    
		onResumeInternal();
	}

	/**
	 * Handle graceful stop.
	 */
	private void onStopInternal()
	{
		if (mApi.isConnected()) {
			try {
				mApi.disconnect();				
			} catch (Exception e) {
				e.printStackTrace();
			}
			mTr.setService(null);
		}
		
		try {
			mApi.setTransport(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (mService != null) {

			if (mServiceBound) {
				mServiceBound = false;
				mContext.stopService(new Intent(mContext, UartService.class));
				mContext.unbindService(mServiceConnection);
			}

			mService.close();
			mService = null;
		}
		
		bleVersion = "";
	}

	@Override
	public void onStop() 
	{
		if (mBtAdapterChangeEventRegistered)
		{
			mBtAdapterChangeEventRegistered = false;
			// Unregister broadcast listeners
			mContext.unregisterReceiver(mReceiver);
		}
		onStopInternal();
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public void dispose() {
		onStop();
	}

	@Override
	public String getType() {
		return "BLE";
	}

	@Override
	public String getAddress() {
		return mAddr;
	}

	@Override
	public String getDetails() {
		String details = "BLE connection";
		if (!mAddr.isEmpty())
			details += (" (" + mAddr + ")");

		return details;
/*
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (bluetoothAdapter == null) {
			return "No BT adapter";
		}
		if (!bluetoothAdapter.isEnabled()) {
			return "BT not enabled";
		}

		if (mApi != null && mApi.isConnected()) {
			try {
				if (bleVersion.length() == 0)
				{
					NurAccessoryExtension ext = new NurAccessoryExtension(mApi);
					bleVersion = "v" + ext.getFwVersion();
				}
			} catch (Exception e) {
				e.printStackTrace();
				bleVersion = "v0.0.1";
			}
		}

		return bleVersion;
*/
	}
	
	String bleVersion = "";
}
