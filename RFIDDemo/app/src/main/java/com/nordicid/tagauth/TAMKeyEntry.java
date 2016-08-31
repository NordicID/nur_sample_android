package com.nordicid.tagauth;

import com.nordicid.nurapi.*;

import java.security.InvalidParameterException;

/**
 * Created by Nordic ID on 13.7.2016.
 */
public class TAMKeyEntry {

	/** Prefix in a text file line for a key entry. */
	public static final String KEY_DEF_PREFIX = "KEY:";

	/** Key byte data, length is */
	private byte []mKeyValue = null;
	private int mKeyNumber = -1;
	
	/**
	 * Basic constructor.
	 *
	 * @see #setKeyNumber(int)
	 * @see #setKeyValue(byte[])
	 */
	public TAMKeyEntry()
	{
		
	}

	/**
	 * Initializing constructor.
	 * @param keyNumber Key number, range: {@link ISO29167_10#MIN_TAM_KEYNUMBER}...{@link ISO29167_10#LAST_TAM_KEYNUMBER}.
	 * @param keyValue Key data: not null, length is exactly {@link ISO29167_10#TAM_KEY_BYTELENGTH} bytes.
	 *
	 * @throws InvalidParameterException Exception is thrown with invalid initializing parameters.
	 * @see #setKeyNumber(int)
	 * @see #setKeyValue(byte[])
     */
	public TAMKeyEntry(int keyNumber, byte []keyValue)  throws InvalidParameterException
	{
		setKeyNumber(keyNumber);
		setKeyValue(keyValue);
	}

	private void checkPresenceThrow(String strPrefix)
	{
		if (mKeyValue == null || mKeyNumber < ISO29167_10.MIN_TAM_KEYNUMBER || mKeyNumber > ISO29167_10.LAST_TAM_KEYNUMBER)
			throw new InvalidParameterException(strPrefix + " : key value not available");		
	}

	/**
	 * Simple key "validation".
	 *
	 * @param keyValue The key bytes to check.
	 *
	 * @return Return true if the key is not null, { @link ISO29167_10#TAM_KEY_BYTELENGTH } bytes long and is not all ones or zeros.
     */
	public static boolean canAcceptKey(byte []keyValue)
	{
		if (keyValue == null || keyValue.length != ISO29167_10.TAM_KEY_BYTELENGTH)
			return false;
		
		int i, c1, c2;
		byte b;
		
		for (i=0, c1=0, c2=0; i<ISO29167_10.TAM_KEY_BYTELENGTH; i++)
		{
			b = keyValue[i];
			if (b == 0) c1++;
			if (b == 0xFF) c2++;
		}
		
		if (c1 == ISO29167_10.TAM_KEY_BYTELENGTH || c2 == ISO29167_10.TAM_KEY_BYTELENGTH)
			return false;
		
		return true;
	}

	/**
	 * Set key value bytes.
	 * @param source The bytes to set. Must be exactly { @link ISO29167_10#TAM_KEY_BYTELENGTH } bytes long, see { #canAcceptKey }.
	 *
	 * @throws InvalidParameterException Exception is thrown with a key value considered to be invalid.
	 *
	 * @see #getKeyValue()
	 * @see #setKeyNumber(int)
	 * @see #getKeyNumber()
	 * @see #canAcceptKey
	 * @see ISO29167_10#TAM_KEY_BYTELENGTH
     */
	public void setKeyValue(byte [] source) throws InvalidParameterException
	{
		if (!canAcceptKey(source))
			throw new InvalidParameterException("setKeyValue() : invalid key");
		
		mKeyValue = Helpers.makeByteArrayCopy(source);
	}

	/**
	 * Get copy of the key value bytes.
	 *
	 * @return Return new byte array containing he key bytes assigned to this instance.
	 *
	 * @throws InvalidParameterException Exception is thrown if the key value or the key number is not present.
	 *
	 * @see #setKeyValue(byte[])
	 * @see #setKeyNumber(int)
	 * @see #getKeyNumber()
     */
	public byte []getKeyValue() throws InvalidParameterException
	{
		checkPresenceThrow("getKeyValue()");
		return Helpers.makeByteArrayCopy(mKeyValue);
	}

	/**
	 * Assign a key number to this instance.
	 * @param number The key number to set. Range = 0...255 as specified by the ISO 29167-10.
	 *
	 * @throws InvalidParameterException Exception is thrown with invalid key number.
	 *
	 * @see #setKeyValue(byte[])
	 * @see #getKeyValue()
	 * @see #getKeyNumber()
     */
	public void setKeyNumber(int number)  throws InvalidParameterException
	{
		if (number < ISO29167_10.MIN_TAM_KEYNUMBER || number > ISO29167_10.LAST_TAM_KEYNUMBER)
			throw new InvalidParameterException("setKeyNumber() : key number not valid");
		mKeyNumber = number;
	}

	/**
	 * Get the key number value asssigned to thhis instance.
	 * @return Returns the key number if it is valid.
	 * @throws InvalidParameterException Exception is thrown if the key number is not set (i.e. it is out of range as it is by default).
	 *
	 * @see #setKeyValue(byte[])
	 * @see #getKeyValue()
	 * @see #setKeyNumber(int)
     */
	public int getKeyNumber()  throws InvalidParameterException
	{
		checkPresenceThrow("getKeyNumber()");
		return mKeyNumber;
	}

	/**
	 * Parse a string that is expected to represent a valid key value.
	 * @param strKeySpec The string to parse. Example for key 0: "KEY:0;ACDCABBADEADBEEFACDCABBADEADBEEF".
	 *
	 * @return Returns a new key entry instance if the syntax is correct.
     */
	public static TAMKeyEntry parseKey(String strKeySpec)
	{
		TAMKeyEntry tke;
		byte []keyValue;
		int keyNumber;
		String []valuePair;

		strKeySpec = strKeySpec.toUpperCase();

		if (!strKeySpec.startsWith(KEY_DEF_PREFIX))
			return null;

		valuePair = Helpers.splitByChar(strKeySpec.substring(KEY_DEF_PREFIX.length()), ';', true);

		try
		{
			keyNumber = Integer.parseInt(valuePair[0]);
			keyValue = NurApi.hexStringToByteArray(valuePair[1]);
		}
		catch (Exception ex)
		{
			return null;
		}


		tke = new TAMKeyEntry();

		try
		{
			tke.setKeyNumber(keyNumber);
			tke.setKeyValue(keyValue);
		}
		catch (Exception ex)
		{
			tke = null;
		}

		return tke;
	}

	/**
	 * For using "contains" -method in ArrayList.
	 * @param other The object that is compared.
	 * @return Returns true if the "other" object is considered to be the same (basically a byte array match).
     */
	@Override
	public boolean equals(Object other)
	{
		// Return true just to prevent addition of null objects.
		if (other == null || other == this) return true;

		if (other instanceof TAMKeyEntry)
		{
			try
			{
				// If the key bytes match...
				return Helpers.byteArrayCompare(mKeyValue, ((TAMKeyEntry)other).getKeyValue());
			}
			catch (Exception ex) { }
		}

		return false;
	}
	
	// For hash table use.
	@Override
	public int hashCode()
	{
		if (mKeyValue != null)
			return mKeyValue.hashCode();

		return 0;
	}	
}

