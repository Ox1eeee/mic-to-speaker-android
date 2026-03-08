package com.bluetooth.bluetoothmictospeaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.bluetooth.bluetoothmictospeaker.audio.AudioViewModel
import com.bluetooth.bluetoothmictospeaker.ui.home.HomeScreen
import com.bluetooth.bluetoothmictospeaker.ui.theme.BluetoothMicToSpeakerTheme
import com.bluetooth.bluetoothmictospeaker.utils.PermissionHelper

class MainActivity : ComponentActivity() {

    private val audioViewModel: AudioViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        audioViewModel.setPermissionGranted(allGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val hasPermission = PermissionHelper.hasAllPermissions(this)
        audioViewModel.setPermissionGranted(hasPermission)
        if (!hasPermission) {
            permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
        }

        setContent {
            BluetoothMicToSpeakerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(
                        viewModel = audioViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}