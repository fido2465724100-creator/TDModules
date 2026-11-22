package com.tdscript.core

enum class TokenType {
    INT, LONG, FLOAT, STRING, BOOLEAN, NULL, IDENTIFIER,
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

class Lexer(sourceCode: String) {
    private val source = if (sourceCode.startsWith("\uFEFF")) sourceCode.substring(1) else sourceCode
    private var pos = 0
    private var line = 1
    private var col = 1
    private var parenDepth = 0

    private val keywords = mapOf(
        "let" to TokenType.LET,
        "var" to TokenType.VAR,
        "if" to TokenType.IF,
        "else" to TokenType.ELSE,
        "while" to TokenType.WHILE,
        "true" to TokenType.BOOLEAN,
        "false" to TokenType.BOOLEAN,
        "null" to TokenType.NULL
    )

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

        while (pos < source.length) {
            val ch = peek() ?: break

            if (ch == ' ' || ch == '\t' || ch == '\r') {
                advance()
                continue
            }

            // Comments: // or #
            if (ch == '#' || (ch == '/' && peek(1) == '/')) {
                while (peek() != null && peek() != '\n' && peek() != '\r') {
                    advance()
                }
                continue
            }

            // Newlines
            if (ch == '\n') {
                val startLine = line
                val startCol = col
                advance()
                if (parenDepth > 0) continue

                // Line continuation check
                if (tokens.isNotEmpty()) {
                    val last = tokens.last().type
                    if (last in listOf(
                            TokenType.PLUS, TokenType.MINUS, TokenType.STAR, TokenType.SLASH,
                            TokenType.PERCENT, TokenType.ASSIGN, TokenType.COLON_ASSIGN,
                            TokenType.EQ, TokenType.NEQ, TokenType.LT, TokenType.LTE,
                            TokenType.GT, TokenType.GTE, TokenType.AND, TokenType.OR, TokenType.COMMA
                        )) continue
                    if (last == TokenType.NEWLINE) continue
                }
                tokens.add(Token(TokenType.NEWLINE, "\n", startLine, startCol))
                continue
            }

            val startLine = line
            val startCol = col

            // Strings
            if (ch == '"' || ch == '\'') {
                val quote = advance()!!
                val sb = java.lang.StringBuilder()
                while (true) {
                    val curr = peek() ?: throw LexerException("Unterminated string literal", line, col)
                    if (curr == '\n' || curr == '\r') throw LexerException("Physical newline inside string literal", line, col)
                    if (curr == quote) {
                        advance()
                        break
                    }
                    if (curr == '\\') {
                        advance()
                        val esc = advance() ?: throw LexerException("Unterminated escape", line, col)
                        when (esc) {
                            '"' -> sb.append('"')
                            '\'' -> sb.append('\'')
                            '\\' -> sb.append('\\')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            else -> throw LexerException("Invalid escape sequence: \\$esc", line, col)
                        }
                    } else {
                        sb.append(advance()!!)
                    }
                }
                tokens.add(Token(TokenType.STRING, sb.toString(), startLine, startCol))
                continue
            }

            // Numbers
            if (ch.isDigit()) {
                val sb = java.lang.StringBuilder()
                while (peek()?.isDigit() == true) {
                    sb.append(advance()!!)
                }
                var isFloat = false
                if (peek() == '.' && peek(1)?.isDigit() == true) {
                    isFloat = true
                    sb.append(advance()!!) // '.'
                    while (peek()?.isDigit() == true) {
                        sb.append(advance()!!)
                    }
                }
                if (isFloat) {
                    tokens.add(Token(TokenType.FLOAT, sb.toString().toDouble(), startLine, startCol))
                } else {
                    if (peek() == 'L' || peek() == 'l') {
                        advance()
                        val raw = sb.toString()
                        val big = raw.toBigInteger()
                        // 9223372036854775808 without minus overflows positive Long, but valid as Long.MIN_VALUE with unary minus
                        if (big == java.math.BigInteger("9223372036854775808")) {
                            tokens.add(Token(TokenType.LONG, Long.MIN_VALUE, startLine, startCol))
                        } else {
                            tokens.add(Token(TokenType.LONG, raw.toLong(), startLine, startCol))
                        }
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

            // Identifiers & keywords
            if (ch.isLetter() || ch == '_') {
                val sb = java.lang.StringBuilder()
                while (peek()?.let { it.isLetterOrDigit() || it == '_' } == true) {
                    sb.append(advance()!!)
                }
                val word = sb.toString()
                if (keywords.containsKey(word)) {
                    val type = keywords[word]!!
                    val value = when (type) {
                        TokenType.BOOLEAN -> word == "true"
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
