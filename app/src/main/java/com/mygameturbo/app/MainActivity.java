package com.mygameturbo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int OVERLAY_REQ = 101;
    private LinearLayout gamesList;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("gt", MODE_PRIVATE);
        gamesList = findViewById(R.id.gamesList);

        findViewById(R.id.settingsIcon).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.addBtn).setOnClickListener(v -> openAppPicker());

        loadGames();

        // Auto permission check on open
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Pehle Overlay permission do", Toast.LENGTH_LONG).show();
        }
    }

    private void openAppPicker() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);

        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            names[i] = apps.get(i).loadLabel(pm).toString();
        }

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
        String existing = prefs.getString("games", "");
        if (existing.contains(pkg + "|")) return;
        String updated = existing.isEmpty() ? pkg + "|" + label : existing + ";" + pkg + "|" + label;
        prefs.edit().putString("games", updated).apply();
    }

    private void loadGames() {
        gamesList.removeAllViews();
        String data = prefs.getString("games", "");
        if (data.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("Abhi koi game add nahi kiya.\nNeeche + button dabao");
            tv.setTextSize(14);
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
            TextView nameView = row.findViewById(R.id.gameName);
            ImageView iconView = row.findViewById(R.id.gameIcon);

            nameView.setText(label);

            // असली game icon load करो
            try {
                Drawable icon = pm.getApplicationIcon(pkg);
                iconView.setImageDrawable(icon);
            } catch (Exception e) {
                iconView.setImageResource(android.R.drawable.ic_menu_compass);
            }

            // Click करने पर launch
            View.OnClickListener launchClick = v -> {
                Intent li = pm.getLaunchIntentForPackage(pkg);
                if (li == null) {
                    // Fallback: resolve activity
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
                    } catch (Exception ex) {
                        Toast.makeText(this, "Launch fail: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(this, "Ye app khul nahi sakta", Toast.LENGTH_SHORT).show();
                }
            };

            row.findViewById(R.id.gameIcon).setOnClickListener(launchClick);
            row.findViewById(R.id.gameName).setOnClickListener(launchClick);

            row.findViewById(R.id.removeBtn).setOnClickListener(v -> {
                removeGame(pkg);
                loadGames();
            });

            gamesList.addView(row);
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
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Pehle overlay permission do", Toast.LENGTH_LONG).show();
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
        if (req == OVERLAY_REQ) {
            if (Settings.canDrawOverlays(this)) {
                startServiceNow();
            } else {
                Toast.makeText(this, "Permission nahi mili!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startServiceNow() {
        try {
            Intent i = new Intent(this, FloatingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(i);
            } else {
                startService(i);
            }
            Toast.makeText(this, "Floating icon chalu ho gaya! Home screen par dekho", Toast.LENGTH_LONG).show();
            // App minimize karo taki icon dikhe
            moveTaskToBack(true);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
