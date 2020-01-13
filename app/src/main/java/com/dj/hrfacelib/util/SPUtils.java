package com.dj.hrfacelib.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SPUtils {

    private static SPUtils spUtils = null;

    public static SPUtils getSpUtils() {
        if(spUtils == null){
            synchronized (SPUtils.class){
                if(spUtils == null){
                    spUtils = new SPUtils();
                }
            }
        }
        return spUtils;
    }

    private SharedPreferences sp = null;

    private void init(Context context){
        if(sp == null){
            sp = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    public void setSharedIntData(Context context, String key, int value) {
        if (sp == null) {
            init(context);
        }
        sp.edit().putInt(key, value).apply();
    }

    public int getSharedIntData(Context context, String key){
        if (sp == null) {
            init(context);
        }
        return sp.getInt(key, 0);
    }

    void setSharedlongData(Context context, String key, Long value) {
        if (sp == null) {
            init(context);
        }
        sp.edit().putLong(key, value).apply();
    }

    Long getSharedlongData(Context context, String key) {
        if (sp == null) {
            init(context);
        }
        return sp.getLong(key, 0L);
    }

    void setSharedFloatData(Context context, String key, Float value) {
        if (sp == null) {
            init(context);
        }
        sp.edit().putFloat(key, value).apply();
    }

    Float getSharedFloatData(Context context, String key) {
        if (sp == null) {
            init(context);
        }
        return sp.getFloat(key, 0f);
    }

    void setSharedBooleanData(Context context, String key, Boolean value) {
        if (sp == null) {
            init(context);
        }
        sp.edit().putBoolean(key, value).apply();
    }

    Boolean getSharedBooleanData(Context context, String key) {
        if (sp == null) {
            init(context);
        }
        return sp.getBoolean(key, false);
    }

    void setSharedStringData(Context context, String key, String value) {
        if (sp == null) {
            init(context);
        }
        sp.edit().putString(key, value).apply();
    }

    String getSharedStringData(Context context, String key)  {
        if (sp == null) {
            init(context);
        }
        return sp.getString(key, "");
    }
}
