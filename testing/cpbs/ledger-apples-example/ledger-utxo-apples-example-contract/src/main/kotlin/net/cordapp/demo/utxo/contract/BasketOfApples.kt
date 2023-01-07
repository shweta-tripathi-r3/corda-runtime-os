package net.cordapp.demo.utxo.contract

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey


@BelongsToContract(BasketOfApplesContract::class)
@CordaSerializable
data class BasketOfApples(
    val description: String,
    val farm: Party,
    val owner: Party,
    val weight: Int,
    override val participants: List<PublicKey>
) : ContractState {

    fun changeOwner(buyer: Party): BasketOfApples {
        val participants = listOf(farm.owningKey, buyer.owningKey)
        return BasketOfApples(description, farm, buyer, weight, participants)
    }
}