package net.corda.simulator.runtime.ledger

import net.corda.simulator.exceptions.NoKeyGeneratedException
import net.corda.simulator.runtime.testutils.generateKeys
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.DigitalSignature
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import net.corda.v5.membership.MemberInfo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.PublicKey

class ConsensualTransactionBuilderBaseTest {

    private val publicKeys = generateKeys(3)
    private val signingService = mock<SigningService>()

    @BeforeEach
    fun `set up signing service mock`() {
        publicKeys.map {
            whenever(signingService.sign(any(), eq(it), any()))
                .thenReturn(DigitalSignature.WithKey(it, "some bytes".toByteArray(), mapOf()))
        }
    }

    @Test
    fun `should produce a transaction on being signed with keys`() {
        val builder = ConsensualTransactionBuilderBase(listOf(MyConsensualState(publicKeys)), signingService, mock())

        val tx = builder.sign(publicKeys)
        assertThat(tx.signatures.map {it.by}, `is`(publicKeys))
    }

    @Test
    @Disabled("Waiting for implementation in Corda")
    fun `should look up first ledger key for a member and use that for signing if no key provided`() {
        val memberLookup = mock<MemberLookup>()
        val memberInfo = mock<MemberInfo>()
        whenever(memberLookup.myInfo()).thenReturn(memberInfo)
        whenever(memberInfo.ledgerKeys).thenReturn(publicKeys)
        whenever(memberInfo.name).thenReturn(MemberX500Name.parse("O=Alice, L=London, C=GB"))

        val builder = ConsensualTransactionBuilderBase(
            listOf(MyConsensualState(publicKeys)),
            signingService,
            memberLookup)
        val tx = builder.sign()

        assertThat(tx.signatures.map {it.by}, `is`(listOf(publicKeys.first())))
    }

    @Test
    @Disabled("Waiting for implementation in Corda")
    fun `should throw an exception on sign without key if no key has been generated for the given member`() {
        // Given a member with no keys
        val memberLookup = mock<MemberLookup>()
        val memberInfo = mock<MemberInfo>()
        whenever(memberLookup.myInfo()).thenReturn(memberInfo)
        whenever(memberInfo.ledgerKeys).thenReturn(listOf())
        whenever(memberInfo.name).thenReturn(MemberX500Name.parse("O=Alice, L=London, C=GB"))

        // When we sign the tx without a key, then it should throw an exception
        val builder = ConsensualTransactionBuilderBase(
            listOf(MyConsensualState(publicKeys)),
            signingService,
            memberLookup)
        assertThrows<NoKeyGeneratedException> { builder.sign() }
    }

    class MyConsensualState(override val participants: List<PublicKey>) : ConsensualState {
        override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {}
    }
}