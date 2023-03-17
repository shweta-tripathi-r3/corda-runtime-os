package net.corda.ledger.json.expression.parsing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.lang.StringBuilder

class VaultNamedQueryParserImplTest {

    private companion object {
        const val QUERY = "my query"
        val PATH_REFERENCE = PathReference("field")
    }

    private val expressionParser = mock<VaultNamedQueryExpressionParser>()
    private val expressionValidator = mock<VaultNamedQueryExpressionValidator>()
    private val converter = mock<VaultNamedQueryConverter>()
    private val postgresVaultNamedQueryParser = VaultNamedQueryParserImpl(expressionParser, expressionValidator, converter)

    @Test
    fun `parses query and validates it`() {
        val expression = listOf(PATH_REFERENCE)
        val output = "output"
        whenever(expressionParser.parse(QUERY)).thenReturn(expression)
        whenever(converter.convert(any(), any())).thenReturn(StringBuilder(output))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo(output)
        verify(expressionParser).parse(QUERY)
        verify(expressionValidator).validateWhereJson(QUERY, expression)
    }

    @Test
    fun `repeated spaces and leading and trailing whitespace are not included in the output`() {
        whenever(expressionParser.parse(any())).thenReturn(listOf())
        whenever(converter.convert(any(), any())).thenReturn(StringBuilder(" SELECT  FROM  WHERE  IS NOT NULL "))
        assertThat(postgresVaultNamedQueryParser.parseWhereJson(QUERY)).isEqualTo("SELECT FROM WHERE IS NOT NULL")
    }
}