package net.corda.ledger.json.expression.parsing

data class PathReference(override val ref: String) : Reference {
    override var parentOffset: Int = -1

    override fun copy(): Token = PathReference(ref)
}

class LeftParentheses : Token {
    var paramCount = 1
    override var parentOffset: Int = -1

    override fun equals(other: Any?): Boolean {
        return other is LeftParentheses
    }

    override fun hashCode(): Int {
        return 0
    }

    override fun copy(): Token = LeftParentheses()
}

class RightParentheses : Token {
    override var parentOffset: Int = -1

    override fun equals(other: Any?): Boolean {
        return other is RightParentheses
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun copy(): Token = RightParentheses()
}

class ParameterEnd : Token {
    override var parentOffset: Int = -1

    override fun equals(other: Any?): Boolean {
        return other is ParameterEnd
    }

    override fun hashCode(): Int {
        return 2
    }

    override fun copy(): Token = ParameterEnd()
}

// Note I have taken operator precedence values from
// https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html
// with 0 at top of table and 13 at bottom
fun operatorFactory(op: String): Operator {
    return when (op) {
        "+" -> PlusOperator() // unary version needs context
        "-" -> MinusOperator() // unary version needs context
        "*" -> MultiplyOperator()
        "/" -> DivideOperator()
        "%" -> ModOperator() // choose Java version of % operator
        "rem" -> RemOperator() // the Kotlin version of % operator
        "||" -> LogicalOrOperator()
        "&&" -> LogicalAndOperator()
        "==" -> EqualsOperator()
        "!=" -> NotEqualsOperator()
        ">" -> GreaterThanOperator()
        ">=" -> GreaterThanEqualsOperator()
        "<" -> LessThanOperator()
        "<=" -> LessThanEqualsOperator()
        "!" -> LogicalNotOperator()
        "->>" -> JsonArrayOrObjectAsText()
        "AS" -> JsonAsNamedField()
        "FROM" -> JsonFrom()
        "SELECT" -> JsonSelect()
        "WHERE" -> JsonWhere()
        "=" -> JsonEqualTo()
        else -> throw IllegalArgumentException("Unknown operator $op")
    }
}

fun functionFactory(functionName: String): FunctionToken? {
    return when (functionName) {
        MinFunction.symbol -> MinFunction()
        MaxFunction.symbol -> MaxFunction()
        AbsFunction.symbol -> AbsFunction()
        SignFunction.symbol -> SignFunction()
        SqrtFunction.symbol -> SqrtFunction()
        PowFunction.symbol -> PowFunction()
        LnFunction.symbol -> LnFunction()
        Log10Function.symbol -> Log10Function()
        EFunction.symbol -> EFunction()
        PiFunction.symbol -> PiFunction()
        RoundFunction.symbol -> RoundFunction()
        IntConversionFunction.symbol -> IntConversionFunction()
        LongConversionFunction.symbol -> LongConversionFunction()
        DoubleConversionFunction.symbol -> DoubleConversionFunction()
        DecimalConversionFunction.symbol -> DecimalConversionFunction()
        SubstringFunction.symbol -> SubstringFunction()
        CopyOfRangeFunction.symbol -> CopyOfRangeFunction()
        IndexOfFunction.symbol -> IndexOfFunction()
        LengthFunction.symbol -> LengthFunction()
        UpperFunction.symbol -> UpperFunction()
        LowerFunction.symbol -> LowerFunction()
        StringConversionFunction.symbol -> StringConversionFunction()
//        IfFunction.symbol -> IfFunction()
//        SecureHashFunction.symbol -> SecureHashFunction()
//        CheckSignatureFunction.symbol -> CheckSignatureFunction()
//        YearFunction.symbol -> YearFunction()
//        MonthFunction.symbol -> MonthFunction()
//        DayOfMonthFunction.symbol -> DayOfMonthFunction()
//        DayOfWeekFunction.symbol -> DayOfWeekFunction()
//        HourFunction.symbol -> HourFunction()
//        MinuteFunction.symbol -> MinuteFunction()
//        SecondFunction.symbol -> SecondFunction()
//        EpochSecondsFunction.symbol -> EpochSecondsFunction()
//        UTCDateTimeFunction.symbol -> UTCDateTimeFunction()
//        LocalDateFunction.symbol -> LocalDateFunction()
//        DateDiffFunction.symbol -> DateDiffFunction()
//        DateAddFunction.symbol -> DateAddFunction()
//        YearFractionFunction.symbol -> YearFractionFunction()
//        BusinessDateFromUTCFunction.symbol -> BusinessDateFromUTCFunction()
//        IsBusinessDateFunction.symbol -> IsBusinessDateFunction()
//        AdjustBusinessDateFunction.symbol -> AdjustBusinessDateFunction()
//        BusinessDaysBetweenFunction.symbol -> BusinessDaysBetweenFunction()
        else -> null
    }
}