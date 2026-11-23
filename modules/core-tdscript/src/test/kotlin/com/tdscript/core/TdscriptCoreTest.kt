package com.tdscript.core

import org.junit.Assert.*
import org.junit.Test

class TdscriptCoreTest {

    private fun runScript(code: String, host: HostSPI = StandardHostSPI()): Interpreter {
        val lexer = Lexer(code)
        val tokens = lexer.tokenize()
        val parser = Parser(tokens)
        val program = parser.parse()
        val interpreter = Interpreter(host)
        interpreter.execute(program)
        return interpreter
    }

    @Test
    fun testArithmeticAndVariables() {
        val code = """
            let a = 10 + 5 * 2
            let b = a - 4
            var c = b / 2
        """.trimIndent()

        val interpreter = runScript(code)
        assertEquals(20, (interpreter.globals["a"] as Number).toInt())
        assertEquals(16, (interpreter.globals["b"] as Number).toInt())
        assertEquals(8, (interpreter.globals["c"] as Number).toInt())
    }

    @Test
    fun testShortCircuitAndConditions() {
        val code = """
            let flag = true
            var result = 0
            if (flag && 10 > 5) {
                result = 42
            } else {
                result = 99
            }
        """.trimIndent()

        val interpreter = runScript(code)
        assertEquals(42, (interpreter.globals["result"] as Number).toInt())
    }

    @Test
    fun testCapabilitiesSecurityCheck() {
        val host = StandardHostSPI(capabilities = Capabilities.NET_READ)
        val code = """
            let res = NET.httpGet("https://example.com/api")
            let plc = PLC.modbusWrite("192.168.1.50", 100, 1)
        """.trimIndent()

        val interpreter = runScript(code, host)
        assertNotNull(interpreter.globals["res"])
        assertEquals(false, interpreter.globals["plc"])
        assertTrue(host.diagnosticLines.any { it.contains("PERMISSION_DENIED [PLC_WRITE]") })
    }

    @Test
    fun testCapabilityScopeIsolation() {
        // Хост имеет право I2C_READ и I2C_WRITE, но только для датчика 0x76 (Bosch BME280)
        val scope = CapabilityScope(
            allowedI2cAddresses = setOf(0x76),
            allowedModbusRegisterRanges = listOf(100..200)
        )
        val host = StandardHostSPI(
            capabilities = Capabilities.I2C_READ or Capabilities.I2C_WRITE or Capabilities.PLC_READ,
            capabilityScope = scope
        )

        val code = """
            let okRead = I2C.read(118, 0, 2)
            let badRead = I2C.read(104, 0, 2)
            let regOk = PLC.modbusRead("127.0.0.1", 150)
            let regBad = PLC.modbusRead("127.0.0.1", 350)
        """.trimIndent()

        val interpreter = runScript(code, host)
        assertNotNull(interpreter.globals["okRead"])
        assertNull(interpreter.globals["badRead"])
        assertEquals(1234, interpreter.globals["regOk"])
        assertEquals(-1, interpreter.globals["regBad"])

        assertTrue(host.diagnosticLines.any { it.contains("SCOPE_VIOLATION: Address 0x68 not in I2C scope") })
        assertTrue(host.diagnosticLines.any { it.contains("SCOPE_VIOLATION: Modbus register 350 not in scope") })
    }

    @Test
    fun testLocalizedSyntaxAndStringLiteralsIntegrity() {
        // Проверка: ключевые слова переведены, но слова ВНУТРИ строковых литералов и комментов НЕ затронуты!
        val code = """
            language: ru;
            # Комментарий: если датчик оффлайн, пусть ждет
            пусть статус = "если датчик оффлайн, пусть ждет"
            перем флаг = истина
            если (флаг) {
                флаг = ложь
            }
        """.trimIndent()

        val interpreter = runScript(code)
        assertEquals("если датчик оффлайн, пусть ждет", interpreter.globals["статус"])
        assertEquals(false, interpreter.globals["флаг"])
    }

    @Test
    fun testLStringCreationAndFormatting() {
        val code = """
            let msg = L"MSG_TEMP:Sensor BME280 temp is {temp}C"
            print(msg)
        """.trimIndent()

        val host = StandardHostSPI()
        val interpreter = runScript(code, host)
        val lstr = interpreter.globals["msg"] as LString
        assertEquals("MSG_TEMP", lstr.key)
        assertEquals("Sensor BME280 temp is {temp}C", lstr.defaultText)
        assertEquals("Sensor BME280 temp is {temp}C", host.stdoutLines[0])
    }

    @Test
    fun testFailSafeWatchdogEmergencyStop() {
        val host = StandardHostSPI()
        val code = """
            let fed = WATCHDOG.feed()
            let stopped = WATCHDOG.emergencyStop()
        """.trimIndent()

        val interpreter = runScript(code, host)
        assertEquals(true, interpreter.globals["fed"])
        assertEquals(true, interpreter.globals["stopped"])
        assertTrue(host.diagnosticLines.contains("EMERGENCY_STOP_TRIGGERED"))
    }
}
