package net.corda.ledger.json.expression.parsing

interface VaultNamedQueryParser {

    fun parseWhereJson(query: String): String
}