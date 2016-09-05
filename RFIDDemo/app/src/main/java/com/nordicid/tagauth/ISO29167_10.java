package com.nordicid.tagauth;

import android.util.Log;

import java.security.InvalidParameterException;
import java.util.Random;


import com.nordicid.nurapi.*;

/**
 * Created by Nordic ID on 28.6.2016.
 */
public class ISO29167_10 {
    public static final String TAG = "ISO29167_10";

    /** TAM EAS key length in bytes. */
    public static final int TAM_KEY_BYTELENGTH = 16;
    /** Minimum allowed key number for the TAMx. */
    public static final int MIN_TAM_KEYNUMBER = 0;
    /** Maximum allowed key number for the TAMx. */
    public static final int LAST_TAM_KEYNUMBER = 255;

    /** TAM1 message length in bits. */
    public static final int TAM1_MSG_BITLEN = 96;
    /** TAM 1 message length in bytes. */
    public static final int TAM1_MSG_BYTELEN = (TAM1_MSG_BITLEN / 8);
    /** TAM challenge length in bytes. */
    public static final int TAM_CHALLENGE_BYTELEN = 10;

    /** Method 1 reply length in bits. */
    public static final int TAM1_RX_BITLEN = 128;
    /** Method 1 reply length in bytes. */
    public static final int TAM1_RX_BYTELEN = (TAM1_RX_BITLEN / 8);

    /** TAM2 custom data block length in bits. */
    public static final int TAM_BLOCK_BITLEN = 64;
    /** TAM2 custom data block length in bytes. */
    public static final int TAM_BLOCK_BYTELEN = (TAM_BLOCK_BITLEN / 8);

    /** TAM2 message length in bits. */
    public static final int TAM2_MSG_BITLEN = 120;
    /** TAM2 message length in bytes. */
    public static final int TAM2_MSG_BYTELEN = (TAM2_MSG_BITLEN / 8);

    /** Value representing the custom data bit (bit 5). */
    public static final byte CUSTOM_DATA_BIT_VALUE = 0x20;

    /** Maximum TAM2 custom data block's offset. */
    public static final int LAST_CUSTOM_DATA_OFFSET = 4095;

    /** Maximum profile number for TAM2. */
    public static final int LAST_PROFILE_NUMBER = 15;

    /** Here: internally limited number of custom data blocks in TAM2. Actual maximum is 32. */
    public static final int MAX_CUSTOM_BLOCKS = 4;

    /** Maximum protection mode indicator for TAM2. */
    public static final int LAST_PROTMODE_NUMBER = 15;

    /** Reply's constant part to look for. */
    public static final int C_TAM = 0x96C5;

    /** TAM2 profile 0: EPC. */
    public static final int PROFILE_EPC = 0;
    /** TAM2 profile 0: TID. */
    public static final int PROFILE_TID = 1;
    /** TAM2 profile 0: user memory. */
    public static final int PROFILE_USERMEM = 2;

    /**
     * Generate challenge for Tag Authentication Method.
     * @return Returns a byte array containing {@link #TAM_CHALLENGE_BYTELEN } number bytes.
     */
    public static byte[]generateChallenge()
    {
        byte []challenge = new byte[TAM_CHALLENGE_BYTELEN];

        (new Random()).nextBytes(challenge);

        return challenge;
    }

    private static void keyChallengeCheckThrow(String prefix, int keyNum, byte []challenge) throws InvalidParameterException
    {
        if (keyNum < MIN_TAM_KEYNUMBER || keyNum > LAST_TAM_KEYNUMBER)
            throw new InvalidParameterException(prefix + " : invalid key number");

        if (challenge == null || challenge.length != TAM_CHALLENGE_BYTELEN)
            throw new InvalidParameterException(prefix + " : invalid challenge");
    }

    /**
     * Build TAM1 message as specified in the ISO 29167-10.
     *
     * @param keyNum The key number to use, range: {@link #MIN_TAM_KEYNUMBER}...{@link #LAST_TAM_KEYNUMBER}.
     * @param challenge The challenge to use in the authentication. Length is exactly {@link #TAM_CHALLENGE_BYTELEN} bytes.
     * @return When successful, returns a byte array containing the TAM1 message. Length is {@link #TAM1_MSG_BYTELEN} bytes.
     *
     * @throws InvalidParameterException Exception is thrown with invalid challenge or key number.
     *
     * @see #getTAM2Message(int, int, int, int, int, byte[])
     * @see #buildTAMMessage(boolean, int, int, int, int, int, byte[])
     */
    public static byte []getTAM1Message(int keyNum, byte []challenge) throws InvalidParameterException
    {
        keyChallengeCheckThrow("getTAM1Message()", keyNum, challenge);
        int dstPtr = 0;
        byte []msg;

        msg = new byte[TAM1_MSG_BYTELEN];

        msg[dstPtr++] = 0;
        msg[dstPtr++] = (byte)keyNum;

        System.arraycopy(challenge, 0, msg, dstPtr, challenge.length);

        return msg;
    }

