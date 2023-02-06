package net.corda.ledger.json.expression.parsing

enum class Associativity {
    Left,
    Right
}

enum class ValueType {
    Boolean,
    Int,
    Long,
    Double,
    Decimal,
    String,
    Bytes,
    Instant,
    LocalDate,
    AvroRecord
}

interface Token {
    var parentOffset: Int
    fun copy(): Token
}

interface Value : Token {
    val valueType: ValueType
    val value: Any
}

interface Reference : Token {
    val ref: String
}

interface Operator : Token {
    val associativity: Associativity
    val precedence: Int
}

interface UnaryOperator : Operator {
    fun evaluate(input: Value): Value
}

interface BinaryOperator : Operator {
    fun evaluate(left: Value, right: Value): Value
}

interface FunctionToken : Token {
    val inputTypes: Set<List<ValueType>>
    var paramCount: Int
    fun evaluate(inputs: List<Value>): Value
}