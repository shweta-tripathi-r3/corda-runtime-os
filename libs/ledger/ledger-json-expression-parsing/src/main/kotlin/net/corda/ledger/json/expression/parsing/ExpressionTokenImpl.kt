package net.corda.ledger.json.expression.parsing

data class PathReference(override val ref: String) : Reference

data class PathReferenceWithSpaces(override val ref: String) : Reference

data class JsonCast(val value: String): Keyword

class LeftParentheses : Token

class RightParentheses : Token

class ParameterEnd : Token

fun operatorFactory(op: String): Keyword {
    return when (op.uppercase()) {
        "!=" -> NotEquals()
        ">" -> GreaterThan()
        ">=" -> GreaterThanEquals()
        "<" -> LessThan()
        "<=" -> LessThanEquals()
        "IS NULL" -> IsNull()
        "IS NOT NULL" -> IsNotNull()
        "->>" -> JsonArrayOrObjectAsText()
        "AS" -> As()
        "FROM" -> From()
        "SELECT" -> Select()
        "WHERE" -> Where()
        "OR" -> Or()
        "AND" -> And()
        "=" -> Equals()
        else -> throw IllegalArgumentException("Unknown operator $op")
    }
}