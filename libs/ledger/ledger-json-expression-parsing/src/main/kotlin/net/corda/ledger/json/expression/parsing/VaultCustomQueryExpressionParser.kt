package net.corda.ledger.json.expression.parsing

interface VaultCustomQueryExpressionParser {

    fun parse(query: String): List<Token>
}

class PostgresVaultCustomQueryExpressionParser : VaultCustomQueryExpressionParser {
    private val stringPattern = Regex(
        """(?<str>('[^']*)'|("[^"]*)")"""
    )

    private val pathPattern = Regex(
        """(?<path>(\$?[a-zA-Z_][a-zA-Z0-9_]*(\[([0-9]+|"[a-zA-Z_][a-zA-Z0-9_]*")])?)(\.[a-zA-Z_][a-zA-Z0-9_]*(\[([0-9]+|"[a-zA-Z_][a-zA-Z0-9_]*")])?)*)"""
    )

    private val numberPattern = Regex(
        """(?<num>[0-9]+(\.[0-9]+)?([eE]-?[0-9]+)?)"""
    )

    // like? need to keep % in that case
    private val opsPattern = Regex(
        """(?<op>(->>)|[+-/*=]|&&|\|\||<(=)?|>(=)?|==|!(=)?|%|rem|(?i)\bas\b|(?i)\bfrom\b|(?i)\bselect\b|(?i)\bwhere\b|(?i)\band\b|(?i)\bor\b|(?i)\bis null\b|(?i)\bis not null\b|(?i)\bin\b)"""
    )

    private val jsonCastPattern = Regex("""(?<cast>::\S+)""")

    override fun parse(query: String): List<Token> {
        val outputTokens = mutableListOf<Token>()
        var index = 0
        while (index < query.length) {
            if (query[index].isWhitespace() || query[index] == '\n') {
                ++index
                continue
            }
            val strMatch = stringPattern.find(query, index)
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
            if (query[index] == '(') {
                outputTokens += LeftParentheses()
                ++index
                continue
            }
            if (query[index] == ')') {
                outputTokens += RightParentheses()
                ++index
                continue
            }
            if (query[index] == ',') {
                outputTokens += ParameterEnd()
                ++index
                continue
            }
            val opsMatch = opsPattern.find(query, index)
            if (opsMatch != null && opsMatch.range.first == index) {
                val ops = opsMatch.groups["op"]
                if (ops != null) {
                    outputTokens += operatorFactory(ops.value)
                    index = opsMatch.range.last + 1
                    continue
                }
            }
            val jsonCastMatch = jsonCastPattern.find(query, index)
            if (jsonCastMatch != null && jsonCastMatch.range.first == index) {
                val cast = jsonCastMatch.groups["cast"]
                if (cast != null) {
                    outputTokens += JsonCast(cast.value.removePrefix("::"))
                    index = jsonCastMatch.range.last + 1
                    continue
                }
            }
            val pathMatch = pathPattern.find(query, index)
            if (pathMatch != null && pathMatch.range.first == index) {
                val path = pathMatch.groups["path"]
                if (path != null) {
                    outputTokens += PathReference(path.value)
                    index = pathMatch.range.last + 1
                    continue
                }
            }
            val numberMatch = numberPattern.find(query, index)
            if (numberMatch != null && numberMatch.range.first == index) {
                val number = numberMatch.groups["num"]
                if (number != null) {
                    outputTokens += Number(number.value)
                    index = numberMatch.range.last + 1
                    continue
                }
            }
            throw IllegalArgumentException("Unexpected input index = $index, value (${query[index]}), $query")
        }
        return outputTokens
    }
}