package net.corda.ledger.json.expression.parsing

import java.math.BigDecimal

class PlusOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 3
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is IntValue && right is IntValue) {
            return IntValue(left.value + right.value)
        }
        if (left is LongValue && right is LongValue) {
            return LongValue(left.value + right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.value + right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return DecimalValue(left.value + right.value)
        }
        if (left is StringValue && right is StringValue) {
            return StringValue(left.value + right.value)
        }
        if (left is ByteArrayValue && right is ByteArrayValue) {
            return ByteArrayValue(left.value + right.value)
        }
        throw IllegalArgumentException("Unable to add $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is PlusOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = PlusOperator()
}

class UnaryPlusOperator : UnaryOperator {
    override val associativity: Associativity = Associativity.Right
    override val precedence: Int = 1
    override var parentOffset: Int = -1

    override fun evaluate(input: Value): Value {
        require(
            input is IntValue
                    || input is LongValue
                    || input is DecimalValue
                    || input is DoubleValue
        ) {
            "Unary plus not applicable to $input"
        }
        return input
    }

    override fun equals(other: Any?): Boolean {
        return other is UnaryPlusOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = UnaryPlusOperator()
}

class MinusOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 3
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is IntValue && right is IntValue) {
            return IntValue(left.value - right.value)
        }
        if (left is LongValue && right is LongValue) {
            return LongValue(left.value - right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.value - right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return DecimalValue(left.value - right.value)
        }
        throw IllegalArgumentException("Unable to subtract $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is MinusOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = MinusOperator()
}

class UnaryMinusOperator : UnaryOperator {
    override val associativity: Associativity = Associativity.Right
    override val precedence: Int = 1
    override var parentOffset: Int = -1

    override fun evaluate(input: Value): Value {
        if (input is IntValue) {
            return IntValue(-input.value)
        }
        if (input is LongValue) {
            return LongValue(-input.value)
        }
        if (input is DoubleValue) {
            return DoubleValue(-input.value)
        }
        if (input is DecimalValue) {
            return DecimalValue(-input.value)
        }
        throw IllegalArgumentException("Unary minus not applicable to $input")
    }

    override fun equals(other: Any?): Boolean {
        return other is UnaryMinusOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = UnaryMinusOperator()
}

class MultiplyOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 2
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is IntValue && right is IntValue) {
            return IntValue(left.value * right.value)
        }
        if (left is LongValue && right is LongValue) {
            return LongValue(left.value * right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.value * right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return DecimalValue(left.value * right.value)
        }
        throw IllegalArgumentException("Unable to multiply $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is MultiplyOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = MultiplyOperator()
}

class DivideOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 2
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is IntValue && right is IntValue) {
            return IntValue(left.value / right.value)
        }
        if (left is LongValue && right is LongValue) {
            return LongValue(left.value / right.value)
        }
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.value / right.value)
        }
        if (left is DecimalValue && right is DecimalValue) {
            return DecimalValue(left.value / right.value)
        }
        throw IllegalArgumentException("Unable to divide $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is DivideOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = DivideOperator()
}

//Java version of % operator
class ModOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 2
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is IntValue && right is IntValue) {
            return IntValue(left.value.mod(right.value))
        }
        if (left is LongValue && right is LongValue) {
            return LongValue(left.value.mod(right.value))
        }
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.value.mod(right.value))
        }
        if (left is DecimalValue && right is DecimalValue) {
            val r = left.value.rem(right.value)
            return if (r != BigDecimal.ZERO && r.signum() != right.value.signum()) DecimalValue(r + right.value) else DecimalValue(
                r
            )
        }
        throw IllegalArgumentException("Unable to mod $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is ModOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = ModOperator()
}

// Kotlin version of % operator
class RemOperator : BinaryOperator {
    override val associativity: Associativity = Associativity.Left
    override val precedence: Int = 2
    override var parentOffset: Int = -1

    override fun evaluate(left: Value, right: Value): Value {
        if (left is IntValue && right is IntValue) {
            return IntValue(left.value.rem(right.value))
        }
        if (left is LongValue && right is LongValue) {
            return LongValue(left.value.rem(right.value))
        }
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.value.rem(right.value))
        }
        if (left is DecimalValue && right is DecimalValue) {
            return DecimalValue(left.value.rem(right.value))
        }
        throw IllegalArgumentException("Unable to rem $left and $right")
    }

    override fun equals(other: Any?): Boolean {
        return other is RemOperator
    }

    override fun hashCode(): Int {
        return precedence
    }

    override fun copy(): Token = RemOperator()
}