package com.example.rpinotify

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rpinotify.databinding.ActivityMainBinding
import com.example.rpinotify.service.NotificationService
import com.example.rpinotify.ui.AddServerDialog
import com.example.rpinotify.ui.ServerAdapter
import com.example.rpinotify.data.NotificationServer
import com.example.rpinotify.viewmodel.NotificationViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var serverAdapter: ServerAdapter
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupAddServerButton()
        startNotificationService()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        serverAdapter = ServerAdapter(
            onServerClick = { server ->
                viewModel.checkServerConnection(server)  // Changed from pingServer to checkServerConnection
            },
            onServerLongClick = { server ->
                showRemoveServerDialog(server)
            }
        )
        binding.serversRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = serverAdapter
        }
    }

    private fun setupAddServerButton() {
        binding.addServerButton.setOnClickListener {
            try {
                Log.d(TAG, "Showing add server dialog")
                AddServerDialog().show(supportFragmentManager, "AddServerDialog")
            } catch (e: Exception) {
                Log.e(TAG, "Error showing dialog", e)
            }
        }
    }

    private fun startNotificationService() {
        Intent(this, NotificationService::class.java).also { intent ->
            startService(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.servers.observe(this) { servers ->
            serverAdapter.submitList(servers)
        }
    }

    private fun showRemoveServerDialog(server: NotificationServer) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove Server")
            .setMessage("Are you sure you want to remove ${server.name.ifEmpty { server.ipAddress }}?")
            .setPositiveButton("Remove") { dialog, _ ->
                try {
                    Log.d(TAG, "Removing server: ${server.ipAddress}")
                    viewModel.removeServer(server)
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing server", e)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}