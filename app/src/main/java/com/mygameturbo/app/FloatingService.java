package com.mygameturbo.app;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import java.util.List;

public class FloatingService extends Service {

    private WindowManager wm;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private LinearLayout panel, handleBox;

    private int initX, initY;
    private float touchX, touchY;
    private boolean dragging;
    private boolean panelOpen = false;
    private boolean snappedRight = false;

    private boolean batteryOn, perfOn, qualityHigh = true, dndOn, crossOn, rotateOn, recordOn;

    private SharedPreferences prefs;
    private Handler timeHandler = new Handler(Looper.getMainLooper());
    private long sessionStart = System.currentTimeMillis();
    private String currentPkg = "";

    @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, makeNotification());
        prefs = getSharedPreferences("gt", MODE_PRIVATE);

        try {
            wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_layout, null);

            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_PHONE;

            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 600;

            wm.addView(floatingView, params);

            panel = floatingView.findViewById(R.id.panel);
            handleBox = floatingView.findViewById(R.id.handleBox);

            loadGames();
            setupControls();

            handleBox.setOnTouchListener((v, e) -> {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x; initY = params.y;
                        touchX = e.getRawX(); touchY = e.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - touchX;
                        float dy = e.getRawY() - touchY;
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) dragging = true;
                        params.x = initX + (int) dx;
                        params.y = initY + (int) dy;
                        wm.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) togglePanel();
                        else snapToEdge();
                        return true;
                }
                return false;
            });

            floatingView.findViewById(R.id.closeBtn).setOnClickListener(v -> closePanel());

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupControls() {
        floatingView.findViewById(R.id.btnBattery).setOnClickListener(v -> {
            batteryOn = !batteryOn;
            ((TextView) floatingView.findViewById(R.id.icoBattery)).setText(batteryOn ? "🔋✓" : "🔋");
            Toast.makeText(this, batteryOn ? "Battery Saver ON" : "OFF", Toast.LENGTH_SHORT).show();
        });

        floatingView.findViewById(R.id.btnPerf).setOnClickListener(v -> {
            perfOn = !perfOn;
            ((TextView) floatingView.findViewById(R.id.icoPerf)).setText(perfOn ? "⚡✓" : "⚡");
            Toast.makeText(this, perfOn ? "Performance ON" : "OFF", Toast.LENGTH_SHORT).show();
        });

        floatingView.findViewById(R.id.btnQuality).setOnClickListener(v -> {
            qualityHigh = !qualityHigh;
            ((TextView) floatingView.findViewById(R.id.txtQuality))
                    .setText(qualityHigh ? "Quality HD" : "Quality Low");
            Toast.makeText(this, qualityHigh ? "High Quality" : "Low (No Lag)", Toast.LENGTH_SHORT).show();
        });

        floatingView.findViewById(R.id.btnDnd).setOnClickListener(v -> {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (nm.isNotificationPolicyAccessGranted()) {
                        dndOn = !dndOn;
                        nm.setInterruptionFilter(dndOn
                                ? NotificationManager.INTERRUPTION_FILTER_NONE
                                : NotificationManager.INTERRUPTION_FILTER_ALL);
                        ((TextView) floatingView.findViewById(R.id.icoDnd)).setText(dndOn ? "🔕✓" : "🔕");
                    } else {
                        Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    }
                }
            } catch (Exception e) {}
        });

        floatingView.findViewById(R.id.btnRam).setOnClickListener(v -> {
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                List<ActivityManager.RunningAppProcessInfo> procs = am.getRunningAppProcesses();
                int k = 0;
                for (ActivityManager.RunningAppProcessInfo p : procs) {
                    if (p.pkgList == null) continue;
                    if (p.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        am.killBackgroundProcesses(p.pkgList[0]);
                        k++;
                    }
                }
                Toast.makeText(this, "Killed " + k + " apps", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {}
        });

        floatingView.findViewById(R.id.btnShot).setOnClickListener(v ->
                Toast.makeText(this, "Vol Down + Power", Toast.LENGTH_LONG).show());

        floatingView.findViewById(R.id.btnBright).setOnClickListener(v -> {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
                Toast.makeText(this, "Brightness max", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Write settings permission do", Toast.LENGTH_SHORT).show();
            }
        });

        floatingView.findViewById(R.id.btnCross).setOnClickListener(v -> {
            crossOn = !crossOn;
            ((TextView) floatingView.findViewById(R.id.icoCross)).setText(crossOn ? "🎯✓" : "🎯");
            Intent i = new Intent(this, StatsOverlayService.class);
            i.putExtra("crosshair", crossOn);
            startService(i);
        });

        floatingView.findViewById(R.id.btnRotate).setOnClickListener(v -> {
            rotateOn = !rotateOn;
            ((TextView) floatingView.findViewById(R.id.icoRotate)).setText(rotateOn ? "🔄✓" : "🔄");
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, rotateOn ? 0 : 1);
            } catch (Exception e) {}
        });

        floatingView.findViewById(R.id.btnSound).setOnClickListener(v -> {
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            am.setStreamVolume(AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            Toast.makeText(this, "Volume max", Toast.LENGTH_SHORT).show();
        });

        floatingView.findViewById(R.id.btnAirplane).setOnClickListener(v -> {
            try {
                Intent i = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            } catch (Exception e) {}
        });

        floatingView.findViewById(R.id.btnTimer).setOnClickListener(v -> {
            long mins = (System.currentTimeMillis() - sessionStart) / 60000;
            Toast.makeText(this, "Session: " + mins + " min", Toast.LENGTH_SHORT).show();
        });

        floatingView.findViewById(R.id.btnCompass).setOnClickListener(v ->
                Toast.makeText(this, "Compass coming soon", Toast.LENGTH_SHORT).show());

        floatingView.findViewById(R.id.btnCalls).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        floatingView.findViewById(R.id.btnRecord).setOnClickListener(v -> {
            recordOn = !recordOn;
            ((TextView) floatingView.findViewById(R.id.icoRecord)).setText(recordOn ? "🎥✓" : "🎥");
            Toast.makeText(this, recordOn ? "Recording hint: screen record from notification" :
                    "Recording OFF", Toast.LENGTH_SHORT).show();
        });
    }

    private void togglePanel() {
        if (panelOpen) closePanel(); else openPanel();
    }

    private void openPanel() {
        panel.setVisibility(View.VISIBLE);
        panel.setAlpha(0f);
        panel.setTranslationX(snappedRight ? 80f : -80f);
        panel.animate().alpha(1f).translationX(0f).setDuration(200).start();
        panelOpen = true;
    }

    private void closePanel() {
        panel.animate().alpha(0f).translationX(snappedRight ? 80f : -80f)
                .setDuration(150).withEndAction(() -> {
                    panel.setVisibility(View.GONE);
                    panel.setAlpha(1f);
                    panel.setTranslationX(0f);
                }).start();
        panelOpen = false;
    }

    private void snapToEdge() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int vw = floatingView.getWidth();
        int sh = getResources().getDisplayMetrics().heightPixels;
        int vh = floatingView.getHeight();

        if (params.x + vw / 2 < sw / 2) { params.x = 0; snappedRight = false; }
        else { params.x = sw - vw; snappedRight = true; }

        if (params.y < 0) params.y = 0;
        if (params.y > sh - vh) params.y = sh - vh;

        wm.updateViewLayout(floatingView, params);
    }

    private void loadGames() {
        LinearLayout container = panel.findViewById(R.id.gamesContainer);
        PackageManager pm = getPackageManager();
        String data = prefs.getString("games", "");
        container.removeAllViews();

        if (data.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Pehle app me games add karo");
            tv.setTextColor(Color.GRAY);
            tv.setPadding(20, 30, 20, 30);
            container.addView(tv);
            return;
        }

        for (String item : data.split(";")) {
            String[] parts = item.split("\\|");
            if (parts.length < 2) continue;
            final String pkg = parts[0];
            String label = parts[1];

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(15, 20, 15, 20);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView ic = new ImageView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(60, 60);
            ic.setLayoutParams(ip);
            try { ic.setImageDrawable(pm.getApplicationIcon(pkg)); }
            catch (Exception e) { ic.setImageResource(android.R.drawable.ic_menu_compass); }

            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setPadding(20, 0, 0, 0);

            row.addView(ic);
            row.addView(tv);

            row.setOnClickListener(v -> {
                currentPkg = pkg;
                launchGame(pm, pkg);
                closePanel();
            });

            container.addView(row);
        }
    }

    private void launchGame(PackageManager pm, String pkg) {
        Intent li = pm.getLaunchIntentForPackage(pkg);
        if (li == null) {
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            main.setPackage(pkg);
            List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
            if (!list.isEmpty()) {
                li = new Intent(Intent.ACTION_MAIN);
                li.addCategory(Intent.CATEGORY_LAUNCHER);
                li.setClassName(pkg, list.get(0).activityInfo.name);
            }
        }
        if (li != null) {
            li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(li); } catch (Exception ignored) {}
        }
    }

    private Notification makeNotification() {
        String ch = "floating_ch";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(ch,
                    "Game Turbo", NotificationManager.IMPORTANCE_LOW);
            c.setShowBadge(false);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(c);
        }
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, ch)
                .setContentTitle("My Game Turbo")
                .setContentText("Edge panel active hai")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            getApplicationContext().startForegroundService(restart);
        else getApplicationContext().startService(restart);
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null && wm != null) {
            try { wm.removeView(floatingView); } catch (Exception ignored) {}
        }
    }
}
