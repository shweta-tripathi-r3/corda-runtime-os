package net.corda.flow.acceptance.dsl

import net.corda.flow.fiber.FlowContinuation
import net.corda.flow.fiber.FlowIORequest
import net.corda.flow.pipeline.FlowEventContext
import net.corda.flow.pipeline.runner.FlowRunner
import net.corda.v5.application.flows.Flow
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class MockFlowRunner : FlowRunner {

    private val flowsToNextSuspensions = mutableMapOf<String, FlowIORequest<*>>()

    override fun runFlow(
        context: FlowEventContext<Any>,
        flowContinuation: FlowContinuation
    ): Future<FlowIORequest<*>> {
        val flowId = checkNotNull(context.checkpoint.flowId) { "No flow id is set, context: $context" }
        val nextSuspension = checkNotNull(flowsToNextSuspensions.remove(flowId)) { "No flow with flow id: $flowId has been set up within the mocking framework" }

        if (context.checkpoint.flowStack.size == 0) {
            context.checkpoint.flowStack.push(MockFlow)
        }

        return CompletableFuture.completedFuture(nextSuspension)
    }

    fun setNextSuspension(flowId: String, nextSuspension: FlowIORequest<*>) {
        flowsToNextSuspensions[flowId] = nextSuspension
    }
}

private object MockFlow : Flow<Unit> {
    override fun call() {
        throw IllegalStateException("Not called")
    }
}