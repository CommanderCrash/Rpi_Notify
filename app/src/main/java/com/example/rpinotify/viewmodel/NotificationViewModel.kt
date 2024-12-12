package com.example.rpinotify.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.rpinotify.data.NotificationServer
import com.example.rpinotify.data.ServerRepository
import com.example.rpinotify.network.ServerConnection
import com.example.rpinotify.service.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val _servers = MutableLiveData<List<NotificationServer>>(emptyList())
    val servers: LiveData<List<NotificationServer>> = _servers

    private val serverConnections = mutableMapOf<String, ServerConnection>()
    private val serverRepository = ServerRepository(application)
    private val TAG = "NotificationViewModel"

    init {
        loadSavedServers()
    }

    private fun loadSavedServers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedServers = serverRepository.loadSavedServers()
                withContext(Dispatchers.Main) {
                    _servers.value = savedServers
                    savedServers.forEach { server ->
                        connectToServer(server)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading servers", e)
            }
        }
    }

    fun addServer(ipAddress: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val server = NotificationServer(ipAddress = ipAddress)
                serverRepository.saveServer(server)
                withContext(Dispatchers.Main) {
                    val currentList = _servers.value.orEmpty().toMutableList()
                    currentList.add(server)
                    _servers.value = currentList
                    connectToServer(server)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding server", e)
            }
        }
    }

    fun removeServer(server: NotificationServer) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                serverRepository.removeServer(server)
                withContext(Dispatchers.Main) {
                    disconnectFromServer(server)
                    val currentList = _servers.value.orEmpty().toMutableList()
                    currentList.remove(server)
                    _servers.value = currentList
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing server", e)
            }
        }
    }

    fun checkServerConnection(server: NotificationServer) {
        viewModelScope.launch {
            try {
                serverConnections[server.id]?.checkConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking server connection", e)
            }
        }
    }

    private fun connectToServer(server: NotificationServer) {
        try {
            val connection = ServerConnection(
                server = server,
                onMessageReceived = { serverName, message ->
                    Log.d(TAG, "Message received: $serverName - $message")
                    val context = getApplication<Application>()
                    val intent = Intent(context, NotificationService::class.java).apply {
                        action = NotificationService.ACTION_SHOW_MESSAGE
                        putExtra("serverName", serverName)
                        putExtra("message", message)
                    }
                    context.startService(intent)
                },
                onStatusChanged = { updatedServer ->
                    viewModelScope.launch(Dispatchers.Main) {
                        updateServerStatus(updatedServer)
                    }
                }
            )
            serverConnections[server.id] = connection
            connection.connect()
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to server", e)
        }
    }

    private fun disconnectFromServer(server: NotificationServer) {
        try {
            serverConnections[server.id]?.disconnect()
            serverConnections.remove(server.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting server", e)
        }
    }

    private fun updateServerStatus(updatedServer: NotificationServer) {
        val currentList = _servers.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == updatedServer.id }
        if (index != -1) {
            currentList[index] = updatedServer
            _servers.value = currentList
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            serverConnections.values.forEach { it.disconnect() }
            serverConnections.clear()
        }
    }
}