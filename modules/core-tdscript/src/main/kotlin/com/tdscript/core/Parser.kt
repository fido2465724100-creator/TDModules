package com.tdscript.core

class ParseException(message: String, val token: Token?) : Exception("ParseError: $message" + (token?.let { " at line ${it.line}, col ${it.col} (token: ${it.type})" } ?: ""))

class Parser(private val tokens: List<Token>) {
    private var pos = 0

    private fun peek(offset: Int = 0): Token {
        val idx = pos + offset
        return if (idx < tokens.size) tokens[idx] else tokens.last()
    }

    private fun advance(): Token {
        val tok = peek()
        if (pos < tokens.size) pos++
        return tok
    }

    private fun expect(type: TokenType, errMsg: String): Token {
        val tok = peek()
        if (tok.type != type) throw ParseException("$errMsg, got ${tok.type}", tok)
        return advance()
    }

    private fun skipTerminators() {
        while (peek().type == TokenType.NEWLINE || peek().type == TokenType.SEMICOLON) {
            advance()
        }
    }

    fun parse(): ProgramNode {
        val statements = mutableListOf<ASTNode>()
        skipTerminators()
        while (peek().type != TokenType.EOF) {
            val stmt = statement()
            if (stmt != null) statements.add(stmt)
            skipTerminators()
        }
        return ProgramNode(statements)
    }

    private fun statement(): ASTNode? {
        skipTerminators()
        val tok = peek()
        return when (tok.type) {
            TokenType.LET, TokenType.VAR -> varDeclaration()
            TokenType.IF -> ifStatement()
            TokenType.WHILE -> whileStatement()
            TokenType.LBRACE -> block()
            TokenType.SEMICOLON, TokenType.NEWLINE -> { advance(); null }
            else -> {
                val expr = expression()
                consumeStatementTerminator()
                ExprStmtNode(expr)
            }
        }
    }

    private fun consumeStatementTerminator() {
        val tok = peek()
        if (tok.type in listOf(TokenType.SEMICOLON, TokenType.NEWLINE, TokenType.EOF, TokenType.RBRACE)) {
            if (tok.type in listOf(TokenType.SEMICOLON, TokenType.NEWLINE)) advance()
        } else {
            throw ParseException("Expected statement terminator (';', newline or EOF)", tok)
        }
    }

    private fun varDeclaration(): ASTNode {
        val kw = advance()
        val isLet = (kw.type == TokenType.LET)
        val ident = expect(TokenType.IDENTIFIER, "Expected identifier after let/var")
        expect(TokenType.ASSIGN, "Expected '=' in variable declaration")
        val expr = expression()
        consumeStatementTerminator()
        return VarDeclNode(ident.value as String, expr, isLet)
    }

    private fun ifStatement(): ASTNode {
        advance() // 'if'
        expect(TokenType.LPAREN, "Expected '(' after if")
        val cond = expression()
        expect(TokenType.RPAREN, "Expected ')' after if condition")
        skipTerminators()

        val thenBranch = statementOrBlock()
        var elseBranch: ASTNode? = null
        skipTerminators()

        // Dangling else attaches to innermost open if
        if (peek().type == TokenType.ELSE) {
            advance()
            skipTerminators()
            elseBranch = statementOrBlock()
        }
        return IfStmtNode(cond, thenBranch, elseBranch)
    }

    private fun whileStatement(): ASTNode {
        advance() // 'while'
        expect(TokenType.LPAREN, "Expected '(' after while")
        val cond = expression()
        expect(TokenType.RPAREN, "Expected ')' after while condition")
        skipTerminators()
        val body = statementOrBlock()
        return WhileStmtNode(cond, body)
    }

    private fun block(): ASTNode {
        expect(TokenType.LBRACE, "Expected '{'")
        val stmts = mutableListOf<ASTNode>()
        skipTerminators()
        while (peek().type != TokenType.RBRACE && peek().type != TokenType.EOF) {
            val s = statement()
            if (s != null) stmts.add(s)
            skipTerminators()
        }
        expect(TokenType.RBRACE, "Expected '}'")
        return BlockNode(stmts)
    }

    private fun statementOrBlock(): ASTNode {
        return if (peek().type == TokenType.LBRACE) block() else (statement() ?: BlockNode(emptyList()))
    }

    fun expression(): ASTNode = assignmentExpression()

    private fun assignmentExpression(): ASTNode {
        val expr = logicalOr()
        if (peek().type == TokenType.ASSIGN || peek().type == TokenType.COLON_ASSIGN) {
            val opTok = advance()
            validateLValue(expr, opTok)
            val rhs = assignmentExpression()
            return AssignmentNode(expr, rhs, opTok.value as String)
        }
        return expr
    }

