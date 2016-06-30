package com.nordicid.accessorydemo;

public class AvgIntBuffer {
	int[] avgTable;
    int avgCount;
    int avgVal;

    public int getValue() { return avgVal; }

    public AvgIntBuffer(int c)
    {
        avgVal = 0;
        avgCount = 0;
        avgTable = new int[c];
    }

    public void Reset()
    {
        avgVal = 0;
        avgCount = 0;
    }

    public int Add(int val)
    {
        for (int n = avgTable.length - 2; n >= 0; n--)
        {
            avgTable[n + 1] = avgTable[n];
        }

        avgTable[0] = val;

        if (avgCount < avgTable.length)
            avgCount++;

        avgVal = 0;
        for (int n = 0; n < avgCount; n++)
        {
            avgVal += avgTable[n];
        }
        avgVal /= avgCount;

        return avgVal;
    }
}
