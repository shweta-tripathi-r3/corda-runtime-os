package net.corda.ledger.utxo.data.state

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.security.PublicKey

class ExtensionsTest {

    class TestContract : Contract {
        class TestState(override val participants: List<PublicKey>) : ContractState

        override fun verify(transaction: UtxoLedgerTransaction) {
            TODO("Not yet implemented")
        }
    }

    @Test
    fun canGetContractClassWhenEnclosed() {
        val state = TestContract.TestState(emptyList())

        Assertions.assertTrue(state.getContractClasses().isNotEmpty())
    }

    @BelongsToContract(TestContract::class)
    class TestState2(override val participants: List<PublicKey>) : ContractState

    @Test
    fun canGetContractClassWithAnnotation() {
        val state = TestState2(emptyList())

        Assertions.assertTrue(state.getContractClasses().isNotEmpty())
    }

    class Enclosed(override val participants: List<PublicKey>) : ContractState

    @Test
    fun gettingContractFailsForOtherClasses() {

        val state1 = Enclosed(emptyList())
        Assertions.assertTrue(state1.getContractClasses().isNotEmpty())

        val state2 = NonEnclosed(emptyList())
        Assertions.assertTrue(state2.getContractClasses().isNotEmpty())
    }
}

class NonEnclosed(override val participants: List<PublicKey>) : ContractState