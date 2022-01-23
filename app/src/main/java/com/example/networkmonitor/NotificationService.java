package com.example.networkmonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationService extends Service {
    private final static int SECOND = 1000;
    private final static int MILLION=1000000;
    private NotificationCompat.Builder builder;
    private NotificationManagerCompat notificationManager;
    private Notification notification;
    private Thread thread;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent intent1 = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0);
        notification = new NotificationCompat.Builder(this, "ChannelId1")
                .setContentTitle("Notification Title")
                .setContentText("Notification text")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .build();
        builder = new NotificationCompat.Builder(this, getString(R.string.channel));
        notificationManager= NotificationManagerCompat.from(this);

        startForeground(1, notification);
        setUpWorkerThread();

        return START_STICKY;
    }

    private void setUpWorkerThread() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    long tempDown = TrafficStats.getTotalRxBytes();
                    long tempUp = TrafficStats.getTotalTxBytes();


                    try {
                        Thread.sleep(SECOND);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    double currDown = (double) TrafficStats.getTotalRxBytes() - tempDown;
                    double currUp = (double) TrafficStats.getTotalTxBytes() - tempUp;

                    Intent intent1 = new Intent(NotificationService.this, MainActivity.class);
                    PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this, 0, intent1, 0);
                    notification = new NotificationCompat.Builder(NotificationService.this, "ChannelId1")
                            .setContentTitle("Download:" + currDown / MILLION + "MB/s  " + "Upload:" + currUp / MILLION + "MB/s")
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentIntent(pendingIntent)
                            .setSilent(true)
                            .build();
                    notificationManager.notify(1, notification);
                }
            }
        };
        thread=new Thread(runnable);
        thread.start();
    }


    private void createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel("ChannelId1", "ForeGround Notification", NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(notificationChannel);

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    /******** Jos uvek ne znas da li ovo sklanja skroz service i thread**********/
    @Override
    public void onDestroy() {
        thread.interrupt();
        thread=null;
        stopForeground(true);
        stopSelf();
        notificationManager.deleteNotificationChannel("ChannelId1");
        super.onDestroy();
    }
}

