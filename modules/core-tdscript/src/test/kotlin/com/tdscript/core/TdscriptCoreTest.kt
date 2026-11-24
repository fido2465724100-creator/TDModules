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

    @Test
    fun testLStringConcatenationProhibited() {
        val code1 = """
            let msg = L"MSG_TEMP:Temp is {temp}"
            let bad = msg + " extra"
        """.trimIndent()
        try {
            runScript(code1)
            fail("Expected RuntimeException for LString concatenation")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("TDS_E105_LSTRING_CONCAT_PROHIBITED") == true)
        }

        val code2 = """
            let msg = L"MSG_TEMP:Temp is {temp}"
            let bad = "Prefix: " + msg
        """.trimIndent()
        try {
            runScript(code2)
            fail("Expected RuntimeException for LString concatenation")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("TDS_E105_LSTRING_CONCAT_PROHIBITED") == true)
        }
    }

    @Test
    fun testStepLimitExceededEmergencyStop() {
        val host = StandardHostSPI()
        val code = """
            var i = 0
            while (true) {
                i = i + 1
            }
        """.trimIndent()
        val lexer = Lexer(code)
        val tokens = lexer.tokenize()
        val parser = Parser(tokens)
        val program = parser.parse()
        val interpreter = Interpreter(host, maxSteps = 50L)
        try {
            interpreter.execute(program)
            fail("Expected RuntimeException for step limit exceeded")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("TDS_E106_STEP_LIMIT_EXCEEDED") == true)
        }
        assertTrue(host.diagnosticLines.contains("EMERGENCY_STOP_TRIGGERED"))
        assertTrue(interpreter.stepCount > 50L)
    }

    @Test
    fun testHexadecimalLiterals() {
        val code = """
            let bmeAddr = 0x76;
            let chipIdReg = 0xD0;
            let ctrlMeas = 0xF4;
            let longHex = 0x1000L;
        """.trimIndent()
        val lexer = Lexer(code)
        val tokens = lexer.tokenize()

        val intTokens = tokens.filter { it.type == TokenType.INT }
        val longTokens = tokens.filter { it.type == TokenType.LONG }

        assertEquals(3, intTokens.size)
        assertEquals(0x76, intTokens[0].value)
        assertEquals(118, intTokens[0].value)
        assertEquals(0xD0, intTokens[1].value)
        assertEquals(208, intTokens[1].value)
        assertEquals(0xF4, intTokens[2].value)
        assertEquals(244, intTokens[2].value)

        assertEquals(1, longTokens.size)
        assertEquals(4096L, longTokens[0].value)

        // Test in Interpreter
        val interpreter = runScript(code)
        assertEquals(118, (interpreter.globals["bmeAddr"] as Number).toInt())
        assertEquals(208, (interpreter.globals["chipIdReg"] as Number).toInt())
        assertEquals(244, (interpreter.globals["ctrlMeas"] as Number).toInt())
    }

    @Test
    fun testCompactRepairErrorFormat() {
        val diag = DiagnosticError(
            errorCode = "TDS_E104_SCOPE_VIOLATION",
            line = 14,
            column = 5,
            message = "Address 0x68 not allowed in I2C scope",
            expected = "0x76 | 0x77",
            actual = "0x68"
        )
        val repair = diag.toRepairFormat()
        assertEquals("E104@14:5 exp=0x76|0x77 got=0x68", repair)

        val json = diag.toJson()
        assertTrue(json.contains("\"error_code\": \"TDS_E104_SCOPE_VIOLATION\""))
        assertTrue(json.contains("\"line\": 14"))
        assertTrue(json.contains("\"column\": 5"))
    }

    @Test
    fun testUnknownMethodThrowsTdsE106() {
        val code1 = """
            let safeI2CAddr = 118;
            let s = safeI2CAddr.toString(16);
        """.trimIndent()
        try {
            runScript(code1)
            fail("Expected RuntimeException with TDS_E106 for safeI2CAddr.toString(16)")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("TDS_E106") == true)
            assertTrue(e.message?.contains("unknown_method=toString on=Int") == true)
        }

        val code2 = """
            I2C.unsupportedMethod(1, 2);
        """.trimIndent()
        try {
            runScript(code2)
            fail("Expected RuntimeException with TDS_E106 for I2C.unsupportedMethod")
        } catch (e: RuntimeException) {
            assertTrue(e.message?.contains("TDS_E106") == true)
            assertTrue(e.message?.contains("unknown_method=unsupportedMethod on=I2C") == true)
        }
    }

    @Test
    fun testByteArrayDeterministicFormattingAndIndexing() {
        val host = StandardHostSPI(Capabilities.HARDWARE or Capabilities.I2C_READ)
        val code = """
            let bytes = I2C.read(118, 208, 1);
            print(bytes, " ", bytes[0]);
            print(" size=", bytes.size());
        """.trimIndent()

        runScript(code, host)
        assertEquals(2, host.stdoutLines.size)
        assertEquals("[0x00] 0", host.stdoutLines[0])
        assertEquals(" size=1", host.stdoutLines[1])
    }

    @Test
    fun testLStringInterpolationFromScope() {
        val host = StandardHostSPI()
        val code = """
            let temp = 24.5;
            let alert = L"ALERT_TEMP:Temperature is {temp}";
            print(alert);
        """.trimIndent()

        val interpreter = runScript(code, host)
        assertEquals(1, host.stdoutLines.size)
        assertEquals("Temperature is 24.5", host.stdoutLines[0])

        val lstr = interpreter.globals["alert"] as LString
        assertEquals("ALERT_TEMP", lstr.key)
        assertEquals("24.5", lstr.params["temp"])
    }
}
