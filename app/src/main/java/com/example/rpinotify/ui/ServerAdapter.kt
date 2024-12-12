package com.example.rpinotify.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rpinotify.data.NotificationServer
import com.example.rpinotify.databinding.ItemServerBinding

class ServerAdapter(
    private val onServerClick: (NotificationServer) -> Unit,
    private val onServerLongClick: (NotificationServer) -> Unit
) : ListAdapter<NotificationServer, ServerAdapter.ServerViewHolder>(ServerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServerViewHolder {
        val binding = ItemServerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ServerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ServerViewHolder(
        private val binding: ItemServerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onServerClick(getItem(position))
                }
            }

            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onServerLongClick(getItem(position))
                }
                true
            }
        }

        fun bind(server: NotificationServer) {
            binding.apply {
                serverName.text = server.name.ifEmpty { server.ipAddress }
                serverStatus.text = server.status
                serverIp.text = server.ipAddress
                pingValue.text = if (server.pingMs > 0) "${server.pingMs}ms" else "-"

                // Apply different background based on connection status
                root.setBackgroundResource(
                    when (server.status) {
                        "Connected" -> com.example.rpinotify.R.drawable.bg_server_connected
                        "Disconnected" -> com.example.rpinotify.R.drawable.bg_server_disconnected
                        else -> com.example.rpinotify.R.drawable.bg_server_default
                    }
                )

                // Set text color based on status
                serverStatus.setTextColor(
                    when (server.status) {
                        "Connected" -> android.graphics.Color.parseColor("#39FF14") // neon green
                        "Disconnected" -> android.graphics.Color.parseColor("#FF3B30") // red
                        else -> android.graphics.Color.parseColor("#00FFFF") // neon blue
                    }
                )
            }
        }
    }

    private class ServerDiffCallback : DiffUtil.ItemCallback<NotificationServer>() {
        override fun areItemsTheSame(oldItem: NotificationServer, newItem: NotificationServer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationServer, newItem: NotificationServer): Boolean {
            return oldItem == newItem
        }
    }
}