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

package com.nordicid.nuraccessory;

import com.nordicid.nurapi.NurApiException;
import com.nordicid.nurapi.NurPacket;

/**
 * Accessory extension's battery information.
 * @author Nordic ID
 *
 * Class containing extended battery information.
 * All fields may not yet be present.
 */
public class NurAccessoryBattery {
	
	/** 5 signed 16-bit values (byte + 4 x uint16_t. */
	public static final int SZ_BATTERY_REPLY = 9;
	
	/** Get battery level and related information sub-command. */
	public static final int ACC_EXT_GET_BATT = 3;
	/** Battery information ("extended information"). */
	public static final int ACC_EXT_GET_BATT_INFO = 9;	
	
	/** Battery level in as 0...100%. */
	public int percentage = NurAccessoryExtension.INTVALUE_NOT_VALID;
	/** Current battery voltage in mV. */
	public int voltage = NurAccessoryExtension.INTVALUE_NOT_VALID;
	/** Current currently drawn in mA. */
	public int current  = 0;
	/** Battery capacity in mAh. */
	public int capacity = NurAccessoryExtension.INTVALUE_NOT_VALID;
	/** True if the device is currently charging the battery. */
	public boolean charging = false;

	// Flags.
	private static final int FLAG_CHARGING = (1 << 0);

	/**
	 * Deserialize a battery reply from the device.
	 * 
	 * @param source Reply bytes from the module.  
	 * @return Returns the class filled with appropriate batteru related information.
	 * @throws Exception
	 */
	public static NurAccessoryBattery deserializeBatteryReply(byte []source) throws Exception
	{
		if (source == null || source.length < SZ_BATTERY_REPLY)
			throw new NurApiException("Accessroy, battery: invalud reply");
		
		NurAccessoryBattery batteryInfo = new NurAccessoryBattery();
		int sourcePtr = 0;

		if ((NurPacket.BytesToWord(source, sourcePtr) & FLAG_CHARGING) != 0)
			batteryInfo.charging = true;
		sourcePtr += 2;

		// Each value occupies 2 bytes.
		batteryInfo.percentage = (source[sourcePtr++] & 0xFF);
		if (batteryInfo.percentage == 0xFF)
			batteryInfo.percentage = NurAccessoryExtension.INTVALUE_NOT_VALID;

		batteryInfo.voltage = (short)NurPacket.BytesToWord(source, sourcePtr);
		sourcePtr += 2;
		batteryInfo.current = (short)NurPacket.BytesToWord(source, sourcePtr);
		sourcePtr += 2;
		batteryInfo.capacity = (short)NurPacket.BytesToWord(source, sourcePtr);

		return batteryInfo;
	}
	
	/**
	 * Battery information get -command.
	 * @return Returns the battery information request command parameter(s). 
	 */
	public static byte []getQueryCommand()
	{
		return new byte [] { ACC_EXT_GET_BATT_INFO } ;		
	}

	public String getPercentageString()
	{
		if (percentage < 0)
			return "N/A";
		return percentage + "%";
	}

	/**
	 * Return battery voltage as a string.
	 *
	 * @return If voltage field is valid then the voltage is returned as a string with unit being mV.
     */
	public String getVoltageString()
	{
		if (voltage < 0)
			return "N/A";
		return voltage + "mV";
	}

	/**
	 * Get battery's current draw as a string.
	 *
	 * @return If current field is valid then the current is returned as a string with unit being mA.
     */
	public String getCurrentString()
	{
		if (current < 0)
			return "N/A";
		return current + "mA";
	}

	/**
	 * Get battery capacity as a string.
	 *
	 * @return If the capacity field is valid then the capacity is returned as a string with the unit beig mAh.
     */
	public String getCapacityString()
	{
		if (capacity < 0)
			return "N/A";
		return capacity + "mAh";
	}
}

