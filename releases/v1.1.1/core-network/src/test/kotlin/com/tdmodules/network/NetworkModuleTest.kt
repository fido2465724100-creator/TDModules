package com.tdmodules.network

import org.junit.Assert.*
import org.junit.Test

class NetworkModuleTest {

    @Test
    fun testLocalIpAddressResolution() {
        val ip = NetworkUtils.getLocalIpAddress()
        assertNotNull(ip)
        assertTrue(ip.isNotEmpty())
    }

    @Test
    fun testSubnetCheck() {
        assertTrue(NetworkUtils.isInSameSubnet("192.168.1.50", "192.168.1.100"))
        assertFalse(NetworkUtils.isInSameSubnet("192.168.1.50", "192.168.2.50"))
        assertFalse(NetworkUtils.isInSameSubnet("10.0.0.1", "192.168.0.1"))
    }
}
