package com.tdscript.core

enum class TokenType {
    INT, LONG, FLOAT, STRING, LSTRING, BOOLEAN, NULL, IDENTIFIER,
    LET, VAR, IF, ELSE, WHILE,
    PLUS, MINUS, STAR, SLASH, PERCENT,
    ASSIGN, COLON_ASSIGN,
    EQ, NEQ, LT, LTE, GT, GTE,
    AND, OR, NOT, INC, DEC,
    LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
    COMMA, DOT, SEMICOLON, NEWLINE, EOF
}

data class Token(
    val type: TokenType,
    val value: Any?,
    val line: Int,
    val col: Int
)

class LexerException(message: String, val line: Int, val col: Int) : Exception("LexerError: $message at line $line, col $col")

class Lexer(sourceCode: String, forcedLanguage: String? = null) {
    private val source = if (sourceCode.startsWith("\uFEFF")) sourceCode.substring(1) else sourceCode
    private var pos = 0
    private var line = 1
    private var col = 1
    private var parenDepth = 0

    // Язык скрипта (по умолчанию "en", либо извлеченный из директивы language: <code>;)
    var activeLanguage: String = forcedLanguage ?: detectLanguageDirective()
        private set

    companion object {
        // Таблица локализованных ключевых слов (Unicode-safe, не заменяет текст в строках и комментах!)
        private val LOCALIZED_KEYWORDS = mapOf(
            "ru" to mapOf(
                "если" to TokenType.IF,
                "иначе" to TokenType.ELSE,
                "пока" to TokenType.WHILE,
                "пусть" to TokenType.LET,
                "перем" to TokenType.VAR,
                "истина" to TokenType.BOOLEAN,
                "ложь" to TokenType.BOOLEAN,
                "пусто" to TokenType.NULL
            ),
            "de" to mapOf(
                "wenn" to TokenType.IF,
                "sonst" to TokenType.ELSE,
                "solange" to TokenType.WHILE,
                "sei" to TokenType.LET,
                "wahr" to TokenType.BOOLEAN,
                "falsch" to TokenType.BOOLEAN,
                "null" to TokenType.NULL
            ),
            "es" to mapOf(
                "si" to TokenType.IF,
                "sino" to TokenType.ELSE,
                "mientras" to TokenType.WHILE,
                "sea" to TokenType.LET,
                "verdadero" to TokenType.BOOLEAN,
                "falso" to TokenType.BOOLEAN,
                "nulo" to TokenType.NULL
            )
        )

        private val CANONICAL_KEYWORDS = mapOf(
            "let" to TokenType.LET,
            "var" to TokenType.VAR,
            "if" to TokenType.IF,
            "else" to TokenType.ELSE,
            "while" to TokenType.WHILE,
            "true" to TokenType.BOOLEAN,
            "false" to TokenType.BOOLEAN,
            "null" to TokenType.NULL
        )
    }

    private fun detectLanguageDirective(): String {
        // Скан первой строки без сдвига основного указателя pos
        val firstLine = source.lines().firstOrNull()?.trim() ?: ""
        val match = Regex("""^language:\s*([a-zA-Z_\-]+)\s*;""").find(firstLine)
        return match?.groupValues?.get(1)?.lowercase() ?: "en"
    }

    private fun peek(offset: Int = 0): Char? {
        val idx = pos + offset
        return if (idx < source.length) source[idx] else null
    }

    private fun advance(): Char? {
        val ch = peek()
        if (ch != null) {
            pos++
            if (ch == '\n') {
                line++
                col = 1
            } else {
                col++
            }
        }
        return ch
    }

    private fun match(expected: Char): Boolean {
        if (peek() == expected) {
            advance()
            return true
        }
        return false
    }

