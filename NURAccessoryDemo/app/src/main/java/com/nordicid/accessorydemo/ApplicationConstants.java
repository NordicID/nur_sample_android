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

/**
 * Created by Nordic ID on 25.5.2016.
 */
public class ApplicationConstants {

    // Requests.
    /** Requesting BLE device search. */
    public static final int REQ_BLE_DEVICESEARCH = 1;

    /** Requesting settings; settings activity has returned data. */
    public static final int REQ_READER_SETTINGS = 2;

    /** Requesting application settings; settings activity has returned data. */
    public static final int REQ_APP_SETTINGS = 3;

    /** To detec a disconnect in accessory actions. */
    public static final int REQ_ACCESSORY_ACTIONS = 4;

    /** Requesting new EPC contents. */
    public static final int REQ_NEW_EPC_DATA = 5;

    // Request results.
    /** Request was OK. */
    public static final int RESULT_OK = 0;

    /** Action was OK and BLE needs restart. */
    public static final int RESULT_OK_BLE_RESTART = 1;

    /** Action was OK and currently connected device is removed. */
    public static final int RESULT_OK_FORGET_DEVICE = 2;

    /** Action was canceled. */
    public static final int RESULT_CANCELED = 100;

    /** Any type of error; generic. */
    public static final int RESULT_ERROR = -1;

    /** Action resulted in sudden disconnect of the accessory / reader. */
    public static final int RESULT_UNEXPECTED_DISCONNECT = -2;

    /** Requesting BLE device search. */
    // public static final int REQ_BLE_DEVICESEARCH = 4;

    /** Request code for Bluetooth enable. */
    public static final int REQUEST_ENABLE_BT = 1000;

    /** Request code for Bluetooth enable. */
    public static final int REQUEST_DISABLE_BT = 1001;

    /** Maximum time to scan BLE devices. Unit is seconds. */
    public static final int MAX_BLE_SCANTIME = 15;

    /** String defining the selected device's address. */
    public static final String BLE_SELECTED_ADDRESS = "DEVICE_ADDRESS";

    /** String defining the selected BLE device's name. */
    public static final String BLE_SELECTED_NAME = "DEVICE_NAME";

    /** String defining the changed device name. */
    public static final String BLE_DEVICE_NEW_NAME = "NEW_DEVICE_NAME";

    /** Preferences' name. */
    public static final String APP_PREFERENCES_NAME = "NUR_BLE_DemoPreferences";

    /** String tot check that there are some preferences present. */
    public static final String PREFERENCES_PRESENT = "PreferencesInitilized";

    /** Contains "currently selected tag's EPC as a string. */
    public static final String SELECTED_EPC_STRING = "SELECTED_EPC";

    /** Contains new EPC data as a string. */
    public static final String NEW_EPC_DATA_STRING = "NEW_EPC_DATA";

    /** Here: 500mW. Change as required. */
    public static int TX_LEVEL_HIGH_0W5 = 0;

    /** Here: around 200mW. Change as required. */
    public static int TX_LEVEL_MEDIUM_0W5 = 4;

    /** Here: around 50mW. Change as required. */
    public static int TX_LEVEL_LOW_0W5 = 10;

    /** Here: 1W. Change as required. */
    public static int TX_LEVEL_HIGH_1W = 0;

    /** Here: around 250mW. Change as required. */
    public static int TX_LEVEL_MEDIUM_1W = 7;

    /** Here: around 100mW. Change as required. */
    public static int TX_LEVEL_LOW_1W = 13;

    /** Two seconds. */
    public static final int SEC_2_MILLIS = 2000;

    /** Three seconds. */
    public static final int SEC_3_MILLIS = 3000;

    /** Four seconds. */
    public static final int SEC_4_MILLIS = 4000;

    /** Five seconds. */
    public static final int SEC_5_MILLIS = 5000;

    /** Six seconds. */
    public static final int SEC_6_MILLIS = 6000;

    /** Seven seconds. */
    public static final int SEC_7_MILLIS = 7000;

    /** Default time to scan BLE devices. Unit is seconds. */
    public static final int DEF_BLE_SCANTIME = SEC_4_MILLIS;

    /** Default barcode scan timeout in milliseconds (commanded scan, not HID). */
    public static final int DEF_BARCODE_SCAN_TIMEOUT = SEC_5_MILLIS;
}
