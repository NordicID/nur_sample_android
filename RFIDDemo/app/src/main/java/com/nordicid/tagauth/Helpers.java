package com.nordicid.tagauth;

import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;

/**
 * Created by Nordic ID on 28.6.2016.
 */
public class Helpers {
    public static final String TAG = "TagAuthHelpers";

    public static byte []makeByteArrayCopy(byte []source) throws InvalidParameterException
    {
        if (source == null || source.length < 1)
            throw new InvalidParameterException("Helpers::makeBytearrayCopy: invalid source");

        byte []newArray = new byte[source.length];
        System.arraycopy(source, 0, newArray, 0, source.length);

        return newArray;
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

    public static String []splitByChar(String stringToSplit, char separator, boolean removeEmpty)
    {
        String expression;
        ArrayList<String> strList = new ArrayList<String>();
        String []arr;
        String tmp;

        expression = String.format("\\%c", separator);
        arr = stringToSplit.split(expression);

        for (String s : arr)
        {
            tmp = s.trim();
            if (removeEmpty && tmp.isEmpty())
                continue;
            strList.add(tmp);
        }

        return strList.toArray(new String[0]);
    }

    public static void closeReader(BufferedReader reader)
    {
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (Exception ex) { }
        }
    }

    public static BufferedReader openInputTextFile(String fileName)
    {
        try
        {
            return new BufferedReader(new FileReader(fileName));
        }
        catch (Exception ex)
        {
            Log.e(TAG, "Open error: " + ex.getMessage());
        }

        return null;
    }
}
