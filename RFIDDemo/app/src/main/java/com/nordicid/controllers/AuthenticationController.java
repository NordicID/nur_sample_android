package com.nordicid.controllers;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.nordicid.nuraccessory.NurAccessoryExtension;
import com.nordicid.nurapi.*;
import com.nordicid.tagauth.Helpers;
import com.nordicid.tagauth.ISO29167_10;
import com.nordicid.tagauth.TAMKeyEntry;

import java.io.BufferedReader;
import java.security.InvalidParameterException;
import java.util.ArrayList;

public class AuthenticationController {

	public static final String TAG = "Auth_Controller";

	/**
	 * Tells the authentication thread that the cause of the failure was reader being disconnected.
	 */
	private boolean mDisconnected = false;

	private int mAuthKeyNumber = -1;

	/**
	 * The key sets.
	 */
	private ArrayList<ArrayList<TAMKeyEntry>> mKeyLists = null;
	/**
	 * Total number TAM key entries in the key sets.
	 */
	private int mTotalKeyCount = 0;
	/**
	 * The key set to use.
	 */
	private int mUsedKeyNumber = 0;

	private boolean mAuthenticationRunning = false;

	private NurApi mApi;
	// Notifications/events are sent here.
	private AuthenticationControllerListener mListener;

	// For API events.
	private NurApiListener mThisClassListener = null;

	public NurApiListener getNurApiListener() {
		return mThisClassListener;
	}

	// Authentication thread.
	private Thread mAuthThread;
	private Runnable mAuthRunnable;

	// Storages for tags been processed.
	private NurTagStorage mProcessedTags;
	private NurTagStorage mOkStorage;
	private NurTagStorage mFailStorage;

	/**
	 * Authentication is somewhat time consuming operation to get through
	 * because of the nature of the "in progress" -reply handling - which is at least somewhat subject to interference.
	 * So: keep these values close to minimum and try to simultaneously authenticate as little tags as possible.
	 */
	public static final int AUTH_INV_Q = 4;
	public static final int AUTH_INV_SESSION = 1;
	public static final int AUTH_INV_ROUNDS = 1;

	/**
	 * Tag authentication parameter error(s):
	 * fatal, can't accept any errors in the authentication parameters.
	 */
	public static final int AUTH_PARAMETER_FAILURE = -1;

	/**
	 * Indicates that there is a reply and it was decrypted and interpreted correctly.
	 */
	public static final int AUTH_OK = 0;

	/** Keys were read from a file OK. */
	public static final int KEYS_OK = AUTH_OK;
	/**
	 * Tag replied to authentication OK, but authentication itself could not be made (= "bad tag", not authentic).
	 */
	public static final int AUTH_FAILED = 1;
	/**
	 * There was e.g. a communication error with the tag; retry required i.e. do not add to "seen tags" yet.
	 */
	public static final int AUTH_RETRY = 2;

	/**
	 * Tag replied with an error, here: indication of that the tag does not support either this kind of authentication
	 * or that there is e.g. a key number error (i.e. not supported key number by the tag).
	 */
	public static final int AUTH_TAG_ERROR = 3;

	/** Key file could not be opened. */
	public static final int FILE_OPEN_ERROR = 4;
	/** Key parsing error. */
	public static final int KEY_PARSE_ERROR = 5;
	/** No keys were found in the key file. */
	public static final int NO_KEYS = 6;


	/**
	 * The reader indicated  "invalid command" - authentication is not supported.
	 */
	public static final int AUTH_NOT_SUPPORTED = 10;

	private double mStartTime = 0;
	/**
	 * Get execution time.
	 * @return Returns the execution (authentication) time in milliseconds if needed.
     */
	public double getElapsedSecs() {
		if (mStartTime == 0)
			return 0;
		return (System.currentTimeMillis() - mStartTime) / 1000.0;
	}

	public int getTotalKeyCount() {
		return mTotalKeyCount;
	}

