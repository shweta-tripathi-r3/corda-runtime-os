package net.corda.simulator.runtime.flows

import net.corda.simulator.SimulatorConfiguration
import net.corda.simulator.exceptions.NoProtocolAnnotationException
import net.corda.simulator.runtime.ledger.SimConsensualLedgerService
import net.corda.simulator.runtime.messaging.ConcurrentFlowMessaging
import net.corda.simulator.runtime.messaging.FlowContext
import net.corda.simulator.runtime.messaging.SimFiber
import net.corda.simulator.runtime.signing.SimKeyStore
import net.corda.simulator.runtime.signing.SimWithJsonSignatureVerificationService
import net.corda.simulator.runtime.tools.SimpleJsonMarshallingService
import net.corda.simulator.runtime.utils.injectIfRequired
import net.corda.v5.application.crypto.DigitalSignatureVerificationService
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.flows.Flow
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.consensual.ConsensualLedgerService

/**
 * Injector for default services for Simulator.
 */
class DefaultServicesInjector(private val configuration: SimulatorConfiguration) : FlowServicesInjector {
    private companion object {
        val log = contextLogger()
    }

    /**
     * Injects sensible default services into the provided flow. Currently injects:<br>
     * <ul>
     *   <li>JsonMarshallingService
     *   <li>FlowEngine
     *   <li>FlowMessaging
     * </ul>
     * As with the real Corda, injected service properties must be marked with the @CordaInject annotation.
     *
     * @flow The flow to inject services into
     * @member The name of the "virtual node"
     * @protocolLookUp The "fiber" through which flow messaging will look up peers
     * @flowFactory A factory for constructing flows
     */
    override fun injectServices(
        flow: Flow,
        member: MemberX500Name,
        fiber: SimFiber,
        flowFactory: FlowFactory,
        keyStore: SimKeyStore
    ) {
        log.info("Injecting services into ${flow.javaClass} for \"$member\"")
        flow.injectIfRequired(JsonMarshallingService::class.java) { createJsonMarshallingService() }
        flow.injectIfRequired(FlowEngine::class.java) { createFlowEngine(configuration, member, fiber) }
        flow.injectIfRequired(FlowMessaging::class.java) {
            createFlowMessaging(configuration, flow, member, fiber, flowFactory)
        }
        flow.injectIfRequired(MemberLookup::class.java) { getOrCreateMemberLookup(member, fiber) }
        flow.injectIfRequired(SigningService::class.java) {
            getOrCreateSigningService(member, fiber)
        }
        flow.injectIfRequired(DigitalSignatureVerificationService::class.java) { createVerificationService() }
        flow.injectIfRequired(PersistenceService::class.java) { getOrCreatePersistenceService(member, fiber) }
        flow.injectIfRequired(ConsensualLedgerService::class.java) {
            createConsensualLedgerService(
                getOrCreateSigningService(member, fiber),
                getOrCreateMemberLookup(member, fiber),
                createVerificationService()
            )
        }
    }

    private fun createConsensualLedgerService(
        signingService: SigningService,
        memberLookup: MemberLookup,
        verificationService: DigitalSignatureVerificationService
    ): ConsensualLedgerService {
        log.info("Injecting ${ConsensualLedgerService::class.java.simpleName}")
        return SimConsensualLedgerService(signingService, memberLookup, verificationService)
    }

    private fun createVerificationService(): DigitalSignatureVerificationService {
        log.info("Injecting ${DigitalSignatureVerificationService::class.java.simpleName}")
        return SimWithJsonSignatureVerificationService()
    }

//    private fun getOrCreateSigningService(
//        jsonMarshallingService: JsonMarshallingService,
//        keyStore: SimKeyStore
//    ): SigningService {
//        log.info("Injecting ${SigningService::class.java.simpleName}")
//        return SimWithJsonSigningService(jsonMarshallingService, keyStore)
//    }

    private fun getOrCreateSigningService(
        member: MemberX500Name,
        fiber: SimFiber
    ): SigningService {
        log.info("Injecting ${SigningService::class.java.simpleName}")
        return fiber.getOrCreateSigningService(member)
    }

    private fun getOrCreateMemberLookup(member: MemberX500Name, fiber: SimFiber): MemberLookup {
        log.info("Injecting ${MemberLookup::class.java.simpleName}")
        return fiber.createMemberLookup(member)
    }

    private fun getOrCreatePersistenceService(member: MemberX500Name, fiber: SimFiber): PersistenceService  {
        log.info("Injecting ${PersistenceService::class.java.simpleName}")
        return fiber.getOrCreatePersistenceService(member)
    }

    private fun createJsonMarshallingService() : JsonMarshallingService {
        log.info("Injecting ${JsonMarshallingService::class.java.simpleName}")
        return SimpleJsonMarshallingService()
    }

    private fun createFlowEngine(
        configuration: SimulatorConfiguration,
        member: MemberX500Name,
        fiber: SimFiber
    ): FlowEngine {
        log.info("Injecting ${FlowEngine::class.java.simpleName}")
        return InjectingFlowEngine(configuration, member, fiber)
    }

    private fun createFlowMessaging(
        configuration: SimulatorConfiguration,
        flow: Flow,
        member: MemberX500Name,
        fiber: SimFiber,
        flowFactory: FlowFactory
    ): FlowMessaging {

        log.info("Injecting ${FlowMessaging::class.java.simpleName}")
        val instanceFlowMap = fiber.lookUpInitiatorInstance(member)
        val protocol: String
        if(instanceFlowMap ==null || instanceFlowMap[flow] == null) {
            val flowClass = flow.javaClass
            protocol = flowClass.getAnnotation(InitiatingFlow::class.java)?.protocol
                ?: flowClass.getAnnotation(InitiatedBy::class.java)?.protocol
                        ?: throw NoProtocolAnnotationException(flowClass)

        }else{
            protocol = instanceFlowMap[flow]!!
        }
        return ConcurrentFlowMessaging(
            FlowContext(configuration, member, protocol),
            fiber,
            this,
            flowFactory
        )
    }
}

