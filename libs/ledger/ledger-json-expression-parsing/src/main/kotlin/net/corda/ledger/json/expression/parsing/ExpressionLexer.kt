package net.corda.ledger.json.expression.parsing

import java.time.Instant

object ExpressionLexer {
    private val longPattern = Regex(
        """(?<long>[0-9][0-9]*L)"""
    )

    private val numberPattern = Regex(
        """(?<num>[0-9][0-9]*(\.[0-9][0-9]*)?([eE]-?[0-9][0-9]*)?)"""
    )

    private val booleanPattern = Regex(
        """(?<bool>true|false)"""
    )

    private val stringPattern = Regex(
        """(?<str>('[^']*)'|("[^"]*)")"""
    )

    private val datePattern = Regex(
        """(?<date>([1-9]\d{3}-((0[1-9]|1[0-2])-(0[1-9]|1\d|2[0-8])|(0[13-9]|1[0-2])-(29|30)|(0[13578]|1[02])-31)|([1-9]\d(0[48]|[2468][048]|[13579][26])|([2468][048]|[13579][26])00)-02-29)T([01]\d|2[0-3]):[0-5]\d:[0-5]\d(\.\d+|.{0})(Z|[+-][01]\d:[0-5]\d))"""
    )

    private val pathPattern = Regex(
        """(?<path>(\$?[a-zA-Z_][a-zA-Z0-9_]*(\[([0-9]+|"[a-zA-Z_][a-zA-Z0-9_]*")])?)(\.[a-zA-Z_][a-zA-Z0-9_]*(\[([0-9]+|"[a-zA-Z_][a-zA-Z0-9_]*")])?)*)"""
    )

    private val opsPattern = Regex(
        """(?<op>(->>)|[+-/*=]|&&|\|\||<(=)?|>(=)?|==|!(=)?|%|rem|\b(AS|as)\b|\b(FROM|from)\b|\b(SELECT|select)\b|\b(WHERE|where)\b)"""
    )

    fun parse(input: String): List<Token> {
        val outputTokens = mutableListOf<Token>()
        var index = 0
        while (index < input.length) {
            if (input[index] == ' ') {
                ++index
                continue
            }
            val dateMatch = datePattern.find(input, index)
            if (dateMatch != null && dateMatch.range.first == index) {
                val date = dateMatch.groups["date"]
                if (date != null) {
                    outputTokens += InstantValue(Instant.parse(date.value))
                    index = dateMatch.range.last + 1
                    continue
                }
            }
            val booleanMatch = booleanPattern.find(input, index)
            if (booleanMatch != null && booleanMatch.range.first == index) {
                val choice = booleanMatch.groups["bool"]
                if (choice != null) {
                    outputTokens += BooleanValue(choice.value == "true")
                    index = choice.range.last + 1
                    continue
                }
            }
            val longMatch = longPattern.find(input, index)
            if (longMatch != null && longMatch.range.first == index) {
                val long = longMatch.groups["long"]
                if (long != null) {
                    outputTokens += LongValue(long.value.substring(0, long.value.length - 1).toLong())
                    index = longMatch.range.last + 1
                    continue
                }
            }
            val numMatch = numberPattern.find(input, index)
            if (numMatch != null && numMatch.range.first == index) {
                val num = numMatch.groups["num"]
                if (num != null) {
                    if (num.value.contains(".") || num.value.contains("e") || num.value.contains("E")) {
                        outputTokens += DoubleValue(num.value.toDouble())
                    } else {
                        outputTokens += IntValue(num.value.toInt())
                    }
                    index = numMatch.range.last + 1
                    continue
                }
            }
            val strMatch = stringPattern.find(input, index)
            if (strMatch != null && strMatch.range.first == index) {
                val str = strMatch.groups["str"]
                if (str != null) {
                    outputTokens += when {
                        " " !in str.value -> PathReference(str.value)
                        else -> PathReferenceWithSpaces(str.value)
                    }
                    index = strMatch.range.last + 1
                    continue
                }
            }
            if (input[index] == '(') {
                outputTokens += LeftParentheses()
                ++index
                continue
            }
            if (input[index] == ')') {
                outputTokens += RightParentheses()
                ++index
                continue
            }
            if (input[index] == ',') {
                outputTokens += ParameterEnd()
                ++index
                continue
            }
            val opsMatch = opsPattern.find(input, index)
            if (opsMatch != null && opsMatch.range.first == index) {
                val ops = opsMatch.groups["op"]
                if (ops != null) {
                    outputTokens += operatorFactory(ops.value)
                    index = opsMatch.range.last + 1
                    continue
                }
            }
            val pathMatch = pathPattern.find(input, index)
            if (pathMatch != null && pathMatch.range.first == index) {
                val path = pathMatch.groups["path"]
                if (path != null) {
                    val function = functionFactory(path.value)
                    if (function != null) {
                        outputTokens += function
                    } else {
                        outputTokens += PathReference(path.value)
                    }
                    index = pathMatch.range.last + 1
                    continue
                }
            }
            throw IllegalArgumentException("Unexpected input $input (${input[index]})")
        }
        for (i in outputTokens.indices) {
            if (outputTokens[i] is MinusOperator) {
                if ((i == 0)
                    || (i < outputTokens.size - 1 && outputTokens[i - 1] is Operator && outputTokens[i + 1] is Value)
                    || (i < outputTokens.size - 1 && outputTokens[i + 1] is LeftParentheses)
                ) {
                    outputTokens[i] = UnaryMinusOperator()
                }
            }
            if (outputTokens[i] is PlusOperator) {
                if ((i == 0)
                    || (i < outputTokens.size - 1 && outputTokens[i - 1] is Operator && outputTokens[i + 1] is Value)
                    || (i < outputTokens.size - 1 && outputTokens[i + 1] is LeftParentheses)
                ) {
                    outputTokens[i] = UnaryPlusOperator()
                }
            }
        }
        return outputTokens
    }
}