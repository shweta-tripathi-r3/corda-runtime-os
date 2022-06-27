package net.corda.ledger.consensual

import net.corda.v5.application.flows.FlowEngine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class ConsensualLedgerServiceImplTest {
    @Test
    fun `Test basic behaviour`() {
        val service = ConsensualLedgerServiceImpl(mock(FlowEngine::class.java))
        val res = service.double(4)
        assertEquals(8, res)
    }
}
