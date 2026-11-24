package com.tdscript.core

object Capabilities {
    // 1.0.1 Core Capabilities
    const val NET_READ        = 0x0001
    const val NET_SCAN        = 0x0002
    const val PLC_READ        = 0x0004
    const val PLC_WRITE       = 0x0008
    const val BT_ACCESS       = 0x0010
    const val HARDWARE        = 0x0020
    const val UI_CORE         = 0x0040
    const val BACKGROUND      = 0x0080

    // 1.1 Hardware, Bus & Robotics Capabilities
    const val I2C_READ        = 0x0100
    const val I2C_WRITE       = 0x0200
    const val SPI_TRANSFER    = 0x0400
    const val SERIAL_RAW      = 0x0800
    const val CAN_BUS         = 0x1000
    const val ROBOT_MOTION    = 0x2000
    const val ROBOT_OVERRIDE  = 0x4000
    const val DRIVER_INSTALL  = 0x8000
}

interface HostSPI {
    val stdoutLines: MutableList<String>
    val diagnosticLines: MutableList<String>
    var capabilities: Int
    var capabilityScope: CapabilityScope

    fun checkCapability(capBit: Int, capName: String): Boolean {
        if ((capabilities and capBit) != 0) return true
        diagnosticLines.add("SecurityException: PERMISSION_DENIED [$capName]")
        return false
    }

    fun print(msg: String) {
        stdoutLines.add(msg)
    }

    // Dynamic Hardware Probe
    fun probe(): List<TransportAdapterInfo> = emptyList()

    // Low-level Bus Transactions with CapabilityScope Check
    fun i2cRead(address: Int, register: Int, length: Int): ByteArray? {
        if (!checkCapability(Capabilities.I2C_READ, "I2C_READ")) return null
        if (!capabilityScope.isI2cAddressAllowed(address)) {
            diagnosticLines.add("SecurityException: SCOPE_VIOLATION: Address 0x${Integer.toHexString(address).uppercase()} not in I2C scope")
            return null
        }
        return ByteArray(length) { 0 }
    }

    fun i2cWrite(address: Int, register: Int, data: ByteArray): Boolean {
        if (!checkCapability(Capabilities.I2C_WRITE, "I2C_WRITE")) return false
        if (!capabilityScope.isI2cAddressAllowed(address)) {
            diagnosticLines.add("SecurityException: SCOPE_VIOLATION: Address 0x${Integer.toHexString(address).uppercase()} not in I2C scope")
            return false
        }
        return true
    }

    fun serialWrite(port: String, data: ByteArray): Boolean {
        if (!checkCapability(Capabilities.SERIAL_RAW, "SERIAL_RAW")) return false
        if (!capabilityScope.isSerialPortAllowed(port)) {
            diagnosticLines.add("SecurityException: SCOPE_VIOLATION: Serial port $port not in scope")
            return false
        }
        return true
    }

    fun serialRead(port: String, maxBytes: Int): ByteArray? {
        if (!checkCapability(Capabilities.SERIAL_RAW, "SERIAL_RAW")) return null
        if (!capabilityScope.isSerialPortAllowed(port)) {
            diagnosticLines.add("SecurityException: SCOPE_VIOLATION: Serial port $port not in scope")
            return null
        }
        return ByteArray(0)
    }

    // Fail-Safe Watchdog
    fun watchdogFeed(): Boolean = true
    fun emergencyStop(): Unit {
        diagnosticLines.add("EMERGENCY_STOP_TRIGGERED")
    }

    // Default 1.0.1 Stubs for backwards compatibility
    fun netHttpGet(url: String): String? = if (!checkCapability(Capabilities.NET_READ, "NET_READ")) null else "HTTP_OK: $url"
    fun netPing(host: String): Float = if (!checkCapability(Capabilities.NET_READ, "NET_READ")) -1.0f else 12.5f
    fun netScanPort(host: String, port: Int): Boolean = if (!checkCapability(Capabilities.NET_SCAN, "NET_SCAN")) false else true
    fun netWol(mac: String): Boolean = if (!checkCapability(Capabilities.NET_SCAN, "NET_SCAN")) false else true

