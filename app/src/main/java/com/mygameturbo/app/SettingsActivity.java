package com.mygameturbo.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private TextView stateAutostart, stateAutoDetect, stateAutoBright, stateAutoDnd,
            stateAutoRotate, stateAutoVolume, statePanelSize, stateEdgeSide, stateIconColor;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);
        prefs = getSharedPreferences("gt", MODE_PRIVATE);

        stateAutostart = findViewById(R.id.stateAutostart);
        stateAutoDetect = findViewById(R.id.stateAutoDetect);
        stateAutoBright = findViewById(R.id.stateAutoBright);
        stateAutoDnd = findViewById(R.id.stateAutoDnd);
        stateAutoRotate = findViewById(R.id.stateAutoRotate);
        stateAutoVolume = findViewById(R.id.stateAutoVolume);
        statePanelSize = findViewById(R.id.statePanelSize);
        stateEdgeSide = findViewById(R.id.stateEdgeSide);
        stateIconColor = findViewById(R.id.stateIconColor);

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        refresh();

        bindToggle(R.id.rowAutostart, "auto_start", stateAutostart);
        bindToggle(R.id.rowAutoDetect, "auto_detect", stateAutoDetect);
        bindToggle(R.id.rowAutoBright, "auto_bright", stateAutoBright);
        bindToggle(R.id.rowAutoDnd, "auto_dnd", stateAutoDnd);
        bindToggle(R.id.rowAutoRotate, "auto_rotate", stateAutoRotate);
        bindToggle(R.id.rowAutoVolume, "auto_volume", stateAutoVolume);

        findViewById(R.id.rowOverlayPerm).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });

        findViewById(R.id.rowDndPerm).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            }
        });

        findViewById(R.id.rowUsagePerm).setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        });

        findViewById(R.id.rowStop).setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            stopService(new Intent(this, StatsOverlayService.class));
            Toast.makeText(this, "Services band", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.rowAbout).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("About")
                    .setMessage("My Game Turbo v8\n\n46 Features\nEdge Panel + Stats + Auto Detect\n\nMade with love for Redmi 9A")
                    .setPositiveButton("OK", null)
                    .show();
        });

        findViewById(R.id.rowPanelSize).setOnClickListener(v -> {
            String[] opts = {"Small", "Medium", "Large"};
            String cur = prefs.getString("panel_size", "Medium");
            int sel = 1;
            for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) sel = i;
            new AlertDialog.Builder(this)
                    .setTitle("Panel Size")
                    .setSingleChoiceItems(opts, sel, (d, w) -> {
                        prefs.edit().putString("panel_size", opts[w]).apply();
                        statePanelSize.setText(opts[w]);
                        d.dismiss();
                    }).show();
        });

        findViewById(R.id.rowEdgeSide).setOnClickListener(v -> {
            String[] opts = {"Left", "Right", "Both"};
            String cur = prefs.getString("edge_side", "Both");
            int sel = 2;
            for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) sel = i;
            new AlertDialog.Builder(this)
                    .setTitle("Edge Side")
                    .setSingleChoiceItems(opts, sel, (d, w) -> {
                        prefs.edit().putString("edge_side", opts[w]).apply();
                        stateEdgeSide.setText(opts[w]);
                        d.dismiss();
                    }).show();
        });

        findViewById(R.id.rowIconColor).setOnClickListener(v -> {
            String[] opts = {"Orange", "Green", "Blue", "Purple", "Red", "Cyan"};
            String cur = prefs.getString("icon_color", "Orange");
            int sel = 0;
            for (int i = 0; i < opts.length; i++) if (opts[i].equals(cur)) sel = i;
            new AlertDialog.Builder(this)
                    .setTitle("Icon Color")
                    .setSingleChoiceItems(opts, sel, (d, w) -> {
                        prefs.edit().putString("icon_color", opts[w]).apply();
                        stateIconColor.setText(opts[w]);
                        d.dismiss();
                    }).show();
        });
    }

    private void bindToggle(int id, String key, TextView stateView) {
        findViewById(id).setOnClickListener(v -> {
            boolean on = !prefs.getBoolean(key, false);
            prefs.edit().putBoolean(key, on).apply();
            stateView.setText(on ? "ON" : "OFF");
        });
    }

    private void refresh() {
        stateAutostart.setText(prefs.getBoolean("auto_start", false) ? "ON" : "OFF");
        stateAutoDetect.setText(prefs.getBoolean("auto_detect", false) ? "ON" : "OFF");
        stateAutoBright.setText(prefs.getBoolean("auto_bright", false) ? "ON" : "OFF");
        stateAutoDnd.setText(prefs.getBoolean("auto_dnd", false) ? "ON" : "OFF");
        stateAutoRotate.setText(prefs.getBoolean("auto_rotate", false) ? "ON" : "OFF");
        stateAutoVolume.setText(prefs.getBoolean("auto_volume", false) ? "ON" : "OFF");
        statePanelSize.setText(prefs.getString("panel_size", "Medium"));
        stateEdgeSide.setText(prefs.getString("edge_side", "Both"));
        stateIconColor.setText(prefs.getString("icon_color", "Orange"));
    }
}
