package com.example.rpinotify.network

import android.util.Log
import com.example.rpinotify.data.NotificationServer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ServerConnection(
    private val server: NotificationServer,
    private val onMessageReceived: (String, String) -> Unit,
    private val onStatusChanged: (NotificationServer) -> Unit
) {
    private var tcpSocket: Socket? = null
    private val isRunning = AtomicBoolean(false)
    private val TAG = "ServerConnection"

    fun connect() {
        Log.d(TAG, "Attempting to connect to ${server.ipAddress}:${server.port}")
        isRunning.set(true)
        
        thread(start = true) {
            try {
                tcpSocket = Socket(server.ipAddress, server.port)
                Log.d(TAG, "Connected successfully to ${server.ipAddress}:${server.port}")
                updateStatus("Connected")
                
                val reader = BufferedReader(InputStreamReader(tcpSocket!!.getInputStream()))
                while (isRunning.get()) {
                    try {
                        val message = reader.readLine()
                        if (message != null) {
                            Log.d(TAG, "Received: $message")
                            handleMessage(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reading message", e)
                        if (isRunning.get()) {
                            reconnect()
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                updateStatus("Failed")
                if (isRunning.get()) {
                    reconnect()
                }
            }
        }
    }

    private fun reconnect() {
        try {
            tcpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        }

        Thread.sleep(5000) // Wait 5 seconds before reconnecting
        
        if (isRunning.get()) {
            Log.d(TAG, "Attempting to reconnect...")
            connect()
        }
    }

    fun checkConnection() {
        thread(start = true) {
            try {
                tcpSocket?.let { socket ->
                    socket.getOutputStream().write("PING\n".toByteArray())
                    socket.getOutputStream().flush()
                    Log.d(TAG, "Ping sent successfully")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ping failed", e)
                reconnect()
            }
        }
    }

    private fun handleMessage(message: String) {
        try {
            Log.d(TAG, "Processing message: $message")
            val parts = message.trim().split("|")
            if (parts.size >= 2) {
                val serverName = parts[0]
                // Join the rest of the parts in case message contains | characters
                val content = parts.subList(1, parts.size).joinToString("|")
                Log.d(TAG, "Parsed message - Server: $serverName, Content: $content")
                onMessageReceived(serverName, content)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    private fun updateStatus(newStatus: String) {
        Log.d(TAG, "Status update for ${server.ipAddress}: $newStatus")
        server.status = newStatus
        onStatusChanged(server)
    }

    fun disconnect() {
        isRunning.set(false)
        try {
            tcpSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket", e)
        }
        updateStatus("Disconnected")
    }
}