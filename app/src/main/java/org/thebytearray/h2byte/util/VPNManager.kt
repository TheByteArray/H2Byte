package org.thebytearray.h2byte.util

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.thebytearray.h2byte.App
import org.thebytearray.h2byte.dto.ServerConfig
import org.thebytearray.h2byte.dto.State
import org.thebytearray.h2byte.service.H2ByteService

object VPNManager {

    private var activityResultLauncher: ActivityResultLauncher<Intent>? = null

    private val _vpnState = MutableStateFlow<State>(State.DISCONNECTED)
    val vpnState: StateFlow<State> = _vpnState.asStateFlow()

    fun setActivityResultLauncher(launcher: ActivityResultLauncher<Intent>) {
        activityResultLauncher = launcher
    }

    fun startVPNConnection() {
        if (_vpnState.value == State.CONNECTED || _vpnState.value == State.CONNECTING) {
            return
        }
        
        val startIntent = Intent(App.app, H2ByteService::class.java).apply {
            action = H2ByteService.START_ACTION
        }
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
        val stopIntent = Intent(App.app, H2ByteService::class.java).apply {
            action = H2ByteService.STOP_ACTION
        }
        App.app.startService(stopIntent)
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

} 