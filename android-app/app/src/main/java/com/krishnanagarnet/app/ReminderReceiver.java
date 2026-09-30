package com.krishnanagarnet.app;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        MainActivity.Db db = new MainActivity.Db(context);
        if (db.getOpenTickets().isEmpty()) return;
        String ch="kn-alerts";
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26) nm.createNotificationChannel(new NotificationChannel(ch,"Krishnanagar Net Alerts",NotificationManager.IMPORTANCE_HIGH));
        PendingIntent pi=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(context,ch)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Krishnanagar Net")
            .setContentText("There are open customer complaints waiting for staff attention.")
            .setAutoCancel(true).setContentIntent(pi).build();
        nm.notify((int)(System.currentTimeMillis()/300000),n);
    }
}