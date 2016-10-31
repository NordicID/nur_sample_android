package com.nordicid.rfiddemo;

public class ResourceIdTargetName {
    private int mID = 0;

    private String mTargetName = "";

    public ResourceIdTargetName(int id, String name)
    {
        mID = id;
        mTargetName = name;
    }

    public int getID() { return mID; }

    public String getTargetName() {
        return mTargetName;
    }
}
