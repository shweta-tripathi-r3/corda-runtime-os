package net.corda.ledger.json.expression.parsing

interface Token

interface Reference : Token {
    val ref: String
}

interface Keyword : Token

data class PathReference(override val ref: String) : Reference

data class PathReferenceWithSpaces(override val ref: String) : Reference

data class Number(override val ref: String): Reference

data class JsonCast(val value: String): Keyword

class LeftParentheses : Token

class RightParentheses : Token

class ParameterEnd : Token