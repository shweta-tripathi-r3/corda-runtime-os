package net.corda.ledger.json.expression.parsing

/*
Since we're only going to parse postgres sql, I need to focus on validating the input sql and that its only using the syntax that we want to allow.
 */
fun main() {
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

    // currently isn't really a valid expression
    // also not focusing on keeping oracle and sql server working in my current code, will fix them up later
    val expression4 = ExpressionLexer.parse("SELECT name, custom ->> 'salary' AS salary, custom ->> 'field with space' AS field FROM people WHERE custom ->> 'salary' = '10' AND (custom ->> 'salary')::int > '5' OR custom ->> 'field with space' IS NULL")
    println("Postgres => ${PostgresExpression.convert(expression4)}")
    println("Oracle => ${OracleExpression.convert(expression4)}")
    println("SQL Server => ${SqlServerExpression.convert(expression4)}")
}