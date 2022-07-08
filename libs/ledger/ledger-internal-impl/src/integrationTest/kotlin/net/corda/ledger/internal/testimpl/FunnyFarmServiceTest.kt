package net.corda.ledger.internal.testimpl

import net.corda.ledger.internal.FunnyFarmService
import net.corda.testing.sandboxes.SandboxSetup
import net.corda.testing.sandboxes.fetchService
import net.corda.testing.sandboxes.groupcontext.VirtualNodeService
import net.corda.testing.sandboxes.lifecycle.EachTestLifecycle
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Suppress("FunctionName")
class FunnyFarmServiceTest {
    companion object {
        private const val TIMEOUT_MILLIS = 2000L

        init {
            println("FunnyFarm test module loaded")
        }
    }

    @RegisterExtension
    private val lifecycle = EachTestLifecycle()

    private lateinit var virtualNode: VirtualNodeService

    @InjectService(timeout = TIMEOUT_MILLIS)
    lateinit var funnyFarmService: FunnyFarmService

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
    fun `Can resolve and inject service`() {
        Assertions.assertEquals("moo", funnyFarmService.sayMoo())
    }
}
