package net.cordapp.demo.utxo.contract

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.UUID

@BelongsToContract(AppleStampContract::class)
@CordaSerializable
data class AppleStamp(
    val id: UUID,
    val stampDesc: String,
    val issuer: Party,
    val holder: Party,
    override val participants: List<PublicKey>
) : ContractState