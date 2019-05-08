package com.biotag.vehiclenumberscanning;

import android.app.Application;

/**
 * Created by Lxh on 2017/11/7.
 */

public class VehiclenumberscanningApplication extends Application {
    protected  static VehiclenumberscanningApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
    public static VehiclenumberscanningApplication getApplication(){
        return mInstance;
    }
}
