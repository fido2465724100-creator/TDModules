package com.tdmodules.network

import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Утилиты для работы с сетевыми интерфейсами и локальными сетями.
 * Определяет приоритетный IP-адрес устройства для Direct P2P соединения
 * (Wi-Fi точка доступа, клиент Wi-Fi, Ethernet, USB-модем/Tethering).
 */
object NetworkUtils {

    /**
     * Возвращает IPv4-адрес устройства в активной локальной сети.
     * Приоритеты интерфейсов:
     * 1. Точка доступа (ap0, softap, wlan_ap)
     * 2. Wi-Fi клиент (wlan0, wlp)
     * 3. Ethernet / USB Tethering (eth0, rndis0, enp)
     * 4. Любой другой активный non-loopback IPv4 адрес.
     */
    fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            val ipList = mutableListOf<Pair<String, String>>()

            for (networkInterface in interfaces) {
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val name = networkInterface.name.lowercase()
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val hostAddress = address.hostAddress ?: continue
                        ipList.add(name to hostAddress)
                    }
                }
            }

            // 1. Поиск точки доступа (ap, softap)
            ipList.find { it.first.contains("ap") }?.let { return it.second }

            // 2. Поиск Wi-Fi (wlan, wlp)
            ipList.find { it.first.contains("wlan") || it.first.contains("wlp") }?.let { return it.second }

            // 3. Поиск Ethernet / USB Tethering (eth, rndis, enp)
            ipList.find { it.first.contains("eth") || it.first.contains("rndis") || it.first.contains("enp") }?.let { return it.second }

            // 4. Любой другой активный non-loopback IPv4
            if (ipList.isNotEmpty()) {
                return ipList.first().second
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    /**
     * Проверяет, принадлежат ли два IPv4 адреса одной /24 подсети (например 192.168.1.X)
     */
    fun isInSameSubnet(ip1: String, ip2: String): Boolean {
        val parts1 = ip1.split(".")
        val parts2 = ip2.split(".")
        if (parts1.size != 4 || parts2.size != 4) return false
        return parts1[0] == parts2[0] && parts1[1] == parts2[1] && parts1[2] == parts2[2]
    }
}
