package com.mygameturbo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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

        // Settings icon click
        ImageView settings = findViewById(R.id.settingsIcon);
        settings.setOnClickListener(v ->
            startActivity(new Intent(this, SettingsActivity.class)));

        // Big Plus button click
        findViewById(R.id.addBtn).setOnClickListener(v -> openAppPicker());

        loadGames();
    }

    private void openAppPicker() {
        // Show all installed apps to pick as game
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(main, 0);

        String[] names = new String[apps.size()];
        for (int i = 0; i < apps.size(); i++) {
            names[i] = apps.get(i).loadLabel(pm).toString();
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
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
        if (existing.contains(pkg)) return;
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
            tv.setPadding(20, 40, 20, 40);
            gamesList.addView(tv);
            return;
        }

        for (String item : data.split(";")) {
            String[] parts = item.split("\\|");
            if (parts.length < 2) continue;
            final String pkg = parts[0];
            String label = parts[1];

            View row = getLayoutInflater().inflate(R.layout.game_row, null);
            ((TextView) row.findViewById(R.id.gameName)).setText(label);
            row.findViewById(R.id.gameIcon).setOnClickListener(v -> {
                Intent li = getPackageManager().getLaunchIntentForPackage(pkg);
                if (li != null) {
                    li.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(li);
                }
            });
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
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, OVERLAY_REQ);
        } else {
            startServiceNow();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == OVERLAY_REQ && Settings.canDrawOverlays(this)) {
            startServiceNow();
        }
    }

    private void startServiceNow() {
        Intent i = new Intent(this, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        Toast.makeText(this, "Floating icon chalu ho gaya", Toast.LENGTH_SHORT).show();
    }
}
