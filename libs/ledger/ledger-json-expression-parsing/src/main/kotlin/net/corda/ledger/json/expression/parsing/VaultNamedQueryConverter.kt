package net.corda.ledger.json.expression.parsing

interface VaultNamedQueryConverter {

    fun convert(output: StringBuilder, expression: List<Token>): StringBuilder
}