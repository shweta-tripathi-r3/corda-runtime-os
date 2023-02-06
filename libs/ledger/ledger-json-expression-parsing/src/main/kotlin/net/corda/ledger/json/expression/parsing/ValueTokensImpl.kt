package net.corda.ledger.json.expression.parsing

import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate

data class BooleanValue(override val value: Boolean) : Value {
    override val valueType: ValueType = ValueType.Boolean
    override var parentOffset: Int = -1

    override fun copy(): Token = BooleanValue(value)
}

data class IntValue(override val value: Int) : Value {
    override val valueType: ValueType = ValueType.Int
    override var parentOffset: Int = -1

    override fun copy(): Token = IntValue(value)
}

data class LongValue(override val value: Long) : Value {
    override val valueType: ValueType = ValueType.Long
    override var parentOffset: Int = -1

    override fun copy(): Token = LongValue(value)
}

class DoubleValue(override val value: Double) : Value {
    override val valueType: ValueType = ValueType.Double
    override var parentOffset: Int = -1
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as DoubleValue

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "DoubleValue(value=$value)"
    }

    override fun copy(): Token = DoubleValue(value)
}

data class DecimalValue(override val value: BigDecimal) : Value {
    override val valueType: ValueType = ValueType.Decimal
    override var parentOffset: Int = -1

    override fun copy(): Token = DecimalValue(value)
}

data class StringValue(override val value: String) : Value {
    override val valueType: ValueType = ValueType.String
    override var parentOffset: Int = -1

    override fun copy(): Token = StringValue(value)
}

class ByteArrayValue(override val value: ByteArray) : Value {
    override val valueType: ValueType = ValueType.Bytes
    override var parentOffset: Int = -1

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ByteArrayValue

        if (!value.contentEquals(other.value)) return false

        return true
    }

    override fun hashCode(): Int {
        return value.contentHashCode()
    }

    override fun toString(): String = "ByteArrayValue(value=${value.toHexString()})"

    override fun copy(): Token = ByteArrayValue(value)

}

data class LocalDateValue(override val value: LocalDate) : Value {
    override val valueType: ValueType = ValueType.LocalDate
    override var parentOffset: Int = -1

    override fun copy(): Token = LocalDateValue(value)
}

data class InstantValue(override val value: Instant) : Value {
    override val valueType: ValueType = ValueType.Instant
    override var parentOffset: Int = -1

    override fun copy(): Token = InstantValue(value)
}

fun valueFactory(value: Any): Value {
    return when (value) {
        is Boolean -> BooleanValue(value)
        is Int -> IntValue(value)
        is Long -> LongValue(value)
        is Double -> DoubleValue(value)
        is BigDecimal -> DecimalValue(value)
        is String -> StringValue(value)
        is Instant -> InstantValue(value)
        is LocalDate -> LocalDateValue(value)
        is ByteArray -> ByteArrayValue(value)
        is ByteBuffer -> {
            val bytes = ByteArray(value.remaining())
            value.get(bytes)
            ByteArrayValue(bytes)
        }
        else -> throw IllegalArgumentException("Unhandled type for $value")
    }
}