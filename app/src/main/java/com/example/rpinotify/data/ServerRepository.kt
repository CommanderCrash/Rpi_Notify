package com.example.rpinotify.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ServerRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("servers", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val TAG = "ServerRepository"

    suspend fun removeServer(server: NotificationServer) = withContext(Dispatchers.IO) {
        try {
            val servers = loadSavedServers().toMutableList()
            servers.removeAll { it.id == server.id }
            prefs.edit().putString("saved_servers", gson.toJson(servers)).commit() // Using commit() instead of apply()
            Log.d(TAG, "Server removed successfully: ${server.ipAddress}")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing server", e)
        }
    }

    suspend fun loadSavedServers(): List<NotificationServer> = withContext(Dispatchers.IO) {
        try {
            val json = prefs.getString("saved_servers", null)
            if (json != null) {
                gson.fromJson(json, Array<NotificationServer>::class.java).toList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading servers", e)
            emptyList()
        }
    }

    suspend fun saveServer(server: NotificationServer) = withContext(Dispatchers.IO) {
        try {
            val servers = loadSavedServers().toMutableList()
            servers.add(server)
            prefs.edit().putString("saved_servers", gson.toJson(servers)).commit()
            Log.d(TAG, "Server saved successfully: ${server.ipAddress}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving server", e)
        }
    }
}