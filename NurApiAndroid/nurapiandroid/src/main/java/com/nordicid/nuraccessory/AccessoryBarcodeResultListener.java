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

/**
 * Created by Nordic ID on 3.6.2016.
 */
public interface AccessoryBarcodeResultListener {

    /**
     * Barcode result listener.
     *
     * @param result Class containing the asynchronous barcode scan result.
     *
     * @see AccessoryBarcodeResult
     */
    public void onBarcodeResult(AccessoryBarcodeResult result);
}
