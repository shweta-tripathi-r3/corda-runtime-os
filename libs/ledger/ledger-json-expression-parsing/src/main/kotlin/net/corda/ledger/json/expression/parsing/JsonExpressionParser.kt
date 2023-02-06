package net.corda.ledger.json.expression.parsing

class JsonExpressionParser {
}

class Scanner {


//    fun tokenize(string: String) {
//        var s = ""
//
//        var index = 0
//        while (index < string.length) {
//            s += string[index]
//            s = s.trim()
//            val peek = string.getOrNull(index + 1)
//            if (s.toin)
//            index +=1
//        }
//    }
}

fun main() {
//    println(ExpressionLexer.parse("(1+21)*2+(332-4)/20"))
    println(ExpressionLexer.parse("SELECT field ->> property AS chosen_field_name FROM table_name"))
}