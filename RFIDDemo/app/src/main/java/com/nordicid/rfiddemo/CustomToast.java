package com.nordicid.rfiddemo;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CustomToast {
    public static final boolean LENGTH_LONG = false;
    public static final boolean LENGTH_SHORT = true;

    private Activity mActivity;

    private boolean mHasColor = false;
    private int mColor = -1;

    public CustomToast(Activity activity) {
        mActivity = activity;
    }

    public CustomToast(Activity activity, int textColor) {
        mActivity = activity;
        mColor = textColor;
        mHasColor = true;
    }

    public void show(int strResource, boolean shortToast, int layout, int imageResource)
    {
        String message;
        message = mActivity.getString(strResource);
        show (message, shortToast, layout, imageResource);
    }

    public void show(String message, boolean shortToast, int layoutID, int imageResource)
    {
        LayoutInflater inflater = mActivity.getLayoutInflater();

        View theView = inflater.inflate(layoutID, (ViewGroup) mActivity.findViewById(R.id.toast_layout_root));

        ImageView imageView = (ImageView) theView.findViewById(R.id.custom_toast_image);
        imageView.setImageResource(imageResource);

        TextView textView = (TextView) theView.findViewById(R.id.custom_toast_text);
        textView.setText(message);

        if (mHasColor)
            textView.setTextColor(mColor);

        Toast toast = new Toast(mActivity);

        toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
        toast.setDuration(shortToast ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG);
        toast.setView(theView);

        toast.show();
    }
}

