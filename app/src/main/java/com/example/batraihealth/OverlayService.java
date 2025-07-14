package com.example.batraihealth;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.app.Service;
import android.os.Handler;
import android.os.Looper;
import android.content.IntentFilter; // Import for IntentFilter

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // Import for LocalBroadcastManager

/**
 * OverlayService is a foreground service responsible for maintaining the overlay
 * on the screen and allowing the main activity to update its content even when
 * the app is in the background. It ensures the app's process is less likely
 * to be killed by the Android system.
 *
 * This updated version includes periodic data refreshing and broadcasting
 * the updated data to the UI (if the UI is active).
 */
public class OverlayService extends Service {

    // Unique ID for the notification channel
    private static final String CHANNEL_ID = "BatteryHealthServiceChannel";
    // Unique ID for the foreground service notification
    private static final int NOTIFICATION_ID = 1;

    // Action for broadcasting battery/CPU data updates
    public static final String ACTION_BATTERY_CPU_UPDATE = "com.example.batraihealth.BATTERY_CPU_UPDATE";
    // Keys for data sent in the broadcast intent
    public static final String EXTRA_BATTERY_LEVEL = "battery_level";
    public static final String EXTRA_CPU_USAGE = "cpu_usage";

    // Handler to schedule periodic tasks
    private Handler handler;
    // Runnable to perform the periodic data update and broadcast
    private Runnable periodicUpdateRunnable;
    // Interval for updates (e.g., every 5 seconds)
    private static final long UPDATE_INTERVAL_MS = 5000; // 5 seconds

    /**
     * Called when the service is first created.
     * This is where you perform one-time setup procedures.
     * It's crucial to call startForeground() here to make it a foreground service.
     */
    @SuppressLint("ForegroundServiceType") // Suppress lint warning for foregroundServiceType as it's handled in AndroidManifest
    @Override
    public void onCreate() {
        super.onCreate();

        // Create a Notification Channel for Android 8.0 (Oreo) and higher.
        // Notification channels are required for all notifications on Android O+
        // and allow users to control notification behavior for specific categories.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, // The unique ID of the channel
                    "Battery Health Monitor Service", // The user-visible name of the channel
                    NotificationManager.IMPORTANCE_LOW // Importance level (LOW means less intrusive)
            );
            // Get the NotificationManager system service
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            // Create the notification channel
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Build the Notification for the Foreground Service.
        // This notification will be displayed persistently in the status bar
        // while the service is running in the foreground.
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BatraiHealth Running") // Title of the notification
                .setContentText("Monitoring battery and CPU info.") // Text content of the notification
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Small icon for the notification (replace with your app's icon)
                .setOngoing(true) // Makes the notification non-dismissible by the user
                .setPriority(NotificationCompat.PRIORITY_LOW) // Matches the channel importance
                .build();

        // Start the service in the foreground.
        // This makes the service run with higher priority, preventing the system
        // from easily killing it to free up resources. It requires the notification.
        startForeground(NOTIFICATION_ID, notification);

        // Initialize Handler with the main Looper to schedule tasks on the main thread
        handler = new Handler(Looper.getMainLooper());

        // Define the Runnable for periodic updates
        periodicUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // --- PENTING: Tempatkan logika pengambilan data baterai dan CPU di sini ---
                // Ini adalah placeholder. Anda perlu mengganti ini dengan kode nyata
                // untuk mendapatkan level baterai dan penggunaan CPU.
                int batteryLevel = getBatteryLevel(); // Implement this method
                double cpuUsage = getCpuUsage();     // Implement this method

                // Buat Intent untuk menyiarkan data yang diperbarui
                Intent intent = new Intent(ACTION_BATTERY_CPU_UPDATE);
                intent.putExtra(EXTRA_BATTERY_LEVEL, batteryLevel);
                intent.putExtra(EXTRA_CPU_USAGE, cpuUsage);

                // Siarkan Intent menggunakan LocalBroadcastManager
                // LocalBroadcastManager lebih efisien dan aman untuk komunikasi dalam aplikasi
                LocalBroadcastManager.getInstance(OverlayService.this).sendBroadcast(intent);

                // Jadwalkan Runnable ini untuk dijalankan lagi setelah interval tertentu
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };

        // Mulai pembaruan berkala segera setelah layanan dibuat
        handler.post(periodicUpdateRunnable);
    }

    /**
     * Placeholder method to get battery level.
     * You need to implement the actual logic here.
     * For demonstration, it returns a random value.
     */
    private int getBatteryLevel() {
        // Example: You would use BatteryManager to get actual battery info
        // Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        // int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        // if (level == -1 || scale == -1) {
        //     return 50; // Default if info not available
        // }
        // return (int) (((float) level / (float) scale) * 100.0f);
        return (int) (Math.random() * 100); // Random for demo
    }

    /**
     * Placeholder method to get CPU usage.
     * You need to implement the actual logic here.
     * This is more complex and often involves reading /proc/stat or similar.
     * For demonstration, it returns a random value.
     */
    private double getCpuUsage() {
        // Example: Reading /proc/stat for CPU usage is complex and requires parsing.
        // This is just a placeholder.
        return Math.random() * 100.0; // Random for demo
    }


    /**
     * Called when a client is attempting to bind to the service.
     * In this case, no binding is needed as the service primarily runs
     * independently to maintain the overlay.
     *
     * @param intent The Intent that was used to bind to this service.
     * @return null, as no binding interface is provided.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding is needed for this service
    }

    /**
     * Called by the system every time a client explicitly starts the service
     * by calling startService(Intent).
     *
     * @param intent The Intent supplied to startService(Intent).
     * @param flags Additional data about this start request.
     * @param startId A unique integer representing this specific request to start.
     * @return START_STICKY indicates that if the service's process is killed,
     * the system should try to re-create the service.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Jika layanan dimulai ulang oleh sistem setelah terbunuh,
        // pastikan Runnable dijadwalkan ulang.
        if (handler != null && periodicUpdateRunnable != null) {
            handler.removeCallbacks(periodicUpdateRunnable); // Hapus callback yang mungkin ada
            handler.post(periodicUpdateRunnable); // Jadwalkan ulang
        }
        return START_STICKY;  // Ensure the service is restarted if it gets killed
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.
     * The service should clean up any resources it holds (threads, registered receivers, etc.)
     * and stop any ongoing tasks.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Hentikan pembaruan berkala saat layanan dihancurkan
        if (handler != null && periodicUpdateRunnable != null) {
            handler.removeCallbacks(periodicUpdateRunnable);
        }
        // Stop the foreground service.
        // Passing 'true' removes the notification.
        stopForeground(true);
        // Any other cleanup can go here
    }
}
