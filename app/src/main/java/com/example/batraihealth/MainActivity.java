package com.example.batraihealth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private FloatingActionButton mainIcon;
    private LinearLayout ballContainer;
    private boolean isExpanded = false;
    private WindowManager windowManager;
    private View overlayView;
    private WindowManager.LayoutParams params;
    private boolean isOverlayVisible = false;
    private Handler handler = new Handler();
    private Runnable updateDataRunnable;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainIcon = findViewById(R.id.mainIcon);
        ballContainer = findViewById(R.id.ballContainer);

        // Setup overlay
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;

        // Event Klik Floating Button
        mainIcon.setOnClickListener(v -> {
            if (isExpanded) {
                ballContainer.setVisibility(View.GONE);
            } else {
                ballContainer.setVisibility(View.VISIBLE);
            }
            isExpanded = !isExpanded;

            if (!isOverlayVisible) {
                windowManager.addView(overlayView, params);
                isOverlayVisible = true;
            } else {
                windowManager.removeView(overlayView);
                isOverlayVisible = false;
            }
        });

        // Update Data
        updateDataRunnable = new Runnable() {
            @Override
            public void run() {
                updateOverlayData();
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateDataRunnable);
    }

    private void updateOverlayData() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, intentFilter);
        int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
        ((TextView) overlayView.findViewById(R.id.batteryTemperatureOverlay)).setText("Battery Temp: " + temp + "°C");

        long freeBytes = new StatFs(Environment.getExternalStorageDirectory().getPath()).getAvailableBytes();
        long freeGB = freeBytes / (1024 * 1024 * 1024);
        ((TextView) overlayView.findViewById(R.id.storageSpaceOverlay)).setText("Free Space: " + freeGB + " GB");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateDataRunnable);
    }
}
