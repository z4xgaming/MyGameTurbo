package com.mygameturbo.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
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
    private int initX, initY;
    private float touchX, touchY;
    private boolean dragging;
    private boolean panelOpen = false;

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, makeNotification());

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
            params.y = 400;

            wm.addView(floatingView, params);

            ImageView icon = floatingView.findViewById(R.id.icon);
            LinearLayout panel = floatingView.findViewById(R.id.panel);

            loadGames(panel);

            icon.setOnTouchListener((v, e) -> {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = params.x;
                        initY = params.y;
                        touchX = e.getRawX();
                        touchY = e.getRawY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = e.getRawX() - touchX;
                        float dy = e.getRawY() - touchY;
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) dragging = true;
                        params.x = initX + (int) dx;
                        params.y = initY + (int) dy;
                        wm.updateViewLayout(floatingView, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            panelOpen = !panelOpen;
                            panel.setVisibility(panelOpen ? View.VISIBLE : View.GONE);
                        } else {
                            int screenW = getResources().getDisplayMetrics().widthPixels;
                            int viewW = floatingView.getWidth();
                            params.x = (params.x + viewW / 2 < screenW / 2) ? 0 : screenW - viewW;
                            wm.updateViewLayout(floatingView, params);
                        }
                        return true;
                }
                return false;
            });
        } catch (Exception e) {
            Toast.makeText(this, "Floating error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadGames(LinearLayout panel) {
        LinearLayout container = panel.findViewById(R.id.gamesContainer);
        PackageManager pm = getPackageManager();
        SharedPreferences prefs = getSharedPreferences("gt", MODE_PRIVATE);
        String data = prefs.getString("games", "");

        container.removeAllViews();

        if (data.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Pehle app me games add karo");
            tv.setTextColor(Color.GRAY);
            tv.setPadding(20, 20, 20, 20);
            container.addView(tv);
            return;
        }

        for (String item : data.split(";")) {
            String[] parts = item.split("\\|");
            if (parts.length < 2) continue;
            final String pkg = parts[0];
            String label = parts[1];

            // Horizontal row with icon + name
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(20, 25, 20, 25);
            row.setGravity(Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(this);
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(70, 70);
            icon.setLayoutParams(ip);
            try {
                Drawable d = pm.getApplicationIcon(pkg);
                icon.setImageDrawable(d);
            } catch (Exception e) {
                icon.setImageResource(android.R.drawable.ic_menu_compass);
            }

            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(15);
            tv.setPadding(20, 0, 0, 0);

            row.addView(icon);
            row.addView(tv);

            row.setOnClickListener(v -> {
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
                panel.setVisibility(View.GONE);
                panelOpen = false;
            });

            container.addView(row);
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
                .setContentText("Floating icon active hai")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restart);
        } else {
            getApplicationContext().startService(restart);
        }
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
