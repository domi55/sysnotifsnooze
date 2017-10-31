package com.iboalali.sysnotifsnooze;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alali on 27-Aug-17.
 */

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private NotificationListenerBroadcastReceiver notificationListenerBroadcastReceiver;
    ComplexPreferences complexPreferences;

    /**
     * This is set on the notification shown by the activity manager about all apps
     * running in the background.  It indicates that the notification should be shown
     * only if any of the given apps do not already have a {@link Notification#FLAG_FOREGROUND_SERVICE}
     * notification currently visible to the user.  This is a string array of all
     * package names of the apps.
     * @hide
     */
    public static final String EXTRA_FOREGROUND_APPS = "android.foregroundApps";

    @Override
    public void onCreate() {
        super.onCreate();
        notificationListenerBroadcastReceiver = new NotificationListenerBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(getString(R.string.string_filter_intent));
        registerReceiver(notificationListenerBroadcastReceiver, filter);
        complexPreferences = ComplexPreferences.getComplexPreferences(getApplicationContext(), getApplicationContext().getString(R.string.shared_pref_name), MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationListenerBroadcastReceiver != null) {
            unregisterReceiver(notificationListenerBroadcastReceiver);
        }
    }

    private void checkAndSnoozeNotification(StatusBarNotification sbn)
    {
        if (sbn.getPackageName().equals("android") && sbn.getNotification().extras.containsKey(EXTRA_FOREGROUND_APPS)) {
            String key = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
            if (key == null) return;

            String[] svcs = sbn.getNotification().extras.getStringArray(EXTRA_FOREGROUND_APPS);

            // checking for null, just to avoid a potential null exception
            if (svcs != null){
                // if key exist, add new package names to the list and put in shared preferences
                if (complexPreferences.contains(getString(R.string.shared_pref_key_package_name))) {
                    List<String> pns = complexPreferences.getObject(getString(R.string.shared_pref_key_package_name), PackageNameList.class).getPackageNames();
                    List<String> newList = new ArrayList<>(pns);
                    for (String s : svcs) {
                        if(!pns.contains(s)){
                            newList.add(s);
                        }
                    }

                    complexPreferences.putObject(getString(R.string.shared_pref_key_package_name), newList);
                    complexPreferences.apply();

                    // if key doesn't exist, just put the whole list in shared preferences
                }else{
                    PackageNameList packageNameList = new PackageNameList();
                    packageNameList.setPackageNames(svcs);

                    complexPreferences.putObject(getString(R.string.shared_pref_key_package_name), packageNameList);
                    complexPreferences.apply();
                }
            }

            snoozeNotification(sbn.getKey(), 10000000000000L);
            //Long.MAX_VALUE = 9223372036854775807 = 292.5 million years -> not working
            //10000000000000 = 317.09792 years -> working

            Log.d(TAG, sbn.getPackageName() + ": " + key + ", snoozed");
        }
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null)
            return;

        //checkAndSnoozeNotification(sbn);
    }

    class NotificationListenerBroadcastReceiver extends BroadcastReceiver{
        private static final String TAG = "NL Broadcast Receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Received Broadcast");

            if (intent.getStringExtra("command").equals("hide")) {
                for (StatusBarNotification sbn : NotificationListener.this.getActiveNotifications()) {
                    //checkAndSnoozeNotification(sbn);
                }
            }else if(intent.getStringExtra("command").equals("extra_hide")){
                for (StatusBarNotification sbn : NotificationListener.this.getActiveNotifications()) {
                    //checkAndSnoozeNotification(sbn);
                    if (sbn.getPackageName().equals("android") && sbn.getNotification().extras.containsKey(EXTRA_FOREGROUND_APPS)) {
                        NotificationListener.this.snoozeNotification(sbn.getKey(), 10000000000000L);

                        String key = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
                        Log.d(TAG, sbn.getPackageName() + ": snoozed");
                    }
                }
            }else if (intent.getStringExtra("command").equals("extra_show")){
                for (StatusBarNotification sbn : NotificationListener.this.getSnoozedNotifications()) {
                    if (sbn.getPackageName().equals("android") && sbn.getNotification().extras.containsKey(EXTRA_FOREGROUND_APPS)) {
                        NotificationListener.this.snoozeNotification(sbn.getKey(), -100000000000000L);
                        NotificationListener.this.cancelNotification(sbn.getKey());

                        

                        String key = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
                        Log.d(TAG, sbn.getPackageName() + ": un-snoozed");
                    }
                }
            }



        }
    }
}


