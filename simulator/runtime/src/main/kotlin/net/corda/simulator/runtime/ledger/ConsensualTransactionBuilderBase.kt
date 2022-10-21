package net.corda.simulator.runtime.ledger

import net.corda.simulator.exceptions.NoKeyGeneratedException
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualTransactionBuilder
import java.security.PublicKey
import java.time.Instant

class ConsensualTransactionBuilderBase(
    override val states: List<ConsensualState>,
    private val signingService: SigningService,
    private val memberLookup: MemberLookup
) : ConsensualTransactionBuilder {

    override fun sign(): ConsensualSignedTransaction {
        return sign(memberLookup.myInfo().ledgerKeys.firstOrNull()
            ?: throw NoKeyGeneratedException(memberLookup.myInfo().name))
    }

    override fun sign(vararg signatories: PublicKey): ConsensualSignedTransaction {
        return sign(signatories.asIterable())
    }

    override fun sign(signatories: Iterable<PublicKey>): ConsensualSignedTransaction {
        val unsignedTx = ConsensualSignedTransactionBase(
            listOf(),
            ConsensualStateLedgerInfo(
                states,
                Instant.now()
            ),
            signingService
        ) as ConsensualSignedTransaction
        return signatories.fold(unsignedTx) { tx, sig -> tx.addSignature(sig).first }
    }

    override fun withStates(vararg states: ConsensualState): ConsensualTransactionBuilder {
        return ConsensualTransactionBuilderBase(this.states.plus(states), signingService, memberLookup )
    }
}