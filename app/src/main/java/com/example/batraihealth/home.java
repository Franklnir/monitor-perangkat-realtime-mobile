package com.example.batraihealth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri; // Import for Uri
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.os.SystemClock;
import android.provider.Settings; // Import for Settings
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.PixelFormat;
import android.widget.Toast; // Import for Toast
import android.os.Build; // Import for Build version checks

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class home extends Activity {

    private TextView batteryTemperatureOverlay, cpuTemperatureOverlay, refreshRateOverlay, storageSpaceOverlay, batteryHealthPercentage, batteryStatus, chargingCycles, batteryVoltage, batteryCurrent, textViewPowerUsage, batteryCapacityText;
    private CheckBox checkBatteryTemp, checkCpuTemp, checkRefreshRate, checkStorage, checkBatteryHealth, checkPowerUsage, checkBatteryCapacity;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private View overlayView;
    private Handler handler = new Handler();
    private Runnable updateBatteryDataRunnable;
    private boolean isMinimized = false;
    private boolean isBackgroundVisible = true;
    private boolean isCircular = false;
    private LinearLayout settingsLayout;
    private ImageView settingsIcon;
    private Button onDisplayButton, hideBackgroundButton, shapeButton;

    private TextView batteryCapacityRealtimeText;
    private int previousChargeLevel = -1;

    private TextView batteryUsageDuration;
    private long startTime;

    private static final int REQUEST_OVERLAY_PERMISSION = 1001; // Request code for overlay permission

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        // Check and request SYSTEM_ALERT_WINDOW permission on Android M (API 23) and above
        // This permission is crucial for drawing overlays on top of other apps.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // If permission is not granted, open the system settings to allow it
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        } else {
            // Permission already granted or not needed (pre-Marshmallow), proceed with UI setup
            initializeOverlayAndUI();
        }
    }

    /**
     * Callback for the result from requesting permissions.
     * This method is called after the user interacts with the permission dialog.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            // Check if the permission is now granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // Permission granted, proceed with initializing the overlay and UI
                initializeOverlayAndUI();
            } else {
                // Permission denied. Inform the user and potentially disable overlay features.
                Toast.makeText(this, "Izin overlay ditolak. Overlay tidak akan ditampilkan.", Toast.LENGTH_LONG).show();
                // You might want to finish the activity or disable features that rely on the overlay
                finish();
            }
        }
    }

    /**
     * Initializes the overlay view and all its UI elements.
     * This method is called only after SYSTEM_ALERT_WINDOW permission is confirmed.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initializeOverlayAndUI() {
        // Start the OverlayService. This service will keep the app process alive
        // and manage the persistent notification required for foreground services.
        Intent serviceIntent = new Intent(this, OverlayService.class);
        startService(serviceIntent);

        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay_layout, null);

        // Define layout parameters for the overlay window.
        // TYPE_APPLICATION_OVERLAY is used for drawing over other apps.
        // FLAG_NOT_FOCUSABLE makes the overlay not intercept touch events outside itself.
        // PixelFormat.TRANSLUCENT allows for transparent backgrounds.
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                // Use TYPE_APPLICATION_OVERLAY for Android O (API 26) and above
                // For older versions, TYPE_PHONE or TYPE_PRIORITY_WINDOW might be used,
                // but TYPE_APPLICATION_OVERLAY is the modern and correct approach.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // Add the overlay view to the window manager, making it visible on screen.
        windowManager.addView(overlayView, params);

        // Initialize UI elements by finding them within the inflated overlayView
        batteryTemperatureOverlay = overlayView.findViewById(R.id.batteryTemperatureOverlay);
        cpuTemperatureOverlay = overlayView.findViewById(R.id.cpuTemperatureOverlay);
        refreshRateOverlay = overlayView.findViewById(R.id.refreshRateOverlay);
        storageSpaceOverlay = overlayView.findViewById(R.id.storageSpaceOverlay);
        batteryHealthPercentage = overlayView.findViewById(R.id.batteryHealthPercentage);
        batteryStatus = overlayView.findViewById(R.id.batteryStatus);
        chargingCycles = overlayView.findViewById(R.id.chargingCycles);
        batteryVoltage = overlayView.findViewById(R.id.batteryVoltage);
        batteryCurrent = overlayView.findViewById(R.id.batteryCurrent);
        textViewPowerUsage = overlayView.findViewById(R.id.textViewPowerUsage);
        batteryCapacityText = overlayView.findViewById(R.id.batteryCapacityText);

        checkBatteryTemp = overlayView.findViewById(R.id.checkBatteryTemp);
        checkCpuTemp = overlayView.findViewById(R.id.checkCpuTemp);
        checkRefreshRate = overlayView.findViewById(R.id.checkRefreshRate);
        checkStorage = overlayView.findViewById(R.id.checkStorage);
        checkBatteryHealth = overlayView.findViewById(R.id.checkBatteryHealth);
        checkPowerUsage = overlayView.findViewById(R.id.checkPowerUsage);
        checkBatteryCapacity = overlayView.findViewById(R.id.checkBatteryCapacity);

        settingsLayout = overlayView.findViewById(R.id.settingsLayout);
        settingsIcon = overlayView.findViewById(R.id.settingsIcon);
        onDisplayButton = overlayView.findViewById(R.id.ondisplayButton);
        hideBackgroundButton = overlayView.findViewById(R.id.hideBackgroundButton);
        shapeButton = overlayView.findViewById(R.id.shapeButton);

        // Initialize battery capacity UI elements
        batteryCapacityRealtimeText = overlayView.findViewById(R.id.batteryCapacityText);

        // Initialize UI elements for battery usage duration
        batteryUsageDuration = overlayView.findViewById(R.id.batteryUsageDuration1);

        // Set the start time for device usage tracking
        startTime = SystemClock.elapsedRealtime();

        // Add listeners for drag to move overlay
        overlayView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Record initial position of the overlay and touch point
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true; // Consume the event
                    case MotionEvent.ACTION_MOVE:
                        // Calculate the displacement and update overlay position
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        params.x = initialX + deltaX;
                        params.y = initialY + deltaY;
                        windowManager.updateViewLayout(overlayView, params); // Update the overlay's position
                        return true; // Consume the event
                    case MotionEvent.ACTION_UP:
                        // Touch up event, no action needed for movement
                        return true; // Consume the event
                    default:
                        return false; // Do not consume other events
                }
            }
        });

        // OnDisplay button listener to toggle overlay size
        onDisplayButton.setOnClickListener(v -> toggleOverlaySize());

        // Settings icon click listener to show/hide settings layout and adjust overlay size
        settingsIcon.setOnClickListener(v -> {
            if (settingsLayout.getVisibility() == View.VISIBLE) {
                settingsLayout.setVisibility(View.GONE); // Hide settings
            } else {
                settingsLayout.setVisibility(View.VISIBLE); // Show settings
            }
            // If the overlay was minimized, expand it when settings are opened
            if (isMinimized) {
                params.width = WindowManager.LayoutParams.WRAP_CONTENT;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                params.x = 0; // Reset position
                params.y = 100; // Reset position
                windowManager.updateViewLayout(overlayView, params);
                isMinimized = false;
            }
        });

        // Hide background button listener to toggle overlay background transparency
        hideBackgroundButton.setOnClickListener(v -> {
            if (isBackgroundVisible) {
                overlayView.setBackgroundColor(getResources().getColor(android.R.color.transparent)); // Make background transparent
            } else {
                overlayView.setBackgroundResource(R.drawable.rounded_rectangle_shape); // Restore original background
            }
            isBackgroundVisible = !isBackgroundVisible; // Toggle state
        });

        // Shape button listener to toggle overlay shape between circular and rectangular
        shapeButton.setOnClickListener(v -> {
            if (isCircular) {
                overlayView.setBackgroundResource(R.drawable.rounded_rectangle_shape); // Change to rectangle
            } else {
                overlayView.setBackgroundResource(R.drawable.circular_shape); // Change to circle
            }
            isCircular = !isCircular; // Toggle state
        });

        // Runnable to periodically update battery and system data
        updateBatteryDataRunnable = new Runnable() {
            @Override
            public void run() {
                displayBatteryTemperature();
                displayCpuTemperature();
                displayRefreshRate();
                displayFreeSpace();
                displayBatteryHealth();
                displayPowerUsage();
                displayBatteryUsageDuration();
                displayBatteryCapacity();
                handler.postDelayed(this, 1000); // Update data every 1 second (1000 milliseconds)
            }
        };
        handler.post(updateBatteryDataRunnable); // Start the periodic updates

        // Checkbox listeners to toggle visibility of various data points on the overlay
        checkBatteryTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            batteryTemperatureOverlay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        checkCpuTemp.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cpuTemperatureOverlay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        checkRefreshRate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            refreshRateOverlay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        checkStorage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            storageSpaceOverlay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        checkBatteryHealth.setOnCheckedChangeListener((buttonView, isChecked) -> {
            batteryHealthPercentage.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            batteryStatus.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            chargingCycles.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            batteryVoltage.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            batteryCurrent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
        checkPowerUsage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textViewPowerUsage.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            batteryUsageDuration.setVisibility(isChecked ? View.VISIBLE : View.GONE); // Also show/hide duration
        });
        checkBatteryCapacity.setOnCheckedChangeListener((buttonView, isChecked) -> {
            batteryCapacityText.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Toggles the size of the overlay between a minimized and expanded state.
     */
    private void toggleOverlaySize() {
        if (isMinimized) {
            // Expand the overlay to wrap content
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.x = 0; // Reset X position
            params.y = 100; // Reset Y position
        } else {
            // Minimize the overlay to a fixed small size
            params.width = 150;
            params.height = 150;
            params.x = 100; // Set X position for minimized state
            params.y = 100; // Set Y position for minimized state
        }
        isMinimized = !isMinimized; // Toggle minimized state
        windowManager.updateViewLayout(overlayView, params); // Apply the new layout parameters
    }

    /**
     * Displays the duration of battery usage since the app started.
     */
    private void displayBatteryUsageDuration() {
        long elapsedTimeMillis = SystemClock.elapsedRealtime() - startTime; // Calculate elapsed time in milliseconds
        long elapsedTimeSeconds = elapsedTimeMillis / 1000;
        long hours = elapsedTimeSeconds / 3600;
        long minutes = (elapsedTimeSeconds % 3600) / 60;

        // Format the duration into hours and minutes
        String formattedDuration = String.format("Aktif %d jam %d menit", hours, minutes);
        batteryUsageDuration.setText(formattedDuration);

        // Show or hide the duration TextView based on the power usage checkbox state
        if (checkPowerUsage.isChecked()) {
            batteryUsageDuration.setVisibility(View.VISIBLE);
        } else {
            batteryUsageDuration.setVisibility(View.GONE);
        }
    }

    /**
     * Called when the activity is being destroyed.
     * It's important to stop the overlay service and remove the view from the window manager.
     * REMOVED: stopService(serviceIntent); to allow the foreground service to persist.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the periodic updates
        handler.removeCallbacks(updateBatteryDataRunnable);
        // Remove the overlay view from the window manager to prevent window leaks
        if (overlayView != null && windowManager != null) {
            windowManager.removeView(overlayView);
            overlayView = null; // Clear reference
        }
        // The OverlayService will NOT be stopped here. It will continue running
        // in the foreground until explicitly stopped by the user or system.
    }

    /**
     * Called when the activity is resumed.
     * Re-starts the overlay service to ensure it's running.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // This might not be strictly necessary if the service is START_STICKY,
        // but it ensures the service is running when the activity is active.
        Intent serviceIntent = new Intent(this, OverlayService.class);
        startService(serviceIntent);
    }

    /**
     * Displays the current battery temperature.
     */
    private void displayBatteryTemperature() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = registerReceiver(null, ifilter);
        if (batteryStatusIntent != null) {
            int temperature = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            double batteryTempInCelsius = temperature / 10.0;
            batteryTemperatureOverlay.setText("Suhu Baterai: " + batteryTempInCelsius + "°C");
        } else {
            batteryTemperatureOverlay.setText("Suhu Baterai: N/A");
        }
    }

    /**
     * Displays the current CPU temperature.
     * Note: Direct file access for CPU temperature is often unreliable and device-dependent.
     */
    private void displayCpuTemperature() {
        String cpuTemp = getCpuTemperature();
        cpuTemperatureOverlay.setText("Suhu CPU: " + cpuTemp);
    }

    /**
     * Attempts to read CPU temperature from a system file.
     * This method is highly device-dependent and may not work on all Android devices.
     *
     * @return CPU temperature string or "Tidak Diketahui" if unable to read.
     */
    private String getCpuTemperature() {
        String temperature = "";
        try {
            // Common path for CPU temperature on many Android devices
            BufferedReader reader = new BufferedReader(new FileReader("/sys/class/thermal/thermal_zone0/temp"));
            temperature = reader.readLine();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            // Fallback for other possible thermal zone paths if the primary one fails
            try {
                BufferedReader reader = new BufferedReader(new FileReader("/sys/devices/virtual/thermal/thermal_zone0/temp"));
                temperature = reader.readLine();
                reader.close();
            } catch (IOException e2) {
                e2.printStackTrace();
                return "Tidak Diketahui"; // If all attempts fail
            }
        }
        // Convert millicelsius to Celsius
        return (temperature.isEmpty()) ? "Tidak Diketahui" : (Integer.parseInt(temperature) / 1000.0) + "°C";
    }

    /**
     * Displays the current screen refresh rate.
     */
    private void displayRefreshRate() {
        float refreshRate = getWindowManager().getDefaultDisplay().getRefreshRate();
        refreshRateOverlay.setText("Refresh Rate: " + refreshRate + "Hz");
    }

    /**
     * Displays the available internal storage space.
     */
    private void displayFreeSpace() {
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        long availableBytes = statFs.getAvailableBlocksLong() * statFs.getBlockSizeLong();
        long freeSpaceGB = availableBytes / (1024 * 1024 * 1024);
        storageSpaceOverlay.setText("Ruang Kosong: " + freeSpaceGB + " GB");
    }

    /**
     * Displays various battery health metrics.
     */
    private void displayBatteryHealth() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatusIntent = registerReceiver(null, ifilter);
        if (batteryStatusIntent != null) {
            int level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            // Calculate battery health percentage (this is often just current charge level)
            int batteryHealth = (int) (((float) level / (float) scale) * 100);
            batteryHealthPercentage.setText("Kesehatan Baterai: " + batteryHealth + "%");

            int health = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
            String status = getHealthStatus(health);
            batteryStatus.setText("Status Baterai: " + status); // Use setText, not setType

            // Charging cycles are generally not exposed via public Android APIs
            chargingCycles.setText("Siklus Pengisian: Tidak Diketahui");

            int voltage = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
            batteryVoltage.setText("Tegangan Baterai: " + voltage / 1000.0 + "V");

            // Battery current (current_now) can be retrieved via BatteryManager
            BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                long currentNow = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                // currentNow is in microamperes, convert to milliamperes
                batteryCurrent.setText("Arus Baterai: " + (currentNow / 1000.0) + " mA");
            } else {
                batteryCurrent.setText("Arus Baterai: N/A");
            }
        } else {
            batteryHealthPercentage.setText("Kesehatan Baterai: N/A");
            batteryStatus.setText("Status Baterai: N/A");
            chargingCycles.setText("Siklus Pengisian: N/A");
            batteryVoltage.setText("Tegangan Baterai: N/A");
            batteryCurrent.setText("Arus Baterai: N/A");
        }
    }

    /**
     * Displays the current power usage (charge/discharge current).
     */
    private void displayPowerUsage() {
        BatteryManager batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (batteryManager != null) {
            long powerUsage = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW); // in microamperes
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatusIntent = registerReceiver(null, ifilter);
            int status = -1;
            if (batteryStatusIntent != null) {
                status = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            }

            // Convert to milliamperes and take absolute value
            long powerUsageInMilliAmps = Math.abs(powerUsage) / 1000;

            if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
                textViewPowerUsage.setText("Daya digunakan (pengisian): " + powerUsageInMilliAmps + " mA");
            } else {
                textViewPowerUsage.setText("Daya digunakan (pemakaian): " + powerUsageInMilliAmps + " mA");
            }
        } else {
            textViewPowerUsage.setText("Gagal mengambil informasi daya baterai");
        }
    }

    /**
     * Displays the current battery capacity percentage.
     * This also attempts to read from a system file, which can be unreliable.
     */
    private void displayBatteryCapacity() {
        try {
            // Common path for battery capacity percentage
            BufferedReader reader = new BufferedReader(new FileReader("/sys/class/power_supply/battery/capacity"));
            String line = reader.readLine();
            reader.close();
            if (line != null) {
                int capacity = Integer.parseInt(line);
                if (capacity != previousChargeLevel) { // Only update if capacity has changed
                    batteryCapacityRealtimeText.setText("Baterai: " + capacity + "%");
                    previousChargeLevel = capacity;
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            batteryCapacityRealtimeText.setText("Baterai: Tidak Diketahui");
        }
    }

    /**
     * Converts BatteryManager health status codes to human-readable strings.
     *
     * @param health The health code from BatteryManager.EXTRA_HEALTH.
     * @return A string representation of the battery health.
     */
    private String getHealthStatus(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "Baik";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "Terlalu Panas";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "Mati";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "Tegangan Berlebih";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "Kegagalan Tidak Spesifik";
            case BatteryManager.BATTERY_HEALTH_COLD: // Added cold status
                return "Dingin";
            default:
                return "Tidak Diketahui";
        }
    }
}
