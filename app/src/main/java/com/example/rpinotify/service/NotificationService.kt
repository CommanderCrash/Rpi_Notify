package com.example.rpinotify.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.rpinotify.MainActivity  // Updated import path
import com.example.rpinotify.R

class NotificationService : Service() {
    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val TAG = "NotificationService"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle the initial start of the service
        if (intent?.action == null) {
            startForeground(NOTIFICATION_ID, createForegroundNotification())
        }
        // Handle message notifications
        else if (intent.action == ACTION_SHOW_MESSAGE) {
            val serverName = intent.getStringExtra("serverName")
            val message = intent.getStringExtra("message")
            Log.d(TAG, "Received message from $serverName: $message")
            if (serverName != null && message != null) {
                showNotification(serverName, message)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "RPI Notify Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for RPi notifications"
            }

            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "RPI Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications from RPi servers"
                enableLights(true)
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(messageChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RPI Notify Service")
            .setContentText("Service is running")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    fun showNotification(serverName: String, message: String) {
        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID)
            .setContentTitle(serverName)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        Log.d(TAG, "Showing notification: $notificationId")
        notificationManager.notify(notificationId, notification)
    }

    companion object {
        private const val CHANNEL_ID = "rpi_notify_service"
        private const val MESSAGE_CHANNEL_ID = "rpi_messages"
        private const val NOTIFICATION_ID = 1
        const val ACTION_SHOW_MESSAGE = "com.example.rpinotify.SHOW_MESSAGE"
    }
}