package net.corda.ledger.json.expression.parsing

interface VaultNamedQueryParser {

    fun parse(query: String): String
}