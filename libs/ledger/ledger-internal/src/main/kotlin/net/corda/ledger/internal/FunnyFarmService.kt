package net.corda.ledger.internal

import net.corda.v5.base.annotations.Suspendable

interface FunnyFarmService {
    @Suspendable
    fun sayMoo(): String
}