package net.corda.simulator.runtime.ledger

import net.corda.simulator.runtime.tools.SimpleJsonMarshallingService
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.crypto.DigitalSignatureMetadata
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.crypto.SecureHash
import net.corda.v5.crypto.SignatureSpec
import net.corda.v5.ledger.common.transaction.TransactionVerificationException
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransaction
import java.security.MessageDigest
import java.security.PublicKey
import java.time.Instant

data class ConsensualSignedTransactionBase(
    override val signatures: List<DigitalSignatureAndMetadata>,
    private val ledgerTransaction: ConsensualStateLedgerInfo,
    private val signingService: SigningService
) : ConsensualSignedTransaction {


    companion object {
        /*
         * This should use Simulator's serialization service when it's ready
         */
        private val serializer = SimpleJsonMarshallingService()
    }

    private val bytes : ByteArray = serializer.format(ledgerTransaction.states).toByteArray()
    override val id by lazy {
        val digest = MessageDigest.getInstance("SHA-256")
        SecureHash(digest.algorithm, digest.digest(bytes))
    }

    override fun addSignature(publicKey: PublicKey): Pair<ConsensualSignedTransaction, DigitalSignatureAndMetadata> {
        val signature = signWithMetadata(publicKey)
        return Pair(addSignature(signature), signature)
    }

    override fun addSignature(signature: DigitalSignatureAndMetadata): ConsensualSignedTransaction {
        return copy(signatures = this.signatures.plus(signature))
    }

    override fun getMissingSignatories(): Set<PublicKey> {
        return ledgerTransaction.requiredSigningKeys.minus(signatures.map {it.by}.toSet())
    }

    override fun toLedgerTransaction(): ConsensualLedgerTransaction =
        object : ConsensualLedgerTransaction {
            override val id: SecureHash = this@ConsensualSignedTransactionBase.id

            override val requiredSignatories: Set<PublicKey> = ledgerTransaction.requiredSigningKeys
            override val states: List<ConsensualState> = ledgerTransaction.states
            override val timestamp: Instant = ledgerTransaction.timestamp

        }

    override fun verifySignatures() {
        if (getMissingSignatories().isNotEmpty()) {
            throw TransactionVerificationException(
                id,
                "Verification failed; ${getMissingSignatories().size} keys missing",
                null
            )
        }
    }

    private fun signWithMetadata(key: PublicKey) : DigitalSignatureAndMetadata {
        val signature = signingService.sign(bytes, key, SignatureSpec.ECDSA_SHA256)
        return DigitalSignatureAndMetadata(signature, DigitalSignatureMetadata(Instant.now(), mapOf()))
    }
}
