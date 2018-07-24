package com.example.ken_z.datag;

import android.app.Activity;
import android.content.Intent;

public class NormalUtils {
    public static void gotoSettings(Activity activity) {
        Intent it = new Intent(activity, DemoNaviSettingActivity.class);
        activity.startActivity(it);
    }

    public static String getTTSAppID() {
        return "11516739";
    }
}
