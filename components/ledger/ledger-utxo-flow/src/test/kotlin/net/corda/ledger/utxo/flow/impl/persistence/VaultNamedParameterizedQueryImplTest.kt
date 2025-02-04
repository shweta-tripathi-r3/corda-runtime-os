package net.corda.ledger.utxo.flow.impl.persistence

import net.corda.flow.external.events.executor.ExternalEventExecutor
import net.corda.flow.persistence.query.ResultSetExecutor
import net.corda.flow.persistence.query.ResultSetFactory
import net.corda.ledger.utxo.flow.impl.persistence.external.events.VaultNamedQueryExternalEventFactory
import net.corda.utilities.days
import net.corda.utilities.time.Clock
import net.corda.v5.application.persistence.CordaPersistenceException
import net.corda.v5.application.persistence.PagedQuery.ResultSet
import net.corda.v5.base.exceptions.CordaRuntimeException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

class VaultNamedParameterizedQueryImplTest {

    private companion object {
        const val TIMESTAMP_LIMIT_PARAM_NAME = "Corda_TimestampLimit"
        val now: Instant = Instant.now().minusSeconds(10)
        val later: Instant = Instant.now().minusSeconds(10)
        val results = listOf("A", "B")
    }

    private val externalEventExecutor = mock<ExternalEventExecutor>()
    private val resultSetFactory = mock<ResultSetFactory>()
    private val resultSet = mock<ResultSet<Any>>()
    private val clock = mock<Clock>()
    private val resultSetExecutorCaptor = argumentCaptor<ResultSetExecutor<Any>>()
    private val mapCaptor = argumentCaptor<Map<String, Any>>()

    private val query = VaultNamedParameterizedQueryImpl(
        externalEventExecutor = externalEventExecutor,
        resultSetFactory = resultSetFactory,
        parameters = mutableMapOf(),
        queryName = "",
        limit = 1,
        offset = 0,
        resultClass = Any::class.java,
        clock = clock
    )

    @BeforeEach
    fun beforeEach() {
        whenever(resultSetFactory.create(mapCaptor.capture(), any(), any(), any(), resultSetExecutorCaptor.capture())).thenReturn(resultSet)
        whenever(resultSet.next()).thenReturn(results)
        whenever(clock.instant()).thenReturn(later)
    }

    @Test
    fun `setLimit updates the limit`() {
        query.execute()
        verify(resultSetFactory).create(any(), eq(1), any(), any<Class<Any>>(), any())

        query.setLimit(10)
        query.execute()
        verify(resultSetFactory).create(any(), eq(10), any(), any<Class<Any>>(), any())
    }

    @Test
    fun `setOffset updates the offset`() {
        query.execute()
        verify(resultSetFactory).create(any(), any(), eq(0), any<Class<Any>>(), any())

        query.setOffset(10)
        query.execute()
        verify(resultSetFactory).create(any(), any(), eq(10), any<Class<Any>>(), any())
    }

    @Test
    fun `setLimit cannot be negative`() {
        assertThatThrownBy { query.setLimit(-1) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `setOffset cannot be negative`() {
        assertThatThrownBy { query.setOffset(-1) }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `cannot set timestamp limit to a future date`() {
        assertThatThrownBy { query.setCreatedTimestampLimit(Instant.now().plusMillis(1.days.toMillis())) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasStackTraceContaining("Timestamp limit must not be in the future.")
    }

    @Test
    fun `setting the timestamp limit adds it to the parameters`() {
        val parameterNameOne = "one"
        val parameterOne = "param one"
        query.setParameter(parameterNameOne, parameterOne)
        query.setCreatedTimestampLimit(now)

        query.execute()
        assertThat(mapCaptor.firstValue).containsAllEntriesOf(mapOf(parameterNameOne to parameterOne, TIMESTAMP_LIMIT_PARAM_NAME to now))
    }

    @Test
    fun `execute sets the timestamp limit to now if not set when there are no other parameters`() {
        query.execute()
        verify(clock).instant()
        assertThat(mapCaptor.firstValue).containsExactlyEntriesOf(mapOf(TIMESTAMP_LIMIT_PARAM_NAME to later))
    }

    @Test
    fun `execute sets the timestamp limit to now if not set when there are other parameters`() {
        val parameterNameOne = "one"
        val parameterOne = "param one"
        query.setParameter(parameterNameOne, parameterOne)
        query.execute()
        verify(clock).instant()
        assertThat(mapCaptor.firstValue).containsExactlyEntriesOf(
            mapOf(
                parameterNameOne to parameterOne,
                TIMESTAMP_LIMIT_PARAM_NAME to later
            )
        )
    }

    @Test
    fun `setParameter sets a parameter`() {
        val parameterNameOne = "one"
        val parameterNameTwo = "two"
        val parameterOne = "param one"
        val parameterTwo = "param two"
        query.setParameter(parameterNameOne, parameterOne)
        query.setParameter(parameterNameTwo, parameterTwo)

        query.execute()
        assertThat(mapCaptor.firstValue).containsAllEntriesOf(mapOf(parameterNameOne to parameterOne, parameterNameTwo to parameterTwo))
    }

    @Test
    fun `setParameters overwrites all parameters`() {
        val parameterNameOne = "one"
        val parameterNameTwo = "two"
        val parameterNameThree = "three"
        val parameterOne = "param one"
        val parameterTwo = "param two"
        val parameterThree = "param three"
        val newParameters = mapOf(parameterNameTwo to parameterTwo, parameterNameThree to parameterThree)

        query.setParameter(parameterNameOne, parameterOne)
        query.setParameters(newParameters)

        query.execute()
        assertThat(mapCaptor.firstValue).containsAllEntriesOf(newParameters)
    }

    @Test
    fun `execute creates a result set, gets the next page and returns the result set`() {
        assertThat(query.execute()).isEqualTo(resultSet)
        verify(resultSetFactory).create(any(), any(), any(), any<Class<Any>>(), any())
        verify(resultSet).next()
    }

    @Test
    fun `rethrows CordaRuntimeExceptions as CordaPersistenceExceptions`() {
        whenever(externalEventExecutor.execute(any<Class<VaultNamedQueryExternalEventFactory>>(), any()))
            .thenThrow(CordaRuntimeException("boom"))

        query.execute()

        val resultSetExecutor = resultSetExecutorCaptor.firstValue
        assertThatThrownBy { resultSetExecutor.execute(emptyMap(), 0) }.isInstanceOf(CordaPersistenceException::class.java)
    }

    @Test
    fun `does not rethrow general exceptions as CordaPersistenceExceptions`() {
        whenever(externalEventExecutor.execute(any<Class<VaultNamedQueryExternalEventFactory>>(), any()))
            .thenThrow(IllegalStateException("boom"))

        query.execute()

        val resultSetExecutor = resultSetExecutorCaptor.firstValue
        assertThatThrownBy { resultSetExecutor.execute(emptyMap(), 0) }.isInstanceOf(IllegalStateException::class.java)
    }
}