    fun plcModbusRead(host: String, reg: Int): Int {
        if (!checkCapability(Capabilities.PLC_READ, "PLC_READ")) return -1
        if (!capabilityScope.isModbusRegisterAllowed(reg)) {
            diagnosticLines.add("SecurityException: SCOPE_VIOLATION: Modbus register $reg not in scope")
            return -1
        }
        return 1234
    }

    fun plcModbusWrite(host: String, reg: Int, value: Int): Boolean {
        if (!checkCapability(Capabilities.PLC_WRITE, "PLC_WRITE")) return false
        if (!capabilityScope.isModbusRegisterAllowed(reg)) {
            diagnosticLines.add("SecurityException: SCOPE_VIOLATION: Modbus register $reg not in scope")
            return false
        }
        return true
    }

    fun phoneAlarm(volume: Int): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true
    fun phoneVibrate(ms: Long): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true
    fun phoneTorch(state: Boolean): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true
    fun phoneBeep(ms: Int): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true

    fun uiToast(msg: String): Boolean = if (!checkCapability(Capabilities.UI_CORE, "UI_CORE")) false else true
    fun uiLog(msg: String): Boolean = if (!checkCapability(Capabilities.UI_CORE, "UI_CORE")) false else true

    fun btConnect(target: String): Boolean = if (!checkCapability(Capabilities.BT_ACCESS, "BT_ACCESS")) false else true
    fun btSend(text: String): Boolean = if (!checkCapability(Capabilities.BT_ACCESS, "BT_ACCESS")) false else true
    fun btReadLine(): String? = if (!checkCapability(Capabilities.BT_ACCESS, "BT_ACCESS")) null else "BT_OK"

    fun aiAsk(prompt: String): String? = if (!checkCapability(Capabilities.NET_READ, "NET_READ")) null else "AI_OK: $prompt"
}

open class StandardHostSPI(
    override var capabilities: Int = Capabilities.NET_READ,
    override var capabilityScope: CapabilityScope = CapabilityScope.UNRESTRICTED
) : HostSPI {
    override val stdoutLines = mutableListOf<String>()
    override val diagnosticLines = mutableListOf<String>()
}

