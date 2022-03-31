package net.corda.flow.sandbox

import net.corda.data.flow.FlowInitiatorType
import net.corda.data.flow.FlowStartContext
import net.corda.data.flow.FlowStatusKey
import net.corda.data.flow.event.StartFlow
import net.corda.flow.pipeline.factory.FlowEventProcessorFactory
import net.corda.securitymanager.SecurityManagerService
import net.corda.testing.sandboxes.SandboxSetup
import net.corda.testing.sandboxes.fetchService
import net.corda.testing.sandboxes.lifecycle.EachTestLifecycle
import net.corda.virtualnode.HoldingIdentity
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.osgi.framework.BundleContext
import org.osgi.test.common.annotation.InjectBundleContext
import org.osgi.test.common.annotation.InjectService
import org.osgi.test.junit5.context.BundleContextExtension
import org.osgi.test.junit5.service.ServiceExtension
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

@ExtendWith(ServiceExtension::class, BundleContextExtension::class)
@TestInstance(PER_CLASS)
class SandboxSecurityManagerTest {
    companion object {
        private const val CPB_INJECT = "sandbox-cpk-inject-package.cpb"
        private const val FLOW_CLASS_NAME = "com.example.sandbox.cpk.inject.ExampleFlow"
        private const val SERVICE_ONE_CLASS_NAME = "com.example.sandbox.cpk.inject.ExampleServiceOne"
        private const val SERVICE_TWO_CLASS_NAME = "com.example.sandbox.cpk.inject.impl.ExampleServiceTwo"
        private const val X500_NAME = "CN=Testing, OU=Application, O=R3, L=London, C=GB"
        private const val CLIENT_ID = "client-1"

        private val holdingIdentity = HoldingIdentity(X500_NAME, UUID.randomUUID().toString())
    }

    @RegisterExtension
    private val lifecycle = EachTestLifecycle()

    @InjectService(timeout = 1000)
    lateinit var flowEventProcessorFactory: FlowEventProcessorFactory;

    @InjectService(timeout = 1000)
    lateinit var securityManagerService: SecurityManagerService

    private lateinit var sandboxFactory: SandboxFactory

    @BeforeAll
    fun setup(
        @InjectService(timeout = 1000)
        sandboxSetup: SandboxSetup,
        @InjectBundleContext
        context: BundleContext,
        @TempDir
        baseDirectory: Path
    ) {
        sandboxSetup.configure(context, baseDirectory)
        lifecycle.accept(sandboxSetup) { setup ->
            sandboxFactory = setup.fetchService(timeout = 1000)
        }
    }

    @Suppress("unused")
    @BeforeEach
    fun reset() {
        securityManagerService.start()
    }

    @Test
    fun testCordaInjectables() {
        val vnodeInfo = sandboxFactory.loadVirtualNode(CPB_INJECT, holdingIdentity)
        try {
            sandboxFactory.getOrCreateSandbox(holdingIdentity).use {
                //val sandbox = sandboxContext.sandboxGroup
                //val flowClass = sandbox.loadClassFromMainBundles(FLOW_CLASS_NAME)
                //val flowBundle = FrameworkUtil.getBundle(flowClass)
                //val flowContext = flowBundle.bundleContext
                //val serviceOneClass = flowBundle.loadClass(SERVICE_ONE_CLASS_NAME)
                //val serviceTwoClass = flowBundle.loadClass(SERVICE_TWO_CLASS_NAME)
                //runFlow("client-1", vnodeInfo)

            }
        } finally {
            sandboxFactory.unloadVirtualNode(vnodeInfo)
        }
    }
    /*
    private fun runFlow(clientId: String, vnodeInfo: VirtualNodeInfo) {
        val rpcStartFlow = createRPCStartFlow(clientId, vnodeInfo.toAvro())
        val flowKey = FlowKey(generateRandomId(), holdingIdentity.toAvro())
        val record = Record(Schemas.Flow.FLOW_EVENT_TOPIC, flowKey, FlowEvent(flowKey, rpcStartFlow))
        flowEventProcessorFactory.create().apply {
            val result = onNext(null, record)
            result.responseEvents.singleOrNull { evt ->
                evt.topic == Schemas.Flow.FLOW_EVENT_TOPIC
            }?.also { evt ->
                @Suppress("unchecked_cast")
                onNext(result.updatedState, evt as Record<FlowKey, FlowEvent>)
            }
        }
    }
    */

    private fun createRPCStartFlow(
        clientId: String,
        virtualNodeInfo: net.corda.data.virtualnode.VirtualNodeInfo): StartFlow {
        return StartFlow(
            FlowStartContext(
                FlowStatusKey(clientId, virtualNodeInfo.holdingIdentity),
                FlowInitiatorType.RPC,
                clientId,
                virtualNodeInfo.holdingIdentity,
                virtualNodeInfo.cpiIdentifier.name,
                virtualNodeInfo.holdingIdentity,
                "com.example.cpk.ExampleFlow",
                Instant.now(),
            ),  "{\"message\":\"Bongo!\"}"
        )
    }

    private fun generateRandomId(): String = UUID.randomUUID().toString()
}
