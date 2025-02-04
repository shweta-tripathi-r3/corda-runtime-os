package net.corda.simulator.runtime.ledger.utxo

import net.corda.crypto.core.fullIdHash
import net.corda.simulator.factories.SimulatorConfigurationBuilder
import net.corda.simulator.runtime.notary.BaseNotaryInfo
import net.corda.simulator.runtime.notary.SimTimeWindow
import net.corda.simulator.runtime.serialization.BaseSerializationService
import net.corda.simulator.runtime.testutils.generateKey
import net.corda.simulator.runtime.testutils.generateKeys
import net.corda.v5.application.crypto.SigningService
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.time.Duration.Companion.days


class UtxoSignedTransactionBaseTest {
    private val notaryX500 = MemberX500Name.parse("O=Notary,L=London,C=GB")
    private val notaryKey = generateKey()
    private val config = SimulatorConfigurationBuilder.create().build()
    private val publicKeys = generateKeys(2)

    @Test
    fun `should be able to provide ledger transaction`(){
        // Given a signed utxo transaction
        val persistenceService = mock<PersistenceService>()
        val serializationService = BaseSerializationService()
        val signingService = mock<SigningService>()

        val signedTx = UtxoSignedTransactionBase(
            publicKeys.map { toSignatureWithMetadata(it, Instant.now()) },
            UtxoStateLedgerInfo(
                listOf(TestUtxoCommand()),
                emptyList(),
                emptyList(),
                publicKeys,
                SimTimeWindow(Instant.now(), Instant.now().plusMillis(1.days.inWholeMilliseconds)),
                listOf(
                    ContractStateAndEncumbranceTag(TestUtxoState("State1", publicKeys), ""),
                    ContractStateAndEncumbranceTag(TestUtxoState("State2", publicKeys), "")
                ),
                emptyList(),
                notaryX500,
                notaryKey
            ),
            signingService,
            serializationService,
            persistenceService,
            config
        )

        // If we try to retrieve the ledger transaction
        val ledgerTx = signedTx.toLedgerTransaction()

        // Then we should get the correct ledger transaction with the sam tx id
        assertThat(ledgerTx.id, `is`(signedTx.id))
    }

    @Test
    fun `should be equal to another transaction with the same data`(){
        // Given two different utxo ledger with same data
        val persistenceService = mock<PersistenceService>()
        val serializationService = BaseSerializationService()
        val signingService = mock<SigningService>()
        val timestamp = Instant.now()
        val timeWindow = SimTimeWindow(Instant.now(), Instant.now().plusMillis(1.days.inWholeMilliseconds))

        val ledgerInfo1 = UtxoStateLedgerInfo(
            listOf(TestUtxoCommand()),
            emptyList(),
            emptyList(),
            publicKeys,
            timeWindow,
            listOf(
                ContractStateAndEncumbranceTag(TestUtxoState("State1", publicKeys), ""),
                ContractStateAndEncumbranceTag(TestUtxoState("State2", publicKeys), "")
            ),
            emptyList(),
            notaryX500,
            notaryKey
        )
        val ledgerInfo2 = UtxoStateLedgerInfo(
            listOf(TestUtxoCommand()),
            emptyList(),
            emptyList(),
            publicKeys,
            timeWindow,
            listOf(
                ContractStateAndEncumbranceTag(TestUtxoState("State1", publicKeys), ""),
                ContractStateAndEncumbranceTag(TestUtxoState("State2", publicKeys), "")
            ),
            emptyList(),
            notaryX500,
            notaryKey
        )

        // When we build two different transaction with them
        val tx1 = UtxoSignedTransactionBase(
            publicKeys.map { toSignatureWithMetadata(it, timestamp) },
            ledgerInfo1,
            signingService,
            serializationService,
            persistenceService,
            config
        )

        val tx2 = UtxoSignedTransactionBase(
            publicKeys.map { toSignatureWithMetadata(it, timestamp) },
            ledgerInfo2,
            signingService,
            serializationService,
            persistenceService,
            config
        )

        // Then the two transaction should be considered as the same
        assertThat(tx1, `is`(tx2))
        assertThat(tx1.hashCode(), `is`(tx2.hashCode()))
    }

    @Test
    fun `should be able to convert to a JPA entity and back again`(){
        // Given a signed utxo  transaction
        val persistenceService = mock<PersistenceService>()
        val serializationService = BaseSerializationService()
        val signingService = mock<SigningService>()

        val tx = UtxoSignedTransactionBase(
            publicKeys.map { toSignatureWithMetadata(it, Instant.now()) },
            UtxoStateLedgerInfo(
                listOf(TestUtxoCommand()),
                emptyList(),
                emptyList(),
                publicKeys,
                SimTimeWindow(Instant.now(), Instant.now().plusMillis(1.days.inWholeMilliseconds)),
                listOf(
                    ContractStateAndEncumbranceTag(TestUtxoState("State1", publicKeys), ""),
                    ContractStateAndEncumbranceTag(TestUtxoState("State2", publicKeys), "")
                ),
                emptyList(),
                notaryX500,
                notaryKey
            ),
            signingService,
            serializationService,
            persistenceService,
            config
        )

        // When we convert to the entity and back again
        val entity = tx.toEntity()
        val txFromEntity = UtxoSignedTransactionBase.fromEntity(
            entity, BaseNotaryInfo(notaryX500, "", emptySet(), notaryKey),
            signingService, serializationService, persistenceService, config
        )

        // We should receive the transaction back
        assertThat(tx, `is`(txFromEntity))
        assertThat(tx.toLedgerTransaction(), `is`(txFromEntity.toLedgerTransaction()))
    }

    @Test
    fun `should be able to add signatories`(){
        // Geven a signed utxo transaction signed with one key
        val signingService = mock<SigningService>()
        val signatures = publicKeys.map {
            val signatureWithMetadata = toSignatureWithMetadata(it)
            whenever(signingService.sign(any(), eq(it), any())).thenReturn(signatureWithMetadata.signature)
            signatureWithMetadata
        }
        val persistenceService = mock<PersistenceService>()
        val serializationService = BaseSerializationService()
        val txWithOneSignature = UtxoSignedTransactionBase(
            listOf(signatures[0]),
            UtxoStateLedgerInfo(
                listOf(TestUtxoCommand()),
                emptyList(),
                emptyList(),
                publicKeys,
                SimTimeWindow(Instant.now(), Instant.now().plusMillis(1.days.inWholeMilliseconds)),
                listOf(
                    ContractStateAndEncumbranceTag(TestUtxoState("State1", publicKeys), ""),
                    ContractStateAndEncumbranceTag(TestUtxoState("State2", publicKeys), "")
                ),
                emptyList(),
                notaryX500,
                notaryKey
            ),
            signingService,
            serializationService,
            persistenceService,
            config
        )

        // When we add another signature to it
        whenever(signingService.findMySigningKeys(any())).thenReturn(mapOf(publicKeys[1] to publicKeys[1]))
        val txWithTwoSignatures = txWithOneSignature.addSignatures(listOf(publicKeys[1]))

        // The final transaction should be signed with both the keys
        assertThat(txWithTwoSignatures.signatures.map { it.by }.toSet(), `is`(publicKeys.map { it.fullIdHash() }.toSet()))
    }
}