package com.example.rpinotify.ui

import android.os.Bundle
import android.util.Log  // Added this import
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.rpinotify.databinding.DialogAddServerBinding
import com.example.rpinotify.viewmodel.NotificationViewModel

class AddServerDialog : DialogFragment() {
    private var _binding: DialogAddServerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationViewModel by activityViewModels()
    private val TAG = "AddServerDialog"  // Added TAG for logging

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.addButton.setOnClickListener {
            try {
                val ipAddress = binding.ipAddressInput.text.toString()
                if (ipAddress.isNotEmpty()) {
                    Log.d(TAG, "Adding server: $ipAddress")
                    viewModel.addServer(ipAddress)
                    dismiss()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding server", e)
            }
        }

        binding.cancelButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}