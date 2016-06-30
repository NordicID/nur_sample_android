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

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by Nordic ID on 25.5.2016.
 */
public class Helpers {

    // Class to help with some commonly used operations such as "OK dialog" etc.
    private Context mContext = null;

    public Helpers(Context ctx)
    {
        mContext = ctx;
    }

    public void shortToast(String message)
    {
        Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
    }

    public void longToast(String message)
    {
        Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
    }

    public static void shortToast(Context ctx, String message)
    {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }

    public static void longToast(Context ctx, String message)
    {
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Lock orientation to portrait.
     * @param activity The calling activity ('this').
     */
    public static void lockToPortrait(Activity activity)
    {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Lock orientation to landscape.
     * @param activity The calling activity ('this').
     */
    public static void lockToLandscape(Activity activity)
    {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    public static String yesNo(boolean yes)
    {
        return (yes ? "YES" : "NO");
    }

    public static String okFail(boolean ok)
    {
        return (ok ? "OK" : "FAILED");
    }

    /**
     * Split a string to a string array based on give character.
     * Can be used e.g. to split comma or semicolon separated string to multiple fields.
     *
     * @param stringToSplit The separated string
     * @param separator Separating character e.g. ',', ';', ':' etc.
     * @param removeEmpty If true then the empty fields are removed.
     *
     * @return Returns an arrays of strings split from the single string. The strings are trimmed i.e. whitespace is removed.
     */
    public static String []splitByChar(String stringToSplit, char separator, boolean removeEmpty)
    {
        String expression;
        ArrayList<String> strList = new ArrayList<>();
        String []arr;
        String tmp;

        expression = String.format("\\%c", separator);
        arr = stringToSplit.split(expression);

        for (String s : arr)
        {
            tmp = s.trim();
            if (removeEmpty && tmp.isEmpty())
                continue;;
            strList.add(tmp);
        }

        return strList.toArray(new String[0]);
    }

    /**
     * Split a string to a string array based on give character.
     * Can be used e.g. to split comma or semicolon separated string to multiple fields.
     * Empty string are removed.
     *
     * @param stringToSplit The separated string
     * @param separator Separating character e.g. ',', ';', ':' etc.
     *
     * @return Returns an arrays of strings split from the single string. The strings are trimmed i.e. whitespace is removed.
     */
    public static String []splitByChar(String stringToSplit, char separator)
    {
        return splitByChar(stringToSplit, separator, true);
    }

    /**
     * Make a string from fields separated by the given character.
     * Can be used e.g. to create comma or semicolon separated data.
     *
     * @param fields The strings to combine.
     * @param separator The character used to separate the fields.
     *
     * @return Returns the fields combined and separated as specified.
     */
    public static String makeSepratedString(String []fields, char separator)
    {
        String combined = "";
        int i;
        combined = fields[0];

        for (i=1;i<fields.length;i++)
            combined += (separator + fields[i]);

        return combined;
    }

    public static void okDialog(Context ctx, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
            }
        };

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", dialogClickListener);
        builder.create();
        builder.show();
    }

    public void okDialog(String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
            }
        };

        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", dialogClickListener);
        builder.create();
        builder.show();
    }

    /**
     * Compare two byte arrays.

     * @param arr1 "Left" side byte array.
     * @param arr2 "Right" side byte array.
     *
     * @return Return true if the arrays' contents are equal.
     */
    public static boolean byteArrayCompare(byte []arr1, byte []arr2)
    {
        if (arr1 == null || arr2 == null || arr1.length != arr2.length || arr1.length < 1 || arr2.length < 1)
            return false;

        int i = 0;

        for (byte b : arr1)
            if (b != arr2[i++]) return false;

        return true;
    }
}
