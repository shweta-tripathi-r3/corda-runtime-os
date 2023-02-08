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
    val expression = ExpressionLexer.parse("SELECT field ->> property AS chosen_field_name FROM table_name WHERE field ->> property = some_value")
    println("Postgres => ${PostgresExpression.convert(expression)}")
    println("Oracle => ${OracleExpression.convert(expression)}")
    println("SQL Server => ${SqlServerExpression.convert(expression)}")

    println()

    val expression2 = ExpressionLexer.parse("SELECT field ->> 'property' AS chosen_field_name FROM table_name WHERE field ->> 'property' = 'some_value'")
    println("Postgres => ${PostgresExpression.convert(expression2)}")
    println("Oracle => ${OracleExpression.convert(expression2)}")
    println("SQL Server => ${SqlServerExpression.convert(expression2)}")

    println()

    val expression3 = ExpressionLexer.parse("SELECT name, custom ->> 'salary' AS salary FROM people WHERE custom ->> 'salary' = '10'")
    println("Postgres => ${PostgresExpression.convert(expression3)}")
    println("Oracle => ${OracleExpression.convert(expression3)}")
    println("SQL Server => ${SqlServerExpression.convert(expression3)}")

    println()

    // lowercase keywords not working?
    val expression4 = ExpressionLexer.parse("SELECT name, custom ->> 'salary' AS salary, custom ->> 'field with space' AS field FROM people WHERE custom ->> 'salary' = '10'")
    println("Postgres => ${PostgresExpression.convert(expression4)}")
    println("Oracle => ${OracleExpression.convert(expression4)}")
    println("SQL Server => ${SqlServerExpression.convert(expression4)}")
}