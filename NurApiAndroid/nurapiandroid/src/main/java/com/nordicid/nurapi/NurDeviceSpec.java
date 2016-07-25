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

import android.content.Context;

/**
 * Created by Nordic ID on 18.7.2016.
 */
public class NurDeviceSpec {

    public static final int BOND_NONE = 0;
    public static final int BOND_BONDED = 1;

    public static final String BLE_TYPESTR = "BLE";
    public static final String USB_TYPESTR = "USB";
    public static final String ETH_TYPESTR = "ETH";

    private String mConnectionType = "";
    private String mConnectionAddress = "";
    private int mPort = 0;
    private String mName = "";
    private int mRSSI = 0;
    private int mHasBeenBonded = BOND_NONE;

    public NurDeviceSpec(String connectionAddress, String name, String connectionType, int port, boolean bonded, int rssi)
    {
        mConnectionType = connectionType;
        mConnectionAddress = connectionAddress;
        mPort = port;
        mName = name;
        setBondState(bonded);
        setRSSI(rssi);
    }

    public NurDeviceSpec(String connectionAddress, String name, String connectionType, int port)
    {
        this(connectionAddress, name, connectionType, port, false, 0);
    }

    public void setBondState(boolean hasBeenBonded)
    {
        mHasBeenBonded = hasBeenBonded ? BOND_BONDED : BOND_NONE;
    }

    public int getBondState()
    {
        return mHasBeenBonded;
    }

    public void setRSSI(int newRssi)
    {
        mRSSI = newRssi;
    }

    public int getRSSI()
    {
        return mRSSI;
    }

    public boolean isTypeOfEthernet()
    {
        if (mConnectionType.equalsIgnoreCase(ETH_TYPESTR))
            return true;
        return false;
    }

    public boolean isTypeOfUSB()
    {
        if (mConnectionType.equalsIgnoreCase(USB_TYPESTR))
            return true;
        return false;
    }

    public boolean isTypeOfBLE()
    {
        if (mConnectionType.equalsIgnoreCase(BLE_TYPESTR))
            return true;
        return false;
    }

    public String getType() {
        return mConnectionType;
    }

    public String getAddress() {
        return mConnectionAddress;
    }

    public int getPort() {
        return mPort;
    }

    public String getName() {
        return mName;
    }

    public static NurApiAutoConnectTransport getAutoConnectTransport(Context ctx, String typeString, NurApi api) throws NurApiException
    {
        NurDeviceSpec spec = new NurDeviceSpec("", "", typeString, 0);

        if (spec.isTypeOfBLE())
            return new NurApiBLEAutoConnect(ctx, api);
        else if (spec.isTypeOfUSB())
            return new NurApiUsbAutoConnect(ctx, api);

        throw new NurApiException("NurDeviceSpec::getTransport() : can't determine type of transport");
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof NurDeviceSpec))
            return false;

        if (((NurDeviceSpec)other).getAddress().equalsIgnoreCase(mConnectionAddress))
            return true;

        return false;
    }

    @Override
    public int hashCode()
    {
        return mConnectionAddress.hashCode();
    }
}
