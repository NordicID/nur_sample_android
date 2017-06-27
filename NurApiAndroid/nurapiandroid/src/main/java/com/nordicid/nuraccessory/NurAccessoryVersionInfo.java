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

import android.util.Log;

public class NurAccessoryVersionInfo {

    private String mBootloaderVersion;

    private String mFullApplicationVersion;

    private String mApplicationVersion;

    public NurAccessoryVersionInfo(String version)
    {
            /* Format : <Applicationversion><space><details>;<bootloaderversion> */
            Log.e("NURAccessoryVersion",version);
            String [] versions = version.split(";");
            mBootloaderVersion = (versions.length > 1) ? versions[1] : "1";
            mFullApplicationVersion = versions[0];
            mApplicationVersion = versions[0].split(" ")[0].replaceAll("[^\\d.]", "");
    }

    public String getApplicationVersion() { return mApplicationVersion; }

    public String getFullApplicationVersion() {return mFullApplicationVersion; }

    public String getBootloaderVersion() {return mBootloaderVersion; }
}
