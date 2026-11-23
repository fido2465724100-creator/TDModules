package com.tdscript.core

/**
 * Machine-readable and LLM repair diagnostic format.
 * Supports:
 * 1. Compact Repair Mode: E104@14:5 exp=0x76|0x77 got=0x68 (token-efficient for LLM agent loops)
 * 2. Full JSON format for logging and inspection.
 */
data class DiagnosticError(
    val errorCode: String,
    val line: Int,
    val column: Int,
    val message: String,
    val expected: String,
    val actual: String,
    val suggestion: String = ""
) {
    /**
     * Compact one-line repair mode for LLM prompt optimization:
     * Example: E104@14:5 exp=0x76|0x77 got=0x68
     */
    fun toRepairFormat(): String {
        val codeMatch = Regex("""(E\d+)""").find(errorCode)
        val shortCode = codeMatch?.value ?: errorCode.removePrefix("TDS_")
        val expClean = expected.replace(" ", "")
        val actClean = actual.replace(" ", "")
        return "$shortCode@$line:$column exp=$expClean got=$actClean"
    }

    /**
     * Full detailed JSON for Base mode and development logs.
     */
    fun toJson(): String {
        return """
            {
              "status": "ERROR",
              "error_code": "$errorCode",
              "line": $line,
              "column": $column,
              "message": "$message",
              "expected": "$expected",
              "actual": "$actual",
              "suggestion": "$suggestion"
            }
        """.trimIndent()
    }

    companion object {
        /**
         * Convenience helper to create a Scope Violation diagnostic in repair mode.
         */
        fun scopeViolation(
            line: Int,
            column: Int,
            expected: String,
            actual: String,
            message: String = "Scope boundary violation"
        ): DiagnosticError {
            return DiagnosticError(
                errorCode = "TDS_E104_SCOPE_VIOLATION",
                line = line,
                column = column,
                message = message,
                expected = expected,
                actual = actual,
                suggestion = "Use addresses within granted CapabilityScope boundaries."
            )
        }
    }
}
