package com.tdscript.core

/**
 * Область действия (Scope) гранулярных аппаратных прав доступа TDscript Core 1.1
 * Изолирует транзакции драйвера или скрипта от несанкционированного доступа к чужим шинам и адресам.
 */
data class CapabilityScope(
    val allowedI2cAddresses: Set<Int> = emptySet(),
    val allowedUsbVidPid: Set<Pair<Int, Int>> = emptySet(),
    val allowedBleServiceUuids: Set<String> = emptySet(),
    val allowedModbusRegisterRanges: List<IntRange> = emptyList(),
    val allowedSerialPorts: Set<String> = emptySet(),
    val allowedNetworkHosts: Set<String> = emptySet()
) {
    fun isI2cAddressAllowed(address: Int): Boolean {
        if (allowedI2cAddresses.isEmpty()) return true // Если скоуп не ограничен, разрешены все
        return allowedI2cAddresses.contains(address)
    }

    fun isUsbVidPidAllowed(vid: Int, pid: Int): Boolean {
        if (allowedUsbVidPid.isEmpty()) return true
        return allowedUsbVidPid.contains(Pair(vid, pid))
    }

    fun isBleServiceAllowed(uuid: String): Boolean {
        if (allowedBleServiceUuids.isEmpty()) return true
        return allowedBleServiceUuids.any { it.equals(uuid, ignoreCase = true) }
    }

    fun isModbusRegisterAllowed(register: Int): Boolean {
        if (allowedModbusRegisterRanges.isEmpty()) return true
        return allowedModbusRegisterRanges.any { register in it }
    }

    fun isSerialPortAllowed(port: String): Boolean {
        if (allowedSerialPorts.isEmpty()) return true
        return allowedSerialPorts.contains(port)
    }

    companion object {
        val UNRESTRICTED = CapabilityScope()
    }
}
