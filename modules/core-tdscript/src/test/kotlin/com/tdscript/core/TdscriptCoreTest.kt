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
        // Host has only NET_READ by default
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
    fun testHeadlessTuiStdout() {
        val host = StandardHostSPI()
        val code = """
            print("LINE_1_TUI_SYSTEM")
            print("LINE_2_TERMINAL_READY")
        """.trimIndent()

        runScript(code, host)
        assertEquals(2, host.stdoutLines.size)
        assertEquals("LINE_1_TUI_SYSTEM", host.stdoutLines[0])
        assertEquals("LINE_2_TERMINAL_READY", host.stdoutLines[1])
    }

    @Test
    fun testWhileLoopAndCounter() {
        val code = """
            var count = 0
            while (count < 5) {
                count = count + 1
            }
        """.trimIndent()

        val interpreter = runScript(code)
        assertEquals(5, (interpreter.globals["count"] as Number).toInt())
    }
}
