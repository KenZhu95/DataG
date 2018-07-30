package com.example.ken_z.datag;

import android.app.Activity;
import android.app.Application;

import com.baidu.mapapi.SDKInitializer;

import java.util.LinkedList;
import java.util.List;

public class MyApplication extends Application {
    private static MyApplication instance;
    private List<Activity> activityList = new LinkedList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        SDKInitializer.initialize(getApplicationContext());
    }

    public void exit() {
        for (Activity act : activityList) {
            act.finish();
        }
        System.exit(0);
    }
    public void addActivity(Activity act) {
        activityList.add(act);
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
