package net.corda.ledger.json.expression.parsing

class VaultNamedQueryParserImpl(
    private val expressionParser: VaultNamedQueryExpressionParser,
    private val expressionValidator: VaultNamedQueryExpressionValidator,
    private val converter: VaultNamedQueryConverter
) : VaultNamedQueryParser {

    override fun parseWhereJson(query: String): String {
        val expression = expressionParser.parse(query)
        expressionValidator.validateWhereJson(query, expression)
        return converter.convert(StringBuilder(""), expression)
            .toString()
            .replace("  ", " ")
            .trim()
    }
}