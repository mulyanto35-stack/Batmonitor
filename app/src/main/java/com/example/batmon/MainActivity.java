package com.example.batmon;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {
    private BatteryManager bm;
    private TextView tv;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Polling hanya aktif saat Activity terlihat (onResume..onPause)
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            update();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        bm = (BatteryManager) getSystemService(BATTERY_SERVICE);
        tv = new TextView(this);
        tv.setTextSize(24);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(32, 32, 32, 32);
        setContentView(tv);
    }

    @Override protected void onResume() { super.onResume(); handler.post(tick); }
    @Override protected void onPause()  { super.onPause(); handler.removeCallbacks(tick); }

    private void update() {
        Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (i == null) return;

        int level  = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale  = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        int mv     = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        int temp   = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean charging = status == BatteryManager.BATTERY_STATUS_CHARGING;

        int raw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        String arus, daya = "-";
        if (raw == Integer.MIN_VALUE || raw == 0 && mv == 0) {
            arus = "tidak didukung";
        } else {
            // Beberapa HP melapor dalam µA, sebagian dalam mA
            float ma = Math.abs(raw) >= 10000 ? Math.abs(raw) / 1000f : Math.abs(raw);
            arus = String.format(Locale.US, "%.0f mA", ma);
            daya = String.format(Locale.US, "%.2f W", ma * mv / 1_000_000f);
        }

        tv.setText(String.format(Locale.US,
                "%s\n\nArus: %s\nVoltase: %d mV (%.2f V)\nDaya: %s\n\nBaterai: %d%%\nSuhu: %.1f °C",
                charging ? "⚡ MASUK (mengisi)" : "🔋 TERPAKAI",
                arus, mv, mv / 1000f, daya,
                level * 100 / scale, temp / 10f));
    }
}
