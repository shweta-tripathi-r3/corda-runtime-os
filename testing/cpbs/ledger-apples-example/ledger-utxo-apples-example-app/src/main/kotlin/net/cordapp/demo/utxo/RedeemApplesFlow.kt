package net.cordapp.demo.utxo

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.loggerFor
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.cordapp.demo.utxo.contract.AppleStamp
import net.cordapp.demo.utxo.contract.BasketOfApples
import net.cordapp.demo.utxo.contract.BasketOfApplesContract
import java.time.Instant
import java.time.temporal.ChronoUnit

@InitiatingFlow(protocol = "redeem-apples")
class RedeemApplesFlow : RPCStartableFlow {

    private companion object {
        val log = loggerFor<RedeemApplesFlow>()
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val request = requestBody.getRequestBodyAs<RedeemApplesRequest>(jsonMarshallingService)
        val buyerName = request.buyer
        val stampId = request.stampId

        // Retrieve the notaries public key (this will change)
        val notary = notaryLookup.notaryServices.single()
        val notaryKey = memberLookup.lookup().single {
            it.memberProvidedContext["corda.notary.service.name"] == notary.name.toString()
        }.ledgerKeys.first()

        val ourIdentity = memberLookup.myInfo().let { Party(it.name, it.ledgerKeys.first()) }

        val buyer = memberLookup.lookup(buyerName)
            ?.let { Party(it.name, it.ledgerKeys.first()) }
            ?: throw IllegalArgumentException("The buyer does not exist within the network")

        val appleStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == stampId }
            ?: throw IllegalArgumentException("No apple stamp matching the stamp id $stampId")

        val basketOfApplesStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
            .firstOrNull()
            ?: throw IllegalArgumentException("There are no baskets of apples")

        val originalBasketOfApples = basketOfApplesStampStateAndRef.state.contractState

        val updatedBasket = originalBasketOfApples.changeOwner(buyer)

        // Create the transaction
        @Suppress("DEPRECATION")
        val transaction = utxoLedgerService.getTransactionBuilder()
            .setNotary(Party(notary.name, notaryKey))
            .addInputStates(appleStampStateAndRef.ref, basketOfApplesStampStateAndRef.ref)
            .addOutputState(updatedBasket)
            .addCommand(BasketOfApplesContract.Commands.Redeem())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(ourIdentity.owningKey, buyer.owningKey))
            .toSignedTransaction(ourIdentity.owningKey)

        val session = flowMessaging.initiateFlow(buyerName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session)).toString()
        } catch (e: Exception) {
            log.warn("Flow failed", e)
            "Flow failed, message: ${e.message}"
        }
    }
}