    fun tokenize(): List<Token> {
        val tokens = mutableListOf<Token>()

        // Если в начале скрипта стоит директива 'language: <code>;', пропускаем её токенизатором
        val trimmed = source.substring(pos).trimStart()
        if (trimmed.startsWith("language:")) {
            val endIdx = source.indexOf(';', pos)
            if (endIdx != -1) {
                // Продвигаем позицию за точку с запятой
                val skipped = source.substring(pos, endIdx + 1)
                for (ch in skipped) {
                    if (ch == '\n') { line++; col = 1 } else { col++ }
                }
                pos = endIdx + 1
            }
        }

        while (pos < source.length) {
            val startLine = line
            val startCol = col
            val ch = peek()!!

            // Whitespace (excluding newline)
            if (ch == ' ' || ch == '\t' || ch == '\r') {
                advance()
                continue
            }

            // Comments
            if (ch == '#' || (ch == '/' && peek(1) == '/')) {
                while (peek() != null && peek() != '\n') {
                    advance()
                }
                continue
            }

            // Block comments
            if (ch == '/' && peek(1) == '*') {
                advance(); advance()
                while (peek() != null && !(peek() == '*' && peek(1) == '/')) {
                    advance()
                }
                if (peek() != null) { advance(); advance() }
                continue
            }

            // Newline
            if (ch == '\n') {
                advance()
                if (parenDepth == 0 && tokens.isNotEmpty()) {
                    val lastType = tokens.last().type
                    val binaryOps = setOf(
                        TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH, TokenType.PERCENT,
                        TokenType.ASSIGN, TokenType.COLON_ASSIGN, TokenType.EQ, TokenType.NEQ,
                        TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE,
                        TokenType.AND, TokenType.OR, TokenType.COMMA, TokenType.DOT
                    )
                    if (lastType !in binaryOps && lastType != TokenType.NEWLINE && lastType != TokenType.SEMICOLON) {
                        tokens.add(Token(TokenType.NEWLINE, "\n", startLine, startCol))
                    }
                }
                continue
            }

            // Numbers
            if (ch.isDigit()) {
                val sb = java.lang.StringBuilder()
                var isFloat = false
                while (peek()?.let { it.isDigit() || it == '.' || it == 'L' || it == 'l' } == true) {
                    val c = peek()!!
                    if (c == '.') {
                        if (isFloat) break
                        if (peek(1)?.isDigit() == true) {
                            isFloat = true
                            sb.append(advance()!!)
                        } else break
                    } else if (c == 'L' || c == 'l') {
                        advance()
                        val raw = sb.toString()
                        val big = raw.toBigInteger()
                        if (big == java.math.BigInteger("9223372036854775808")) {
                            tokens.add(Token(TokenType.LONG, Long.MIN_VALUE, startLine, startCol))
                        } else {
                            tokens.add(Token(TokenType.LONG, raw.toLong(), startLine, startCol))
                        }
                        sb.setLength(0)
                        break
                    } else {
                        sb.append(advance()!!)
                    }
                }
                if (sb.isNotEmpty()) {
                    if (isFloat) {
                        tokens.add(Token(TokenType.FLOAT, sb.toString().toDouble(), startLine, startCol))
                    } else {
                        val raw = sb.toString()
                        val big = raw.toBigInteger()
                        if (big == java.math.BigInteger("2147483648")) {
                            tokens.add(Token(TokenType.INT, Int.MIN_VALUE, startLine, startCol))
                        } else if (big > Int.MAX_VALUE.toBigInteger() || big < Int.MIN_VALUE.toBigInteger()) {
                            tokens.add(Token(TokenType.LONG, raw.toLong(), startLine, startCol))
                        } else {
                            tokens.add(Token(TokenType.INT, raw.toInt(), startLine, startCol))
                        }
                    }
                }
                continue
            }

            // String literals
            if (ch == '"' || ch == '\'') {
                val quote = ch
                advance()
                val sb = java.lang.StringBuilder()
                while (peek() != null && peek() != quote) {
                    val c = advance()!!
                    if (c == '\\') {
                        when (val esc = advance()) {
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            '\\' -> sb.append('\\')
                            '"' -> sb.append('"')
                            '\'' -> sb.append('\'')
                            else -> throw LexerException("Invalid escape sequence: \\$esc", line, col)
                        }
                    } else {
                        sb.append(c)
                    }
                }
                if (peek() == quote) advance() else throw LexerException("Unterminated string literal", startLine, startCol)
                tokens.add(Token(TokenType.STRING, sb.toString(), startLine, startCol))
                continue
            }

            // LString literal L"..."
            if (ch == 'L' && (peek(1) == '"' || peek(1) == '\'')) {
                advance() // consume 'L'
                val quote = advance()!! // consume quote
                val sb = java.lang.StringBuilder()
                while (peek() != null && peek() != quote) {
                    val c = advance()!!
                    if (c == '\\') {
                        when (val esc = advance()) {
                            'n' -> sb.append('\n')
                            't' -> sb.append('\t')
                            '\\' -> sb.append('\\')
                            '"' -> sb.append('"')
                            '\'' -> sb.append('\'')
                            else -> sb.append(esc)
                        }
                    } else {
                        sb.append(c)
                    }
                }
                if (peek() == quote) advance()
                val fullRaw = sb.toString()
                val colonIdx = fullRaw.indexOf(':')
                val (key, defaultText) = if (colonIdx != -1) {
                    Pair(fullRaw.substring(0, colonIdx).trim(), fullRaw.substring(colonIdx + 1).trim())
                } else {
                    Pair(fullRaw.trim(), fullRaw.trim())
                }
                tokens.add(Token(TokenType.LSTRING, LString(key, defaultText), startLine, startCol))
                continue
            }

            // Identifiers & Unicode keywords
            if (ch.isLetter() || ch == '_') {
                val sb = java.lang.StringBuilder()
                while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true) {
                    sb.append(advance()!!)
                }
                val word = sb.toString()
                val lowerWord = word.lowercase()

                // 1. Проверяем канонические ключевые слова
                if (CANONICAL_KEYWORDS.containsKey(lowerWord)) {
                    val type = CANONICAL_KEYWORDS[lowerWord]!!
                    val value = when (type) {
                        TokenType.BOOLEAN -> lowerWord == "true"
                        TokenType.NULL -> null
                        else -> word
                    }
                    tokens.add(Token(type, value, startLine, startCol))
                }
                // 2. Проверяем локализованные ключевые слова активного языка
                else if (LOCALIZED_KEYWORDS[activeLanguage]?.containsKey(lowerWord) == true) {
                    val type = LOCALIZED_KEYWORDS[activeLanguage]!![lowerWord]!!
                    val value = when (type) {
                        TokenType.BOOLEAN -> (lowerWord == "истина" || lowerWord == "wahr" || lowerWord == "verdadero")
                        TokenType.NULL -> null
                        else -> word
                    }
                    tokens.add(Token(type, value, startLine, startCol))
                } else {
                    tokens.add(Token(TokenType.IDENTIFIER, word, startLine, startCol))
                }
                continue
            }

            // Punctuation & Operators
            advance()
            when (ch) {
                '+' -> if (match('+')) tokens.add(Token(TokenType.INC, "++", startLine, startCol))
                       else tokens.add(Token(TokenType.PLUS, "+", startLine, startCol))
                '-' -> if (match('-')) tokens.add(Token(TokenType.DEC, "--", startLine, startCol))
                       else tokens.add(Token(TokenType.MINUS, "-", startLine, startCol))
                '*' -> tokens.add(Token(TokenType.STAR, "*", startLine, startCol))
                '/' -> tokens.add(Token(TokenType.SLASH, "/", startLine, startCol))
                '%' -> tokens.add(Token(TokenType.PERCENT, "%", startLine, startCol))
                '=' -> if (match('=')) tokens.add(Token(TokenType.EQ, "==", startLine, startCol))
                       else tokens.add(Token(TokenType.ASSIGN, "=", startLine, startCol))
                ':' -> if (match('=')) tokens.add(Token(TokenType.COLON_ASSIGN, ":=", startLine, startCol))
                       else throw LexerException("Unexpected token ':'", startLine, startCol)
                '!' -> if (match('=')) tokens.add(Token(TokenType.NEQ, "!=", startLine, startCol))
                       else tokens.add(Token(TokenType.NOT, "!", startLine, startCol))
                '<' -> if (match('=')) tokens.add(Token(TokenType.LTE, "<=", startLine, startCol))
                       else tokens.add(Token(TokenType.LT, "<", startLine, startCol))
                '>' -> if (match('=')) tokens.add(Token(TokenType.GTE, ">=", startLine, startCol))
                       else tokens.add(Token(TokenType.GT, ">", startLine, startCol))
                '&' -> if (match('&')) tokens.add(Token(TokenType.AND, "&&", startLine, startCol))
                       else throw LexerException("Unexpected token '&'", startLine, startCol)
                '|' -> if (match('|')) tokens.add(Token(TokenType.OR, "||", startLine, startCol))
                       else throw LexerException("Unexpected token '|'", startLine, startCol)
                '(' -> { parenDepth++; tokens.add(Token(TokenType.LPAREN, "(", startLine, startCol)) }
                ')' -> { if (parenDepth > 0) parenDepth--; tokens.add(Token(TokenType.RPAREN, ")", startLine, startCol)) }
                '[' -> { parenDepth++; tokens.add(Token(TokenType.LBRACKET, "[", startLine, startCol)) }
                ']' -> { if (parenDepth > 0) parenDepth--; tokens.add(Token(TokenType.RBRACKET, "]", startLine, startCol)) }
                '{' -> { parenDepth++; tokens.add(Token(TokenType.LBRACE, "{", startLine, startCol)) }
                '}' -> { if (parenDepth > 0) parenDepth--; tokens.add(Token(TokenType.RBRACE, "}", startLine, startCol)) }
                ',' -> tokens.add(Token(TokenType.COMMA, ",", startLine, startCol))
                '.' -> tokens.add(Token(TokenType.DOT, ".", startLine, startCol))
                ';' -> tokens.add(Token(TokenType.SEMICOLON, ";", startLine, startCol))
                else -> throw LexerException("Unexpected character: $ch", startLine, startCol)
            }
        }

        tokens.add(Token(TokenType.EOF, null, line, col))
        return tokens
    }
}
