package net.corda.ledger.json.expression.parsing

import net.corda.v5.base.types.toHexString
import java.lang.Math.E
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import kotlin.math.*

fun ByteArray.toHexString(): String = printHexBinary(this)

private val hexCode = "0123456789ABCDEF".toCharArray()
private fun printHexBinary(data: ByteArray): String {
    val r = StringBuilder(data.size * 2)
    for (b in data) {
        r.append(hexCode[(b.toInt() shr 4) and 0xF])
        r.append(hexCode[b.toInt() and 0xF])
    }
    return r.toString()
}

class MinFunction : FunctionToken {
    companion object {
        const val symbol: String = "min"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Int, ValueType.Int),
        listOf(ValueType.Long, ValueType.Long),
        listOf(ValueType.Double, ValueType.Double),
        listOf(ValueType.Decimal, ValueType.Decimal),
        listOf(ValueType.Instant, ValueType.Instant),
        listOf(ValueType.LocalDate, ValueType.LocalDate),
        listOf(ValueType.String, ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 2) {
            "Min only implemented for two inputs"
        }
        if (inputs[0] is IntValue && inputs[1] is IntValue) {
            return IntValue(min(inputs[0].value as Int, inputs[1].value as Int))
        }
        if (inputs[0] is LongValue && inputs[1] is LongValue) {
            return LongValue(min(inputs[0].value as Long, inputs[1].value as Long))
        }
        if (inputs[0] is DoubleValue && inputs[1] is DoubleValue) {
            return DoubleValue(min(inputs[0].value as Double, inputs[1].value as Double))
        }
        if (inputs[0] is DecimalValue && inputs[1] is DecimalValue) {
            val left = inputs[0].value as BigDecimal
            val right = inputs[1].value as BigDecimal
            return if (left <= right) {
                DecimalValue(left)
            } else {
                DecimalValue(right)
            }
        }
        if (inputs[0] is InstantValue && inputs[1] is InstantValue) {
            val left = inputs[0].value as Instant
            val right = inputs[1].value as Instant
            return if (left <= right) {
                InstantValue(left)
            } else {
                InstantValue(right)
            }
        }
        if (inputs[0] is LocalDateValue && inputs[1] is LocalDateValue) {
            val left = inputs[0].value as LocalDate
            val right = inputs[1].value as LocalDate
            return if (left <= right) {
                LocalDateValue(left)
            } else {
                LocalDateValue(right)
            }
        }
        if (inputs[0] is StringValue && inputs[1] is StringValue) {
            val left = inputs[0].value as String
            val right = inputs[1].value as String
            return if (left <= right) {
                StringValue(left)
            } else {
                StringValue(right)
            }
        }
        if (inputs[0] is ByteArrayValue && inputs[1] is ByteArrayValue) {
            val left = inputs[0].value as ByteArray
            val right = inputs[1].value as ByteArray
            return if (left.toHexString() <= right.toHexString()) {
                ByteArrayValue(left)
            } else {
                ByteArrayValue(right)
            }
        }
        throw IllegalArgumentException("Invalid inputs to min $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is MinFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = MinFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class MaxFunction : FunctionToken {
    companion object {
        const val symbol: String = "max"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Int, ValueType.Int),
        listOf(ValueType.Long, ValueType.Long),
        listOf(ValueType.Double, ValueType.Double),
        listOf(ValueType.Decimal, ValueType.Decimal),
        listOf(ValueType.Instant, ValueType.Instant),
        listOf(ValueType.LocalDate, ValueType.LocalDate),
        listOf(ValueType.String, ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 2) {
            "Max only implemented for two inputs"
        }
        if (inputs[0] is IntValue && inputs[1] is IntValue) {
            return IntValue(max(inputs[0].value as Int, inputs[1].value as Int))
        }
        if (inputs[0] is LongValue && inputs[1] is LongValue) {
            return LongValue(max(inputs[0].value as Long, inputs[1].value as Long))
        }
        if (inputs[0] is DoubleValue && inputs[1] is DoubleValue) {
            return DoubleValue(max(inputs[0].value as Double, inputs[1].value as Double))
        }
        if (inputs[0] is DecimalValue && inputs[1] is DecimalValue) {
            val left = inputs[0].value as BigDecimal
            val right = inputs[1].value as BigDecimal
            return if (left >= right) {
                DecimalValue(left)
            } else {
                DecimalValue(right)
            }
        }
        if (inputs[0] is InstantValue && inputs[1] is InstantValue) {
            val left = inputs[0].value as Instant
            val right = inputs[1].value as Instant
            return if (left >= right) {
                InstantValue(left)
            } else {
                InstantValue(right)
            }
        }
        if (inputs[0] is LocalDateValue && inputs[1] is LocalDateValue) {
            val left = inputs[0].value as LocalDate
            val right = inputs[1].value as LocalDate
            return if (left >= right) {
                LocalDateValue(left)
            } else {
                LocalDateValue(right)
            }
        }
        if (inputs[0] is StringValue && inputs[1] is StringValue) {
            val left = inputs[0].value as String
            val right = inputs[1].value as String
            return if (left >= right) {
                StringValue(left)
            } else {
                StringValue(right)
            }
        }
        if (inputs[0] is ByteArrayValue && inputs[1] is ByteArrayValue) {
            val left = inputs[0].value as ByteArray
            val right = inputs[1].value as ByteArray
            return if (left.toHexString() >= right.toHexString()) {
                ByteArrayValue(left)
            } else {
                ByteArrayValue(right)
            }
        }
        throw IllegalArgumentException("Invalid inputs to max $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is MaxFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = MaxFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class AbsFunction : FunctionToken {
    companion object {
        const val symbol: String = "abs"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "abs only implemented for one input"
        }
        if (inputs[0] is IntValue) {
            return IntValue(abs(inputs[0].value as Int))
        }
        if (inputs[0] is LongValue) {
            return LongValue(abs(inputs[0].value as Long))
        }
        if (inputs[0] is DoubleValue) {
            return DoubleValue(abs(inputs[0].value as Double))
        }
        if (inputs[0] is DecimalValue) {
            return DecimalValue((inputs[0].value as BigDecimal).abs())
        }
        throw IllegalArgumentException("Invalid inputs to abs $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is AbsFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = AbsFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class SignFunction : FunctionToken {
    companion object {
        const val symbol: String = "sign"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "sign only implemented for one input"
        }
        if (inputs[0] is IntValue) {
            return IntValue((inputs[0].value as Int).sign)
        }
        if (inputs[0] is LongValue) {
            return IntValue((inputs[0].value as Long).sign)
        }
        if (inputs[0] is DoubleValue) {
            return IntValue((inputs[0].value as Double).sign.toInt())
        }
        if (inputs[0] is DecimalValue) {
            return IntValue((inputs[0].value as BigDecimal).signum())
        }
        throw IllegalArgumentException("Invalid inputs to sign $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is SignFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = SignFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class IntConversionFunction : FunctionToken {
    companion object {
        const val symbol: String = "toInt"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Boolean),
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal),
        listOf(ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "int conversion only implemented for one input"
        }
        if (inputs[0] is BooleanValue) {
            return IntValue(if (inputs[0].value as Boolean) 1 else 0)
        }
        if (inputs[0] is IntValue) {
            return inputs[0]
        }
        if (inputs[0] is LongValue) {
            return IntValue((inputs[0].value as Long).toInt())
        }
        if (inputs[0] is DoubleValue) {
            return IntValue((inputs[0].value as Double).toInt())
        }
        if (inputs[0] is DecimalValue) {
            return IntValue((inputs[0].value as BigDecimal).toInt())
        }
        if (inputs[0] is StringValue) {
            return IntValue((inputs[0].value as String).toInt())
        }
        throw IllegalArgumentException("Invalid inputs to min $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is IntConversionFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = IntConversionFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class LongConversionFunction : FunctionToken {
    companion object {
        const val symbol: String = "toLong"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Boolean),
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal),
        listOf(ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "long conversion only implemented for one input"
        }
        if (inputs[0] is BooleanValue) {
            return LongValue(if (inputs[0].value as Boolean) 1L else 0L)
        }
        if (inputs[0] is IntValue) {
            return LongValue((inputs[0].value as Int).toLong())
        }
        if (inputs[0] is LongValue) {
            return inputs[0]
        }
        if (inputs[0] is DoubleValue) {
            return LongValue((inputs[0].value as Double).toLong())
        }
        if (inputs[0] is DecimalValue) {
            return LongValue((inputs[0].value as BigDecimal).toLong())
        }
        if (inputs[0] is StringValue) {
            return LongValue((inputs[0].value as String).toLong())
        }
        throw IllegalArgumentException("Invalid inputs to long $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is LongConversionFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = LongConversionFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class DoubleConversionFunction : FunctionToken {
    companion object {
        const val symbol: String = "toDouble"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Boolean),
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal),
        listOf(ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "double conversion only implemented for one input"
        }
        if (inputs[0] is BooleanValue) {
            return DoubleValue(if (inputs[0].value as Boolean) 1.0 else 0.0)
        }
        if (inputs[0] is IntValue) {
            return DoubleValue((inputs[0].value as Int).toDouble())
        }
        if (inputs[0] is LongValue) {
            return DoubleValue((inputs[0].value as Long).toDouble())
        }
        if (inputs[0] is DoubleValue) {
            return inputs[0]
        }
        if (inputs[0] is DecimalValue) {
            return DoubleValue((inputs[0].value as BigDecimal).toDouble())
        }
        if (inputs[0] is StringValue) {
            return DoubleValue((inputs[0].value as String).toDouble())
        }
        throw IllegalArgumentException("Invalid inputs to double $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is DoubleConversionFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = DoubleConversionFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class DecimalConversionFunction : FunctionToken {
    companion object {
        const val symbol: String = "toDecimal"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Boolean),
        listOf(ValueType.Int),
        listOf(ValueType.Long),
        listOf(ValueType.Double),
        listOf(ValueType.Decimal),
        listOf(ValueType.String)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "decimal conversion only implemented for one input"
        }
        if (inputs[0] is BooleanValue) {
            return DecimalValue(if (inputs[0].value as Boolean) BigDecimal.ONE else BigDecimal.ZERO)
        }
        if (inputs[0] is IntValue) {
            return DecimalValue((inputs[0].value as Int).toBigDecimal())
        }
        if (inputs[0] is LongValue) {
            return DecimalValue((inputs[0].value as Long).toBigDecimal())
        }
        if (inputs[0] is DoubleValue) {
            return DecimalValue((inputs[0].value as Double).toBigDecimal())
        }
        if (inputs[0] is DecimalValue) {
            return inputs[0]
        }
        if (inputs[0] is StringValue) {
            return DecimalValue((inputs[0].value as String).toBigDecimal())
        }
        throw IllegalArgumentException("Invalid inputs to decimal $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is DecimalConversionFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = DecimalConversionFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class SqrtFunction : FunctionToken {
    companion object {
        const val symbol: String = "sqrt"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Double)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "sqrt only implemented for one input"
        }
        if (inputs[0] is DoubleValue) {
            return DoubleValue(sqrt(inputs[0].value as Double))
        }
        throw IllegalArgumentException("Invalid inputs to sqrt $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is SqrtFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = SqrtFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class PowFunction : FunctionToken {
    companion object {
        const val symbol: String = "pow"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Double, ValueType.Double)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 2) {
            "pow only implemented for two inputs"
        }
        if (inputs[0] is DoubleValue && inputs[1] is DoubleValue) {
            return DoubleValue((inputs[0].value as Double).pow(inputs[1].value as Double))
        }
        throw IllegalArgumentException("Invalid inputs to pow $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is PowFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = PowFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class LnFunction : FunctionToken {
    companion object {
        const val symbol: String = "ln"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Double)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "ln only implemented for one input"
        }
        if (inputs[0] is DoubleValue) {
            return DoubleValue(ln(inputs[0].value as Double))
        }
        throw IllegalArgumentException("Invalid inputs to ln $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is LnFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = LnFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class Log10Function : FunctionToken {
    companion object {
        const val symbol: String = "log10"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Double)
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 1) {
            "log10 only implemented for one input"
        }
        if (inputs[0] is DoubleValue) {
            return DoubleValue(log10(inputs[0].value as Double))
        }
        throw IllegalArgumentException("Invalid inputs to log10 $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is Log10Function
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = Log10Function()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class EFunction : FunctionToken {
    companion object {
        const val symbol: String = "ConstE"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf()
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.isEmpty()) {
            "ConstE only implemented for zero inputs"
        }
        return DoubleValue(E)
    }

    override fun equals(other: Any?): Boolean {
        return other is EFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = EFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class PiFunction : FunctionToken {
    companion object {
        const val symbol: String = "ConstPi"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf()
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.isEmpty()) {
            "ConstPi only implemented for zero inputs"
        }
        return DoubleValue(PI)
    }

    override fun equals(other: Any?): Boolean {
        return other is PiFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = PiFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}

class RoundFunction : FunctionToken {
    companion object {
        const val symbol: String = "round"
    }

    override var parentOffset: Int = -1
    override var paramCount: Int = 0
    override val inputTypes: Set<List<ValueType>> = setOf(
        listOf(ValueType.Double, ValueType.Int, ValueType.String),
        listOf(ValueType.Decimal, ValueType.Int, ValueType.String),
    )

    override fun evaluate(inputs: List<Value>): Value {
        require(inputs.size == 3) {
            "round only implemented for three inputs: value(Double/BigDecimal), decimal places, mode UP/DOWN/CEILING/FLOOR/HALF_UP/HALF_DOWN/HALF_EVEN"
        }
        val value = inputs[0].value
        val decimalPlaces = inputs[1].value as Int
        val roundingMode = inputs[2].value as String
        val rounding = when (roundingMode.lowercase()) {
            "down" -> RoundingMode.DOWN
            "up" -> RoundingMode.UP
            "ceiling" -> RoundingMode.CEILING
            "floor" -> RoundingMode.FLOOR
            "half_down" -> RoundingMode.HALF_DOWN
            "half_up" -> RoundingMode.HALF_UP
            "half_even" -> RoundingMode.HALF_EVEN
            else -> throw IllegalArgumentException("Invalid rounding mode to round $roundingMode")
        }
        if (value is BigDecimal) {
            return DecimalValue(value.setScale(decimalPlaces, rounding))
        }
        if (value is Double) {
            return DoubleValue(BigDecimal(value.toString()).setScale(decimalPlaces, rounding).toDouble())
        }
        throw IllegalArgumentException("Invalid inputs to round $inputs")
    }

    override fun equals(other: Any?): Boolean {
        return other is RoundFunction
    }

    override fun hashCode(): Int {
        return symbol.hashCode()
    }

    override fun copy(): Token {
        val newCopy = RoundFunction()
        newCopy.paramCount = paramCount
        return newCopy
    }
}