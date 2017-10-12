package com.example.yunwen.cloud_tts;

import android.app.Application;
import android.content.Context;

import com.squareup.leakcanary.LeakCanary;

/**
 * Created by 孟晨 on 2017/10/10.
 */

public class MyApplication extends Application {

    private static Context mContext;
    public static Context getContext() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        LeakCanary.install(this);
    }
}
