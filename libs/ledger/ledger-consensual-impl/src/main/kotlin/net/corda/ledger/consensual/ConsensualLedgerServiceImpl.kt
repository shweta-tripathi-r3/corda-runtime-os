package net.corda.ledger.consensual

import net.corda.ledger.internal.FunnyFarmService
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.receive
import net.corda.v5.application.messaging.unwrap
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import net.corda.v5.ledger.consensual.ConsensualTestRequest
import net.corda.v5.ledger.consensual.ConsensualTestResponse
import net.corda.v5.serialization.SingletonSerializeAsToken
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import org.osgi.service.component.annotations.ServiceScope


@Component(service = [ConsensualLedgerService::class, SingletonSerializeAsToken::class], scope = ServiceScope.PROTOTYPE)
class ConsensualLedgerServiceImpl @Activate constructor(
    @Reference(service = FlowEngine::class)
    val flowEngine: FlowEngine
) : ConsensualLedgerService, SingletonSerializeAsToken {

    override fun double(n: Int): Int {
        return n*2
    }


    @CordaSerializable
    class DummyPayload

    companion object {
        val log = contextLogger()
    }

    class TestFlowImpl(private val session: FlowSession) : SubFlow<String> {
        @Suspendable
        override fun call(): String {
            log.info("SystemFlow test: TestFlowImpl sending dummy")
            session.send(DummyPayload())
            log.info("SystemFlow test: TestFlowImpl awaiting reply")
            return session.receive<String>().unwrap { it }
        }
    }

    class TestFlowResponderImpl(private val params: ConsensualTestResponse) : SubFlow<Unit> {

        @CordaInject
        lateinit var funnyFarmService: FunnyFarmService

        @Suspendable
        override fun call() {
            log.info("SystemFlow test: TestFlowResponder awaiting dummy")
            params.session.receive<DummyPayload>()
            log.info("SystemFlow test: TestFlowResponder sending response")
            params.session.send("${params.createResponse()} - service says ${funnyFarmService.sayMoo()}")
            log.info("SystemFlow test: TestFlowResponder returning")
        }
    }

    @Suspendable
    override fun receiveTestFlow(request: ConsensualTestResponse) {
        log.info("SystemFlow test: receiveTestFlow called")
        flowEngine.subFlow(TestFlowResponderImpl(request))
        log.info("SystemFlow test: receiveTestFlow returning")
    }

    @Suspendable
    override fun startTestFlow(request: ConsensualTestRequest): String {
        log.info("SystemFlow test: startTestFlow called")
        val result = flowEngine.subFlow(TestFlowImpl(request.session))
        log.info("SystemFlow test: startTestFlow returning")
        return result
    }
}