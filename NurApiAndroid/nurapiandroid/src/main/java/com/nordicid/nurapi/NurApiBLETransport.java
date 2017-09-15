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

import java.io.IOException;
import java.util.Arrays;

import android.util.Log;

/**
 * USB transport for NUR in Android OS.
 * 
 * @author Nordic ID
 * @version 1.0.0
 */
public class NurApiBLETransport implements NurApiTransport 
{
	public static final String TAG = "NurApiBLETransport";
	
	private UartService mService = null;
	private String mAddr;
	RingBuffer mRxBuf = new RingBuffer(1024 * 64);
	Object readLock = new Object();

	/**
	 * The transport constructor.
	 */
	public NurApiBLETransport()
	{       
		mAddr = "NA";
	}
	
	public void setAddress(String addr)
	{
		mAddr = addr;
	}
	
	public String getAddress()
	{
		return mAddr;
	}
	
	public void writeRxBuffer(byte []buffer) {
		mRxBuf.Write(buffer);
		synchronized (readLock) {
			readLock.notifyAll();
		} 
	}
	
	public void setService(UartService srv)
	{
		mRxBuf.Reset();
		mService = srv;
		if (srv == null)
			mConnected = false;
	}
	
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public int readData(byte []buffer) throws IOException
	{
		if (!isConnected()) {
			Log.i(TAG, "read disconnected!");
			return -1;
		}
		
		if (mRxBuf.isEmpty())
		{
			synchronized (readLock) {
				try {
					readLock.wait();
				} catch (InterruptedException e) {
					return 0;
				}
			}
		}
		
		/*if (mRxBuf.isEmpty()) 
		{
			try {
				Thread.sleep(10);				
			} catch (InterruptedException e) {
			}
		}*/
		int len = mRxBuf.getCount();
		if (len == 0)
			return 0;

		if (len > buffer.length)
			len = buffer.length;
		mRxBuf.Read(buffer, len);
		return len;
	}

	/**
	 * {@inheritDoc} 
	 */	
	@Override
	public int writeData(byte []buffer, int len) throws IOException
	{
		if (!isConnected()) {
			Log.i(TAG, "write disconnected!");
			return -1;
		}
		byte [] subArray = Arrays.copyOf(buffer, len);
		mService.writeRXCharacteristic(subArray);
		return subArray.length;
	}

	/**
	 * {@inheritDoc} 
	 */	
	@Override
	public void connect() throws Exception
	{
		if (mService == null)
			throw new Exception("Conn failed; service null!");
		
		Log.i(TAG, "connect OK");
		mConnected = true;
	}
	
	boolean mConnected = false;

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public void disconnect()
	{
		mConnected = false;
		mRxBuf.Reset();
		synchronized (readLock) {
			readLock.notifyAll();
		}
		Log.i(TAG, "disconnect OK");
	}

	/**
	 * {@inheritDoc} 
	 */	
	@Override
	public boolean isConnected()
	{
		return mConnected;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean disableAck()
	{
		return true;
	}
}
