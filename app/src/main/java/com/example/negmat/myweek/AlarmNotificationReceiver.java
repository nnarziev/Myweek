package com.example.negmat.myweek;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class AlarmNotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Event event = null;
        try {
            event = Event.parseJson(new JSONObject(intent.getStringExtra("event_json")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (event == null) {
            Log.e("ERROR", "A notification for an event was cancelled due to the exception above.");
            return;
        }

        toggleNotification(context, event, true);
    }

    public static void toggleNotification(Context context, Event event, boolean enable) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "notification_channel");
        builder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(event.event_name + String.format(Locale.US, " after %d minutes", Tools.NOTIFY_DEL_MINS))
                .setContentText(event.event_note)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_SOUND)
                .setContentInfo("Reminder about coming event.");

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null)
            if (enable) notificationManager.notify((int) event.event_id, builder.build());
            else notificationManager.cancel((int) event.event_id);
    }
}
