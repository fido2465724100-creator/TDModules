package com.tdscript.core

sealed class ASTNode

data class ProgramNode(val statements: List<ASTNode>) : ASTNode()

data class VarDeclNode(val name: String, val expr: ASTNode, val isLet: Boolean) : ASTNode()

data class AssignmentNode(val target: ASTNode, val expr: ASTNode, val op: String) : ASTNode()

data class IfStmtNode(val condition: ASTNode, val thenBranch: ASTNode, val elseBranch: ASTNode?) : ASTNode()

data class WhileStmtNode(val condition: ASTNode, val body: ASTNode) : ASTNode()

data class BlockNode(val statements: List<ASTNode>) : ASTNode()

data class ExprStmtNode(val expr: ASTNode) : ASTNode()

data class BinaryExprNode(val left: ASTNode, val op: String, val right: ASTNode) : ASTNode()

data class UnaryExprNode(val op: String, val operand: ASTNode) : ASTNode()

data class PostfixUpdateNode(val targetIdent: String, val op: String) : ASTNode()

data class CallExprNode(val callee: ASTNode, val arguments: List<ASTNode>) : ASTNode()

data class MemberAccessNode(val obj: ASTNode, val propertyName: String) : ASTNode()

data class IndexAccessNode(val obj: ASTNode, val indexExpr: ASTNode) : ASTNode()

data class IdentifierNode(val name: String) : ASTNode()

data class LiteralNode(val value: Any?, val valType: String) : ASTNode()
