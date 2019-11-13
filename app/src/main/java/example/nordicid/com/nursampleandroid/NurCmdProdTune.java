package example.nordicid.com.nursampleandroid;

import com.nordicid.nurapi.NurApi;
import com.nordicid.nurapi.NurApiErrors;
import com.nordicid.nurapi.NurApiException;
import com.nordicid.nurapi.NurCmd;
import com.nordicid.nurapi.NurPacket;
import com.nordicid.nurapi.NurTuneResponse;

public class NurCmdProdTune extends NurCmd
{
    static final public int CMD = 102;

    private int mType = NurApi.AT_WIDE;
    private int mBand = -1;	/* All bands */
    private int mAntenna = 0;
    private boolean mUserSave = false;

    private NurTuneResponse []mResp = null;

    public NurTuneResponse []getResponse()
    {
        return mResp;
    }

    /** Prod tune
     * @throws NurApiException may throw exceptions in case of communication error or invalid reply
     */
    public NurCmdProdTune() throws NurApiException
    {
        super(CMD, 0, 28);
        mResp = new NurTuneResponse[NurApi.AT_NR_OF_BANDS];
    }

    @Override
    public int serializePayload(byte []data, int offset)
    {
        int origOffset;
        byte []tuneCode = new byte[] {
                0x69, 0x61, 0x41, 0x36, 0x54, 0x59, 0x31, 0x4D
        };

        origOffset = offset;

		/*
			Params:
			- type (4)
			- antenna (4)
			- band (4)
			- user save (4)
			- good enough (4)
			- code[8] = { 0 }
		*/

        /* Tune depth */
        offset += NurPacket.PacketDword(data, offset, mType);
        /* Antenna */
        offset += NurPacket.PacketDword(data, offset, mAntenna);
        /* Band */
        offset += NurPacket.PacketDword(data, offset, mBand);
        /* Save ? */
        offset += NurPacket.PacketDword(data, offset, mUserSave ? 1 : 0);
        /* -100 covers all. */
        offset += NurPacket.PacketDword(data, offset, -100);
        /* ProductionTuneMagic */
        offset += NurPacket.PacketBytes(data, offset, tuneCode, tuneCode.length);

        return (offset - origOffset);
    }

    @Override
    public void deserializePayload(byte []data, int offset, int dataLen) throws Exception
    {
        int I, Q, idBm;
        int i, ant, f;

        if (status != NurApiErrors.NUR_NO_ERROR)
            throw new NurApiException(status);

        ant = NurPacket.BytesToDword(data, offset);

        f = NurApi.AT_BAND0;
        offset += 16;	/* ant + 3 x reserved */

        for (i=0;i<NurApi.AT_NR_OF_BANDS;i++)
        {
            I = NurPacket.BytesToDword(data, offset);
            offset += 4;
            Q = NurPacket.BytesToDword(data, offset);
            offset += 4;
            idBm = NurPacket.BytesToDword(data, offset);
            offset += 4;

            mResp[i] = new NurTuneResponse(ant, f, I, Q, idBm);
            f += NurApi.AT_BANDWIDTH;
        }
    }
}