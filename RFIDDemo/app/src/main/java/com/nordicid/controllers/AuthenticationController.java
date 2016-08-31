package com.nordicid.controllers;

import android.util.Log;

import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiErrors;
import com.nordicid.nurapi.NurApiListener;
import com.nordicid.nurapi.NurAuthenticateParam;
import com.nordicid.nurapi.NurAuthenticateResp;
import com.nordicid.nurapi.NurEventAutotune;
import com.nordicid.nurapi.NurEventClientInfo;
import com.nordicid.nurapi.NurEventDeviceInfo;
import com.nordicid.nurapi.NurEventEpcEnum;
import com.nordicid.nurapi.NurEventFrequencyHop;
import com.nordicid.nurapi.NurEventIOChange;
import com.nordicid.nurapi.NurEventInventory;
import com.nordicid.nurapi.NurEventNxpAlarm;
import com.nordicid.nurapi.NurEventProgrammingProgress;
import com.nordicid.nurapi.NurEventTagTrackingChange;
import com.nordicid.nurapi.NurEventTagTrackingData;
import com.nordicid.nurapi.NurEventTraceTag;
import com.nordicid.nurapi.NurEventTriggeredRead;
import com.nordicid.nurapi.NurTag;
import com.nordicid.tagauth.Helpers;
import com.nordicid.tagauth.ISO29167_10;
import com.nordicid.tagauth.TAMKeyEntry;

import java.io.BufferedReader;
import java.util.ArrayList;

public class AuthenticationController {

	public static final String TAG = "Auth_Controller";
	private int mAuthRounds = 0;
	/** Tells the authentication thread that the cause of the failure was reader being disconnected. */
	private boolean mDisconnected = false;

	/** The key sets. */
	private ArrayList<ArrayList<TAMKeyEntry>> mKeyLists = null;
	/** Total number TAM key entries in the key sets. */
	private int mTotalKeyCount = 0;
	/** The key set to use. */
	private int mUsedKeyNumber = 0;
	/** Challenge generated for the authentication tries. */
	private byte[] mExpectedChallenge;

	private long mStartTime;
	
	private boolean mAuthenticationRunning = false;
	
	private NurApi mApi;
	private AuthenticationControllerListener mListener;
	
	private NurApiListener mThisClassListener = null;

	public NurApiListener getNurApiListener()
	{		
		return mThisClassListener;
	}

	private Thread mAuthThread;
	private Runnable mAuthRunnable;

	/**
	 * Tag authentication parameter error(s):
	 * fatal, can't accept any errors in the authentication parameters.
	 */
	private static final int AUTH_PARAMETER_FAILURE = -1;

	/** Indicates that there is a reply and it was decrypted and interpreted correctly. */
	private static final int AUTH_OK = 0;
	/** Tag replied to authentication OK, but authentication itself could not be made (= "bad tag", not authentic). */
	private static final int AUTH_FAILED = 1;
	/** There was e.g. a communication error with the tag; retry required i.e. do not add to "seen tags" yet. */
	private static final int AUTH_RETRY = 2;
	/**
	 * Tag replied with an error, here: indication of that the tag does not support either this kind of authentication
	 * or that there is e.g. a key number error (i.e. not supported key number by the tag).
	 */
	private static final int AUTH_TAG_ERROR = 3;

	public double getElapsedSecs()
	{		
		if (mStartTime == 0)
			return 0;
		return (double)(System.currentTimeMillis() - mStartTime) / 1000.0;
	}

	public int getTotalKeyCount()
	{
		return mTotalKeyCount;
	}

