package net.corda.simulator.runtime.ledger

import net.corda.simulator.runtime.tools.SimpleJsonMarshallingService
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.crypto.DigitalSignatureMetadata
import net.corda.v5.application.crypto.DigitalSignatureVerificationService
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.receive
import net.corda.v5.crypto.SecureHash
import net.corda.v5.crypto.SignatureSpec
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransactionVerifier
import net.corda.v5.ledger.consensual.transaction.ConsensualTransactionBuilder
import java.security.PublicKey
import java.time.Instant

@Suppress("ForbiddenComment")
// TODO: inject clock for signature metadata timestamp
class SimConsensualLedgerService(
    private val signingService: SigningService,
    private val memberLookup: MemberLookup,
    private val verificationService: DigitalSignatureVerificationService,
) : ConsensualLedgerService {

    override fun finality(
        signedTransaction: ConsensualSignedTransaction,
        sessions: List<FlowSession>
    ): ConsensualSignedTransaction {
        var finalSignedTransaction = signedTransaction
        sessions.forEach {
            it.send(signedTransaction)
            val signature = it.receive<DigitalSignatureAndMetadata>()
            val serializer = SimpleJsonMarshallingService()
            verificationService.verify(
                signature.by, SignatureSpec.ECDSA_SHA256,
                signature.signature.bytes,
                serializer.format(signedTransaction.toLedgerTransaction().states).toByteArray()
            )
            finalSignedTransaction = signedTransaction.addSignature(signature);
        }
        return finalSignedTransaction
    }

    override fun findLedgerTransaction(id: SecureHash): ConsensualLedgerTransaction? {
        TODO("Not yet implemented")
    }

    override fun findSignedTransaction(id: SecureHash): ConsensualSignedTransaction? {
        TODO("Not yet implemented")
    }

    override fun getTransactionBuilder(): ConsensualTransactionBuilder {
        return ConsensualTransactionBuilderBase(listOf(), signingService, memberLookup)
    }

    override fun receiveFinality(
        session: FlowSession,
        verifier: ConsensualSignedTransactionVerifier
    ): ConsensualSignedTransaction {
        var signedTransaction = session.receive<ConsensualSignedTransaction>()
        verifier.verify(signedTransaction)
        val signature = sign(signedTransaction, memberLookup.myInfo().ledgerKeys[0])
        session.send(signature)
        signedTransaction = signedTransaction.addSignature(signature)
        return signedTransaction
    }

    private fun sign(
        signedTransaction: ConsensualSignedTransaction,
        publicKey: PublicKey
    ):DigitalSignatureAndMetadata{
        val serializer = SimpleJsonMarshallingService()
        val signature = signingService.sign(serializer.format(signedTransaction.toLedgerTransaction().states).toByteArray(),
            publicKey, SignatureSpec.ECDSA_SHA256)
        return DigitalSignatureAndMetadata(signature, DigitalSignatureMetadata(Instant.now(), mapOf()))
    }
}


