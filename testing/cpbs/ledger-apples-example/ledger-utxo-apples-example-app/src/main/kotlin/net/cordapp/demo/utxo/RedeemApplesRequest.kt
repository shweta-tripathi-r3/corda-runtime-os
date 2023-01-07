package net.cordapp.demo.utxo

import net.corda.v5.base.types.MemberX500Name
import java.util.UUID

data class RedeemApplesRequest(val buyer: MemberX500Name, val stampId: UUID)