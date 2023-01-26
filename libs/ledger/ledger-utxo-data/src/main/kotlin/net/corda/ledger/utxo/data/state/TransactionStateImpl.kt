package net.corda.ledger.utxo.data.state

import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.EncumbranceGroup
import net.corda.v5.ledger.utxo.TransactionState

/**
 * Represents a transaction state, composed of a [ContractState] and associated information.
 */
data class TransactionStateImpl<out T : ContractState>(
    override val contractState: T,
    override val notary: Party,
    override val encumbrance: EncumbranceGroup?,
) : TransactionState<T> {
    // TODO : Consider, should the transaction state only account for the subclass's Contract, or should it account for all Contracts in the type hierarchy?
    override val contractType: Class<out Contract> get() = contractState.getContractClasses().first()
    override val contractStateType: Class<out T> get() = contractState.javaClass
}