	public AuthenticationController(NurApi na) {
		mApi = na;
		
		mThisClassListener = new NurApiListener() {
			@Override
			public void connectedEvent() {
				if (mListener != null) {
					mListener.readerConnected();
				}
			}

			@Override
			public void disconnectedEvent() {
				if (mListener != null) {
					mListener.readerDisconnected();
					mDisconnected = true;
					stopAuthentication();
				}
			}

			@Override
			public void inventoryStreamEvent(NurEventInventory event) { }
			@Override public void IOChangeEvent(NurEventIOChange event) {
				// TODO
				// Handle BLE trigger
			}
			@Override public void bootEvent(String arg0) {}
			@Override public void clientConnectedEvent(NurEventClientInfo arg0) {}
			@Override public void clientDisconnectedEvent(NurEventClientInfo arg0) {}
			@Override public void deviceSearchEvent(NurEventDeviceInfo arg0) {}
			@Override public void frequencyHopEvent(NurEventFrequencyHop arg0) {}
			@Override public void inventoryExtendedStreamEvent(NurEventInventory arg0) {}
			@Override public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) {}
			@Override public void programmingProgressEvent(NurEventProgrammingProgress arg0) {}
			@Override public void traceTagEvent(NurEventTraceTag arg0) { }
			@Override public void triggeredReadEvent(NurEventTriggeredRead arg0) {}
			@Override public void logEvent(int arg0, String arg1) {}
			@Override public void debugMessageEvent(String arg0) {}
			@Override public void epcEnumEvent(NurEventEpcEnum event) {}
			@Override public void autotuneEvent(NurEventAutotune event) { }
			@Override public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
			@Override public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }			
		};
	}

	private void authenticationWorker()
	{

	}

	public void clearAllKeys()
	{
		int i;
		mTotalKeyCount = 0;
		if (mKeyLists == null) {
			mKeyLists = new ArrayList<ArrayList<TAMKeyEntry>>();

			for (i=0; i< ISO29167_10.LAST_TAM_KEYNUMBER; i++)
				mKeyLists.add(new ArrayList<TAMKeyEntry>());
		}
		else
			for (i=0;i<ISO29167_10.LAST_TAM_KEYNUMBER;i++)
				mKeyLists.get(i).clear();
	}

	public boolean readKeysFromFile(String fileName)
	{
		BufferedReader reader;
		String line = "";
		boolean error = false;
		boolean done = false;
		TAMKeyEntry newKeyEntry;
		ArrayList<TAMKeyEntry>target;

		reader = Helpers.openInputTextFile(fileName);

		if (reader == null)
		{
			Log.e(TAG, "Could not open \"" + fileName + "\" for reading.");
			return false;
		}

		clearAllKeys();

		do {
			try
			{
				line = reader.readLine().trim();
			}
			catch (Exception ex)
			{
				// Interpret as EOF; number keys is checked for error.
				done = true;
			}

			if (!done)
			{
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				newKeyEntry = TAMKeyEntry.parseKey(line);

				if (newKeyEntry == null)
				{
					error = true;
					continue;
				}

				target = mKeyLists.get(newKeyEntry.getKeyNumber());
				if (!target.contains(newKeyEntry))
				{
					target.add(newKeyEntry);
					mTotalKeyCount++;
				}
				else
					Log.d(TAG, "Duplicate for key index " + mKeyLists.indexOf(target));
			}

		} while (!done && !error);

		Helpers.closeReader(reader);

		if (mTotalKeyCount == 0)
			error = true;

		if (error)
		{
			clearAllKeys();
			// shortToast("Error parsing keys.");
			return false;
		}

		return true;
	}

	/**
	 * Simple TAM1 authentication.
	 */
	private int authenticateTag(NurTag tag, int keyNum, ArrayList<TAMKeyEntry> keysToCheck) {
		NurAuthenticateParam tagAuthParam;
		NurAuthenticateResp tagAuthReply;
		int keyCheckIndex;
		byte[] decryptedReplyBytes;

		// Do one authentication, then check whether the reply is OK with the available key.

		try {
			// Get parameters: store challenge for comparison.
			tagAuthParam = ISO29167_10.buildTAMMessage(false, keyNum, 0, 0, 0, 0, mExpectedChallenge);
		} catch (Exception ex) {
			// Should not happen under any circumstances.
			Log.e(TAG, "authenticateTag : parameter error");
			return AUTH_PARAMETER_FAILURE;
		}

		try {
			tagAuthReply = mApi.gen2v2AuthenticateByEpc(tag.getEpc(), tagAuthParam);
		} catch (Exception ex) {
			// Indicates e.g. communication error.
			// NOTE: tags not supporting "Authenticate" command at all may cause serious trouble.
			return AUTH_RETRY;
		}

		if (tagAuthReply.status != NurApiErrors.NUR_NO_ERROR || tagAuthReply.tagError != -1)
		{
			Log.e(TAG, "authenticateTag, status = " + tagAuthReply.status + ", tag error = " + tagAuthReply.tagError);
			// Some error: report if the tag is detected not to support this type of authentication.
			if (tagAuthReply.tagError != -1)
				return AUTH_TAG_ERROR;
			return AUTH_RETRY;
		}

		// So now there is a reply from the tag. Check whether it is OK.
		keyCheckIndex = 0;
		while (keyCheckIndex < keysToCheck.size()) {
			// ECB decryption of the reply contents.
			try {
				decryptedReplyBytes = NurApi.AES128_ECBDecrypt(tagAuthReply.reply, keysToCheck.get(keyCheckIndex).getKeyValue());
			} catch (Exception ex) {
				// Again, should not happen at this point.
				Log.e(TAG, "authenticateTag : decryption error");
				return AUTH_PARAMETER_FAILURE;
			}
			// Checks the C_TAM and challenge parts.
			if (ISO29167_10.firstBlockOK(decryptedReplyBytes, mExpectedChallenge)) {
				Log.d(TAG, "authenticateTag() : returning AUTH_OK!");
				return AUTH_OK;
			}

			keyCheckIndex++;
		}

		Log.e(TAG, "authenticateTag() : returning AUTH_FAILED!");
		return AUTH_FAILED;
	}

	public boolean isAuthenticationRunning() {
		return mAuthenticationRunning;
	}

	public boolean startTAM1Authentication()
	{
		if (isAuthenticationRunning() || mTotalKeyCount < 1)
			return false;

		mAuthRunnable = new Runnable() {
			@Override
			public void run() {
				authenticationWorker();
			}
		};
		mAuthThread = new Thread(mAuthRunnable);

		mAuthenticationRunning = true;
		mDisconnected = false;

		mAuthThread.start();
		return true;
	}

	public void stopAuthentication()
	{

	}

	public void setListener(AuthenticationControllerListener l) {
		mListener = l;
	}
	
	public void clearInventoryReadings() {
		mStartTime = 0;
		mApi.getStorage().clear();
		
		if (isAuthenticationRunning())
			mStartTime = System.currentTimeMillis();
	}
	
	public interface AuthenticationControllerListener {
		public void onNewOkTags(ArrayList<byte []> okTagList);
		public void onNewFailedTags(ArrayList<byte []> okTagList);
		public void readerDisconnected();
		public void readerConnected();
		public void authenticationStateChanged(boolean executing);
	}
}
