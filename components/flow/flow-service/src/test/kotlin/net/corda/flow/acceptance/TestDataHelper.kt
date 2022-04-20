package net.corda.flow.acceptance

import net.corda.data.flow.FlowInitiatorType
import net.corda.data.flow.FlowKey
import net.corda.data.flow.FlowStartContext
import net.corda.data.identity.HoldingIdentity
import net.corda.flow.ALICE_X500_HOLDING_IDENTITY
import net.corda.flow.ALICE_X500_NAME
import net.corda.v5.base.types.MemberX500Name
import java.time.Instant

fun getBasicFlowStartContext(holdingIdentity: HoldingIdentity = ALICE_X500_HOLDING_IDENTITY): FlowStartContext {
    return FlowStartContext.newBuilder()
        .setStatusKey(FlowKey("request id", holdingIdentity))
        .setInitiatorType(FlowInitiatorType.RPC)
        .setRequestId("request id")
        .setIdentity(holdingIdentity)
        .setCpiId("cpi id")
        .setInitiatedBy(holdingIdentity)
        .setFlowClassName("flow class name")
        .setCreatedTimestamp(Instant.MIN)
        .build()
}