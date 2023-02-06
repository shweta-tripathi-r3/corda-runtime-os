package net.corda.ledger.json.expression.parsing

class GreaterThanOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 5
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is BooleanValue && right is BooleanValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is IntValue && right is IntValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is LongValue && right is LongValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is StringValue && right is StringValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is InstantValue && right is InstantValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is LocalDateValue && right is LocalDateValue) {
            return BooleanValue(left.value > right.value)
        }
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.toHexString() > right.value.toHexString())
        }
        throw IllegalArgumentException("Unable to compare $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is GreaterThanOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = GreaterThanOperator()
}

class GreaterThanEqualsOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 5
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is BooleanValue && right is BooleanValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is IntValue && right is IntValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is LongValue && right is LongValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is StringValue && right is StringValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is InstantValue && right is InstantValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is LocalDateValue && right is LocalDateValue) {
            return BooleanValue(left.value >= right.value)
        }
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.toHexString() >= right.value.toHexString())
        }
        throw IllegalArgumentException("Unable to compare $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is GreaterThanEqualsOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = GreaterThanEqualsOperator()
}

class LessThanOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 5
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is BooleanValue && right is BooleanValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is IntValue && right is IntValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is LongValue && right is LongValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is StringValue && right is StringValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is InstantValue && right is InstantValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is LocalDateValue && right is LocalDateValue) {
            return BooleanValue(left.value < right.value)
        }
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.toHexString() < right.value.toHexString())
        }
        throw IllegalArgumentException("Unable to compare $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is LessThanOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = LessThanOperator()
}

class LessThanEqualsOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 5
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is BooleanValue && right is BooleanValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is IntValue && right is IntValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is LongValue && right is LongValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is StringValue && right is StringValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is InstantValue && right is InstantValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is LocalDateValue && right is LocalDateValue) {
            return BooleanValue(left.value <= right.value)
        }
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return BooleanValue(left.value.toHexString() <= right.value.toHexString())
        }
        throw IllegalArgumentException("Unable to compare $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is LessThanEqualsOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = LessThanEqualsOperator()
}