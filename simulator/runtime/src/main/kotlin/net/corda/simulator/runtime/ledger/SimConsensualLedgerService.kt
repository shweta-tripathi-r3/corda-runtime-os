package net.corda.simulator.runtime.ledger

import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransactionVerifier
import net.corda.v5.ledger.consensual.transaction.ConsensualTransactionBuilder

@Suppress("ForbiddenComment")
// TODO: inject clock for signature metadata timestamp
class SimConsensualLedgerService(
    private val signingService: SigningService,
    private val memberLookup: MemberLookup
) : ConsensualLedgerService {

    override fun finality(
        signedTransaction: ConsensualSignedTransaction,
        sessions: List<FlowSession>
    ): ConsensualSignedTransaction {
        return signedTransaction
    }

    override fun getTransactionBuilder(): ConsensualTransactionBuilder {
        return ConsensualTransactionBuilderBase(listOf(), signingService, memberLookup)
    }

    override fun receiveFinality(
        session: FlowSession,
        verifier: ConsensualSignedTransactionVerifier
    ): ConsensualSignedTransaction {
        TODO("Not yet implemented")
    }
}


