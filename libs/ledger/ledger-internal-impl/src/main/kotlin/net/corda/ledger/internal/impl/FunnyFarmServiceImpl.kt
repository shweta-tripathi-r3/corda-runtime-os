package net.corda.ledger.internal.impl

import net.corda.ledger.internal.FunnyFarmService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.serialization.SingletonSerializeAsToken
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.ServiceScope.PROTOTYPE

@Component(
    service = [FunnyFarmService::class, SingletonSerializeAsToken::class],
    property = [ "corda.system=true" ],
    scope = PROTOTYPE
)
class FunnyFarmServiceImpl : FunnyFarmService, SingletonSerializeAsToken {
    @Suspendable
    override fun sayMoo(): String {
        return "moo"
    }
}
