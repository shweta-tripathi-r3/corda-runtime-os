package net.corda.ledger.json.expression.parsing

object ExpressionLexer {
    private val stringPattern = Regex(
        """(?<str>('[^']*)'|("[^"]*)")"""
    )

    private val pathPattern = Regex(
        """(?<path>(\$?[a-zA-Z_][a-zA-Z0-9_]*(\[([0-9]+|"[a-zA-Z_][a-zA-Z0-9_]*")])?)(\.[a-zA-Z_][a-zA-Z0-9_]*(\[([0-9]+|"[a-zA-Z_][a-zA-Z0-9_]*")])?)*)"""
    )

    private val opsPattern = Regex(
        """(?<op>(->>)|[+-/*=]|&&|\|\||<(=)?|>(=)?|==|!(=)?|%|rem|\b(AS|as)\b|\b(FROM|from)\b|\b(SELECT|select)\b|\b(WHERE|where)\b|\b(AND|and)\b|\b(OR|or)\b|\b(IS NULL|is null)\b|\b(IS NOT NULL|is not null)\b)"""
    )

    private val jsonCastPattern = Regex("""(?<cast>::\S+)""")

    fun parse(input: String): List<Token> {
        val outputTokens = mutableListOf<Token>()
        var index = 0
        while (index < input.length) {
            if (input[index] == ' ') {
                ++index
                continue
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
            val jsonCastMatch = jsonCastPattern.find(input, index)
            if (jsonCastMatch != null && jsonCastMatch.range.first == index) {
                val cast = jsonCastMatch.groups["cast"]
                if (cast != null) {
                    outputTokens += JsonCast(cast.value.removePrefix("::"))
                    index = jsonCastMatch.range.last + 1
                    continue
                }
            }
            val pathMatch = pathPattern.find(input, index)
            if (pathMatch != null && pathMatch.range.first == index) {
                val path = pathMatch.groups["path"]
                if (path != null) {
                    outputTokens += PathReference(path.value)
                    index = pathMatch.range.last + 1
                    continue
                }
            }
            throw IllegalArgumentException("Unexpected input $input (${input[index]})")
        }
        return outputTokens
    }
}