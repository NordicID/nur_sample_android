package com.nordicid.rfiddemo;

import android.media.AudioManager;
import android.media.SoundPool;

import com.nordicid.apptemplate.AppTemplate;

/**
 * Created by Mikko on 22.9.2016.
 */
public class Beeper {

    public static final int SHORT = 0;
    public static final int LONG = 1;
    public static final int FAIL = 2;
    private static final int LAST = 3;

    static SoundPool mSoundPool = null;
    static boolean mEnabled = true;

    static int[] mSoundIDs = new int[LAST];

    static public void setEnabled(boolean val) {
        mEnabled = val;
    }

    static public boolean getEnabled() {
        return mEnabled;
    }

    static public void beep(int type)
    {
        if (!mEnabled)
            return;

        if (mSoundPool == null)
        {
            init();
        }

        int id = mSoundIDs[type];
        try {
            mSoundPool.stop(id);
        } catch (Exception e) { }
        try {
            mSoundPool.play(id, 1, 1, 1, 0, 1);
        } catch (Exception e) { }
    }

    static public void init()
    {
        if (mSoundPool == null)
        {
            mSoundPool = new SoundPool(LAST, AudioManager.STREAM_MUSIC, 0);
            mSoundIDs[SHORT] = mSoundPool.load(AppTemplate.getAppTemplate(), R.raw.blep, 1);
            mSoundIDs[LONG] = mSoundPool.load(AppTemplate.getAppTemplate(), R.raw.bleep, 1);
            mSoundIDs[FAIL] = mSoundPool.load(AppTemplate.getAppTemplate(), R.raw.blipblipblip, 1);
        }
    }
}
