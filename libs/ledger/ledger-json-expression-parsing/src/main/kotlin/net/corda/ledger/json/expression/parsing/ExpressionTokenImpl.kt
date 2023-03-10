package net.corda.ledger.json.expression.parsing

data class PathReference(override val ref: String) : Reference

data class PathReferenceWithSpaces(override val ref: String) : Reference

class LeftParentheses : Token

class RightParentheses : Token

class ParameterEnd : Token

fun operatorFactory(op: String): Operator {
    return when (op) {
        "||" -> LogicalOrOperator()
        "&&" -> LogicalAndOperator()
        "==" -> EqualsOperator()
        "!=" -> NotEqualsOperator()
        ">" -> GreaterThanOperator()
        ">=" -> GreaterThanEqualsOperator()
        "<" -> LessThanOperator()
        "<=" -> LessThanEqualsOperator()
        "!" -> LogicalNotOperator()
        "->>" -> JsonArrayOrObjectAsText()
        "AS" -> JsonAsNamedField()
        "FROM" -> JsonFrom()
        "SELECT" -> JsonSelect()
        "WHERE" -> JsonWhere()
        "=" -> JsonEqualTo()
        else -> throw IllegalArgumentException("Unknown operator $op")
    }
}