    private fun validateLValue(node: ASTNode, opTok: Token) {
        var curr = node
        while (curr is MemberAccessNode || curr is IndexAccessNode) {
            curr = if (curr is MemberAccessNode) curr.obj else (curr as IndexAccessNode).obj
        }
        if (curr !is IdentifierNode) {
            throw ParseException("Invalid assignment target (L-Value must be identifier or accessor chain)", opTok)
        }
    }

    private fun logicalOr(): ASTNode {
        var node = logicalAnd()
        while (peek().type == TokenType.OR) {
            val op = advance().value as String
            val right = logicalAnd()
            node = BinaryExprNode(node, op, right)
        }
        return node
    }

    private fun logicalAnd(): ASTNode {
        var node = equality()
        while (peek().type == TokenType.AND) {
            val op = advance().value as String
            val right = equality()
            node = BinaryExprNode(node, op, right)
        }
        return node
    }

    private fun equality(): ASTNode {
        var node = relational()
        while (peek().type in listOf(TokenType.EQ, TokenType.NEQ)) {
            val op = advance().value as String
            val right = relational()
            node = BinaryExprNode(node, op, right)
        }
        return node
    }

    private fun relational(): ASTNode {
        var node = additive()
        while (peek().type in listOf(TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE)) {
            val op = advance().value as String
            val right = additive()
            node = BinaryExprNode(node, op, right)
        }
        return node
    }

    private fun additive(): ASTNode {
        var node = multiplicative()
        while (peek().type in listOf(TokenType.PLUS, TokenType.MINUS)) {
            val op = advance().value as String
            val right = multiplicative()
            node = BinaryExprNode(node, op, right)
        }
        return node
    }

    private fun multiplicative(): ASTNode {
        var node = unary()
        while (peek().type in listOf(TokenType.STAR, TokenType.SLASH, TokenType.PERCENT)) {
            val op = advance().value as String
            val right = unary()
            node = BinaryExprNode(node, op, right)
        }
        return node
    }

    private fun unary(): ASTNode {
        if (peek().type in listOf(TokenType.NOT, TokenType.MINUS)) {
            val op = advance().value as String
            val operand = unary()
            return UnaryExprNode(op, operand)
        }
        return postfix()
    }

    private fun postfix(): ASTNode {
        var node = primary()
        while (true) {
            when (peek().type) {
                TokenType.DOT -> {
                    advance()
                    val ident = expect(TokenType.IDENTIFIER, "Expected identifier after '.'")
                    node = MemberAccessNode(node, ident.value as String, ident.line, ident.col)
                }
                TokenType.LBRACKET -> {
                    val lbracket = advance()
                    val idx = expression()
                    expect(TokenType.RBRACKET, "Expected ']' after index")
                    node = IndexAccessNode(node, idx, lbracket.line, lbracket.col)
                }
                TokenType.LPAREN -> {
                    val lparen = advance()
                    val args = mutableListOf<ASTNode>()
                    if (peek().type != TokenType.RPAREN) {
                        args.add(expression())
                        while (peek().type == TokenType.COMMA) {
                            advance()
                            args.add(expression())
                        }
                    }
                    expect(TokenType.RPAREN, "Expected ')' after argument list")
                    node = CallExprNode(node, args, lparen.line, lparen.col)
                }
                else -> break
            }
        }

        if (peek().type in listOf(TokenType.INC, TokenType.DEC)) {
            val opTok = advance()
            if (node !is IdentifierNode) {
                throw ParseException("Invalid update target: '++' and '--' can only be applied to a simple Identifier", opTok)
            }
            if (peek().type in listOf(TokenType.INC, TokenType.DEC)) {
                throw ParseException("Consecutive postfix update operators are not allowed", peek())
            }
            return PostfixUpdateNode(node.name, opTok.value as String)
        }

        return node
    }

    private fun primary(): ASTNode {
        val tok = peek()
        return when (tok.type) {
            TokenType.INT, TokenType.LONG, TokenType.FLOAT,
            TokenType.STRING, TokenType.LSTRING, TokenType.BOOLEAN, TokenType.NULL -> {
                advance()
                LiteralNode(tok.value, tok.type.name)
            }
            TokenType.IDENTIFIER -> {
                advance()
                IdentifierNode(tok.value as String)
            }
            TokenType.LPAREN -> {
                advance()
                val expr = expression()
                expect(TokenType.RPAREN, "Expected ')' after grouped expression")
                expr
            }
            else -> throw ParseException("Unexpected token in expression: ${tok.type} ('${tok.value}')", tok)
        }
    }
}
