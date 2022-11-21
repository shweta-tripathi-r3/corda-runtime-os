package com.r3.corda.notary.plugin.nonvalidating.client

import com.r3.corda.notary.plugin.common.NotaryException
import com.r3.corda.notary.plugin.common.generateRequestSignature
import com.r3.corda.notary.plugin.common.NotarisationRequest
import com.r3.corda.notary.plugin.common.NotarisationResponse
import com.r3.corda.notary.plugin.nonvalidating.api.INPUTS_GROUP
import com.r3.corda.notary.plugin.nonvalidating.api.NOTARY_GROUP
import com.r3.corda.notary.plugin.nonvalidating.api.NonValidatingNotarisationPayload
import com.r3.corda.notary.plugin.nonvalidating.api.OUTPUTS_GROUP
import com.r3.corda.notary.plugin.nonvalidating.api.REFERENCES_GROUP
import com.r3.corda.notary.plugin.nonvalidating.api.TIMEWINDOW_GROUP
import net.corda.ledger.common.flow.transaction.filtered.factory.ComponentGroupFilterParameters
import net.corda.ledger.common.flow.transaction.filtered.factory.FilteredTransactionFactory
import net.corda.ledger.utxo.flow.impl.transaction.UtxoSignedTransactionImpl
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.annotations.VisibleForTesting
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.notary.plugin.api.PluggableNotaryClientFlow
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.TimeWindow
import net.corda.v5.ledger.utxo.TransactionState
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction

/**
 * The client that is used for the non-validating notary logic. This class is very simple and uses the basic
 * send-and-receive logic and it will also initiate the server side of the non-validating notary.
 */
// TODO CORE-7292 What is the best way to define the protocol
@InitiatingFlow(protocol = "non-validating-notary")
class NonValidatingNotaryClientFlowImpl(
    private val stx: UtxoSignedTransaction,
    private val notary: Party
) : PluggableNotaryClientFlow {

    @CordaInject
    private lateinit var flowMessaging: FlowMessaging

    @CordaInject
    private lateinit var memberLookupService: MemberLookup

    @CordaInject
    private lateinit var serializationService: SerializationService

    @CordaInject
    private lateinit var signingService: SigningService

    // TODO Currently this service is not part of the public API, why?
    @CordaInject
    private lateinit var filteredTransactionFactory: FilteredTransactionFactory

    @CordaInject
    private lateinit var notaryLookup: NotaryLookup

    /**
     * Constructor used for testing to initialize the necessary services
     */
    @VisibleForTesting
    @Suppress("LongParameterList")
    internal constructor(
        stx: UtxoSignedTransaction,
        notary: Party,
        flowMessaging: FlowMessaging,
        memberLookupService: MemberLookup,
        serializationService: SerializationService,
        signingService: SigningService
    ): this(stx, notary) {
        this.flowMessaging = flowMessaging
        this.serializationService = serializationService
        this.memberLookupService = memberLookupService
        this.signingService = signingService
    }

    /**
     * The main logic of the flow is defined in this function. The execution steps are:
     * 1. Initiating flow with the notary
     * 2. Generating signature for the request payload
     * 3. Creating the payload (using concrete implementation)
     * 4. Sending the request payload to the notary
     * 5. Receiving the response from the notary and returning it or throwing an exception if an error is received
     */
    @Suspendable
    override fun call(): List<DigitalSignatureAndMetadata> {
        val session = flowMessaging.initiateFlow(notary.name)

        val payload = generatePayload(stx)

        val notarisationResponse = session.sendAndReceive(
            NotarisationResponse::class.java,
            payload
        )

        return notarisationResponse.error?.let {
            throw NotaryException(it, stx.id)
        } ?: notarisationResponse.signatures
    }

    /**
     * This function generates a notarisation request and a signature from that given request via serialization.
     * Then attaches that signature to a [NonValidatingNotarisationPayload].
     */
    @Suspendable
    internal fun generatePayload(stx: UtxoSignedTransaction): NonValidatingNotarisationPayload {
        // TODO Since the API of `UtxoSignedTransaction` has no public property to access its wire transaction we need to
        //  do this ugly cast here. Once it gets added to the public API we can remove this.
        val signedTxImpl = stx as UtxoSignedTransactionImpl

        val filteredTx = filteredTransactionFactory.create(
            signedTxImpl.wireTransaction,
            listOf(
                ComponentGroupFilterParameters.AuditProof(INPUTS_GROUP, StateAndRef::class.java),
                ComponentGroupFilterParameters.AuditProof(REFERENCES_GROUP, StateAndRef::class.java),
                ComponentGroupFilterParameters.AuditProof(OUTPUTS_GROUP, TransactionState::class.java),
                ComponentGroupFilterParameters.AuditProof(TIMEWINDOW_GROUP, TimeWindow::class.java)
            )
        ) {
            // We don't need extra filters, only need the given component groups
            true
        }

        val notarisationRequest = NotarisationRequest(
            stx.toLedgerTransaction().inputStateAndRefs.map { it.ref },
            stx.id
        )

        val requestSignature = generateRequestSignature(
            notarisationRequest,
            memberLookupService.myInfo(),
            serializationService,
            signingService
        )

        // TODO CORE-7249 Filtering needed
        return NonValidatingNotarisationPayload(
            filteredTx,
            requestSignature
        )
    }
}
