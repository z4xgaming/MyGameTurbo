package com.mygameturbo.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.overlayBtn).setOnClickListener(v -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        });

        findViewById(R.id.stopBtn).setOnClickListener(v -> {
            stopService(new Intent(this, FloatingService.class));
            Toast.makeText(this, "Floating icon band ho gaya", Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.aboutBtn).setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage("My Game Turbo v1.0\n\nFloating gaming launcher\n\nMade with ❤️")
                .setPositiveButton("OK", null)
                .show();
        });

        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }
}
