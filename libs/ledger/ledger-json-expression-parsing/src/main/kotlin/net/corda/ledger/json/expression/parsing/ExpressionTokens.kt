package net.corda.ledger.json.expression.parsing

interface Token

interface Reference : Token {
    val ref: String
}

interface Operator : Token