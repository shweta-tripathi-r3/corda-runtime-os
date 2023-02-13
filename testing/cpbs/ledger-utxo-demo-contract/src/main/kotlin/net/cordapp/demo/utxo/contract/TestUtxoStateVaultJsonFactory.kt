package net.cordapp.demo.utxo.contract

import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.crypto.calculateHash
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.ContractStateVaultJsonFactory

class TestUtxoStateVaultJsonFactory: ContractStateVaultJsonFactory<TestUtxoState> {

    override val stateType: Class<TestUtxoState> = TestUtxoState::class.java

    override fun create(state: TestUtxoState, jsonMarshallingService: JsonMarshallingService): String {
        return jsonMarshallingService.format(TestUtxoStatePojo(state.testField))
    }
}

data class TestUtxoStatePojo(val testField: String)