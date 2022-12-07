package net.corda.ledger.utxo.flow.impl.transaction.serializer.tests

import net.corda.ledger.common.testkit.publicKeyExample
import net.corda.ledger.utxo.data.transaction.UtxoOutputInfoComponent
import net.corda.ledger.utxo.testkit.UtxoLedgerIntegrationTest
import net.corda.ledger.utxo.testkit.createExample
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.StateRef
import net.corda.v5.ledger.utxo.transaction.UtxoFilteredData
import net.corda.v5.ledger.utxo.transaction.UtxoFilteredTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.security.PublicKey

class UtxoFilteredTransactionAMQPSerializationTest : UtxoLedgerIntegrationTest() {

    private companion object {
        val inputHash = SecureHash.parse("SHA256:1234567890abcdef")
        val outputInfo = UtxoOutputInfoComponent(
            encumbrance = null,
            notary = Party(MemberX500Name("alice", "LDN", "GB"), publicKeyExample),
            contractStateTag = MyState::class.java.name,
            contractTag = "contract tag"
        )
    }

    @Test
    fun `can serialize and deserialize utxo filtered transaction with outputs audit proof`() {
        val outputState1 = MyState(0)
        val outputState2 = MyState(1)
        val utxoSignedTransaction = utxoSignedTransactionFactory.createExample(
            jsonMarshallingService,
            jsonValidator,
            wireTransactionFactory,
            componentGroups = listOf(
                emptyList(), // Notary
                emptyList(), // Signatories
                listOf(
                    serializationService.serialize(outputInfo).bytes,
                    serializationService.serialize(outputInfo).bytes
                ), // output infos
                emptyList(), // command infos
                emptyList(), // attachments
                listOf(
                    serializationService.serialize(StateRef(inputHash, 0)).bytes,
                    serializationService.serialize(StateRef(inputHash, 1)).bytes
                ), // inputs
                emptyList(), // references
                listOf(
                    serializationService.serialize(outputState1).bytes,
                    serializationService.serialize(outputState2).bytes
                ), // outputs
                emptyList(), // commands
            )
        )
        val utxoFilteredTransaction = utxoLedgerService.filterSignedTransaction(utxoSignedTransaction)
            .withInputStates()
            .withOutputStates()
            .toFilteredTransaction()

        assertThat(utxoFilteredTransaction.id).isNotNull

        val bytes = serializationService.serialize(utxoFilteredTransaction)
        assertThat(bytes).isNotNull
        val deserialized = serializationService.deserialize(bytes, UtxoFilteredTransaction::class.java)

        // check that the deserialized UtxoFilteredTransaction is fully functional
        assertThat(deserialized).isNotNull
        assertThat(deserialized.id).isEqualTo(utxoSignedTransaction.id)

        assertThat(deserialized.commands).isInstanceOf(UtxoFilteredData.Removed::class.java)

        assertThat(deserialized.inputStateRefs).isInstanceOf(UtxoFilteredData.Audit::class.java)
        val inputs = deserialized.inputStateRefs as UtxoFilteredData.Audit<StateRef>
        assertThat(inputs.size).isEqualTo(2)
        assertThat(inputs.values.size).isEqualTo(2)
        assertThat(inputs.values[0]?.transactionHash).isEqualTo(inputHash)

        assertThat(deserialized.outputStateAndRefs).isInstanceOf(UtxoFilteredData.Audit::class.java)
        val outputs = deserialized.outputStateAndRefs as UtxoFilteredData.Audit<StateAndRef<*>>
        assertThat(outputs.size).isEqualTo(2)
        assertThat(outputs.values.size).isEqualTo(2)
        assertThat(outputs.values[0]?.state?.contractState).isEqualTo(outputState1)
        assertThat(outputs.values[1]?.state?.contractState).isEqualTo(outputState2)
    }

    @Test
    fun `can serialize and deserialize utxo filtered transaction with outputs size proof`() {
        val utxoSignedTransaction = utxoSignedTransactionFactory.createExample(
            jsonMarshallingService,
            jsonValidator,
            wireTransactionFactory,
            componentGroups = listOf(
                emptyList(), // Notary
                emptyList(), // Signatories
                listOf(
                    serializationService.serialize(outputInfo).bytes,
                    serializationService.serialize(outputInfo).bytes
                ), // output infos
                emptyList(), // command infos
                emptyList(), // attachments
                listOf(
                    serializationService.serialize(StateRef(inputHash, 0)).bytes,
                    serializationService.serialize(StateRef(inputHash, 1)).bytes
                ), // inputs
                emptyList(), // references
                listOf(
                    serializationService.serialize(MyState(0)).bytes,
                    serializationService.serialize(MyState(1)).bytes
                ), // outputs
                emptyList(), // commands
            )
        )
        val utxoFilteredTransaction = utxoLedgerService.filterSignedTransaction(utxoSignedTransaction)
                .withInputStates()
                .withOutputStatesSize()
                .toFilteredTransaction()

        assertThat(utxoFilteredTransaction.id).isNotNull

        val bytes = serializationService.serialize(utxoFilteredTransaction)
        assertThat(bytes).isNotNull
        val deserialized = serializationService.deserialize(bytes, UtxoFilteredTransaction::class.java)

        // check that the deserialized UtxoFilteredTransaction is fully functional
        assertThat(deserialized).isNotNull
        assertThat(deserialized.id).isEqualTo(utxoSignedTransaction.id)

        assertThat(deserialized.commands).isInstanceOf(UtxoFilteredData.Removed::class.java)

        assertThat(deserialized.inputStateRefs).isInstanceOf(UtxoFilteredData.Audit::class.java)
        val inputs = deserialized.inputStateRefs as UtxoFilteredData.Audit<StateRef>
        assertThat(inputs.size).isEqualTo(2)
        assertThat(inputs.values.size).isEqualTo(2)
        assertThat(inputs.values[0]?.transactionHash).isEqualTo(inputHash)

        assertThat(deserialized.outputStateAndRefs).isInstanceOf(UtxoFilteredData.SizeOnly::class.java)
        val outputs = deserialized.outputStateAndRefs as UtxoFilteredData.SizeOnly
        assertThat(outputs.size).isEqualTo(2)
    }

    data class MyState(val value: Int) : ContractState {
        override val participants: List<PublicKey> = emptyList()
    }
}