package com.tdscript.core

object Capabilities {
    const val NET_READ   = 0x0001
    const val NET_SCAN   = 0x0002
    const val PLC_READ   = 0x0004
    const val PLC_WRITE  = 0x0008
    const val BT_ACCESS  = 0x0010
    const val HARDWARE   = 0x0020
    const val UI_CORE    = 0x0040
    const val BACKGROUND = 0x0080
}

interface HostSPI {
    val stdoutLines: MutableList<String>
    val diagnosticLines: MutableList<String>
    var capabilities: Int

    fun checkCapability(capBit: Int, capName: String): Boolean {
        if ((capabilities and capBit) != 0) return true
        diagnosticLines.add("SecurityException: PERMISSION_DENIED [$capName]")
        return false
    }

    fun print(msg: String) {
        stdoutLines.add(msg)
    }

    // Default Host SPI stubs
    fun netHttpGet(url: String): String? = if (!checkCapability(Capabilities.NET_READ, "NET_READ")) null else "HTTP_OK: $url"
    fun netPing(host: String): Float = if (!checkCapability(Capabilities.NET_READ, "NET_READ")) -1.0f else 12.5f
    fun netScanPort(host: String, port: Int): Boolean = if (!checkCapability(Capabilities.NET_SCAN, "NET_SCAN")) false else true
    fun netWol(mac: String): Boolean = if (!checkCapability(Capabilities.NET_SCAN, "NET_SCAN")) false else true

    fun plcModbusRead(host: String, reg: Int): Int = if (!checkCapability(Capabilities.PLC_READ, "PLC_READ")) -1 else 1234
    fun plcModbusWrite(host: String, reg: Int, value: Int): Boolean = if (!checkCapability(Capabilities.PLC_WRITE, "PLC_WRITE")) false else true

    fun phoneAlarm(volume: Int): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true
    fun phoneVibrate(ms: Long): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true
    fun phoneTorch(state: Boolean): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true
    fun phoneBeep(ms: Int): Boolean = if (!checkCapability(Capabilities.HARDWARE, "HARDWARE")) false else true

    fun uiToast(msg: String): Boolean = if (!checkCapability(Capabilities.UI_CORE, "UI_CORE")) false else true
    fun uiLog(msg: String): Boolean = if (!checkCapability(Capabilities.UI_CORE, "UI_CORE")) false else true
}

class StandardHostSPI(override var capabilities: Int = Capabilities.NET_READ) : HostSPI {
    override val stdoutLines = mutableListOf<String>()
    override val diagnosticLines = mutableListOf<String>()
}

class Interpreter(val host: HostSPI = StandardHostSPI()) {
    // Script Global Scope: single symbol table
    val globals = mutableMapOf<String, Any?>()

    fun execute(program: ProgramNode) {
        for (stmt in program.statements) {
            execStmt(stmt)
        }
    }

    fun execStmt(stmt: ASTNode) {
        when (stmt) {
            is VarDeclNode -> {
                val value = evalExpr(stmt.expr)
                globals[stmt.name] = value
            }
            is AssignmentNode -> evalAssignment(stmt)
            is ExprStmtNode -> evalExpr(stmt.expr)
            is IfStmtNode -> {
                val cond = evalExpr(stmt.condition)
                if (isTruthy(cond)) {
                    execStmt(stmt.thenBranch)
                } else if (stmt.elseBranch != null) {
                    execStmt(stmt.elseBranch)
                }
            }
            is WhileStmtNode -> {
                while (isTruthy(evalExpr(stmt.condition))) {
                    execStmt(stmt.body)
                }
            }
            is BlockNode -> {
                for (s in stmt.statements) execStmt(s)
            }
            else -> {}
        }
    }

    fun isTruthy(value: Any?): Boolean {
        return when (value) {
            null -> false
            is Boolean -> value
            is Number -> value.toDouble() != 0.0
            is String -> value.isNotEmpty()
            else -> true
        }
    }

    fun formatValue(v: Any?): String {
        return when (v) {
            null -> "null"
            is Boolean -> if (v) "true" else "false"
            is Double -> if (v.isInfinite()) "Infinity" else if (v.isNaN()) "NaN" else v.toString()
            is Float -> if (v.isInfinite()) "Infinity" else if (v.isNaN()) "NaN" else v.toString()
            else -> v.toString()
        }
    }

