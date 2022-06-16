package com.example.modumessenger.Global;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AppInfoUtil extends Application {

    public static String getAppId(Context context) {
        String appId = "";

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo i = pm.getPackageInfo(context.getPackageName(), 0);
            appId = i.applicationInfo.loadDescription(pm) + "";
        } catch(PackageManager.NameNotFoundException e) {
        }

        return appId;
    }

    public static String getAppName(Context context) {
        String appName = "";

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo i = pm.getPackageInfo(context.getPackageName(), 0);
            appName = i.applicationInfo.loadLabel(pm) + "";
        } catch(PackageManager.NameNotFoundException e) {
        }

        return appName;
    }

    public static String getPackageName(Context context) {
        String packageName = "";

        try {
            PackageManager packagemanager = context.getPackageManager();
            ApplicationInfo appInfo = packagemanager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            packageName = packagemanager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return packageName;
    }

    public static String getVersion(Context context) {
        String versionName = "";

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionName = pInfo.versionName + "";
        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return versionName;
    }

    public static int getVersionCode(Context context) {
        int versionCode = 1;
        try {
            PackageInfo i = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            versionCode = i.versionCode;
        } catch(PackageManager.NameNotFoundException e) {

        }

        return versionCode;
    }

}
