package net.corda.ledger.json.expression.parsing

class PostgresVaultNamedQueryConverter : VaultNamedQueryConverter {

    override fun convert(output: StringBuilder, expression: List<Token>): StringBuilder {
        for (token in expression) {
            when (token) {
                is PathReference -> output.append(token.ref)
                is PathReferenceWithSpaces -> output.append(token.ref)
                is Parameter -> output.append(token.ref)
                is Number -> output.append(token.ref)
                is JsonArrayOrObjectAsText -> output.append(" ->> ")
                is Select -> output.append(" SELECT ")
                is As -> output.append(" AS ")
                is From -> output.append(" FROM ")
                is Where -> output.append(" WHERE ")
                is And -> output.append(" AND ")
                is Or -> output.append(" OR ")
                is Equals -> output.append(" = ")
                is NotEquals -> output.append(" != ")
                is GreaterThan -> output.append(" > ")
                is GreaterThanEquals -> output.append(" >= ")
                is LessThan -> output.append(" < ")
                is LessThanEquals -> output.append(" <= ")
                is In -> output.append(" IN ")
                is IsNull -> output.append(" IS NULL ")
                is IsNotNull -> output.append(" IS NOT NULL ")
                is LeftParentheses -> output.append("(")
                is RightParentheses -> {
                    if (output.lastOrNull()?.isWhitespace() == true) {
                        output.deleteAt(output.length - 1)
                    }
                    output.append(")")
                }
                is JsonCast -> output.append("::${token.value}")
                is ParameterEnd -> output.append(", ")
                else -> throw IllegalArgumentException("Invalid token in expression - $token")
            }
        }
        return output
    }
}