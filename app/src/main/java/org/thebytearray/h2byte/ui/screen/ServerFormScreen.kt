package org.thebytearray.h2byte.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.thebytearray.h2byte.dto.ServerConfig
import org.thebytearray.h2byte.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerFormScreen(navController: NavController, viewModel: AppViewModel, serverIndex: Int? = null) {
    val server = serverIndex?.let { viewModel.servers.value.getOrNull(it) }
    var name by remember { mutableStateOf(server?.name ?: "") }
    var address by remember { mutableStateOf(server?.address ?: "") }
    var authToken by remember { mutableStateOf(server?.authToken ?: "") }
    var uploadSpeed by remember { mutableStateOf(server?.uploadSpeedMbps?.toString() ?: "") }
    var downloadSpeed by remember { mutableStateOf(server?.downloadSpeedMbps?.toString() ?: "") }
    var allowInsecure by remember { mutableStateOf(server?.allowInsecure ?: false) }
    var showPassword by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var authTokenError by remember { mutableStateOf<String?>(null) }
    var uploadSpeedError by remember { mutableStateOf<String?>(null) }
    var downloadSpeedError by remember { mutableStateOf<String?>(null) }

    fun validateFields(): Boolean {
        nameError = if (name.isBlank()) "Server name is required" else null
        addressError = if (address.isBlank()) "Server address is required" else null
        authTokenError = if (authToken.isBlank()) "Auth token is required" else null
        uploadSpeedError = if (uploadSpeed.isBlank()) "Upload speed is required" 
            else if (!uploadSpeed.all { it.isDigit() }) "Must be a number" else null
        downloadSpeedError = if (downloadSpeed.isBlank()) "Download speed is required"
            else if (!downloadSpeed.all { it.isDigit() }) "Must be a number" else null

        return nameError == null && addressError == null && authTokenError == null && 
               uploadSpeedError == null && downloadSpeedError == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (serverIndex != null) "Edit Hysteria2 Server" else "Add Hysteria2 Server") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Server Details",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    nameError = null
                },
                label = { Text("Server Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nameError != null,
                supportingText = { nameError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { 
                    address = it
                    addressError = null
                },
                label = { Text("Server Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = addressError != null,
                supportingText = { addressError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = authToken,
                onValueChange = { 
                    authToken = it
                    authTokenError = null
                },
                label = { Text("Auth Token") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (showPassword) "Hide password" else "Show password"
                        )
                    }
                },
                isError = authTokenError != null,
                supportingText = { authTokenError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Speed Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = uploadSpeed,
                onValueChange = { 
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        uploadSpeed = it
                        uploadSpeedError = null
                    }
                },
                label = { Text("Upload Speed (Mbps)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uploadSpeedError != null,
                supportingText = { uploadSpeedError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = downloadSpeed,
                onValueChange = { 
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        downloadSpeed = it
                        downloadSpeedError = null
                    }
                },
                label = { Text("Download Speed (Mbps)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = downloadSpeedError != null,
                supportingText = { downloadSpeedError?.let { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Security Settings",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = allowInsecure,
                    onCheckedChange = { allowInsecure = it }
                )
                Text(
                    text = "Allow Insecure Connection",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (validateFields()) {
                        val serverConfig = ServerConfig(
                            name = name,
                            address = address,
                            authToken = authToken,
                            uploadSpeedMbps = uploadSpeed.toIntOrNull() ?: 0,
                            downloadSpeedMbps = downloadSpeed.toIntOrNull() ?: 0,
                            allowInsecure = allowInsecure
                        )
                        if (serverIndex != null) {
                            viewModel.editServer(serverIndex, serverConfig)
                        } else {
                            viewModel.addServer(serverConfig)
                        }
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Server")
            }
        }
    }
} 