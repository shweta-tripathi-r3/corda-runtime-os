package net.corda.simulator.runtime.ledger

import net.corda.simulator.crypto.HsmCategory
import net.corda.simulator.runtime.signing.BaseSimKeyStore
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.crypto.DigitalSignatureMetadata
import net.corda.v5.application.crypto.DigitalSignatureVerificationService
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.crypto.DigitalSignature
import net.corda.v5.crypto.SignatureSpec
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import net.corda.v5.ledger.consensual.transaction.ConsensualSignedTransaction
import net.corda.v5.membership.MemberInfo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.security.PublicKey
import java.time.Instant

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
    fun `should sign a transaction with a given key`() {

        // Given a signed transaction is generated
        val signingService = mock<SigningService>()
        val ledgerInfo = ConsensualStateLedgerInfo(
            listOf(NameConsensualState("CordaDev", publicKeys)), Instant.now())
        whenever(signingService.sign(any(), eq(publicKeys[0]), any())).thenReturn(toSignature(publicKeys[0]).signature)
        whenever(signingService.sign(any(), eq(publicKeys[1]), any())).thenReturn(toSignature(publicKeys[1]).signature)
        val unsignedTx = ConsensualSignedTransactionBase(
            listOf(),
            ledgerInfo,
            signingService
        )
        val signedTransaction =  unsignedTx.addSignature(publicKeys[0]).first
        val ledgerService = SimConsensualLedgerService(signingService, mock(), mock())

        // When the transaction is passed to the ledger service along with a public key
        val method = SimConsensualLedgerService::class.java.getDeclaredMethod(
            "sign", ConsensualSignedTransaction::class.java, PublicKey::class.java)
        method.isAccessible = true
        val signature: DigitalSignatureAndMetadata = method.invoke(ledgerService, signedTransaction, publicKeys[1]) as DigitalSignatureAndMetadata

        // Then a new signature should be added for the given public key
        Assertions.assertNotNull(signature)
        assertThat(signature.by, `is`(publicKeys[1]))
    }

    @Test
    fun `should get transaction signed from counterparties when finality is called`(){
        // Given a signed transaction is generated
        val ledgerInfo = ConsensualStateLedgerInfo(
            listOf(NameConsensualState("CordaDev", publicKeys)), Instant.now())
        val signingService = mock<SigningService>()
        whenever(signingService.sign(any(), eq(publicKeys[0]), any())).thenReturn(toSignature(publicKeys[0]).signature)
        whenever(signingService.sign(any(), eq(publicKeys[1]), any())).thenReturn(toSignature(publicKeys[1]).signature)
        val unsignedTx = ConsensualSignedTransactionBase(
            listOf(),
            ledgerInfo,
            signingService
        )
        val signedTransaction = unsignedTx.addSignature(publicKeys[0]).first

        // And a flow session is created
        val signature = DigitalSignatureAndMetadata(toSignature(publicKeys[1]).signature, DigitalSignatureMetadata(Instant.now(), mapOf()))
        val flowSession = mock<FlowSession>()
        whenever(flowSession.receive<Any>(any())).thenReturn(signature)

        //When the transaction is sent to the ledger service for finality
        val verificationService = mock<DigitalSignatureVerificationService>()
        val ledgerService = SimConsensualLedgerService(signingService, mock(), verificationService)
        val finalSignedTx = ledgerService.finality(signedTransaction, listOf(flowSession))

        // Then the transaction should get signed by the counterparty
        Assertions.assertNotNull(finalSignedTx)
        assertThat(finalSignedTx.signatures.size, `is`(2))
        assertThat(finalSignedTx.signatures[0].by, `is`(publicKeys[0]))
        assertThat(finalSignedTx.signatures[1].by, `is`(publicKeys[1]))
    }

    @Test
    fun `should sign transaction when receive finality is called`(){
        // Given a signed transaction is generated
        val ledgerInfo = ConsensualStateLedgerInfo(
            listOf(NameConsensualState("CordaDev", publicKeys)), Instant.now())
        val signingService = mock<SigningService>()
        whenever(signingService.sign(any(), eq(publicKeys[0]), any())).thenReturn(toSignature(publicKeys[0]).signature)
        whenever(signingService.sign(any(), eq(publicKeys[1]), any())).thenReturn(toSignature(publicKeys[1]).signature)
        val unsignedTx = ConsensualSignedTransactionBase(
            listOf(),
            ledgerInfo,
            signingService
        )
        val signedTransaction = unsignedTx.addSignature(publicKeys[0]).first

        // And a flow session is created
        val flowSession = mock<FlowSession>()
        whenever(flowSession.receive<Any>(any())).thenReturn(signedTransaction)

        //When the ledger sevice is called for receive finality
        val memberLookup = mock<MemberLookup>()
        val memberInfo = mock<MemberInfo>()
        whenever(memberLookup.myInfo()).thenReturn(memberInfo)
        whenever(memberInfo.ledgerKeys).thenReturn(listOf(publicKeys[1]))
        val ledgerService = SimConsensualLedgerService(signingService, memberLookup, mock())
        val finalSignedTx = ledgerService.receiveFinality(flowSession, mock())

        // Then the transaction should get signed
        Assertions.assertNotNull(finalSignedTx)
        assertThat(finalSignedTx.signatures.size, `is`(2))
        assertThat(finalSignedTx.signatures[0].by, `is`(publicKeys[0]))
        assertThat(finalSignedTx.signatures[1].by, `is`(publicKeys[1]))
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

    private fun toSignature(it: PublicKey) = DigitalSignatureAndMetadata(
        DigitalSignature.WithKey(it, "some bytes".toByteArray(), mapOf()),
        DigitalSignatureMetadata(Instant.now(), mapOf())
    )


    data class NameConsensualState(val name: String, override val participants: List<PublicKey>) : ConsensualState {
        override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {}
    }
}