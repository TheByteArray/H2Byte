package org.thebytearray.h2byte.ui.viewmodel

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.thebytearray.h2byte.App
import org.thebytearray.h2byte.dto.ServerConfig
import org.thebytearray.h2byte.dto.State
import org.thebytearray.h2byte.service.H2ByteService
import org.thebytearray.h2byte.util.MmkvManager

class AppViewModel : ViewModel() {
    private val _servers = MutableStateFlow<List<ServerConfig>>(emptyList())
    val servers: StateFlow<List<ServerConfig>> = _servers.asStateFlow()

    private val _selectedServerIndex = MutableStateFlow<Int>(-1)
    val selectedServerIndex: StateFlow<Int> = _selectedServerIndex.asStateFlow()

    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    private val _vpnState = MutableStateFlow<State>(State.DISCONNECTED)
    val vpnState: StateFlow<State> = _vpnState.asStateFlow()

    init {
        loadServers()
        loadSelectedServer()
    }

    private fun loadServers() {
        viewModelScope.launch {
            _servers.value = MmkvManager.getServers()
        }
    }

    private fun loadSelectedServer() {
        viewModelScope.launch {
            _selectedServerIndex.value = MmkvManager.getSelectedServerIndex()
        }
    }

    fun addServer(server: ServerConfig) {
        viewModelScope.launch {
            val currentServers = _servers.value.toMutableList()
            currentServers.add(server)
            _servers.value = currentServers
            MmkvManager.saveServers(currentServers)
        }
    }

    fun editServer(index: Int, server: ServerConfig) {
        viewModelScope.launch {
            val currentServers = _servers.value.toMutableList()
            if (index in currentServers.indices) {
                currentServers[index] = server
                _servers.value = currentServers
                MmkvManager.saveServers(currentServers)
            }
        }
    }

    fun deleteServer(index: Int) {
        viewModelScope.launch {
            val currentServers = _servers.value.toMutableList()
            if (index in currentServers.indices) {
                currentServers.removeAt(index)
                _servers.value = currentServers
                MmkvManager.saveServers(currentServers)
                
                // If the deleted server was selected, clear the selection
                if (_selectedServerIndex.value == index) {
                    _selectedServerIndex.value = -1
                    MmkvManager.clearSelectedServer()
                } else if (_selectedServerIndex.value > index) {
                    // Adjust the selected index if it was after the deleted server
                    _selectedServerIndex.value -= 1
                    MmkvManager.saveSelectedServerIndex(_selectedServerIndex.value)
                }
            }
        }
    }

    fun selectServer(index: Int) {
        viewModelScope.launch {
            if (index in _servers.value.indices) {
                _selectedServerIndex.value = index
                MmkvManager.saveSelectedServerIndex(index)
            }
        }
    }

    fun getSelectedServer(): ServerConfig? {
        val index = _selectedServerIndex.value
        return if (index in _servers.value.indices) {
            _servers.value[index]
        } else {
            null
        }
    }

    fun setActivityResultLauncher(launcher: ActivityResultLauncher<Intent>) {
        activityResultLauncher = launcher
    }

    fun startVPNConnection() {
        if (_vpnState.value == State.CONNECTED || _vpnState.value == State.CONNECTING) {
            return
        }
        
        val startIntent = Intent(App.app, H2ByteService::class.java)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            App.app.startForegroundService(startIntent)
        } else {
            App.app.startService(startIntent)
        }
        _vpnState.value = State.CONNECTING
    }

    fun isVpnPrepared(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    App.app,
                    POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        val vpnServicePrepareIntent = VpnService.prepare(App.app)
        return vpnServicePrepareIntent == null
    }

    fun stopVPNConnection() {
        val stopIntent = Intent(App.app, H2ByteService::class.java)
        App.app.stopService(stopIntent)
        _vpnState.value = State.DISCONNECTED
    }

    fun prepareVpnConnection(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    POST_NOTIFICATIONS
                ) != PermissionChecker.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(activity, arrayOf(POST_NOTIFICATIONS), 101)
                return
            }
        }

        val vpnServicePrepareIntent = VpnService.prepare(activity)
        if (vpnServicePrepareIntent != null) {
            if (activityResultLauncher == null) {
                throw IllegalStateException("ActivityResultLauncher not set. Call setActivityResultLauncher first.")
            }
            activityResultLauncher?.launch(vpnServicePrepareIntent)
        } else {
            // If prepare returns null, VPN permission is already granted
            startVPNConnection()
        }
    }

    fun updateVpnState(newState: State) {
        _vpnState.value = newState
    }

    fun buildConfig(config: ServerConfig?): String {
        return """
            {
                "server": "${config?.address}",
                "auth": "${config?.authToken}",
                "tls": { "insecure": ${config?.allowInsecure} },
                "bandwidth": { "up": "${config?.uploadSpeedMbps} mbps", "down": "${config?.downloadSpeedMbps} mbps" },
                "fast_open": true,
                "lazy": true,
                "socks5": { "listen": "127.0.0.1:8920" }
            }
        """.trimIndent()
    }
}