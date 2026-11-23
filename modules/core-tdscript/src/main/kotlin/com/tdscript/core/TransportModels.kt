package com.tdscript.core

enum class TransportType {
    I2C,
    SPI,
    SERIAL_USB,
    BLE,
    NETWORK_TCP,
    CAN
}

data class TransportAdapterInfo(
    val id: String,
    val type: TransportType,
    val available: Boolean,
    val details: String = ""
)

data class DriverManifest(
    val name: String,
    val version: String,
    val minCoreVersion: String = "1.1.0",
    val minHostApi: String = "1.1.0",
    val title: String,
    val vendor: String,
    val targetChip: String,
    val transportType: TransportType,
    val requiredCapabilities: List<String>,
    val capabilityScope: CapabilityScope,
    val entrypoint: String,
    val checksumSha256: String,
    val signatureHex: String? = null,
    val signerPublicKey: String? = null
)
