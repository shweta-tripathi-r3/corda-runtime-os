package net.cordapp.demo.consensual

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import net.corda.v5.ledger.consensual.ConsensualTestRequest


@InitiatingFlow(protocol = "SystemTestFlow")
class SystemTestFlow : RPCStartableFlow {
    data class SystemTestRequest(val counterParty: String)
    data class SystemTestResponse(val response: String)

    @CordaInject
    lateinit var marshallingService: JsonMarshallingService

    @CordaInject
    lateinit var messaging: FlowMessaging

    @CordaInject
    lateinit var ledger: ConsensualLedgerService

    companion object {
        val log = contextLogger()
    }

    override fun call(requestBody: RPCRequestData): String {
        log.info("SystemFlow test: Initiating Flow called - parsing out counterparty")
        val counterPartyString = requestBody.getRequestBodyAs<SystemTestRequest>(marshallingService).counterParty
        val counterParty = MemberX500Name.parse(counterPartyString)

        log.info("SystemFlow test: Initiating Flow initiating flow session")
        val session = messaging.initiateFlow(counterParty)

        log.info("SystemFlow test: Initiating flow calling system flow")
        val result = ledger.startTestFlow(ConsensualTestRequest(session))

        log.info("SystemFlow test: Initiating flow marshalling result and returning")
        return marshallingService.format(SystemTestResponse(result))
    }
}