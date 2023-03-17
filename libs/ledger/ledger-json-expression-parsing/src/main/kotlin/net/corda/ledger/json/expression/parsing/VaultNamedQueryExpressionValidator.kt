package net.corda.ledger.json.expression.parsing

interface VaultNamedQueryExpressionValidator {

    fun validateWhereJson(query: String, expression: List<Token>)
}

class VaultNamedQueryExpressionValidatorImpl : VaultNamedQueryExpressionValidator {

    override fun validateWhereJson(query: String, expression: List<Token>) {
        for (token in expression) {
            when (token) {
                is Select -> throw exception(query, "SELECT")
                is From -> throw exception(query, "FROM")
            }
        }
    }

    private fun exception(query: String, keyword: String): IllegalArgumentException {
        return IllegalArgumentException("Vault named queries cannot contain the $keyword keyword. Query: $query")
    }
}