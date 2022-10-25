package net.corda.simulator.runtime.ledger

import net.corda.simulator.runtime.testutils.generateKeys
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.crypto.DigitalSignatureMetadata
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.crypto.DigitalSignature
import net.corda.v5.ledger.common.transaction.TransactionVerificationException
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.MessageDigest
import java.security.PublicKey
import java.time.Instant.now

class ConsensualSignedTransactionBaseTest {

    private val publicKeys = generateKeys(3)
    private val digest = MessageDigest.getInstance("SHA-256")

    @Test
    fun `should verify if no signatures are missing`() {
        val ledgerInfo = ConsensualStateLedgerInfo(listOf(MyConsensualState(publicKeys)), now())
        val tx = ConsensualSignedTransactionBase(
            publicKeys.map { toSignature(it) },
            ledgerInfo,
            mock()
        )
        assertTrue(tx.getMissingSigningKeys().isEmpty())
        assertDoesNotThrow { tx.verifySignatures() }
    }

    @Test
    fun `should fail verification if any signatures are missing`() {
        val ledgerInfo = ConsensualStateLedgerInfo(listOf(MyConsensualState(publicKeys)), now())
        val tx = ConsensualSignedTransactionBase(
            listOf(publicKeys[0], publicKeys[1]).map { toSignature(it) },
            ledgerInfo,
            mock()
        )
        assertThat(tx.getMissingSigningKeys(), `is`(setOf(publicKeys[2])))
        assertThrows<TransactionVerificationException> { tx.verifySignatures() }
    }

    @Test
    fun `should be able to add signatories via the injected SigningService`() {
        val signingService = mock<SigningService>()
        whenever(signingService.sign(any(), eq(publicKeys[1]), any())).thenReturn(toSignature(publicKeys[1]).signature)
        whenever(signingService.sign(any(), eq(publicKeys[2]), any())).thenReturn(toSignature(publicKeys[2]).signature)

        val ledgerInfo = ConsensualStateLedgerInfo(listOf(MyConsensualState(publicKeys)), now())
        val signedByOneTx = ConsensualSignedTransactionBase(
            listOf(toSignature(publicKeys[0])),
            ledgerInfo,
            signingService
        )
        val signedByTwoTx = signedByOneTx.addSignature(publicKeys[1]).first
        val signedByAllTx = signedByTwoTx.addSignature(toSignature(publicKeys[2]))
        assertTrue(signedByAllTx.getMissingSigningKeys().isEmpty())
    }

    @Test
    fun `should be able to provide the ledger transaction`() {
        val ledgerInfo = ConsensualStateLedgerInfo(listOf(MyConsensualState(publicKeys)), now())
        val tx = ConsensualSignedTransactionBase(
            publicKeys.map { toSignature(it) },
            ledgerInfo,
            mock()
        )
        val ledgerTransaction = tx.toLedgerTransaction()
        assertThat(ledgerTransaction.id, `is`(tx.id))
        assertThat(ledgerTransaction.states, `is`(ledgerInfo.states))
        assertThat(ledgerTransaction.timestamp, `is`(ledgerInfo.timestamp))
        assertThat(ledgerTransaction.requiredSigningKeys, `is`(publicKeys.toSet()))
    }

    @Test
    fun `should not change the timestamp or id even when signed`() {
        val ledgerInfo = ConsensualStateLedgerInfo(listOf(MyConsensualState(publicKeys)), now())
        val signingService = mock<SigningService>()
        whenever(signingService.sign(any(), eq(publicKeys[0]), any())).thenReturn(toSignature(publicKeys[0]).signature)

        val unsignedTx = ConsensualSignedTransactionBase(
            listOf(),
            ledgerInfo,
            signingService
        )
        val signedTx = unsignedTx.addSignature(publicKeys[0]).first

        val ledgerTransaction = signedTx.toLedgerTransaction()

        assertThat(ledgerTransaction.id, `is`(unsignedTx.id))
        assertThat(ledgerTransaction.timestamp, `is`(ledgerInfo.timestamp))
    }

    private fun toSignature(it: PublicKey) = DigitalSignatureAndMetadata(
        DigitalSignature.WithKey(it, "some bytes".toByteArray(), mapOf()),
        DigitalSignatureMetadata(now(), mapOf())
    )

    class MyConsensualState(override val participants: List<PublicKey>) : ConsensualState {
        override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {}
    }
}