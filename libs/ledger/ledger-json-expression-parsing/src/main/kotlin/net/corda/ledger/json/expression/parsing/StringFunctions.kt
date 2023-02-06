package net.corda.ledger.json.expression.parsing

import java.math.BigDecimal
import java.time.Instant
import java.util.*

class SubstringFunction : FunctionToken {
    companion object {
        const val symbol: String = "substring"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.String, ValueType.Int, ValueType.Int)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 3) {
            "Substring only implemented for three inputs"
        }
        if (inputs[0] is StringValue && inputs[1] is IntValue && inputs[2] is IntValue) {
            val str = inputs[0].value as String
            val start = inputs[1].value as Int
            val endExclusive = inputs[2].value as Int
            return StringValue(str.substring(start, endExclusive))
        }
        throw IllegalArgumentException("Invalid inputs to min $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is SubstringFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = SubstringFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class CopyOfRangeFunction : FunctionToken {
    companion object {
        const val symbol: String = "copyOfRange"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Bytes, ValueType.Int, ValueType.Int)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 3) {
            "Substring only implemented for three inputs"
        }
        if (inputs[0] is ByteArrayValue && inputs[1] is IntValue && inputs[2] is IntValue) {
            val bytes = inputs[0].value as ByteArray
            val start = inputs[1].value as Int
            require(start >= 0 && start <= bytes.size) {
                "Invalid start Index $start"
            }
            val endExclusive = inputs[2].value as Int
            require(endExclusive >= 0 && endExclusive <= bytes.size) {
                "Invalid end Index $endExclusive"
            }
            return ByteArrayValue(bytes.copyOfRange(start, endExclusive))
        }
        throw IllegalArgumentException("Invalid inputs to min $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is CopyOfRangeFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = CopyOfRangeFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class IndexOfFunction : FunctionToken {
    companion object {
        const val symbol: String = "indexOf"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.String, ValueType.String),
        listOf(ValueType.String, ValueType.String, ValueType.Int),
        listOf(ValueType.Bytes, ValueType.Bytes),
        listOf(ValueType.Bytes, ValueType.Bytes, ValueType.Int)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 2 || inputs.size == 3) {
            "Invalid inputs"
        }
        if (inputs[0] is StringValue && inputs[1] is StringValue) {
            val str = inputs[0].value as String
            val search = inputs[1].value as String
            return if (inputs.size == 2) {
                IntValue(str.indexOf(search))
            } else {
                val startIndex = inputs[2].value as Int
                IntValue(str.indexOf(search, startIndex))
            }
        }
        if (inputs[0] is ByteArrayValue && inputs[1] is ByteArrayValue) {
            val bytes = inputs[0].value as ByteArray
            val search = inputs[1].value as ByteArray
            var offset = if (inputs.size == 2) 0 else inputs[2].value as Int
            if (offset < 0) {
                offset = 0 // mirror logic in Java String.indexOf
            }
            if (offset >= bytes.size) {
                return if (search.isEmpty()) IntValue(bytes.size) else IntValue(-1) // mirror logic in Java String.indexOf
            }
            var matched = 0
            while (offset < bytes.size) {
                if (offset + matched < bytes.size
                    && bytes[offset + matched] == search[matched]
                ) {
                    ++matched
                    if (matched >= search.size) {
                        return IntValue(offset)
                    }
                } else {
                    matched = 0
                    ++offset
                }
            }
            return IntValue(-1)
        }
        throw IllegalArgumentException("Invalid inputs to min $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is IndexOfFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = IndexOfFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class LengthFunction : FunctionToken {
    companion object {
        const val symbol: String = "length"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.String),
        listOf(ValueType.Bytes)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "Invalid inputs"
        }
        if (inputs[0] is StringValue) {
            val str = inputs[0].value as String
            return IntValue(str.length)
        }
        if (inputs[0] is ByteArrayValue) {
            val bytes = inputs[0].value as ByteArray
            return IntValue(bytes.size)
        }
        throw IllegalArgumentException("Length only applies to strings and byte arrays")
    }

    override fun equals(other: Any?): Boolean {
        return other is LengthFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = LengthFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class UpperFunction : FunctionToken {
    companion object {
        const val symbol: String = "upper"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "Invalid inputs"
        }
        if (inputs[0] is StringValue) {
            return StringValue((inputs[0].value as String).uppercase(Locale.US))
        }
        throw IllegalArgumentException("Upper only applies to strings")
    }

    override fun equals(other: Any?): Boolean {
        return other is UpperFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = UpperFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class LowerFunction : FunctionToken {
    companion object {
        const val symbol: String = "lower"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "Invalid inputs"
        }
        if (inputs[0] is StringValue) {
            return StringValue((inputs[0].value as String).lowercase(Locale.US))
        }
        throw IllegalArgumentException("lower only applies to strings")
    }

    override fun equals(other: Any?): Boolean {
        return other is LowerFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = LowerFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}


class StringConversionFunction : FunctionToken {
    companion object {
        const val symbol: String = "toString"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Boolean),
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal),
        listOf(ValueType.String),
        listOf(ValueType.Instant)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "string conversion only implemented for one input"
        }
        if (inputs[0] is BooleanValue) {
            return StringValue((inputs[0].value as Boolean).toString())
        }
        if (inputs[0] is IntValue) {
            return StringValue((inputs[0].value as Int).toString())
        }
        if (inputs[0] is LongValue) {
            return StringValue((inputs[0].value as Long).toString())
        }
        if (inputs[0] is DoubleValue) {
            return StringValue((inputs[0].value as Double).toString())
        }
        if (inputs[0] is DecimalValue) {
            return StringValue((inputs[0].value as BigDecimal).toString())
        }
        if (inputs[0] is StringValue) {
            return inputs[0]
        }
        if (inputs[0] is InstantValue) {
            return StringValue((inputs[0].value as Instant).toString())
        }
        throw IllegalArgumentException("Invalid inputs to string $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is StringConversionFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = StringConversionFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}