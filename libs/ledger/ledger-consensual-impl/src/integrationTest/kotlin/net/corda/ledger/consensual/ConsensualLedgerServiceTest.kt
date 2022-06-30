package net.corda.ledger.consensual

import net.corda.testing.sandboxes.SandboxSetup
import net.corda.testing.sandboxes.fetchService
import net.corda.testing.sandboxes.groupcontext.VirtualNodeService
import net.corda.testing.sandboxes.lifecycle.EachTestLifecycle
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import org.junit.jupiter.api.Assertions.assertEquals
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

@ExtendWith(ServiceExtension::class, BundleContextExtension::class)
@TestInstance(PER_CLASS)
@Suppress("FunctionName")
class ConsensualLedgerServiceTest {
    companion object {
        private const val TIMEOUT_MILLIS = 2000L
        private const val CPB = "META-INF/consensual-ledger.cpb"
        private const val CPK_BASIC_FLOW = "net.cordapp.demo.consensual.ConsensualFlow"

        init {
            println("ConsensualLedgerServiceTest init static")
        }
    }

    @RegisterExtension
    private val lifecycle = EachTestLifecycle()

    private lateinit var virtualNode: VirtualNodeService

    @InjectService(timeout = TIMEOUT_MILLIS)
    lateinit var consensualLedgerService: ConsensualLedgerService

    @BeforeAll
    fun setup(
        @InjectService(timeout = 2000)
        sandboxSetup: SandboxSetup,
        @InjectBundleContext
        bundleContext: BundleContext,
        @TempDir
        testDirectory: Path
    ) {
        sandboxSetup.configure(bundleContext, testDirectory)
        lifecycle.accept(sandboxSetup) { setup ->
            virtualNode = setup.fetchService(timeout = 1000)
        }
    }

    @Suppress("unused")
    @BeforeEach
    fun reset() {}

    @Test
    fun `Can resolve amnd inject service`() {
        assertEquals(42, consensualLedgerService.double(21))
    }
}
