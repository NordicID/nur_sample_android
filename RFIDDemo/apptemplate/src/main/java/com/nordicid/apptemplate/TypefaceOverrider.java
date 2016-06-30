package com.nordicid.apptemplate;

import java.lang.reflect.Field;

import android.content.Context;
import android.graphics.Typeface;

public final class TypefaceOverrider {

    public static void setDefaultFont(Context context,  
    		String staticTypefaceFieldName, String fontAssetName) {
    	
        final Typeface newTypeface = Typeface.createFromAsset(context.getAssets(), fontAssetName);
        
        try {
        	
            final Field staticField = Typeface.class.getDeclaredField(staticTypefaceFieldName);
            staticField.setAccessible(true);
            staticField.set(null, newTypeface);
            
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

}
