package net.cordapp.demo.utxo.contract

import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.crypto.calculateHash
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.ContractStateVaultJsonFactory

class ContractStateVaultJsonFactoryImpl: ContractStateVaultJsonFactory<ContractState> {

    override val stateType: Class<ContractState> = ContractState::class.java

    override fun create(state: ContractState, jsonMarshallingService: JsonMarshallingService): String {
        return """
            {
                "participants": "${state.participants.map { "\n${it.calculateHash()}\n" }}"
            }
        """.trimIndent()
    }
}