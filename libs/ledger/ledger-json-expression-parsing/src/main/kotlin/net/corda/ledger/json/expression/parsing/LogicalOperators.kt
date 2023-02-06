package net.corda.ledger.json.expression.parsing

class EqualsOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 6
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.contentEquals(right.value))
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value == right.value) // This ensures IEEE behaviour for NaN
        }
        return BooleanValue(left.value == right.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is EqualsOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = EqualsOperator()
}

class NotEqualsOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 6
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(!left.value.contentEquals(right.value))
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value != right.value) // This ensures IEEE behaviour for NaN
        }
        return BooleanValue(left.value != right.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is NotEqualsOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = NotEqualsOperator()
}

class LogicalAndOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 10
    override var parentOffset: Int = -1
    var secondLeg = false

    override fun evaluate(left: Value, right: Value): Value {
        if (left is BooleanValue && right is BooleanValue) {
            return BooleanValue(left.value && right.value)
        }
        throw IllegalArgumentException("Logic and not applicable to $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is LogicalAndOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = LogicalAndOperator()
}

class LogicalOrOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 11
    override var parentOffset: Int = -1
    var secondLeg = false

    override fun evaluate(left: Value, right: Value): Value {
        if (left is BooleanValue && right is BooleanValue) {
            return BooleanValue(left.value || right.value)
        }
        throw IllegalArgumentException("Logic or not applicable to $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is LogicalOrOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = LogicalOrOperator()
}

class LogicalNotOperator : UnaryOperator {
    override val associativity: Associativity = Associativity.Right
    override val precedence: Int = 1
    override var parentOffset: Int = -1

    override fun evaluate(input: Value): Value {
        if (input is BooleanValue) {
            return BooleanValue(!input.value)
        }
        throw IllegalArgumentException("Logic invert not applicable to $input")
    }

    override fun equals(other: Any?): Boolean {
        return other is LogicalNotOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = LogicalNotOperator()
}

class IfFunction : FunctionToken {
    companion object {
        const val symbol: String = "if"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    var ifChoice: Boolean? = null
    val inputOffsets = mutableListOf<Int>()
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Boolean, ValueType.Boolean, ValueType.Boolean),
        listOf(ValueType.Boolean, ValueType.Int, ValueType.Int),
        listOf(ValueType.Boolean, ValueType.Long, ValueType.Long),
        listOf(ValueType.Boolean, ValueType.Double, ValueType.Double),
        listOf(ValueType.Boolean, ValueType.Decimal, ValueType.Decimal),
        listOf(ValueType.Boolean, ValueType.String, ValueType.String),
        listOf(ValueType.Boolean, ValueType.Bytes, ValueType.Bytes),
        listOf(ValueType.Boolean, ValueType.Instant, ValueType.Instant),
        listOf(ValueType.Boolean, ValueType.AvroRecord, ValueType.AvroRecord)
    )

    override fun evaluate(inputs: List<Value>): Value {
        throw IllegalArgumentException("If function managed in evaluator logic")
    }

    override fun equals(other: Any?): Boolean {
        return other is IfFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = IfFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class JsonArrayOrObjectAsText : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 6
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.contentEquals(right.value))
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value == right.value) // This ensures IEEE behaviour for NaN
        }
        return BooleanValue(left.value == right.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is EqualsOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = EqualsOperator()
}

class JsonAsNamedField : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 6
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.contentEquals(right.value))
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value == right.value) // This ensures IEEE behaviour for NaN
        }
        return BooleanValue(left.value == right.value)
    }

    override fun equals(other: Any?): Boolean {
        return other is EqualsOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = EqualsOperator()
}