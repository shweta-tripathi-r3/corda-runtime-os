package net.corda.ledger.json.expression.parsing

object OracleExpression {

    fun convert(expression: List<Token>): String {
        val output = StringBuilder("")
        for (token in expression) {
            when (token) {
                is PathReference -> output.append(token.ref)
                is PathReferenceWithSpaces -> output.append("\"${token.ref}\"")
                is JsonArrayOrObjectAsText -> {
                    output.append(".")
                }
                is Select -> {
                    output.append("SELECT ")
                }
                is As -> {
                    output.append(" AS ")
                }
                is From -> {
                    output.append(" FROM ")
                }
                is Where -> {
                    output.append(" WHERE ")
                }
                is Equals -> {
                    output.append(" = ")
                }
                is ParameterEnd -> {
                    output.append(", ")
                }
            }
        }
        output.append(";")
        return output.toString()
    }
}