    fun evalExpr(expr: ASTNode): Any? {
        return when (expr) {
            is LiteralNode -> expr.value
            is IdentifierNode -> globals[expr.name]
            is AssignmentNode -> evalAssignment(expr)
            is PostfixUpdateNode -> {
                val current = globals[expr.targetIdent]
                val num = (current as? Number)?.toLong() ?: 0L
                val oldVal = num
                val newVal = if (expr.op == "++") num + 1 else num - 1
                globals[expr.targetIdent] = newVal
                oldVal
            }
            is UnaryExprNode -> {
                val operand = evalExpr(expr.operand)
                when (expr.op) {
                    "!" -> !isTruthy(operand)
                    "-" -> {
                        when (operand) {
                            null -> null
                            is Int -> -operand
                            is Long -> -operand
                            is Double -> -operand
                            is Float -> -operand
                            else -> null
                        }
                    }
                    else -> null
                }
            }
            is BinaryExprNode -> {
                if (expr.op == "&&") {
                    val left = evalExpr(expr.left)
                    if (!isTruthy(left)) return false
                    val right = evalExpr(expr.right)
                    return isTruthy(right)
                }
                if (expr.op == "||") {
                    val left = evalExpr(expr.left)
                    if (isTruthy(left)) return true
                    val right = evalExpr(expr.right)
                    return isTruthy(right)
                }

                val left = evalExpr(expr.left)
                val right = evalExpr(expr.right)

                when (expr.op) {
                    "+" -> {
                        if (left is String || right is String) {
                            formatValue(left) + formatValue(right)
                        } else if (left is Number && right is Number) {
                            if (left is Double || right is Double || left is Float || right is Float) {
                                left.toDouble() + right.toDouble()
                            } else {
                                val res = left.toLong() + right.toLong()
                                if (res in Int.MIN_VALUE..Int.MAX_VALUE && left is Int && right is Int) res.toInt() else res
                            }
                        } else null
                    }
                    "-" -> {
                        if (left is Number && right is Number) {
                            if (left is Double || right is Double) left.toDouble() - right.toDouble()
                            else left.toLong() - right.toLong()
                        } else null
                    }
                    "*" -> {
                        if (left is Number && right is Number) {
                            if (left is Double || right is Double) left.toDouble() * right.toDouble()
                            else left.toLong() * right.toLong()
                        } else null
                    }
                    "/" -> {
                        if (left is Number && right is Number) {
                            val isFlt = left is Double || right is Double || left is Float || right is Float
                            if (right.toDouble() == 0.0) {
                                if (isFlt) Double.POSITIVE_INFINITY else null
                            } else {
                                if (isFlt) left.toDouble() / right.toDouble()
                                else {
                                    val div = left.toDouble() / right.toDouble()
                                    if (div % 1.0 == 0.0) div.toLong() else div
                                }
                            }
                        } else null
                    }
                    "%" -> {
                        if (left is Number && right is Number && right.toLong() != 0L) {
                            left.toLong() % right.toLong()
                        } else null
                    }
                    "==" -> {
                        // Strict equality without coercion
                        if (left == null && right == null) true
                        else if (left == null || right == null) false
                        else if (left::class != right::class) {
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
            if (target is IdentifierNode) {
                when (target.name) {
                    "JSON" -> if (method == "parse" && args.isNotEmpty()) return parseSimpleJson(args[0]?.toString() ?: "")
                    "NET" -> when (method) {
                        "httpGet" -> return host.netHttpGet(args.getOrNull(0)?.toString() ?: "")
                        "ping" -> return host.netPing(args.getOrNull(0)?.toString() ?: "")
                        "scanPort" -> return host.netScanPort(args.getOrNull(0)?.toString() ?: "", (args.getOrNull(1) as? Number)?.toInt() ?: 80)
                        "wol" -> return host.netWol(args.getOrNull(0)?.toString() ?: "")
                    }
                    "PLC" -> when (method) {
                        "modbusRead" -> return host.plcModbusRead(args.getOrNull(0)?.toString() ?: "", (args.getOrNull(1) as? Number)?.toInt() ?: 0)
                        "modbusWrite" -> return host.plcModbusWrite(args.getOrNull(0)?.toString() ?: "", (args.getOrNull(1) as? Number)?.toInt() ?: 0, (args.getOrNull(2) as? Number)?.toInt() ?: 0)
                    }
                    "PHONE" -> when (method) {
                        "alarm" -> return host.phoneAlarm((args.getOrNull(0) as? Number)?.toInt() ?: 100)
                        "vibrate" -> return host.phoneVibrate((args.getOrNull(0) as? Number)?.toLong() ?: 100L)
                        "torch" -> return host.phoneTorch(args.getOrNull(0) == true)
                        "beep" -> return host.phoneBeep((args.getOrNull(0) as? Number)?.toInt() ?: 150)
                    }
                    "UI" -> when (method) {
                        "toast" -> return host.uiToast(args.getOrNull(0)?.toString() ?: "")
                        "log" -> return host.uiLog(args.getOrNull(0)?.toString() ?: "")
                    }
                }
            }
        }
        return null
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
            // simple JSON key/val parser for tests
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
                        val key = p.substring(0, colonIdx).trim().trim('"')
                        val v = parseSimpleJson(p.substring(colonIdx + 1).trim())
                        map[key] = v
                    }
                }
            }
            return map
        } else if (s.startsWith("[") && s.endsWith("]")) {
            val list = mutableListOf<Any?>()
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
                for (p in parts) list.add(parseSimpleJson(p.trim()))
            }
            return list
        } else if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length - 1)
        } else if (s == "true") return true
        else if (s == "false") return false
        else if (s == "null") return null
        else if (s.toIntOrNull() != null) return s.toInt()
        else if (s.toLongOrNull() != null) return s.toLong()
        else if (s.toDoubleOrNull() != null) return s.toDouble()
        return s
    }
}
