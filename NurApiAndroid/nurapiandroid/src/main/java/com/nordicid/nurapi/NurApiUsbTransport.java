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

import java.io.IOException;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * USB transport for NUR in Android OS.
 * 
 * @author Nordic ID
 * @version 1.0.0
 */
public class NurApiUsbTransport implements NurApiTransport
{
	static final String TAG = "NurApiUsbTransport";

	private UsbManager mManager = null;
	private UsbDevice mDevice = null;
	private UsbDeviceConnection mDeviceConnection = null;
	private UsbInterface mInterface = null;
	
	private static UsbEndpoint mInput = null;
	private static UsbEndpoint mOutput = null;
	
	private final int TRANSFER_TIMEOUT = 2500;
	
	/**
	 * The transport constructor.
	 * 
	 * @param manager See {@link #android.hardware.usb.UsbManager}.
	 * @param dev See {@link #android.hardware.usb.UsbDevice}.
	 */
	public NurApiUsbTransport(UsbManager manager, UsbDevice dev)
	{
		mManager = manager;
		mDevice = dev;
	}
	
	/**
	 * {@inheritDoc} 
	 */
	@Override
	public int readData(byte []buffer) throws IOException
	{
		int ret = -1;
		if (mDeviceConnection == null) {
			Log.d(TAG, "readData mDeviceConnection == null");
			return ret;
		}

		try {
			ret = mDeviceConnection.bulkTransfer(mInput, buffer, buffer.length, TRANSFER_TIMEOUT);
			//Log.d(TAG, "readData ret: " + ret);
		} catch (Exception e)
		{
			Log.d(TAG, "readData error: " + e.getMessage());
			disconnect();
		}
		return ret;
	}

	/**
	 * {@inheritDoc} 
	 */	
	@Override
	public int writeData(byte []buffer, int len) throws IOException
	{
		int ret = -1;
		if (mDeviceConnection == null) {
			Log.d(TAG, "writeData mDeviceConnection == null");
			return ret;
		}

		try {
			ret = mDeviceConnection.bulkTransfer(mOutput, buffer, len, TRANSFER_TIMEOUT);
			//Log.d(TAG, "writeData ret: " + ret);
		} catch (Exception e)
		{
			Log.d(TAG, "writeData error: " + e.getMessage());
			disconnect();
		}
		return ret;
	}

	/**
	 * {@inheritDoc} 
	 */	
	@Override
	public void connect() throws Exception
	{
		try
		{
			mDeviceConnection = mManager.openDevice(mDevice);
	
			if (mDevice.getInterfaceCount() != 2)
			{
				throw new Exception("Invalid interface count");
			}

			mInterface = mDevice.getInterface(1);
			mDeviceConnection.claimInterface(mInterface, true);
	
			int endPts = mInterface.getEndpointCount();
	
			for (int i = 0; i < endPts; i++)
			{
				UsbEndpoint endpoint = mInterface.getEndpoint(i);
				
				if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK)
				{
					if (endpoint.getDirection() == UsbConstants.USB_DIR_IN)
					{
						mInput = endpoint;
					}
					else if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT)
					{
						mOutput = endpoint;
					}
				}
			}
			
			if (mInput == null || mOutput == null)
			{
				throw new Exception("Could not open EP's");
			}

			Log.d(TAG, "connect OK");
		}
		catch (Exception ex)
		{
			Log.d(TAG, "connect failed: " + ex.getMessage());
			disconnect();
			throw ex;
		}
	}

	/**
	 * {@inheritDoc} 
	 */
	@Override
	public void disconnect()
	{
		Log.d(TAG, "disconnect");

		synchronized (this) {
			if (mDeviceConnection != null) {
				if (mInterface != null) {
					mDeviceConnection.releaseInterface(mInterface);
				}
				mDeviceConnection.close();
			}
		}
		
		mDeviceConnection = null;
		mInterface = null;
		mInput = null;
		mOutput = null;
	}

	/**
	 * {@inheritDoc} 
	 */	
	@Override
	public boolean isConnected()
	{
		return (mDeviceConnection != null);
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
