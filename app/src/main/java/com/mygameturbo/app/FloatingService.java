package com.mygameturbo.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
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
    public void onCreate() {
        super.onCreate();
        startForeground(1, makeNotification());

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
    }

    private void loadGames(LinearLayout panel) {
        LinearLayout container = panel.findViewById(R.id.gamesContainer);
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);

        for (ResolveInfo info : apps) {
            ApplicationInfo ai = info.activityInfo.applicationInfo;
            boolean isGame = (ai.flags & ApplicationInfo.FLAG_IS_GAME) != 0
                    || (Build.VERSION.SDK_INT >= 26 && ai.category == ApplicationInfo.CATEGORY_GAME);
            if (!isGame) continue;

            TextView tv = new TextView(this);
            tv.setText(ai.loadLabel(pm));
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(14);
            tv.setPadding(20, 25, 20, 25);
            tv.setOnClickListener(v -> {
                Intent li = pm.getLaunchIntentForPackage(ai.packageName);
                if (li != null) {
                    li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(li);
                }
                panel.setVisibility(View.GONE);
                panelOpen = false;
            });
            container.addView(tv);
        }

        if (container.getChildCount() == 0) {
            TextView tv = new TextView(this);
            tv.setText("Koi game nahi mila");
            tv.setTextColor(Color.GRAY);
            tv.setPadding(20, 20, 20, 20);
            container.addView(tv);
        }
    }

    private Notification makeNotification() {
        String ch = "floating_ch";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(ch,
                    "Game Turbo", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                    .createNotificationChannel(c);
        }
        return new NotificationCompat.Builder(this, ch)
                .setContentTitle("My Game Turbo")
                .setContentText("Floating icon active hai")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) wm.removeView(floatingView);
    }
}
