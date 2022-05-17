package net.corda.flow.service

import net.corda.configuration.read.ConfigurationReadService
import net.corda.cpiinfo.read.CpiInfoReadService
import net.corda.flow.scheduler.FlowWakeUpScheduler
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.LifecycleCoordinatorScheduler
import net.corda.lifecycle.LifecycleCoordinatorSchedulerFactory
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.createCoordinator
import net.corda.lifecycle.impl.LifecycleCoordinatorFactoryImpl
import net.corda.lifecycle.impl.registry.LifecycleRegistryImpl
import net.corda.sandboxgroupcontext.service.SandboxGroupContextComponent
import net.corda.schema.configuration.ConfigKeys
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Suppress("Unsed")
class FlowServiceTest {

    class TestLifecycleCoordinatorSchedulerFactory : LifecycleCoordinatorSchedulerFactory {
        override fun create(): LifecycleCoordinatorScheduler {
            return TestLifecycleCoordinatorScheduler()
        }
    }

    class TestLifecycleCoordinatorScheduler : LifecycleCoordinatorScheduler {
        override fun execute(task: Runnable) {
            task.run()
        }

        override fun timerSchedule(command: Runnable, delay: Long, unit: TimeUnit): ScheduledFuture<*> {
            TODO("Not yet implemented")
        }
    }

    private val configurationReadService = mock<ConfigurationReadService>()
    private val flowExecutor = mock<FlowExecutor>()
    private val flowWakeUpScheduler = mock<FlowWakeUpScheduler>()

    private val registry = LifecycleRegistryImpl()
    private val coordinatorFactory = LifecycleCoordinatorFactoryImpl(
        registry,
        TestLifecycleCoordinatorSchedulerFactory()
    )

    private val configurationReadServiceCoordinator = registerCoordinator<ConfigurationReadService>()
    private val sandboxGroupContextComponentCoordinator = registerCoordinator<SandboxGroupContextComponent>()
    private val virtualNodeInfoReadServiceCoordinator = registerCoordinator<VirtualNodeInfoReadService>()
    private val cpiInfoReadServiceCoordinator = registerCoordinator<CpiInfoReadService>()
    private val flowExecutorCoordinator = registerCoordinator<FlowExecutor>()

    private val flowService = FlowService(
        coordinatorFactory,
        configurationReadService,
        flowExecutor,
        flowWakeUpScheduler
    )

    @Test
    fun `lifecycle - start flow executor when flow service starts`(){
        flowService.start()
        verify(flowExecutor).start()
    }

    @Test
    fun `lifecycle - Service not up until all dependencies are up`() {
        val flowServiceCoordinator = getCoordinator<FlowService>()
        flowService.start()

        assertThat(flowServiceCoordinator.isUp()).isFalse

        configurationReadServiceCoordinator.markAsUp()
        assertThat(flowServiceCoordinator.isUp()).isFalse

        sandboxGroupContextComponentCoordinator.markAsUp()
        assertThat(flowServiceCoordinator.isUp()).isFalse

        virtualNodeInfoReadServiceCoordinator.markAsUp()
        assertThat(flowServiceCoordinator.isUp()).isFalse

        cpiInfoReadServiceCoordinator.markAsUp()
        assertThat(flowServiceCoordinator.isUp()).isFalse

        flowExecutorCoordinator.markAsUp()
        assertThat(flowServiceCoordinator.isUp()).isTrue
    }

    @Test
    fun `lifecycle - When all dependencies are up then register for configuration change`() {
        val flowServiceCoordinator = getCoordinator<FlowService>()
        flowService.start()

        registry.markAllAsUpApartFrom<FlowService>()

        verify(configurationReadService).registerComponentForUpdates(
            eq(flowServiceCoordinator),
            eq(setOf(ConfigKeys.BOOT_CONFIG, ConfigKeys.MESSAGING_CONFIG))
        )
    }

    @Test
    fun `lifecycle - when any dependency goes down the service goes down`(){
        val flowServiceCoordinator = getCoordinator<FlowService>()
        flowService.start()

        registry.markAllAsUpApartFrom<FlowService>()

        assertThat(flowServiceCoordinator.isUp()).isTrue

        configurationReadServiceCoordinator.markAsDown()

        assertThat(flowServiceCoordinator.isDown()).isTrue
    }

    @Test
    fun `lifecycle - when any dependency goes to error the service goes to error`(){
        val flowServiceCoordinator = getCoordinator<FlowService>()
        flowService.start()

        registry.markAllAsUpApartFrom<FlowService>()

        assertThat(flowServiceCoordinator.isUp()).isTrue

        cpiInfoReadServiceCoordinator.markAsError()

        assertThat(flowServiceCoordinator.isError()).isTrue
    }

    private inline fun <reified T> registerCoordinator(): LifecycleCoordinator {
        return coordinatorFactory.createCoordinator<T>(mock())
    }

    private fun LifecycleCoordinator.isUp(): Boolean {
        return this.status == LifecycleStatus.UP
    }

    private fun LifecycleCoordinator.isDown(): Boolean {
        return this.status == LifecycleStatus.DOWN
    }

    private fun LifecycleCoordinator.isError(): Boolean {
        return this.status == LifecycleStatus.ERROR
    }

    private fun LifecycleCoordinator.markAsUp() {
        this.start()
        this.updateStatus(LifecycleStatus.UP)
    }

    private fun LifecycleCoordinator.markAsDown() {
        this.start()
        this.updateStatus(LifecycleStatus.DOWN)
    }

    private fun LifecycleCoordinator.markAsError() {
        this.start()
        this.updateStatus(LifecycleStatus.ERROR)
    }

    private inline fun <reified T>  LifecycleRegistryImpl.markAllAsUpApartFrom() {
        val exclusionName = LifecycleCoordinatorName.forComponent<T>()
        this.componentStatus()
            .filter { it.key != exclusionName }
            .forEach { this.getCoordinator(it.key).markAsUp() }
    }

    private inline fun <reified T> getCoordinator(): LifecycleCoordinator {
        return registry.getCoordinator(LifecycleCoordinatorName.forComponent<T>())
    }
}