	public AuthenticationController(NurApi api) {
		mApi = api;

		mProcessedTags = new NurTagStorage();
		mOkStorage = new NurTagStorage();
		mFailStorage = new NurTagStorage();

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

			@Override
			public void IOChangeEvent(NurEventIOChange event) {
				if (event.source == NurAccessoryExtension.TRIGGER_SOURCE && event.direction == 0)
				{
					// Handle BLE trigger
					if (mAuthenticationRunning)
						stopAuthentication();
					else
						startTAM1Authentication();
				}
			}

			@Override
			public void clientConnectedEvent(NurEventClientInfo arg0) { }
			@Override
			public void bootEvent(String arg0) { }
			@Override
			public void clientDisconnectedEvent(NurEventClientInfo arg0) { }
			@Override
			public void deviceSearchEvent(NurEventDeviceInfo arg0) { }
			@Override
			public void frequencyHopEvent(NurEventFrequencyHop arg0) { }
			@Override
			public void inventoryExtendedStreamEvent(NurEventInventory arg0) { }
			@Override
			public void nxpEasAlarmEvent(NurEventNxpAlarm arg0) { }
			@Override
			public void programmingProgressEvent(NurEventProgrammingProgress arg0) { }
			@Override
			public void traceTagEvent(NurEventTraceTag arg0) { }
			@Override
			public void triggeredReadEvent(NurEventTriggeredRead arg0) { }
			@Override
			public void logEvent(int arg0, String arg1) { }
			@Override
			public void debugMessageEvent(String arg0) { }
			@Override
			public void epcEnumEvent(NurEventEpcEnum event) { }
			@Override
			public void autotuneEvent(NurEventAutotune event) { }
			@Override
			public void tagTrackingScanEvent(NurEventTagTrackingData event) { }
			@Override
			public void tagTrackingChangeEvent(NurEventTagTrackingChange event) { }
		};
	}

	// Indicate that the tag was either authenticated ok or failed to authenticate.
	private void tagProcessed(NurTag tag, boolean okResult) {
		mProcessedTags.addTag(tag);

		if (okResult) {
			mOkStorage.addTag(tag);
			mListener.onNewOkTag(tag);
		} else {
			mFailStorage.addTag(tag);
			mListener.onNewFailedTag(tag);
		}
	}

	// Authentication worker thread.
	private void authenticationWorker() {
		NurRespInventory resp;
		int index, error;
		NurTagStorage localStorage;
		NurTag tag;
		boolean interrupted = false;
		int storageSize;
		boolean ignored;
		ArrayList<TAMKeyEntry> keySource;
		// This could be generated once; here it is generated per inventory round basis.
		byte[] expectedChallenge;

		keySource = mKeyLists.get(mUsedKeyNumber);

		Log.d(TAG, "authenticate() : start");

		error = AUTH_OK;
		do {
			try {
				mApi.clearIdBuffer();
				resp = mApi.inventory(AUTH_INV_ROUNDS, AUTH_INV_Q, AUTH_INV_SESSION);
			} catch (Exception ex) {
				// Break if connection lost.
				Log.e(TAG, "authenticate(), inventory error : " + ex.getMessage());
				if (ex.getClass() == NurApiException.class) {
					error = ((NurApiException) ex).error;
					if (error == NurApiErrors.TR_TIMEOUT || error == NurApiErrors.TR_NOT_CONNECTED || error == NurApiErrors.TRANSPORT)
						interrupted = true;
				}

				Log.e(TAG, "authenticate() : now continue (1), interrupted: " + (interrupted ? "YES" : "NO"));
				continue;
			}

			if (resp.numTagsFound < 1 || !mAuthenticationRunning || mDisconnected || interrupted) {
				continue;
			}

			Log.d(TAG, "authenticate() : fetching tags.");
			try {
				mApi.fetchTags();
			} catch (Exception ex) {
				Log.e(TAG, "authenticate(), tag fetch exception: " + ex.getMessage());
				// Break if connection lost.
				if (ex.getClass() == NurApiException.class) {
					error = ((NurApiException) ex).error;
					if (error == NurApiErrors.TR_TIMEOUT || error == NurApiErrors.TR_NOT_CONNECTED)
						interrupted = true;
				}
				// else ignore this error
				Log.e(TAG, "authenticate() : now continue (1), interrupted: " + (interrupted ? "YES" : "NO"));
				continue;
			}

			if (!mAuthenticationRunning || mDisconnected)
				continue;

			index = 0;
			localStorage = mApi.getStorage();
			storageSize = mApi.getStorage().size();
			Log.d(TAG, "Processing " + resp.numTagsFound + " tags.");

			while (!interrupted && (index < storageSize) && mAuthenticationRunning && !mDisconnected) {

				Log.e(TAG, "authenticate() : process " + index);
				try {
					tag = localStorage.get(index);
					index++;
				} catch (Exception ex) {
					Log.e(TAG, "Storage failure at index " + index);
					index++;
					continue;
				}

				ignored = mProcessedTags.hasTag(tag);

				if (ignored) {
					Log.d(TAG, "Tag at index " + (index - 1) + " skipped.");
					continue;
				}

				expectedChallenge = ISO29167_10.generateChallenge();
				// Try to authenticate with available keys.
				error = doTAM1Authentication(tag, mAuthKeyNumber, keySource, expectedChallenge);
				Log.d(TAG, "*** authenticate(K = " + mAuthKeyNumber + "), result = " + authResultToString(error));

				if (error == AUTH_NOT_SUPPORTED) {
					interrupted = true;
					Log.e(TAG, "*** authenticate(): bailout, not supported error!");
				}
				else {
					if (error == AUTH_OK)
						tagProcessed(tag, true);
					else if (error == AUTH_FAILED)
						tagProcessed(tag, false);
					else
						Log.d(TAG, "*** authenticate(): tag with EPC " + tag.getEpcString() + " produced authError " + error);
					notifyCountChange();
				}
			}
		} while (mAuthenticationRunning && !mDisconnected && !interrupted);

		if (error == AUTH_NOT_SUPPORTED) {
			mAuthenticationRunning = false;
			if (mListener != null)
				mListener.authenticationStateChanged(false, true);

		}
		Log.d(TAG, "authenticate() : thread exit");
	}

	private void notifyCountChange() {
		if(mListener != null)
			mListener.processedCountChanged(mProcessedTags.size());
	}

	/**
	 * Clear all currently stored key data.
	 */
	public void clearAllKeys()
	{
		int i;
		mTotalKeyCount = 0;

		if (mKeyLists == null) {
			mKeyLists = new ArrayList<ArrayList<TAMKeyEntry>>();

			for (i=0; i <= ISO29167_10.LAST_TAM_KEYNUMBER; i++)
				mKeyLists.add(new ArrayList<TAMKeyEntry>());
		}
		else
			for (i=0; i <= ISO29167_10.LAST_TAM_KEYNUMBER; i++)
				mKeyLists.get(i).clear();
	}

	/**
	 * Clear all currently stored processed tags.
	 */
	public void clearAllTags()
	{
		mProcessedTags.clear();
		mOkStorage.clear();
		mFailStorage.clear();

		if (mListener != null)
			mListener.resetAll();
	}

	public String authResultToString(int result)
	{
		switch (result)
		{
			case AUTH_PARAMETER_FAILURE: return "authentication parameter error";
			case AUTH_OK: return "authentication OK";
			case AUTH_FAILED: return "authentication failure";
			case AUTH_RETRY: return "authentication needs retry";
			case AUTH_TAG_ERROR: return "tag replied with an error";
		}

		return "unknown result " + result;
	}

	/**
	 * Interpret key error.
	 * @param error Error code.
	 * @return Returns the key error message.
	 *
	 * @see #readKeysFromFile(String)
     */
	public static String keyErrorToString(int error)
	{
		switch (error)
		{
			case KEYS_OK: return "no error";
			case FILE_OPEN_ERROR: return "key file could not be opened";
			case KEY_PARSE_ERROR: return "error during key data parsing";
			case NO_KEYS: return "no keys were parsed from the file";
		}

		return "unknown error " + error;
	}

	/**
	 * Read keys from a file.
	 *
	 * Key line format:
	 * key:<N>;key_value
	 * where N is 0...255 and key value is 32-character string containing hex numbers
	 * e.g. 000102030405060708090A0B0C0D0E0F
	 * Comment line starts with '#'-character.
	 *
	 * @param fileName File to read.
	 *
	 * @return Returns #KEYS_OK if no errors occurred in key parsing.
     */
	public int readKeysFromFile(String fileName)
	{

		BufferedReader reader;
		String line = "";
		int error = KEYS_OK;
		boolean done = false;
		TAMKeyEntry newKeyEntry;
		ArrayList<TAMKeyEntry> target;

		reader = Helpers.openInputTextFile(fileName);

		if (reader == null)
		{
			Log.e(TAG, "Could not open \"" + fileName + "\" for reading.");
			return FILE_OPEN_ERROR;
		}

		clearAllKeys();

		do {
			try {
				line = reader.readLine().trim();
			}
			catch (Exception ex) {
				// Interpret as EOF; number of keys is checked for error.
				done = true;
			}

			if (!done)
			{
				if (line.isEmpty() || line.startsWith("#"))
					continue;

				newKeyEntry = TAMKeyEntry.parseKey(line);

				if (newKeyEntry == null)
				{
					error = KEY_PARSE_ERROR;
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

		} while (!done && error == KEYS_OK);

		Helpers.closeReader(reader);

		if (error == KEYS_OK && mTotalKeyCount == 0)
			error = NO_KEYS;

		if (error != 0)
		{
			clearAllKeys();
			return error;
		}

		return KEYS_OK;
	}

	/**
	 * Simple TAM1 authentication.
	 */
	private int doTAM1Authentication(NurTag tag, int keyNum, ArrayList<TAMKeyEntry> keysToCheck, byte []expectedChallenge) {
		NurAuthenticateParam tagAuthParam;
		NurAuthenticateResp tagAuthReply;
		int keyCheckIndex;
		byte[] decryptedReplyBytes;

		// Do one authentication, then check whether the reply is OK with the available key.

		try {
			// Get parameters: store challenge for comparison.
			tagAuthParam = ISO29167_10.buildTAMMessage(false, keyNum, 0, 0, 0, 0, expectedChallenge);
		} catch (Exception ex) {
			// Should not happen under any circumstances.
			Log.e(TAG, "doTAM1Authentication : parameter error");
			return AUTH_PARAMETER_FAILURE;
		}

		tagAuthReply = null;
		try {
			tagAuthReply = mApi.gen2v2AuthenticateByEpc(tag.getEpc(), tagAuthParam);
		}
		catch (NurApiException ne) {
			if (ne.error == NurApiErrors.INVALID_COMMAND) {
				Log.e(TAG, "FATAL ERROR: reader does not support authentication!");
				return AUTH_NOT_SUPPORTED;
			}

			// Other error.
			return AUTH_RETRY;
		}
		catch (Exception ex) {
			// Indicates e.g. communication error.
			// NOTE: tags not supporting "Authenticate" command at all may cause serious trouble.
			Log.e(TAG, "doTAM1Authentication(" + tag.getEpcString() + "): error = " + ex.getMessage());

			return AUTH_RETRY;
		}

		if (tagAuthReply.status != NurApiErrors.NUR_NO_ERROR || tagAuthReply.tagError != -1)
		{
			Log.e(TAG, "doTAM1Authentication, status = " + tagAuthReply.status + ", tag error = " + tagAuthReply.tagError);
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
				Log.e(TAG, "doTAM1Authentication : decryption error");
				return AUTH_PARAMETER_FAILURE;
			}
			// Checks the C_TAM and challenge parts.
			if (ISO29167_10.firstBlockOK(decryptedReplyBytes, expectedChallenge)) {
				return AUTH_OK;
			}

			keyCheckIndex++;
		}

		Log.e(TAG, "doTAM1Authentication() : returning AUTH_FAILED!");
		return AUTH_FAILED;
	}

	public boolean isAuthenticationRunning() {
		return mAuthenticationRunning;
	}

	/**
	 * Set the key number to use in the TAM1 authentication.
	 *
	 * @param keyNumber Key number in range 0...255.
	 *
	 * @throws InvalidParameterException Exception is thrown if there are no key available with given number or the key number is out of range.
     */
	public void setAuthKeyNumber(int keyNumber) throws InvalidParameterException
	{
		if (keyNumber < 0 || keyNumber > ISO29167_10.LAST_TAM_KEYNUMBER || mKeyLists.get(keyNumber).isEmpty())
			throw new InvalidParameterException("setAuthKeyNumber() : number is invalid or no entries found");

		mAuthKeyNumber = keyNumber;
	}

	/**
	 * Get the currently used key number.
	 *
	 * @return Returns the key number (here; "list index") currently in use.
	 *
	 * @throws InvalidParameterException Exception is thrown if the currently set key number is not in range 0...255.
     */
	public int getAuthKeyNumber() throws InvalidParameterException
	{
		if (mAuthKeyNumber < 0 || mAuthKeyNumber > ISO29167_10.LAST_TAM_KEYNUMBER)
			throw new InvalidParameterException("setAuthKeyNumber() : number is invalid or no entries found");

		return mAuthKeyNumber;
	}

	/**
	 * Start the authentication thread.
	 *
	 * @return Returns true if the authentication was started OK or already running.
     */
	public boolean startTAM1Authentication()
	{
		if (mTotalKeyCount < 1 || !mApi.isConnected()) {
			return false;
		}

		if (isAuthenticationRunning()) {
			return true;
		}

		if (mAuthKeyNumber < 0 || mAuthKeyNumber > ISO29167_10.LAST_TAM_KEYNUMBER || mKeyLists.get(mAuthKeyNumber).isEmpty()) {
			return false;
		}

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

		if (mListener != null)
			mListener.authenticationStateChanged(true, false);

		return true;
	}

	public void stopAuthentication()
	{
		if (mAuthenticationRunning) {
			mAuthenticationRunning = false;
			try {
				mAuthThread.join();
			}
			catch (Exception ex) {
				// TODO
				ex.printStackTrace();
			}
			mDisconnected = false;
		}

		if (mListener != null)
			mListener.authenticationStateChanged(false, false);
	}

	public void setListener(AuthenticationControllerListener l) {
		mListener = l;
	}

	public interface AuthenticationControllerListener {
		/**
		 * Update the total number of tags processed.
		 * @param newCount
         */
		public void processedCountChanged(int newCount);

		/**
		 * Add a tag that was successfully authenticated.
		 * @param newTag Tag that was authenticated.
         */
		public void onNewOkTag(NurTag newTag);

		/**
		 * Add a tag that failed to authenticate.
		 * @param newTag Tag that failed to authenticate.
         */
		public void onNewFailedTag(NurTag newTag);
		/** Reader disconnected. */
		public void readerDisconnected();
		/** Reader connected. */
		public void readerConnected();
		/** Start stop change. */
		public void authenticationStateChanged(boolean executing, boolean errorOccurred);
		/** Clear all authentication data. */
		public void resetAll();
	}
}