    /**
     * Build TAM2 message as specified in the ISO 29167-10.
     *
     * @param keyNum The key number to use, range: {@link #MIN_TAM_KEYNUMBER}...{@link #LAST_TAM_KEYNUMBER}.
     * @param profile The profile to use as specified by ISO 29167-10, range: 0...{@link #LAST_PROTMODE_NUMBER}.
     * @param offset The 8-byte (4-word, 64-bit) offset where the custom data is read from, range: 0...{@link #LAST_CUSTOM_DATA_OFFSET}.
     * @param blockCount Number of blocks to read, count is limited here to 0...{@link #MAX_CUSTOM_BLOCKS}.
     * @param protMode Protection mode as defined in the ISO 29167-10, range is 0...{@link #LAST_PROTMODE_NUMBER}.
     * @param challenge The challenge to use in the authentication. Length is exactly {@link #TAM_CHALLENGE_BYTELEN} bytes.
     *
     * @return When successful, returns a byte array containing the TAM2 message. Length is {@link #TAM2_MSG_BYTELEN} bytes.
     *
     * @throws InvalidParameterException Any of the invalid parameters cause an exception.
     *
     * @see #getTAM1Message(int, byte[])
     * @see #buildTAMMessage(boolean, int, int, int, int, int, byte[])
     */
    public static byte[] getTAM2Message(int keyNum, int profile, int offset, int blockCount, int protMode, byte []challenge) throws InvalidParameterException
    {
        byte[] message;
        int dstOffset = 0;

        keyChallengeCheckThrow("getTAM2Message()", keyNum, challenge);

        if (profile < 0 || profile > LAST_PROFILE_NUMBER || offset < 0 || offset > LAST_CUSTOM_DATA_OFFSET ||
            blockCount < 0 || blockCount > MAX_CUSTOM_BLOCKS || protMode < 0 || protMode > LAST_PROTMODE_NUMBER)
            throw new InvalidParameterException("getTAM2Message() : profile or offset or count or mode is invalid");

        profile &= 0x0F;
        offset &= NurApi.MAX_TAM_OFFSET;
        blockCount &= 0x1F;
        protMode &= 0x0F;

        message = new byte[TAM2_MSG_BYTELEN];

        message[dstOffset++] = CUSTOM_DATA_BIT_VALUE;	// Bit 5 = custom data.
        message[dstOffset++] = (byte)keyNum;

        System.arraycopy(challenge, 0, message, dstOffset, TAM_CHALLENGE_BYTELEN);

        dstOffset += TAM_CHALLENGE_BYTELEN;

		/* Profile 4 bits, 4 MSBs from offset. */
        message[dstOffset++] = (byte)((profile << 4) + ((offset >> 8) & 0x0F));

		/* 8 LSBs of the offset */
        message[dstOffset++] = (byte)(offset & 0xFF);

		/* Block count and protection mode. */
        blockCount--;	// Because ISO 29167-10 -> 0 => 1, 1 => 2, ..., 15 (0x0F) => 16.
        message[dstOffset] = (byte)(((blockCount & 0x0F) << 4) + protMode);

        return message;
    }

