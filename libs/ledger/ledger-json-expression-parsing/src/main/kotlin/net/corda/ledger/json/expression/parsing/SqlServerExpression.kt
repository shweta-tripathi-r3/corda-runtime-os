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
                is JsonArrayOrObjectAsText -> {
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(second = true)
                }
                is JsonSelect -> {
                    output.append("SELECT ")
                }
                is JsonAsNamedField -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" AS ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is JsonFrom -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" FROM ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is JsonWhere -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" WHERE ")
                    jsonArrayOrObjectAsTextComponents = jsonArrayOrObjectAsTextComponents.copy(first = null)
                }
                is JsonEqualTo -> {
                    if (jsonArrayOrObjectAsTextComponents.first != null) {
                        output.append(jsonArrayOrObjectAsTextComponents.first)
                    }
                    output.append(" = ")
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