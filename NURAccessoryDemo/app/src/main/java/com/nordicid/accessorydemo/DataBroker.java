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

package com.nordicid.accessorydemo;

import com.nordicid.nuraccessory.*;
import com.nordicid.nurapi.*;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Nordic ID on 26.5.2016.
 */
public class DataBroker {

    // Singleton class used to pass the various class instance and settings to the different parts of the application.
    private static boolean _initialized = false;
    private static DataBroker _dbInstance = null;
    private static BluetoothAdapter _btAdapter = null;

    private static int _bleScanTimeout = ApplicationConstants.DEF_BLE_SCANTIME;
    private static int _barcodeScanTimeout = ApplicationConstants.DEF_BARCODE_SCAN_TIMEOUT;
    private static boolean _rememberDevice = true;

    private static NurApi _nurApi = null;
    
    // Transport used by the API.
    private static NurApiBLEAutoConnect _autoConnectTransport = null;
    private static NurAccessoryExtension _accessoryExtension = null;

    private static Context _preferenceContext = null;
    private static final String PREF_STORE_DEVICE = "AUTO_STORE";
    private static final String PREF_AUTO_ADDRESS = "AUTO_ADDRESS";
    private static final String PREF_AUTO_NAME = "AUTO_NAME";

    private static final String PREF_SEARCH_TIMEOUT = "SEARCH_TIMEOUT";
    private static final String PREF_BARCODE_SCAN_TIMEOUT = "BARCODE_SCAN_TIMEOUT";

    private static NurDeviceSpec _autoconnectDevice = null;

    public static DataBroker getInstance()
    {
        if (!_initialized) {
            _dbInstance = new DataBroker();
            _btAdapter = BluetoothAdapter.getDefaultAdapter();
            _nurApi = new NurApi();
            _initialized = true;
        }
        return _dbInstance;
    }

    public static void setAutoTransport(NurApiBLEAutoConnect acTransport)
    {
		_autoConnectTransport = acTransport;
    }
    
    public static NurApiBLEAutoConnect getAutoTransport()
    {
        return _autoConnectTransport;
    }

    public static void setAccessoryExtension(NurAccessoryExtension accessoryExtension)
    {
        _accessoryExtension = accessoryExtension;
    }

    public static NurAccessoryExtension getAccessoryExtension()
    {
        return _accessoryExtension;
    }

    private DataBroker() {
    }

    public NurApi getNurApi()
    {
        return _nurApi;
    }

    public static int getDeviceSearchTimeout()
    {
        return _bleScanTimeout;
    }

    public static void setDeviceSearchTimeout(int timeout)
    {
        _bleScanTimeout = timeout;
    }

    public static int getBarcodeScanTimeout()
    {
        return _barcodeScanTimeout;
    }

    public static void setBarcodeScanTimeout(int timeout)
    {
        _barcodeScanTimeout = timeout;
    }

    public static boolean rememberDevice()
    {
        return _rememberDevice;
    }

    public static void rememberDevice(boolean remember)
    {
        _rememberDevice = remember;
    }

    public boolean supportsBluetooth()
    {
        return (_btAdapter != null);
    }

    public BluetoothAdapter getBtAdapter()
    {
        return _btAdapter;
    }

    private static boolean checkForOkMac(String strMac)
    {
        String []macParts;
        int a, b, c, d, e, f;
        int i;

        macParts = Helpers.splitByChar(strMac, ':');

        if (macParts.length != 6)
            return false;

        i = 0;
        try {
            a = Integer.parseInt(macParts[i++], 16);
            b = Integer.parseInt(macParts[i++], 16);
            c = Integer.parseInt(macParts[i++], 16);
            d = Integer.parseInt(macParts[i++], 16);
            e = Integer.parseInt(macParts[i++], 16);
            f = Integer.parseInt(macParts[i], 16);
        }
        catch (Exception ex) { return false; }

        if (a < 0 || a > 255 || b < 0 || b > 255 ||
                c < 0 || c > 255 || d < 0 || d > 255 ||
                e < 0 || e > 255 || f < 0 || f > 255)
            return false;

        return true;
    }

