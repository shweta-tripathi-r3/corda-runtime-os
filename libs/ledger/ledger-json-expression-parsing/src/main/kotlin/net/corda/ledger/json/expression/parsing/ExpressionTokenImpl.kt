package net.corda.ledger.json.expression.parsing

data class PathReference(override val ref: String) : Reference

data class PathReferenceWithSpaces(override val ref: String) : Reference

class LeftParentheses : Token

class RightParentheses : Token

class ParameterEnd : Token

fun operatorFactory(op: String): Keyword {
    return when (op.uppercase()) {
        "||" -> Or()
        "&&" -> And()
        "!=" -> NotEquals()
        ">" -> GreaterThan()
        ">=" -> GreaterThanEquals()
        "<" -> LessThan()
        "<=" -> LessThanEquals()
        "!" -> Not()
        "->>" -> JsonArrayOrObjectAsText()
        "AS" -> As()
        "FROM" -> From()
        "SELECT" -> Select()
        "WHERE" -> Where()
        "=" -> Equals()
        else -> throw IllegalArgumentException("Unknown operator $op")
    }
}