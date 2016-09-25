package com.nordicid.controllers;

import android.content.EntityIterator;
import android.os.SystemClock;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Mikko on 25.9.2016.
 */

public class AvgBuffer {
    public class Entry
    {
        public long time;
        public double val;
    }

    LinkedList<Entry> mValues = new LinkedList<Entry>();
    int mMaxAge = 0;
    int mMaxSize = 10;

    private double mAvgValue = 0;//Double.NaN;
    private double mSumValue = 0;//Double.NaN;

    public AvgBuffer(int maxSize, int maxAge)
    {
        mMaxSize = maxSize;
        mMaxAge = maxAge;
    }

    boolean removeOld()
    {
        if (mMaxAge == 0)
            return false;

        boolean ret = false;
        Iterator<Entry> i = mValues.descendingIterator();
        while (i.hasNext())
        {
            Entry e = i.next();
            if (System.currentTimeMillis() - e.time > mMaxAge)
            {
                i.remove();
                ret = true;
            }
        }
        return ret;
    }

    public double getAvgValue()
    {
        return mAvgValue;
    }

    public double getSumValue()
    {
        return mSumValue;
    }

    void calcAvg()
    {
        if (mValues.size() == 0)
        {
            mAvgValue = 0;//Double.NaN;
            mSumValue = 0;
            return;
        }

        double avgVal = 0;
        Iterator<Entry> i = mValues.iterator();
        while (i.hasNext())
        {
            avgVal += i.next().val;
        }
        mSumValue = avgVal;
        mAvgValue = avgVal / mValues.size();
    }

    public void add(double val)
    {
        removeOld();

        while (mValues.size() >= mMaxSize)
        {
            mValues.remove(0);
        }

        Entry e = new Entry();
        e.time = System.currentTimeMillis();
        e.val = val;
        mValues.add(e);

        calcAvg();
    }

    public void clear()
    {
        mValues.clear();
        mAvgValue = 0;//Double.NaN;
        mSumValue = 0;
    }
}
