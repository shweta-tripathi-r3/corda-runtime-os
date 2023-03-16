package net.corda.ledger.json.expression.parsing

interface VaultCustomQueryParser {

    fun parse(query: String): String
}