package net.corda.ledger.json.expression.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class PostgresVaultNamedQueryParserIntegrationTest {

    // Need to support strings with no spaces between some keywords
    private companion object {
        @JvmStatic
        fun inputsToOutputs(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("WHERE non_json_column = 'some_value'", "WHERE non_json_column = 'some_value'"),
                Arguments.of("WHERE field ->> property = 'some_value'", "WHERE field ->> property = 'some_value'"),
                Arguments.of("WHERE field ->> property = :value", "WHERE field ->> property = :value"),
                Arguments.of(
                    "WHERE \"field name\" ->> \"json property\" = 'some_value'",
                    "WHERE \"field name\" ->> \"json property\" = 'some_value'"
                ),
                Arguments.of("WHERE (field ->> property)::int = 5", "WHERE (field ->> property)::int = 5"),
                Arguments.of("WHERE (field ->> property)::int != 5", "WHERE (field ->> property)::int != 5"),
                Arguments.of("WHERE (field ->> property)::int < 5", "WHERE (field ->> property)::int < 5"),
                Arguments.of("WHERE (field ->> property)::int <= 5", "WHERE (field ->> property)::int <= 5"),
                Arguments.of("WHERE (field ->> property)::int > 5", "WHERE (field ->> property)::int > 5"),
                Arguments.of("WHERE (field ->> property)::int >= 5", "WHERE (field ->> property)::int >= 5"),
                Arguments.of("WHERE (field ->> property)::int <= :value", "WHERE (field ->> property)::int <= :value"),
                Arguments.of("WHERE (field ->> property)::int = 1234.5678900", "WHERE (field ->> property)::int = 1234.5678900"),

                Arguments.of(
                    "WHERE field ->> property = 'some_value' AND field ->> property2 = 'another value'",
                    "WHERE field ->> property = 'some_value' AND field ->> property2 = 'another value'"
                ),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' OR field ->> property2 = 'another value'",
                    "WHERE field ->> property = 'some_value' OR field ->> property2 = 'another value'"
                ),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' AND field ->> property2 = 'another value' OR field ->> property3 = 'third property'",
                    "WHERE field ->> property = 'some_value' AND field ->> property2 = 'another value' OR field ->> property3 = 'third property'"
                ),
                Arguments.of(
                    "WHERE (field ->> property = 'some_value' AND field ->> property2 = 'another value') OR field ->> property3 = 'third property'",
                    "WHERE (field ->> property = 'some_value' AND field ->> property2 = 'another value') OR field ->> property3 = 'third property'"
                ),
                Arguments.of(
                    "WHERE field ->> property = 'some_value' AND (field ->> property2 = 'another value') OR field ->> property3 = 'third property')",
                    "WHERE field ->> property = 'some_value' AND (field ->> property2 = 'another value') OR field ->> property3 = 'third property')"
                ),
                Arguments.of(
                    "WHERE (field ->> property = 'some_value' AND (field ->> property2 = 'another value') OR field ->> property3 = 'third property'))",
                    "WHERE (field ->> property = 'some_value' AND (field ->> property2 = 'another value') OR field ->> property3 = 'third property'))"
                ),
                Arguments.of("WHERE field ->> property IS NULL", "WHERE field ->> property IS NULL"),
                Arguments.of("WHERE field ->> property IS NOT NULL", "WHERE field ->> property IS NOT NULL"),
                Arguments.of(
                    "WHERE field ->> property IN ('asd', 'fields value', 'asd')",
                    "WHERE field ->> property IN ('asd', 'fields value', 'asd')"
                ),
                Arguments.of(
                    "WHERE (field ->> property IN ('asd', 'fields value', 'asd') AND field ->> property2 = 'another value')",
                    "WHERE (field ->> property IN ('asd', 'fields value', 'asd') AND field ->> property2 = 'another value')"
                ),
                Arguments.of(
                    """
                        where
                            ("custom"->>'salary'='10'
                            and (custom ->> 'salary')::int>9.00000000
                            or custom ->> 'field with space' is null)
                    """,
                    "WHERE (\"custom\" ->> 'salary' = '10' AND (custom ->> 'salary')::int > 9.00000000 OR custom ->> 'field with space' IS NULL)"
                ),
                Arguments.of(
                    """WHERE custom ->> 'TestUtxoState.testField' = :testField
                        |AND custom ->> 'Corda.participants' IN :participants
                        |AND custom?:contractStateType
                        |AND created > :created""".trimMargin(),
                    "WHERE custom ->> 'TestUtxoState.testField' = :testField AND custom ->> 'Corda.participants' IN :participants AND custom ? :contractStateType AND created > :created"
                )
            )
        }
    }

    @ParameterizedTest
    @MethodSource("inputsToOutputs")
    fun `queries are parsed from a postgres query and output back into a postgres query`(input: String, output: String) {
        val vaultNamedQueryParser = VaultNamedQueryParserImpl(
            PostgresVaultNamedQueryExpressionParser(),
            VaultNamedQueryExpressionValidatorImpl(),
            PostgresVaultNamedQueryConverter()
        )
        assertThat(vaultNamedQueryParser.parseWhereJson(input)).isEqualTo(output)
    }


//    @Test
//    fun `query with a single WHERE clause`() {
//
//    }
//
//    @Test
//    fun `query with a single WHERE clause and a parameter`() {
//
//    }
//
//    @Test
//    fun `query with an AND`() {
//
//    }
//
//    @Test
//    fun `query with multiple ANDs`() {
//
//    }
//
//    @Test
//    fun `query with an OR`() {
//
//    }
//
//    @Test
//    fun `query

}