    /**
     * Build authentication parameters for the NUR API.
     *
     * @param authIsTAM2 Set to true if the scheme is TAM2; false is TAM1.
     * @param keyNum Key number to use as specified by ISO 29167-10. Range is 0...15.
     * @param profile The profile for TAM2 as specified by the ISO 29167-10. Range is 0...15.
     * @param offset Custom data address as an offset from the beginning of the given memory region ("profile", ISO 29167-10). 12-bit value i.e range is 0x000...0xFFF (0...4095).
     * @param blockCount Number of 64-bit (8-byte) custom data blocks for TAM2. NUR API supports 1 to 4 blocks.
     * @param protMode Protection mode as specified by the ISO 29167-10.
     * @param usedChallenge TAM1 or 2 challenge. If null, one is generated.
     *
     * @return Return the authentication parameters to perform either TAM1 or TAM2.
     *
     * @throws InvalidParameterException Exception can be thrown with illegal parameters.
     * @throws NurApiException Exception can be thrown when the final message is set up.
     *
     * @see #getTAM1Message(int, byte[])
     * @see #getTAM2Message(int, int, int, int, int, byte[])
     */
    public static NurAuthenticateParam buildTAMMessage(boolean authIsTAM2, int keyNum, int profile, int offset, int blockCount, int protMode, byte []usedChallenge) throws InvalidParameterException, NurApiException
    {
        NurAuthenticateParam authParam;
        byte []message = null;
        byte []challenge;
        int messageBitLength = 0;
        int receptionBitLength = 0;

        authParam = new NurAuthenticateParam();
        if (usedChallenge == null)
            challenge = generateChallenge();
        else
            challenge = Helpers.makeByteArrayCopy(usedChallenge);

        if (authIsTAM2)
        {
            message = getTAM2Message(keyNum, profile, offset, blockCount, protMode, challenge);
            messageBitLength = TAM2_MSG_BITLEN;
            receptionBitLength = TAM1_RX_BITLEN + blockCount * TAM_BLOCK_BITLEN;

            // Tag reply padding required? (bit length mod 128 should always be 0).
            if ((receptionBitLength % TAM1_RX_BITLEN) != 0)
                receptionBitLength += TAM_BLOCK_BITLEN;
        }
        else
        {
            message = getTAM1Message(keyNum, challenge);
            messageBitLength = TAM1_MSG_BITLEN;
            receptionBitLength = TAM1_RX_BITLEN;
        }

        authParam.setMessage(messageBitLength, message);
        authParam.rxLength = receptionBitLength;
        return authParam;
    }

    /**
     * Check whether the TAM1 reply or TAM2 reply's first block is OK.
     *
     * @param tamReplyFirstBlock Bytes received from the authentication.
     * @param challengeUsed Challenge that was used in the authentication.
     *
     * @return Returns true if the reply length is correct, C_TAM is found and the decrypted challenge matches the the used one.
     *
     * @see #C_TAM
     * @see #TAM_CHALLENGE_BYTELEN
     * @see #getTAM1Message(int, byte[])
     * @see #getTAM2Message(int, int, int, int, int, byte[])
     * @see #buildTAMMessage(boolean, int, int, int, int, int, byte[])
     */
    public static boolean firstBlockOK(byte []tamReplyFirstBlock, byte []challengeUsed)
    {
        if (tamReplyFirstBlock == null || challengeUsed == null) {
            Log.e(TAG, "firstBlockOK : null parameter(s).");
            return false;
        }

        if (tamReplyFirstBlock.length != TAM1_RX_BYTELEN || challengeUsed.length != TAM_CHALLENGE_BYTELEN) {
            Log.e(TAG, "firstBlockOK, length error(s): reply = " + tamReplyFirstBlock.length + " bytes, challenge = " + challengeUsed.length + " bytes");
            return false;
        }

        int i, offset, cTam;

        // Offset 0: C_TAM, 2 bytes (big-endian WORD)
        offset = 0;
        cTam = (tamReplyFirstBlock[offset++] & 0xFF);
        cTam <<= 8;
        cTam |= (tamReplyFirstBlock[offset++] & 0xFF);

        if (cTam != C_TAM) {
            Log.e(TAG, "firstBlockOK, no C_TAM match: " + String.format("0x%04X != 0x%04X, bytes = %02X, %02X", cTam, C_TAM, tamReplyFirstBlock[0], tamReplyFirstBlock[1]));
            return false;
        }

        // Getting the TRnd32:
        // int tRnd32 = 0;
        // tRnd32 = (tamReplyFirstBlock[offset++] & 0xFF);
        // tRnd32 <<= 8;
        // tRnd32 |= (tamReplyFirstBlock[offset++] & 0xFF);
        // tRnd32 <<= 8;
        // tRnd32 |= (tamReplyFirstBlock[offset++] & 0xFF);
        // tRnd32 <<= 8;
        // tRnd32 |= (tamReplyFirstBlock[offset++] & 0xFF);

        // NOTE: if the TRnd32 was acquired, then comment the next one out:
        offset += 4;

        for (i=0; i<TAM_CHALLENGE_BYTELEN; i++, offset++)
        {
            if (challengeUsed[i] != tamReplyFirstBlock[offset]) {
                return false;
            }
        }

        // C_TAM was found and challenge matched.
        return true;
    }
}