class Interpreter(
    val host: HostSPI = StandardHostSPI(),
    val maxSteps: Long = 10_000_000L
) {
    // Script Global Scope: single symbol table
    val globals = mutableMapOf<String, Any?>()
    var stepCount: Long = 0L
        private set

    fun execute(program: ProgramNode) {
        stepCount = 0L
        for (stmt in program.statements) {
            execStmt(stmt)
        }
    }

    private fun checkStepLimit() {
        stepCount++
        if (stepCount > maxSteps) {
            host.emergencyStop()
            throw RuntimeException("TDS_E106_STEP_LIMIT_EXCEEDED: Maximum execution step limit ($maxSteps) exceeded. Host emergencyStop triggered.")
        }
    }

    fun execStmt(stmt: ASTNode) {
        checkStepLimit()
        when (stmt) {
            is VarDeclNode -> {
                val value = evalExpr(stmt.expr)
                globals[stmt.name] = value
            }
            is ExprStmtNode -> evalExpr(stmt.expr)
            is IfStmtNode -> {
                val cond = isTruthy(evalExpr(stmt.condition))
                if (cond) {
                    execStmt(stmt.thenBranch)
                } else if (stmt.elseBranch != null) {
                    execStmt(stmt.elseBranch)
                }
            }
            is WhileStmtNode -> {
                while (isTruthy(evalExpr(stmt.condition))) {
                    checkStepLimit()
                    execStmt(stmt.body)
                }
            }
            is BlockNode -> {
                for (s in stmt.statements) {
                    execStmt(s)
                }
            }
            is AssignmentNode -> evalAssignment(stmt)
            else -> {}
        }
    }

    fun evalExpr(expr: ASTNode): Any? {
        return when (expr) {
            is AssignmentNode -> evalAssignment(expr)
            is LiteralNode -> {
                if (expr.value is LString) {
                    val lstr = expr.value
                    val regex = Regex("""\{([a-zA-Z_][a-zA-Z0-9_]*)\}""")
                    val params = lstr.params.toMutableMap()
                    regex.findAll(lstr.defaultText).forEach { match ->
                        val varName = match.groupValues[1]
                        if (globals.containsKey(varName)) {
                            params[varName] = formatValue(globals[varName])
                        }
                    }
                    LString(lstr.key, lstr.defaultText, params)
                } else {
                    expr.value
                }
            }
            is IdentifierNode -> globals[expr.name]
            is UnaryExprNode -> {
                val operand = evalExpr(expr.operand)
                when (expr.op) {
                    "-" -> {
                        when (operand) {
                            is Int -> {
                                if (operand == Int.MIN_VALUE) operand
                                else -operand
                            }
                            is Long -> {
                                if (operand == Long.MIN_VALUE) operand
                                else -operand
                            }
                            is Float -> -operand
                            is Double -> -operand
                            else -> null
                        }
                    }
                    "!" -> !isTruthy(operand)
                    else -> null
                }
            }
            is PostfixUpdateNode -> {
                val curVal = globals[expr.targetIdent]
                if (curVal is Number) {
                    val newVal = if (expr.op == "++") {
                        if (curVal is Long) curVal + 1L else curVal.toInt() + 1
                    } else {
                        if (curVal is Long) curVal - 1L else curVal.toInt() - 1
                    }
                    globals[expr.targetIdent] = newVal
                    curVal
                } else null
            }
            is BinaryExprNode -> {
                if (expr.op == "&&") {
                    val left = evalExpr(expr.left)
                    return if (!isTruthy(left)) false else isTruthy(evalExpr(expr.right))
                }
                if (expr.op == "||") {
                    val left = evalExpr(expr.left)
                    return if (isTruthy(left)) true else isTruthy(evalExpr(expr.right))
                }

                val left = evalExpr(expr.left)
                val right = evalExpr(expr.right)

                when (expr.op) {
                    "+" -> {
                        if (left is LString || right is LString) {
                            throw RuntimeException("TDS_E105_LSTRING_CONCAT_PROHIBITED: Direct concatenation of LString is forbidden to preserve 109-language grammar integrity.")
                        }
                        if (left is String || right is String) {
                            formatValue(left) + formatValue(right)
                        } else if (left is Number && right is Number) {
                            if (left is Double || right is Double || left is Float || right is Float) {
                                left.toDouble() + right.toDouble()
                            } else {
                                val res = left.toLong() + right.toLong()
                                if (left is Long || right is Long || res > Int.MAX_VALUE || res < Int.MIN_VALUE) res
                                else res.toInt()
                            }
                        } else null
                    }
                    "-" -> {
                        if (left is Number && right is Number) {
                            if (left is Double || right is Double || left is Float || right is Float) {
                                left.toDouble() - right.toDouble()
                            } else {
                                val res = left.toLong() - right.toLong()
                                if (left is Long || right is Long || res > Int.MAX_VALUE || res < Int.MIN_VALUE) res
                                else res.toInt()
                            }
                        } else null
                    }
                    "*" -> {
                        if (left is Number && right is Number) {
                            if (left is Double || right is Double || left is Float || right is Float) {
                                left.toDouble() * right.toDouble()
                            } else {
                                val res = left.toLong() * right.toLong()
                                if (left is Long || right is Long || res > Int.MAX_VALUE || res < Int.MIN_VALUE) res
                                else res.toInt()
                            }
                        } else null
                    }
                    "/" -> {
                        if (left is Number && right is Number) {
                            if (right.toDouble() == 0.0) {
                                if (left is Double || left is Float || right is Double || right is Float) Double.POSITIVE_INFINITY
                                else null // Integer division by zero evaluates to null
                            } else {
                                if (left is Double || right is Double || left is Float || right is Float) {
                                    left.toDouble() / right.toDouble()
                                } else {
                                    left.toLong() / right.toLong()
                                }
                            }
                        } else null
                    }
                    "%" -> {
                        if (left is Number && right is Number) {
                            if (right.toLong() == 0L) null
                            else left.toLong() % right.toLong()
                        } else null
                    }
                    "==" -> {
                        if (left == null && right == null) true
                        else if (left == null || right == null) false
                        else if (left::class != right::class) {
                            // Strict typing: different types are never strictly equal
                            if (left is Number && right is Number && left !is Boolean && right !is Boolean) {
                                left.toDouble() == right.toDouble()
                            } else false
                        } else left == right
                    }
                    "!=" -> {
                        if (left == null && right == null) false
                        else if (left == null || right == null) true
                        else if (left::class != right::class) {
                            if (left is Number && right is Number && left !is Boolean && right !is Boolean) {
                                left.toDouble() != right.toDouble()
                            } else true
                        } else left != right
                    }
                    "<" -> compareNumbers(left, right) { a, b -> a < b }
                    "<=" -> compareNumbers(left, right) { a, b -> a <= b }
                    ">" -> compareNumbers(left, right) { a, b -> a > b }
                    ">=" -> compareNumbers(left, right) { a, b -> a >= b }
                    else -> null
                }
            }
            is MemberAccessNode -> {
                val obj = evalExpr(expr.obj)
                if (obj is Map<*, *>) obj[expr.propertyName] else null
            }
            is IndexAccessNode -> {
                val obj = evalExpr(expr.obj)
                val idx = evalExpr(expr.indexExpr)
                when {
                    obj is List<*> && idx is Number -> {
                        val i = idx.toInt()
                        if (i in 0 until obj.size) obj[i] else null
                    }
                    obj is ByteArray && idx is Number -> {
                        val i = idx.toInt()
                        if (i in 0 until obj.size) (obj[i].toInt() and 0xFF) else null
                    }
                    obj is Map<*, *> -> obj[idx]
                    else -> null
                }
            }
            is CallExprNode -> evalCall(expr)
            else -> null
        }
    }

    private inline fun compareNumbers(left: Any?, right: Any?, crossinline op: (Double, Double) -> Boolean): Boolean {
        if (left !is Number || right !is Number) return false
        val isLeftIntegral = left is Long || left is Int || left is Short || left is Byte
        val isRightIntegral = right is Long || right is Int || right is Short || right is Byte
        return if (isLeftIntegral && isRightIntegral) {
            val l = left.toLong()
            val r = right.toLong()
            when {
                op(0.0, 1.0) && !op(1.0, 0.0) && !op(0.0, 0.0) -> l < r   // <
                op(0.0, 1.0) && !op(1.0, 0.0) && op(0.0, 0.0) -> l <= r  // <=
                !op(0.0, 1.0) && op(1.0, 0.0) && !op(0.0, 0.0) -> l > r   // >
                !op(0.0, 1.0) && op(1.0, 0.0) && op(0.0, 0.0) -> l >= r  // >=
                else -> false
            }
        } else {
            op(left.toDouble(), right.toDouble())
        }
    }

    private fun evalCall(call: CallExprNode): Any? {
        val args = call.arguments.map { evalExpr(it) }

        if (call.callee is IdentifierNode && call.callee.name == "print") {
            host.print(args.joinToString("") { formatValue(it) })
            return null
        }

        if (call.callee is MemberAccessNode) {
            val target = call.callee.obj
            val method = call.callee.propertyName
            val line = if (call.callee.line > 0) call.callee.line else (if (call.line > 0) call.line else 1)
            val col = if (call.callee.col > 0) call.callee.col else (if (call.col > 0) call.col else 1)

            if (target is IdentifierNode) {
                when (target.name) {
                    "JSON" -> {
                        if (method == "parse") {
                            if (args.isEmpty()) throw RuntimeException("TDS_E106: E106@$line:$col invalid_signature for method=parse on=JSON: expected string argument")
                            return parseSimpleJson(args[0]?.toString() ?: "")
                        }
                        throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=JSON")
                    }
                    "NET" -> when (method) {
                        "httpGet" -> return host.netHttpGet(args.getOrNull(0)?.toString() ?: "")
                        "ping" -> return host.netPing(args.getOrNull(0)?.toString() ?: "")
                        "scanPort" -> return host.netScanPort(args.getOrNull(0)?.toString() ?: "", (args.getOrNull(1) as? Number)?.toInt() ?: 80)
                        "wol" -> return host.netWol(args.getOrNull(0)?.toString() ?: "")
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=NET")
                    }
                    "PLC" -> when (method) {
                        "modbusRead" -> return host.plcModbusRead(args.getOrNull(0)?.toString() ?: "", (args.getOrNull(1) as? Number)?.toInt() ?: 0)
                        "modbusWrite" -> return host.plcModbusWrite(args.getOrNull(0)?.toString() ?: "", (args.getOrNull(1) as? Number)?.toInt() ?: 0, (args.getOrNull(2) as? Number)?.toInt() ?: 0)
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=PLC")
                    }
                    "PHONE" -> when (method) {
                        "alarm" -> return host.phoneAlarm((args.getOrNull(0) as? Number)?.toInt() ?: 100)
                        "vibrate" -> return host.phoneVibrate((args.getOrNull(0) as? Number)?.toLong() ?: 100L)
                        "torch" -> return host.phoneTorch(args.getOrNull(0) == true)
                        "beep" -> return host.phoneBeep((args.getOrNull(0) as? Number)?.toInt() ?: 150)
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=PHONE")
                    }
                    "UI" -> when (method) {
                        "toast" -> return host.uiToast(args.getOrNull(0)?.toString() ?: "")
                        "log" -> return host.uiLog(args.getOrNull(0)?.toString() ?: "")
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=UI")
                    }
                    "BT" -> when (method) {
                        "connect" -> return host.btConnect(args.getOrNull(0)?.toString() ?: "")
                        "send" -> return host.btSend(args.getOrNull(0)?.toString() ?: "")
                        "readLine" -> return host.btReadLine()
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=BT")
                    }
                    "AI" -> when (method) {
                        "ask" -> return host.aiAsk(args.getOrNull(0)?.toString() ?: "")
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=AI")
                    }
                    "I2C" -> when (method) {
                        "read" -> {
                            val addr = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                            val reg = (args.getOrNull(1) as? Number)?.toInt() ?: 0
                            val len = (args.getOrNull(2) as? Number)?.toInt() ?: 1
                            return host.i2cRead(addr, reg, len)
                        }
                        "write" -> {
                            val addr = (args.getOrNull(0) as? Number)?.toInt() ?: 0
                            val reg = (args.getOrNull(1) as? Number)?.toInt() ?: 0
                            val data = args.getOrNull(2) as? ByteArray ?: ByteArray(0)
                            return host.i2cWrite(addr, reg, data)
                        }
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=I2C")
                    }
                    "SERIAL" -> when (method) {
                        "write" -> {
                            val port = args.getOrNull(0)?.toString() ?: ""
                            val data = args.getOrNull(1) as? ByteArray ?: ByteArray(0)
                            return host.serialWrite(port, data)
                        }
                        "read" -> {
                            val port = args.getOrNull(0)?.toString() ?: ""
                            val max = (args.getOrNull(1) as? Number)?.toInt() ?: 64
                            return host.serialRead(port, max)
                        }
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=SERIAL")
                    }
                    "WATCHDOG" -> when (method) {
                        "feed" -> return host.watchdogFeed()
                        "emergencyStop" -> { host.emergencyStop(); return true }
                        else -> throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=WATCHDOG")
                    }
                }
            }

            // Instance method dispatch on object/primitive
            val targetObj = evalExpr(target)
            val typeName = if (targetObj != null) targetObj::class.simpleName ?: "Object" else (if (target is IdentifierNode) target.name else "null")
            when (targetObj) {
                is LString -> {
                    if (method == "format") {
                        val lang = args.getOrNull(0)?.toString() ?: "en"
                        return targetObj.format(lang)
                    }
                }
                is List<*> -> {
                    if (method == "size" || method == "length") return targetObj.size
                }
                is ByteArray -> {
                    if (method == "size" || method == "length") return targetObj.size
                }
                is Map<*, *> -> {
                    if (method == "size" || method == "length") return targetObj.size
                    if (method == "containsKey" && args.isNotEmpty()) return targetObj.containsKey(args[0])
                }
                is String -> {
                    if (method == "length") return targetObj.length
                }
            }
            throw RuntimeException("TDS_E106: E106@$line:$col unknown_method=$method on=$typeName")
        }

        if (call.callee is IdentifierNode) {
            val line = if (call.line > 0) call.line else 1
            val col = if (call.col > 0) call.col else 1
            throw RuntimeException("TDS_E106: E106@$line:$col unknown_function=${call.callee.name}")
        }

        val line = if (call.line > 0) call.line else 1
        val col = if (call.col > 0) call.col else 1
        throw RuntimeException("TDS_E106: E106@$line:$col invalid_callable")
    }

    private fun evalAssignment(assign: AssignmentNode): Any? {
        val value = evalExpr(assign.expr)
        when (val target = assign.target) {
            is IdentifierNode -> globals[target.name] = value
            is MemberAccessNode -> {
                val obj = evalExpr(target.obj)
                if (obj is MutableMap<*, *>) {
                    (obj as MutableMap<String, Any?>)[target.propertyName] = value
                }
            }
            is IndexAccessNode -> {
                val obj = evalExpr(target.obj)
                val idx = evalExpr(target.indexExpr)
                if (obj is MutableList<*> && idx is Number) {
                    val list = obj as MutableList<Any?>
                    val i = idx.toInt()
                    if (i in 0 until list.size) list[i] = value
                    else if (i == list.size) list.add(value)
                    else {
                        while (list.size < i) list.add(null)
                        list.add(value)
                    }
                } else if (obj is MutableMap<*, *>) {
                    (obj as MutableMap<Any?, Any?>)[idx] = value
                }
            }
            else -> {}
        }
        return value
    }

    private fun parseSimpleJson(str: String): Any? {
        val s = str.trim()
        if (s.startsWith("{") && s.endsWith("}")) {
            val map = mutableMapOf<String, Any?>()
            val inner = s.substring(1, s.length - 1).trim()
            if (inner.isNotEmpty()) {
                var depth = 0
                var start = 0
                val parts = mutableListOf<String>()
                for (i in 0 until inner.length) {
                    val c = inner[i]
                    if (c == '{' || c == '[') depth++
                    else if (c == '}' || c == ']') depth--
                    else if (c == ',' && depth == 0) {
                        parts.add(inner.substring(start, i))
                        start = i + 1
                    }
                }
                parts.add(inner.substring(start))
                for (p in parts) {
                    val colonIdx = p.indexOf(':')
                    if (colonIdx != -1) {
                        val key = p.substring(0, colonIdx).trim().trim('"', '\'')
                        val rawVal = p.substring(colonIdx + 1).trim()
                        map[key] = parseJsonValue(rawVal)
                    }
                }
            }
            return map
        }
        return null
    }

    private fun parseJsonValue(raw: String): Any? {
        if (raw == "null") return null
        if (raw == "true") return true
        if (raw == "false") return false
        if (raw.startsWith("\"") && raw.endsWith("\"")) return raw.substring(1, raw.length - 1)
        if (raw.startsWith("[") && raw.endsWith("]")) {
            val list = mutableListOf<Any?>()
            val inner = raw.substring(1, raw.length - 1).trim()
            if (inner.isNotEmpty()) {
                val parts = inner.split(",")
                for (part in parts) {
                    list.add(parseJsonValue(part.trim()))
                }
            }
            return list
        }
        if (raw.contains(".")) return raw.toDoubleOrNull()
        return raw.toIntOrNull() ?: raw.toLongOrNull() ?: raw
    }

    private fun isTruthy(v: Any?): Boolean {
        return when (v) {
            null -> false
            is Boolean -> v
            is Number -> v.toDouble() != 0.0
            is String -> v.isNotEmpty()
            else -> true
        }
    }

    private fun formatValue(v: Any?): String {
        return when (v) {
            null -> "null"
            is LString -> {
                val params = v.params.toMutableMap()
                val regex = Regex("""\{([a-zA-Z_][a-zA-Z0-9_]*)\}""")
                regex.findAll(v.defaultText).forEach { match ->
                    val varName = match.groupValues[1]
                    if (!params.containsKey(varName) && globals.containsKey(varName)) {
                        params[varName] = formatValue(globals[varName])
                    }
                }
                v.copy(params = params).format("en")
            }
            is Double -> if (v.isInfinite()) "Infinity" else v.toString()
            is ByteArray -> "[" + v.joinToString(", ") { "0x" + (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() } + "]"
            else -> v.toString()
        }
    }
}
