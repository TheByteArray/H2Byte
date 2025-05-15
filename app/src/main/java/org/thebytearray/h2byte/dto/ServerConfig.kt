package org.thebytearray.h2byte.dto

data class ServerConfig(
    val name: String,
    val address: String,
    val authToken: String,
    val uploadSpeedMbps: Int,
    val downloadSpeedMbps: Int,
    val allowInsecure: Boolean
) 