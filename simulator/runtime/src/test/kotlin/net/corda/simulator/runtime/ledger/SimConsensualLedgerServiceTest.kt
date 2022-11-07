package net.corda.simulator.runtime.ledger

import net.corda.simulator.crypto.HsmCategory
import net.corda.simulator.runtime.signing.BaseSimKeyStore
import net.corda.v5.application.crypto.DigitalSignatureVerificationService
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.crypto.DigitalSignature
import net.corda.v5.crypto.SignatureSpec
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import net.corda.v5.membership.MemberInfo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.PublicKey

class SimConsensualLedgerServiceTest {


    private val keyStore = BaseSimKeyStore()
    private val publicKeys = listOf(
        keyStore.generateKey("key1", HsmCategory.LEDGER, "any-scheme"),
        keyStore.generateKey("key2", HsmCategory.LEDGER, "any-scheme"),
        keyStore.generateKey("key3", HsmCategory.LEDGER, "any-scheme")
    )

    @Test
    fun `should be able to build a consensual transaction and sign with a key`() {

        // Given a key has been generated on the node, so the SigningService can sign with it
        val signingService = mock<SigningService>()
        val verificationService = mock<DigitalSignatureVerificationService>()
        val ledgerService = SimConsensualLedgerService(signingService, mock(), verificationService)

        whenever(signingService.sign(any(), eq(publicKeys[0]), eq(SignatureSpec.ECDSA_SHA256)))
            .thenReturn(DigitalSignature.WithKey(publicKeys[0], "My fake signed things".toByteArray(), mapOf()))

        // When we build a transaction via the ledger service and sign it with a key
        val state = NameConsensualState("CordaDev", publicKeys)
        val txBuilder = ledgerService.getTransactionBuilder()
            .withStates(state)
        val tx = txBuilder.sign(publicKeys[0])

        // Then the ledger transaction version should have the states in it
        assertThat(tx.toLedgerTransaction().states, `is`(listOf(state)))

        // And the signatures should have come from the signing service
        assertThat(tx.signatures.size, `is`(1))
        assertThat(tx.signatures[0].by, `is`(publicKeys[0]))
        assertThat(String(tx.signatures[0].signature.bytes), `is`("My fake signed things"))
    }

    @Test
    @Disabled("Waiting for this to be implemented in Corda")
    fun `should use the key from member lookup when no key provided`() {

        // Given a key has been generated on the node
        // And we can look it up through the member lookup
        val signingService = mock<SigningService>()
        val verificationService = mock<DigitalSignatureVerificationService>()
        val memberLookup = mock<MemberLookup>()
        val memberInfo = mock<MemberInfo>()

        whenever(memberLookup.myInfo()).thenReturn(memberInfo)
        whenever(memberInfo.ledgerKeys).thenReturn(listOf(publicKeys[0]))

        val ledgerService = SimConsensualLedgerService(signingService, memberLookup, verificationService)
        val state = NameConsensualState("CordaDev", publicKeys)

        whenever(signingService.sign(any(), eq(publicKeys[0]), eq(SignatureSpec.ECDSA_SHA256)))
            .thenReturn(DigitalSignature.WithKey(publicKeys[0], "My fake signed things".toByteArray(), mapOf()))

        // When we build a transaction via the ledger service and sign it without giving a key
        val txBuilder = ledgerService.getTransactionBuilder()
            .withStates(state)
        val tx = txBuilder.sign()

        // Then the ledger transaction version should have the states in it
        assertThat(tx.toLedgerTransaction().states, `is`(listOf(state)))

        // And the signatures should have come from the signing service
        assertThat(tx.signatures.size, `is`(1))
        assertThat(tx.signatures[0].by, `is`(publicKeys[0]))
        assertThat(String(tx.signatures[0].signature.bytes), `is`("My fake signed things"))
    }

    data class NameConsensualState(val name: String, override val participants: List<PublicKey>) : ConsensualState {
        override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {}
    }
}