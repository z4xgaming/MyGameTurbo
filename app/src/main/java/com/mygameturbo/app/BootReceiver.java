package com.mygameturbo.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        SharedPreferences prefs = ctx.getSharedPreferences("gt", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("auto_start", false)) return;

        Intent svc = new Intent(ctx, FloatingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.startForegroundService(svc);
        else ctx.startService(svc);
    }
}
