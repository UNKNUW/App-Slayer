package com.appslayer.official;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;

public class MyService extends Service {

    public static final String CHANNEL_ID = "AppSlayerChannel";
    public static final int NOTIF_ID = 1;

    private BroadcastReceiver toastReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        registerToastReceiver();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Notifikasi awal service
        Notification notif = buildNotification("Modul AKTIF", "Now Loading...⌛");
        startForeground(NOTIF_ID, notif);

        // Jalankan script service.sh secara background
        new Thread(new Runnable() {
				public void run() {
					try {
						String scriptPath = getFilesDir() + "/service.sh";
						Process process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", scriptPath});
						process.waitFor();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();

        return START_STICKY;
    }

    private Notification buildNotification(String line1, String line2) {
        int color;
        String upperLine = line1.toUpperCase();

        if (upperLine.contains("EXTREME")) {
            color = Color.RED;
        } else if (upperLine.contains("AGGRESSIVE")) {
            color = Color.YELLOW;
        } else if (upperLine.contains("NORMAL")) {
            color = Color.GREEN;
        } else if (upperLine.contains("BERHASIL") || upperLine.contains("SUCCESS")) {
            color = Color.GREEN;
        } else if (upperLine.contains("GAGAL") || upperLine.contains("FAILED")) {
            color = Color.RED;
        } else {
            color = Color.BLUE;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
            builder.setPriority(Notification.PRIORITY_LOW);
        }

        builder.setContentTitle("Background App Slayer")
            .setContentText(line1 + "\n" + line2)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(color)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setStyle(new Notification.BigTextStyle().bigText(line1 + "\n" + line2));

        return builder.build();
    }

	private void createNotificationChannel() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(
				CHANNEL_ID,
				"App Slayer Silent Channel",
				NotificationManager.IMPORTANCE_DEFAULT // Tidak getar
			);
			channel.enableVibration(false);
			channel.setSound(null, null);
			channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
			channel.setShowBadge(false);

			NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			if (manager != null) {
				manager.createNotificationChannel(channel);
			}
		}
	}

    private void registerToastReceiver() {
        toastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.appslayer.official.ACTION_TOAST_MODE".equals(intent.getAction())) {
                    String msg = intent.getStringExtra("msg");
                    String status = intent.getStringExtra("status");

                    Notification updated = buildNotification(msg, status);
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    if (nm != null) {
                        nm.notify(NOTIF_ID, updated);
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.appslayer.official.ACTION_TOAST_MODE");
        registerReceiver(toastReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (toastReceiver != null) {
            unregisterReceiver(toastReceiver);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
