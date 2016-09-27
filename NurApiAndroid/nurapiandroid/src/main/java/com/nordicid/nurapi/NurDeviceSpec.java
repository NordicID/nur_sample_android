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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Nordic ID on 18.7.2016.
 */
public class NurDeviceSpec {

    private String mSpecStr = "";
    private boolean mNeedRegen = false;
    private LinkedHashMap<String, String> mParams = new LinkedHashMap<String,String>();

    public void parse(String specStr)
    {
        mParams.clear();
        mSpecStr = specStr;

        String []parts = specStr.split(";");
        for (String part : parts) {
            int equalPos = part.indexOf('=');
            if (equalPos != -1) {
                String key = part.substring(0, equalPos);
                String val = part.substring(equalPos+1);
                mParams.put(key, val);
            } else {
                mParams.put(part, null);
            }
        }
    }

    public String getSpec()
    {
        if (mNeedRegen) {
            regenerateSpec();
        }
        return mSpecStr;
    }

    public boolean hasPart(String name) {
        return mParams.containsKey(name);
    }

    public String getPart(String name, String def) {
        try {
            return getPart(name);
        }
        catch (Exception ex)
        {
            return def;
        }
    }

    public String getPart(String name) throws Exception {
        String ret = mParams.get(name);
        if (ret == null)
            throw new Exception("Part " + name + " not found");
        return ret;
    }

    public int getPartInt(String name) throws Exception {
        return Integer.parseInt(getPart(name));
    }

    public boolean getPartBoolean(String name) throws Exception {
        return Boolean.parseBoolean(getPart(name));
    }

    public void setPart(String key, String val)
    {
        mNeedRegen = true;
        mParams.put(key, val);
    }

    private void regenerateSpec()
    {
        String ret = "";
        Iterator<LinkedHashMap.Entry<String,String>> itr = mParams.entrySet().iterator();
        while (itr.hasNext())
        {
            LinkedHashMap.Entry<String,String> entry = itr.next();
            if (ret.length() > 0)
                ret += ";";
            ret += entry.getKey();
            if (entry.getValue() != null)
                ret += "=" + entry.getValue();
        }
        mSpecStr = ret;
        mNeedRegen = false;
    }

    public NurDeviceSpec(String specStr)
    {
        parse(specStr);
    }


    public boolean getBondState()
    {
        try {
            return getPartBoolean("bonded");
        } catch (Exception e) {
            return false;
        }
    }

    public int getRSSI()
    {
        try {
            return getPartInt("rssi");
        } catch (Exception e) {
            return 0;
        }
    }

    public String getType()
    {
        try {
            return getPart("type");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getAddress() {
        try {
            return getPart("addr");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public int getPort() {
        try {
            return getPartInt("port");
        } catch (Exception e) {
            return -1;
        }
    }

    public String getName() {
        try {
            return getPart("name");
        } catch (Exception e) {
            return "Unknown";
        }
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == null || !(other instanceof NurDeviceSpec))
            return false;

        if (((NurDeviceSpec)other).getSpec().equalsIgnoreCase(this.getSpec()))
            return true;

        return false;
    }

    @Override
    public int hashCode()
    {
        return getSpec().hashCode();
    }

    public static NurApiAutoConnectTransport createAutoConnectTransport(Context ctx, NurApi api, NurDeviceSpec spec) throws NurApiException
    {
        switch (spec.getType())
        {
            case "BLE":
                return new NurApiBLEAutoConnect(ctx, api);
            case "USB":
                return new NurApiUsbAutoConnect(ctx, api);
            case "TCP":
                return new NurApiSocketAutoConnect(ctx, api);
        }

        throw new NurApiException("NurDeviceSpec::createAutoConnectTransport() : can't determine type of transport: " + spec.getType());
    }
}
