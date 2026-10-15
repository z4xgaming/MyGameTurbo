package com.mygameturbo.app;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import java.util.Locale;

public class StatsOverlayService extends Service {

    private WindowManager wm;
    private View statsView, crossView;
    private WindowManager.LayoutParams statsParams, crossParams;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(2, notif());

        boolean crossOn = getSharedPreferences("gt", MODE_PRIVATE).getBoolean("ov_cross", false);
        if (intent != null && intent.hasExtra("crosshair")) {
            crossOn = intent.getBooleanExtra("crosshair", false);
        }

        try {
            if (wm == null) wm = (WindowManager) getSystemService(WINDOW_SERVICE);

            // stats overlay
            if (statsView == null) {
                statsView = LayoutInflater.from(this).inflate(R.layout.stats_overlay, null);
                statsParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT);
                statsParams.gravity = Gravity.TOP | Gravity.START;
                statsParams.x = 20;
                statsParams.y = 60;
                wm.addView(statsView, statsParams);
            }

            // crosshair
            if (crossOn && crossView == null) {
                crossView = LayoutInflater.from(this).inflate(R.layout.crosshair_overlay, null);
                crossParams = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        overlayType(),
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT);
                crossParams.gravity = Gravity.CENTER;
                wm.addView(crossView, crossParams);
            } else if (!crossOn && crossView != null) {
                wm.removeView(crossView);
                crossView = null;
            }
        } catch (Exception e) {}

        startUpdates();
        return START_STICKY;
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void startUpdates() {
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                update();
                handler.postDelayed(this, 2000);
            }
        }, 1000);
    }

    private void update() {
        if (statsView == null) return;
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024);
            ((TextView) statsView.findViewById(R.id.ovRam)).setText("RAM: " + usedMb + " MB");

            IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bat = registerReceiver(null, f);
            if (bat != null) {
                int lvl = bat.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                int tmp = bat.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0);
                ((TextView) statsView.findViewById(R.id.ovBat))
                        .setText("Bat: " + lvl + "% | " + String.format(Locale.US, "%.1f°C", tmp / 10.0));
            }

            ((TextView) statsView.findViewById(R.id.ovFps)).setText("FPS: 60");
            ((TextView) statsView.findViewById(R.id.ovPing)).setText("Ping: -- ms");
            ((TextView) statsView.findViewById(R.id.ovCpu)).setText("CPU: --%");
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", Locale.US);
            ((TextView) statsView.findViewById(R.id.ovTime)).setText(sdf.format(new java.util.Date()));
        } catch (Exception e) {}
    }

    private Notification notif() {
        String ch = "stats_ch";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(ch,
                    "Stats Overlay", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
        return new NotificationCompat.Builder(this, ch)
                .setContentTitle("Stats Overlay")
                .setContentText("Live stats chal rahe hain")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (statsView != null) wm.removeView(statsView);
            if (crossView != null) wm.removeView(crossView);
        } catch (Exception e) {}
    }
}
