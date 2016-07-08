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

package com.nordicid.nurfwupdate;

/**
 * Created by Nordic ID on 30.5.2016.
 *
 * Class that carries the BLE address and the device's name.
 */
public class BLEDeviceDescription {
    private String mAddress = "";
    private String mName = "";

    /**
     * Basic constructor.
     *
     * @param address MAC address of the device as a string as in "AA:BB:CC...".
     * @param name Name of the device.
     */
    public BLEDeviceDescription(String address, String name)
    {
        mAddress = address.toUpperCase();
        mName = name;
    }

    /**
     * Get the MAC address string.
     *
     * @return MAC address like "AA:BB:CC...".
     */
    public String getAddress()
    {
        return mAddress;
    }

    /**
     * The device name.
     *
     * @return Returns the name of the device that was acquired during the scan.
     */
    public String getName()
    {
        return mName;
    }

    public void setName(String newName)
    {
        mName = newName;
    }

    /**
     * Device description as string.
     * @return Returned in format "name : MAC".
     */
    public String getStringDesc()
    {
        return mName + " : " + mAddress.toUpperCase();
    }
}