    private boolean internalLoadPreferences(Context ctx)
    {
        if (ctx == null)
            return false;

        // Store this as the default.
        _preferenceContext = ctx;

        SharedPreferences prefs = _preferenceContext.getSharedPreferences(ApplicationConstants.APP_PREFERENCES_NAME, Context.MODE_PRIVATE);

        if (prefs.getAll().size() == 0)
        {
            // Build default.
            return savePreferences();
        }

        String strAutoAddr = "";
        String strAutoName = "";

        strAutoAddr = prefs.getString (PREF_AUTO_ADDRESS, "");
        strAutoName = prefs.getString(PREF_AUTO_NAME, "");

        // public NurDeviceSpec(String connectionAddress, String name, String connectionType, int port)
        if (!strAutoAddr.isEmpty() && !strAutoName.isEmpty() && checkForOkMac(strAutoAddr))
            _autoconnectDevice = new NurDeviceSpec(strAutoAddr, strAutoName, NurDeviceSpec.BLE_TYPESTR, 0);
        else
            _autoconnectDevice = null;

        _bleScanTimeout = prefs.getInt(PREF_SEARCH_TIMEOUT, ApplicationConstants.DEF_BLE_SCANTIME);
        _barcodeScanTimeout = prefs.getInt(PREF_BARCODE_SCAN_TIMEOUT, ApplicationConstants.DEF_BARCODE_SCAN_TIMEOUT);
        _rememberDevice = prefs.getBoolean(PREF_STORE_DEVICE, true);

        return true;
    }

    public boolean loadSettings(Context ctx)
    {
        return internalLoadPreferences(ctx);
    }

    public void setPreferenceContext(Context ctx)
    {
        _preferenceContext = ctx;
    }

    public boolean setAutoconnectDevice(String strAddress, String strName)
    {
        if (!strAddress.isEmpty() && !strName.isEmpty() && _preferenceContext != null)
        {
            SharedPreferences prefs = _preferenceContext.getSharedPreferences(ApplicationConstants.APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            _autoconnectDevice = new NurDeviceSpec(strAddress, strName, NurDeviceSpec.BLE_TYPESTR, 0);
            editor.putString(PREF_AUTO_ADDRESS, strAddress);
            editor.putString(PREF_AUTO_NAME, strName);

            editor.apply();
            return true;
        }

        return false;
    }

    public void removeAutoConnectDevice()
    {
        _autoconnectDevice = null;
        if (_preferenceContext != null)
        {
            SharedPreferences prefs = _preferenceContext.getSharedPreferences(ApplicationConstants.APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            editor.putString(PREF_AUTO_ADDRESS, "");
            editor.putString(PREF_AUTO_NAME, "");

            editor.apply();
        }
    }

    public NurDeviceSpec getAutoconnectDevice()
    {
        return _autoconnectDevice;
    }

    public boolean savePreferences()
    {
        if (_preferenceContext == null)
            return false;

        SharedPreferences prefs = _preferenceContext.getSharedPreferences(ApplicationConstants.APP_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String strAutoAddr = "";
        String strAutoName = "";
        
        if (_autoconnectDevice != null)
        {
            strAutoAddr = _autoconnectDevice.getAddress();
            strAutoName = _autoconnectDevice.getName();
        }

        if (!strAutoAddr.isEmpty() && !strAutoName.isEmpty() && checkForOkMac(strAutoAddr))
        {
            editor.putString(PREF_AUTO_ADDRESS, strAutoAddr);
            editor.putString(PREF_AUTO_NAME, strAutoName);
        }
        else
        {
            editor.putString(PREF_AUTO_ADDRESS, "");
            editor.putString(PREF_AUTO_NAME, "");
        }

        editor.putInt(PREF_SEARCH_TIMEOUT, _bleScanTimeout);
        editor.putInt(PREF_BARCODE_SCAN_TIMEOUT, _barcodeScanTimeout);
        editor.putBoolean(PREF_STORE_DEVICE, _rememberDevice);

        editor.apply();
        return true;
    }
}
