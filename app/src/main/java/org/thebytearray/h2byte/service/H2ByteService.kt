package org.thebytearray.h2byte.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import cmd.Cmd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.thebytearray.h2byte.R
import org.thebytearray.h2byte.dto.ServerConfig
import org.thebytearray.h2byte.dto.State
import org.thebytearray.h2byte.util.Constant
import org.thebytearray.h2byte.util.MmkvManager
import org.thebytearray.h2byte.util.VPNManager
import java.io.File
import java.net.ServerSocket

class H2ByteService : VpnService() {

    companion object {
        private const val VPN_MTU = 1500
        private const val PRIVATE_VLAN4_ROUTER = "10.10.14.2"
        private const val TUN2SOCKS_LIB = "libtun2socks.so"
        private const val LOOPBACK_ADDRESS = "127.0.0.1"
        private const val SOCKS_PORT = 8920
        private const val SOCK_PATH = "sock_path"
        private const val TAG = "H2ByteService"
        private const val NOTIFICATION_ID = 1
        
        const val START_ACTION = "org.thebytearray.h2byte.START_VPN"
        const val STOP_ACTION = "org.thebytearray.h2byte.STOP_VPN"
    }

    private lateinit var tun2SocksProcess: Process
    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START_ACTION -> {
                Log.d(TAG, "Received START_ACTION")
                onCreate()
            }
            STOP_ACTION -> {
                Log.d(TAG, "Received STOP_ACTION")
                onDestroy()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "VPN Service Created")

        createNotificationChannel()
        ServiceCompat.startForeground(
            /* service = */ this,
            /* id = */ NOTIFICATION_ID,
            /* notification = */ createNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        VPNManager.updateVpnState(State.CONNECTING)
        initializeVpnTunnel()
        launchTun2Socks()
        startHysteriaTunnel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constant.CHANNEL_ID,
                Constant.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "H2Byte VPN Service"
                setShowBadge(false)
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification() = NotificationCompat.Builder(this, Constant.CHANNEL_ID)
        .setContentTitle("H2Byte VPN")
        .setContentText("VPN Service is running")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setOngoing(true)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()

    private fun startHysteriaTunnel() {
        val selectedServer = MmkvManager.getSelectedServer()
        val config = buildConfig(selectedServer)
        Thread {
            Cmd.startTunnel(config)
            VPNManager.updateVpnState(State.CONNECTED)
        }.start()
    }

    private fun launchTun2Socks() {
        Log.i(TAG, "Start run $TUN2SOCKS_LIB")
        val tun2SocksCommand = arrayListOf(
            File(applicationContext.applicationInfo.nativeLibraryDir, TUN2SOCKS_LIB).absolutePath,
            "--netif-ipaddr", PRIVATE_VLAN4_ROUTER,
            "--netif-netmask", "255.255.255.252",
            "--socks-server-addr", "$LOOPBACK_ADDRESS:$SOCKS_PORT",
            "--tunmtu", VPN_MTU.toString(),
            "--sock-path", SOCK_PATH,
            "--enable-udprelay",
            "--loglevel", "notice"
        )

        try {
            val processBuilder = ProcessBuilder(tun2SocksCommand)
            processBuilder.redirectErrorStream(true)
            tun2SocksProcess = processBuilder
                .directory(applicationContext.filesDir)
                .start()
            
            Thread {
                Log.i(TAG, "$TUN2SOCKS_LIB check")
                tun2SocksProcess.waitFor()
                Log.i(TAG, "$TUN2SOCKS_LIB exited")
                if (isRunning) {
                    Log.i(TAG, "$TUN2SOCKS_LIB restart")
                    launchTun2Socks()
                }
            }.start()
            
            Log.i(TAG, "$TUN2SOCKS_LIB process info: ${tun2SocksProcess}")
            sendFileDescriptor()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tun2socks process", e)
            VPNManager.updateVpnState(State.DISCONNECTED)
        }
    }

    private fun sendFileDescriptor() {
        vpnInterface?.fileDescriptor?.let { fileDescriptor ->
            val localSocketPath = File(applicationContext.filesDir, SOCK_PATH).absolutePath
            Log.i(TAG, "LocalSocket path: $localSocketPath")

            CoroutineScope(Dispatchers.IO).launch {
                var tries = 0
                while (true) {
                    try {
                        Thread.sleep(50L shl tries)
                        Log.i(TAG, "LocalSocket sendFd tries: $tries")
                        LocalSocket().use { localSocket ->
                            localSocket.connect(
                                LocalSocketAddress(
                                    localSocketPath,
                                    LocalSocketAddress.Namespace.FILESYSTEM
                                )
                            )
                            localSocket.setFileDescriptorsForSend(arrayOf(fileDescriptor))
                            localSocket.outputStream.write(42)
                        }
                        Log.i(TAG, "File descriptor sent successfully")
                        break
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send file descriptor, try: $tries", e)
                        if (tries > 5) {
                            VPNManager.updateVpnState(State.DISCONNECTED)
                            break
                        }
                        tries += 1
                    }
                }
            }
        } ?: Log.e(TAG, "VPN interface is null. Failed to send file descriptor.")
    }

    private fun initializeVpnTunnel(): Int? {
        return try {
            vpnInterface = Builder().setMtu(VPN_MTU)
                .addAddress("10.0.88.88", 16)
                .addDnsServer("1.0.0.1")
                .addDisallowedApplication(packageName)
                .addRoute("0.0.0.0", 0)
                .establish()
            Log.i(TAG, "VPN Tunnel Established")
            isRunning = true
            VPNManager.updateVpnState(State.CONNECTED)
            vpnInterface?.fd
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN tunnel", e)
            VPNManager.updateVpnState(State.DISCONNECTED)
            null
        }
    }

    private fun buildConfig(config: ServerConfig?): String {
        return """
            {
                "server": "${config?.address}",
                "auth": "${config?.authToken}",
                "tls": { "insecure": ${config?.allowInsecure} },
                "bandwidth": { "up": "${config?.uploadSpeedMbps} mbps", "down": "${config?.downloadSpeedMbps} mbps" },
                "fast_open": true,
                "lazy": true,
                "socks5": { "listen": "${LOOPBACK_ADDRESS}:${SOCKS_PORT}" }
            }
        """.trimIndent()
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")

        isRunning = false
        try {
            Cmd.stopTunnel()
            if (::tun2SocksProcess.isInitialized) {
                tun2SocksProcess.destroy()
                Log.i(TAG, "$TUN2SOCKS_LIB process destroyed")
            }
            vpnInterface?.close()
            vpnInterface = null
            VPNManager.updateVpnState(State.DISCONNECTED)
            Log.d(TAG, "Resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during service destruction", e)
            VPNManager.updateVpnState(State.DISCONNECTED)
        }

        stopForeground(true)
        Log.d(TAG, "Foreground stopped")

        stopSelf()
        Log.d(TAG, "Service stopped")
    }
}
