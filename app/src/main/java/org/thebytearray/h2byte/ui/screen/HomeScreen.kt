package org.thebytearray.h2byte.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.thebytearray.h2byte.dto.State
import org.thebytearray.h2byte.ui.component.ServerItem
import org.thebytearray.h2byte.ui.viewmodel.AppViewModel
import org.thebytearray.h2byte.util.VPNManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: AppViewModel) {
    val servers by viewModel.servers.collectAsState()
    val selectedIndex by viewModel.selectedServerIndex.collectAsState()
    val vpnState by VPNManager.vpnState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("H2Byte") },
                actions = {
                    IconButton(onClick = { navController.navigate("server_form") }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Server")
                    }
                }
            )
        },
        floatingActionButton = {
            val selectedServer = viewModel.getSelectedServer()
            if (selectedServer != null) {
                FloatingActionButton(
                    onClick = {
                        when (vpnState) {
                            State.CONNECTED -> {
                                Log.d("HACKER", "HomeScreen: Stop Connection called")
                                VPNManager.stopVPNConnection()
                            }
                            State.DISCONNECTED -> {
                                if (VPNManager.isVpnPrepared()) {
                                    VPNManager.startVPNConnection()
                                } else {
                                    VPNManager.prepareVpnConnection(context as android.app.Activity)
                                }
                            }
                            State.CONNECTING -> {} // Do nothing while connecting
                        }
                    }
                ) {
                    Icon(
                        imageVector = when (vpnState) {
                            State.CONNECTED -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = when (vpnState) {
                            State.CONNECTED -> "Pause VPN"
                            else -> "Start VPN"
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(servers) { index, server ->
                    ServerItem(
                        server = server,
                        isSelected = index == selectedIndex,
                        onClick = { viewModel.selectServer(index) },
                        onDelete = { viewModel.deleteServer(index) },
                        onEdit = { navController.navigate("server_form/$index") }
                    )
                }
            }
        }
    }
} 