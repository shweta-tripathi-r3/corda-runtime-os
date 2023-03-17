package net.corda.ledger.json.expression.parsing

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PostgresVaultNamedQueryParserTest {

    private companion object {
        const val QUERY = "my query"
        val PATH_REFERENCE = PathReference("field")
    }

    private val expressionParser = mock<VaultNamedQueryExpressionParser>()
    private val expressionValidator = mock<VaultNamedQueryExpressionValidator>()
    private val postgresVaultNamedQueryParser = PostgresVaultNamedQueryParser(expressionParser, expressionValidator)

    @Test
    fun `parses query and validates it`() {
        val expression = listOf(PATH_REFERENCE)
        whenever(expressionParser.parse(QUERY)).thenReturn(expression)
        postgresVaultNamedQueryParser.parseWhereJson(QUERY)
        verify(expressionParser).parse(QUERY)
        verify(expressionValidator).validateWhereJson(QUERY, expression)
    }

    @Test
    fun `PathReference is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("field")
    }

    @Test
    fun `PathReferenceWithSpace is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PathReferenceWithSpaces("'field name'")))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("'field name'")
    }

    @Test
    fun `Number is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(Number("1")))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("1")
    }

    @Test
    fun `JsonArrayOrObjectAsText is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(
            listOf(
                PATH_REFERENCE,
                JsonArrayOrObjectAsText(),
                PATH_REFERENCE
            )
        )
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} ->> ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `Select is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, Select(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} SELECT ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `As is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, As(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} AS ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `From is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, From(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} FROM ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `Where is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, Where(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} WHERE ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `And is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, And(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} AND ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `Or is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, Or(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} OR ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `Equals is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, Equals(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} = ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `NotEquals is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, NotEquals(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} != ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `GreaterThan is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, GreaterThan(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} > ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `GreaterThanEquals is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, GreaterThanEquals(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} >= ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `LessThan is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, LessThan(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} < ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `LessThanEquals is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, LessThanEquals(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} <= ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `In is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, In(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} IN ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `IsNull is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, IsNull(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} IS NULL ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `IsNotNull is appended to the output with a space on either side`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(PATH_REFERENCE, IsNotNull(), PATH_REFERENCE))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("${PATH_REFERENCE.ref} IS NOT NULL ${PATH_REFERENCE.ref}")
    }

    @Test
    fun `LeftParentheses is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(LeftParentheses()))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("(")
    }

    @Test
    fun `RightParentheses is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(RightParentheses()))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo(")")
    }

    @Test
    fun `JsonCast is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(JsonCast("int")))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("::int")
    }

    @Test
    fun `ParameterEnd is appended directly to the output with no spaces`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(ParameterEnd()))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo(",")
    }

    @Test
    fun `repeated spaces and leading and trailing whitespace are not included in the output`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(Select(), From(), Where(), IsNotNull()))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("SELECT FROM WHERE IS NOT NULL")
    }

    @Test
    fun `unexpected token throws an exception`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(listOf(object : Token {}))
        assertThatThrownBy { postgresVaultNamedQueryParser.parseWhereJson(QUERY) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `empty expressions return an empty output`() {
        whenever(expressionParser.parse(QUERY)).thenReturn(emptyList())
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("")
    }
}