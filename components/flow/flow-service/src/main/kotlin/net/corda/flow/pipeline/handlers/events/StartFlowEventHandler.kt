package net.corda.flow.pipeline.handlers.events

import net.corda.data.flow.event.StartFlow
import net.corda.data.flow.state.waiting.WaitingFor
import net.corda.flow.pipeline.FlowEventContext
import net.corda.flow.pipeline.handlers.waiting.WaitingForStartFlow
import net.corda.v5.base.util.contextLogger
import org.osgi.service.component.annotations.Component

@Component(service = [FlowEventHandler::class])
class StartFlowEventHandler : FlowEventHandler<StartFlow> {

    private companion object {
        val logger = contextLogger()
    }
    override val type = StartFlow::class.java

    override fun preProcess(context: FlowEventContext<StartFlow>): FlowEventContext<StartFlow> {
        logger.info("Lorcan - start flow with id  ${context.inputEventPayload.startContext.statusKey.id}, mdc : ${context.mdcProperties}")
        context.checkpoint.initFlowState(context.inputEventPayload.startContext)
        context.checkpoint.waitingFor =  WaitingFor(WaitingForStartFlow)
        return context
    }
}