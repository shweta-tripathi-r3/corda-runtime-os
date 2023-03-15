package net.corda.ledger.json.expression.parsing

object SqlServerExpression {

    fun convert(expression: List<Token>): String {
        val output = StringBuilder("")
        var jsonArrayOrObjectAsTextComponents: Pair<String?, Boolean> = null to false
        for (token in expression) {
            when (token) {
                is PathReference -> {
                    if (jsonArrayOrObjectAsTextComponents.first == null) {
                        jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = token.ref)
                    } else if (jsonArrayOrObjectAsTextComponents.second) {
                        output.append("JSON_VALUE(${jsonArrayOrObjectAsTextComponents.first}, '$.${token.ref.trim('\'')}')")
                        jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null, second = false)
                    }
                }
                is PathReferenceWithSpaces -> {
                    if (jsonArrayOrObjectAsTextComponents.first == null) {
                        jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = token.ref)
                    } else if (jsonArrayOrObjectAsTextComponents.second) {
                        output.append("JSON_VALUE(${jsonArrayOrObjectAsTextComponents.first}, '$.\"${token.ref.trim('\'')}\"')")
                        jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null, second = false)
                    }
                }
                is JsonArrayOrObjectAsText -> {
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(second = true)
                }
                is Select -> {
                    output.append("SELECT ")
                }
                is As -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" AS ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is From -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" FROM ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is Where -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" WHERE ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is Equals -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" = ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is ParameterEnd -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(", ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
            }
        }
        if (jsonArrayOrObjectAsTextComponents.first != null) {
            output.append(jsonArrayOrObjectAsTextComponents.first)
        }
        return output.toString()
    }
}