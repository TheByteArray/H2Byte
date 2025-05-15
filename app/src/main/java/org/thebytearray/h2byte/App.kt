package org.thebytearray.h2byte

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import org.thebytearray.h2byte.util.Constant.CHANNEL_ID
import org.thebytearray.h2byte.util.Constant.CHANNEL_NAME
import org.thebytearray.h2byte.util.MmkvManager

class App : Application() {
    companion object {
        lateinit var app: App
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        MmkvManager.initialize(this)
    }
}