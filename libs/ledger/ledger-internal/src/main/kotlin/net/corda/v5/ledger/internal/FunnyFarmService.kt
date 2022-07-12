package net.corda.v5.ledger.internal

import net.corda.v5.base.annotations.Suspendable

interface FunnyFarmService {
    @Suspendable
    fun sayMoo(): String
}