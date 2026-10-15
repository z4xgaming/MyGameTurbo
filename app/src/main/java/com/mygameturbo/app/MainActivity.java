package com.mygameturbo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_REQ = 101;
    private LinearLayout gamesList, historyList;
    private SharedPreferences prefs;
    private TextView tabGames, tabStats, tabTools;
    private View pageGames, pageStats, pageTools;

    private TextView statFps, statPing, statRam, statBattery, statTemp, statTime;
    private TextView toolOverlayState, toolCrosshairState, toolRotateState, toolDndState;
    private Handler statHandler = new Handler(Looper.getMainLooper());
    private long sessionStart = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        prefs = getSharedPreferences("gt", MODE_PRIVATE);

        gamesList = findViewById(R.id.gamesList);
        historyList = findViewById(R.id.historyList);
        tabGames = findViewById(R.id.tabGames);
        tabStats = findViewById(R.id.tabStats);
        tabTools = findViewById(R.id.tabTools);
        pageGames = findViewById(R.id.pageGames);
        pageStats = findViewById(R.id.pageStats);
        pageTools = findViewById(R.id.pageTools);

        statFps = findViewById(R.id.statFps);
        statPing = findViewById(R.id.statPing);
        statRam = findViewById(R.id.statRam);
        statBattery = findViewById(R.id.statBattery);
        statTemp = findViewById(R.id.statTemp);
        statTime = findViewById(R.id.statTime);

        toolOverlayState = findViewById(R.id.toolOverlayState);
        toolCrosshairState = findViewById(R.id.toolCrosshairState);
        toolRotateState = findViewById(R.id.toolRotateState);
        toolDndState = findViewById(R.id.toolDndState);

        findViewById(R.id.settingsIcon).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.addBtn).setOnClickListener(v -> openAppPicker());

        tabGames.setOnClickListener(v -> showTab(0));
        tabStats.setOnClickListener(v -> showTab(1));
        tabTools.setOnClickListener(v -> showTab(2));

        setupTools();
        loadGames();
        loadHistory();
        startStatsUpdates();
    }

    private void showTab(int idx) {
        pageGames.setVisibility(idx == 0 ? View.VISIBLE : View.GONE);
        pageStats.setVisibility(idx == 1 ? View.VISIBLE : View.GONE);
        pageTools.setVisibility(idx == 2 ? View.VISIBLE : View.GONE);
        int active = getResources().getColor(R.color.orange);
        int inactive = getResources().getColor(R.color.text_secondary);
        tabGames.setTextColor(idx == 0 ? active : inactive);
        tabStats.setTextColor(idx == 1 ? active : inactive);
        tabTools.setTextColor(idx == 2 ? active : inactive);
    }

    private void setupTools() {
        findViewById(R.id.toolOverlay).setOnClickListener(v -> {
            boolean on = !prefs.getBoolean("ov_stats", false);
            prefs.edit().putBoolean("ov_stats", on).apply();
            toolOverlayState.setText(on ? "ON" : "OFF");
            Intent i = new Intent(this, StatsOverlayService.class);
            if (on) startService(i); else stopService(i);
        });

        findViewById(R.id.toolCrosshair).setOnClickListener(v -> {
            boolean on = !prefs.getBoolean("ov_cross", false);
            prefs.edit().putBoolean("ov_cross", on).apply();
            toolCrosshairState.setText(on ? "ON" : "OFF");
            Intent i = new Intent(this, StatsOverlayService.class);
            i.putExtra("crosshair", on);
            startService(i);
        });

        findViewById(R.id.toolRotate).setOnClickListener(v -> {
            boolean on = !prefs.getBoolean("auto_rotate", false);
            prefs.edit().putBoolean("auto_rotate", on).apply();
            toolRotateState.setText(on ? "ON" : "OFF");
            if (on) {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 1);
                Toast.makeText(this, "Auto rotation ON", Toast.LENGTH_SHORT).show();
            } else {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 0);
                Toast.makeText(this, "Auto rotation OFF", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.toolDnd).setOnClickListener(v -> {
            try {
                android.app.NotificationManager nm =
                        (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (nm.isNotificationPolicyAccessGranted()) {
                        boolean on = !prefs.getBoolean("dnd_on", false);
                        prefs.edit().putBoolean("dnd_on", on).apply();
                        nm.setInterruptionFilter(on
                                ? android.app.NotificationManager.INTERRUPTION_FILTER_NONE
                                : android.app.NotificationManager.INTERRUPTION_FILTER_ALL);
                        toolDndState.setText(on ? "ON" : "OFF");
                    } else {
                        startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    }
                }
            } catch (Exception e) { }
        });

        findViewById(R.id.toolBrightness).setOnClickListener(v -> {
            try {
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS, 255);
                Toast.makeText(this, "Brightness max", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Permission chahiye", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
        });

        findViewById(R.id.toolSound).setOnClickListener(v -> {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService(AUDIO_SERVICE);
            am.setStreamVolume(android.media.AudioManager.STREAM_MUSIC,
                    am.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC), 0);
            Toast.makeText(this, "Volume max", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.toolClear).setOnClickListener(v -> {
            android.app.ActivityManager amgr =
                    (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            try {
                List<android.app.ActivityManager.RunningAppProcessInfo> procs = amgr.getRunningAppProcesses();
                int k = 0;
                for (android.app.ActivityManager.RunningAppProcessInfo p : procs) {
                    if (p.pkgList == null) continue;
                    if (p.importance > android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                        amgr.killBackgroundProcesses(p.pkgList[0]);
                        k++;
                    }
                }
                Toast.makeText(this, "Killed " + k + " apps", Toast.LENGTH_SHORT).show();
            } catch (Exception e) { }
        });
    }

    private void startStatsUpdates() {
        statHandler.postDelayed(new Runnable() {
            @Override public void run() {
                updateStats();
                statHandler.postDelayed(this, 2000);
            }
        }, 2000);
    }

    private void updateStats() {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long usedMb = (mi.totalMem - mi.availMem) / (1024 * 1024);
            statRam.setText(usedMb + " MB");

            android.content.IntentFilter ifilter = new android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent bat = registerReceiver(null, ifilter);
            if (bat != null) {
                int level = bat.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                int temp = bat.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0);
                statBattery.setText(level + "%");
                statTemp.setText(String.format(Locale.US, "%.1f°C", temp / 10.0));
            }

            long mins = (System.currentTimeMillis() - sessionStart) / 60000;
            statTime.setText(mins + "m");
            statFps.setText("60");
            statPing.setText("--");
        } catch (Exception e) { }
    }

    private void openAppPicker() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);
        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) names[i] = apps.get(i).loadLabel(pm).toString();

        new AlertDialog.Builder(this)
                .setTitle("Game chuno")
                .setItems(names, (d, which) -> {
                    String pkg = apps.get(which).activityInfo.packageName;
                    String label = apps.get(which).loadLabel(pm).toString();
                    saveGame(pkg, label);
                    loadGames();
                    Toast.makeText(this, label + " add ho gaya", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void saveGame(String pkg, String label) {
        String ex = prefs.getString("games", "");
        if (ex.contains(pkg + "|")) return;
        String updated = ex.isEmpty() ? pkg + "|" + label : ex + ";" + pkg + "|" + label;
        prefs.edit().putString("games", updated).apply();
    }

    private void loadGames() {
        gamesList.removeAllViews();
        String data = prefs.getString("games", "");
        if (data.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Abhi koi game add nahi kiya.\nUpar + button dabao");
            tv.setTextColor(0xFFAAAAAA);
            tv.setPadding(20, 40, 20, 40);
            gamesList.addView(tv);
            return;
        }
        PackageManager pm = getPackageManager();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (String item : data.split(";")) {
            String[] parts = item.split("\\|");
            if (parts.length < 2) continue;
            final String pkg = parts[0];
            String label = parts[1];
            View row = inflater.inflate(R.layout.game_row, null);
            ((TextView) row.findViewById(R.id.gameName)).setText(label);
            ImageView iv = row.findViewById(R.id.gameIcon);
            try { Drawable ic = pm.getApplicationIcon(pkg); iv.setImageDrawable(ic); }
            catch (Exception e) { iv.setImageResource(android.R.drawable.ic_menu_compass); }

            long timeMs = prefs.getLong("time_" + pkg, 0);
            ((TextView) row.findViewById(R.id.gameTime)).setText((timeMs / 60000) + "m played");

            View.OnClickListener launch = v -> launchGame(pkg);
            row.findViewById(R.id.gameIcon).setOnClickListener(launch);
            row.findViewById(R.id.gameName).setOnClickListener(launch);
            row.findViewById(R.id.removeBtn).setOnClickListener(v -> {
                removeGame(pkg);
                loadGames();
            });
            gamesList.addView(row);
        }
    }

    private void loadHistory() {
        historyList.removeAllViews();
        String hist = prefs.getString("history", "");
        if (hist.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Abhi koi history nahi");
            tv.setTextColor(0xFFAAAAAA);
            tv.setPadding(10, 10, 10, 10);
            historyList.addView(tv);
            return;
        }
        String[] lines = hist.split(";;");
        int start = Math.max(0, lines.length - 10);
        for (int i = lines.length - 1; i >= start; i--) {
            TextView tv = new TextView(this);
            tv.setText(lines[i]);
            tv.setTextColor(0xFFCCCCCC);
            tv.setTextSize(12);
            tv.setPadding(10, 8, 10, 8);
            historyList.addView(tv);
        }
    }

    private void launchGame(String pkg) {
        PackageManager pm = getPackageManager();
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
            try {
                startActivity(li);
                String time = new SimpleDateFormat("dd MMM HH:mm", Locale.US).format(new Date());
                String hist = prefs.getString("history", "");
                hist = time + " — " + pkg + ";;" + hist;
                if (hist.length() > 4000) hist = hist.substring(0, 4000);
                prefs.edit().putString("history", hist).apply();
            } catch (Exception ignored) {}
        }
    }

    private void removeGame(String pkg) {
        String data = prefs.getString("games", "");
        StringBuilder sb = new StringBuilder();
        for (String item : data.split(";")) {
            if (!item.startsWith(pkg + "|")) {
                if (sb.length() > 0) sb.append(";");
                sb.append(item);
            }
        }
        prefs.edit().putString("games", sb.toString()).apply();
    }

    public void startTurbo(View v) {
        String data = prefs.getString("games", "");
        if (data.isEmpty()) {
            Toast.makeText(this, "Pehle game add karo!", Toast.LENGTH_LONG).show();
            return;
        }
        String firstPkg = data.split(";")[0].split("\\|")[0];
        prefs.edit().putString("autoLaunch", firstPkg).apply();

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission do", Toast.LENGTH_LONG).show();
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, OVERLAY_REQ);
            return;
        }
        startServiceNow();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == OVERLAY_REQ && Settings.canDrawOverlays(this)) startServiceNow();
    }

    private void startServiceNow() {
        try {
            Intent i = new Intent(this, FloatingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i);
            else startService(i);

            String auto = prefs.getString("autoLaunch", "");
            if (!auto.isEmpty()) launchGame(auto);
            moveTaskToBack(true);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
