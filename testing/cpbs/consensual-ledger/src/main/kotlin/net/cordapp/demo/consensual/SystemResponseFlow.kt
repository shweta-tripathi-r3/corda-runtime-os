package net.cordapp.demo.consensual

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import net.corda.v5.ledger.consensual.ConsensualTestResponse

@InitiatedBy(protocol = "SystemTestFlow")
class SystemResponseFlow : ResponderFlow {

    companion object{
        val log = contextLogger()
    }

    @CordaInject
    lateinit var ledger: ConsensualLedgerService

    override fun call(session: FlowSession) {
        log.info("SystemFlow test: Responder Flow called")
        val request = object : ConsensualTestResponse {
            override val session = session

            override fun createResponse(): String {
                return "Ledger test response"
            }
        }

        log.info("SystemFlow test: Responder calling systemflow")
        ledger.receiveTestFlow(request)
        log.info("SystemFlow test: Responder returning")
    }
}