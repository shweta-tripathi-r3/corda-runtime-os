package net.corda.ledger.json.expression.parsing

object PostgresExpression {

    fun convert(expression: List<Token>): String {
        val output = StringBuilder("")
        for (token in expression) {
            when (token) {
                is PathReference -> output.append(token.ref)
                is JsonArrayOrObjectAsText -> {
                    output.append(" ->> ")
                }
                is JsonSelect -> {
                    output.append("SELECT ")
                }
                is JsonAsNamedField -> {
                    output.append(" AS ")
                }
                is JsonFrom -> {
                    output.append(" FROM ")
                }
                is JsonWhere -> {
                    output.append(" WHERE ")
                }
                is JsonEqualTo -> {
                    output.append(" = ")
                }
            }
        }
        output.append(";")
        return output.toString()
    }
}