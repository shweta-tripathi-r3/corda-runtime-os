package net.corda.p2p.gateway.domino

import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.impl.LifecycleCoordinatorFactoryImpl
import net.corda.test.util.eventually
import net.corda.v5.base.util.millis
import net.corda.v5.base.util.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.util.UUID

class LifecycleWithCoordinatorTest {

    @Test
    fun `test LifecycleWithCoordinator`() {
        val lifecycleCoordinatorFactory = LifecycleCoordinatorFactoryImpl()
        val b = B(lifecycleCoordinatorFactory)
        val c = C(lifecycleCoordinatorFactory)
        val a = A(lifecycleCoordinatorFactory, b, c)

        println("Starting A!")

        a.start()

        eventually(1.seconds) {
            assertThat(a.state).isEqualTo(LifecycleWithCoordinator.State.Started)
            assertThat(b.state).isEqualTo(LifecycleWithCoordinator.State.Started)
            assertThat(c.state).isEqualTo(LifecycleWithCoordinator.State.Started)
        }

        println("Failing B!")

        b.failed()

        eventually(1.seconds) {
            assertThat(a.state).isEqualTo(LifecycleWithCoordinator.State.StoppedDueToError)
            assertThat(b.state).isEqualTo(LifecycleWithCoordinator.State.StoppedDueToError)
            assertThat(c.state).isEqualTo(LifecycleWithCoordinator.State.StoppedByParent)
        }

        println("Failing C!")

        c.failed()

        eventually(1.seconds) {
            assertThat(a.state).isEqualTo(LifecycleWithCoordinator.State.StoppedDueToError)
            assertThat(b.state).isEqualTo(LifecycleWithCoordinator.State.StoppedDueToError)
            assertThat(c.state).isEqualTo(LifecycleWithCoordinator.State.StoppedByParent)
        }

        println("B recovered!")

        b.recoverFromFailure()

        eventually(1.seconds) {
            assertThat(a.state).isEqualTo(LifecycleWithCoordinator.State.Started)
            assertThat(b.state).isEqualTo(LifecycleWithCoordinator.State.Started)
            assertThat(c.state).isEqualTo(LifecycleWithCoordinator.State.Started)
        }
    }

}

class B(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory): LifecycleWithCoordinator(lifecycleCoordinatorFactory, "B-${UUID.randomUUID()}") {

    override val children: Collection<LifecycleWithCoordinator>
        get() = emptyList()

    fun failed() {
        gotError(RuntimeException("something went wrong!"))
    }

    fun recoverFromFailure() {
        if (state == State.StoppedDueToError) {
            start()
        }
    }

}

class C(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory): LifecycleWithCoordinator(lifecycleCoordinatorFactory, "C-${UUID.randomUUID()}") {
    override val children: Collection<LifecycleWithCoordinator>
        get() = emptyList()

    fun failed() {
        gotError(RuntimeException("something went wrong!"))
    }

    fun recoverFromFailure() {
        if (state == State.StoppedDueToError) {
            start()
        }
    }

}

class A(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory, private val b: B, private val c: C): LifecycleWithCoordinator(lifecycleCoordinatorFactory, "A-${UUID.randomUUID()}") {

    override val children: Collection<LifecycleWithCoordinator>
        get() = listOf(b, c)

}