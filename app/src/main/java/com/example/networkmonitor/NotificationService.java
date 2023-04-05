/*
Copyright (c) 2022, Nikola Nešković
All rights reserved.

This source code is licensed under the BSD-style license found in the
LICENSE file in the root directory of this source tree.
*/
package com.example.networkmonitor;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.DecimalFormat;

public class NotificationService extends Service {
    private final static int SECOND = 1000;
    private final static int MILLION=1000000;
    private static final int THOUSAND =1000;
    private static final int BYTES_IN_MB =1024*1024;
    private static final int BYTES_IN_KB =1024;
    private NotificationManagerCompat notificationManager;
    private Notification notification;
    private boolean running;
    private long start=0;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Intent intent1 = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent1, PendingIntent.FLAG_IMMUTABLE);
        notification = new NotificationCompat.Builder(this, "ChannelId1")
                .setContentTitle("Notification Title")
                .setContentText("Notification text")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .build();
        notificationManager= NotificationManagerCompat.from(this);
        running=true;
        start=TrafficStats.getMobileRxBytes()+TrafficStats.getMobileTxBytes();

        startForeground(1, notification);
        setUpWorkerThread();

        return START_STICKY;
    }

    @SuppressWarnings("BusyWait")
    private void setUpWorkerThread() {
        Runnable runnable = () -> {
            while (running) {
                long tempDown = TrafficStats.getTotalRxBytes();
                long tempUp = TrafficStats.getTotalTxBytes();

                try {
                    Thread.sleep(SECOND);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double currDown = (double) TrafficStats.getTotalRxBytes() - tempDown;
                double currUp = (double) TrafficStats.getTotalTxBytes() - tempUp;
                double totalMobile=(double) TrafficStats.getMobileRxBytes()+TrafficStats.getMobileTxBytes()-start;

                String download=roundingSpeed(currDown);
                String upload=roundingSpeed(currUp);
                String totalMobilestring=roundingValue(totalMobile);

                Intent intent1 = new Intent(NotificationService.this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(NotificationService.this, 0, intent1, PendingIntent.FLAG_IMMUTABLE);
                notification = new NotificationCompat.Builder(NotificationService.this, "ChannelId1")
                        .setContentTitle("Download: " + download + "   " + "Upload: " + upload)
                        .setContentText("Mobile: "+totalMobile)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentIntent(pendingIntent)
                        .setSilent(true)
                        .build();
                notificationManager.notify(1, notification);
            }
            Log.d("nit","thread stopped");
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private String roundingSpeed(double value) {

        DecimalFormat df = new DecimalFormat("0.00");

        if (value>MILLION){
            return df.format(value/BYTES_IN_MB)+"MB/s";
        }
        if (value>THOUSAND){
            return df.format(value/BYTES_IN_KB)+"KB/s";
        }
        return df.format(value)+"B/s";

    }

    private String roundingValue(double value){
        DecimalFormat df = new DecimalFormat("0.00");
        if (value>MILLION){
            return df.format(value/BYTES_IN_MB)+"MB";
        }
        if (value>THOUSAND){
            return df.format(value/BYTES_IN_KB)+"KB";
        }
        return (df.format(value)+"B");
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
        super.onDestroy();
        running=false;
        notificationManager.deleteNotificationChannel("ChannelId1");
        stopForeground(true);
        stopSelf();
    }
}

