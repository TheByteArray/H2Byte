package org.thebytearray.h2byte

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.thebytearray.h2byte.ui.screen.HomeScreen
import org.thebytearray.h2byte.ui.screen.ServerFormScreen
import org.thebytearray.h2byte.ui.theme.H2ByteTheme
import org.thebytearray.h2byte.ui.viewmodel.AppViewModel
import org.thebytearray.h2byte.util.VPNManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vpnPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                VPNManager.startVPNConnection()
            }
        }
        VPNManager.setActivityResultLauncher(vpnPermissionLauncher)

        setContent {

            H2ByteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val viewModel: AppViewModel = viewModel()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(navController, viewModel)
                        }
                        composable("server_form") {
                            ServerFormScreen(navController, viewModel)
                        }
                        composable("server_form/{serverIndex}") { backStackEntry ->
                            val serverIndex = backStackEntry.arguments?.getString("serverIndex")?.toIntOrNull()
                            ServerFormScreen(navController, viewModel, serverIndex)
                        }
                    }
                }
            }
        }
    }
}