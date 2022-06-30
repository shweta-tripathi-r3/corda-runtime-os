package net.cordapp.demo.consensual

import net.corda.application.impl.services.json.JsonMarshallingServiceImpl
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class TestSystemTestFlow {

    @Test
    fun testRunningFlow(){
        val marshallingService : JsonMarshallingService = JsonMarshallingServiceImpl()

        val flow = SystemTestFlow()

        flow.marshallingService = marshallingService

        flow.ledger = mock(ConsensualLedgerService::class.java).also {
            whenever(it.startTestFlow(any())).thenReturn("teststring")
        }

        flow.messaging = mock(FlowMessaging::class.java).also {
            whenever(it.initiateFlow(any())).thenReturn(mock(FlowSession::class.java))
        }

        val request = "{\"counterParty\": \"O=Alice, L=London, C=GB\"}"


        val result = flow.call(RPCRequestDataTestImpl(request))

        Assertions.assertEquals("{\"response\":\"teststring\"}", result)

    }

}