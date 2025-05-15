package org.thebytearray.h2byte.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tencent.mmkv.MMKV
import org.thebytearray.h2byte.dto.ServerConfig

object MmkvManager {
    private const val MMKV_ID = "h2byte_mmkv"
    private const val KEY_SERVERS = "servers"
    private const val KEY_SELECTED_SERVER_INDEX = "selected_server_index"

    private lateinit var mmkv: MMKV
    private val gson = Gson()

    fun initialize(context: Context) {
        MMKV.initialize(context)
        mmkv = MMKV.mmkvWithID(MMKV_ID, MMKV.MULTI_PROCESS_MODE)
    }

    fun saveServers(servers: List<ServerConfig>) {
        val json = gson.toJson(servers)
        mmkv.encode(KEY_SERVERS, json)
    }

    fun getServers(): List<ServerConfig> {
        val json = mmkv.getString(KEY_SERVERS, "[]")
        val type = object : TypeToken<List<ServerConfig>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveSelectedServerIndex(index: Int) {
        mmkv.encode(KEY_SELECTED_SERVER_INDEX, index)
    }

    fun getSelectedServer(): ServerConfig? {
        val selectedIndex = getSelectedServerIndex()
        val servers = getServers()
        return if (selectedIndex >= 0 && selectedIndex < servers.size) {
            servers[selectedIndex]
        } else {
            null
        }
    }

    fun getSelectedServerIndex(): Int {
        return mmkv.getInt(KEY_SELECTED_SERVER_INDEX, -1)
    }

    fun clearSelectedServer() {
        mmkv.removeValueForKey(KEY_SELECTED_SERVER_INDEX)
    }
} 