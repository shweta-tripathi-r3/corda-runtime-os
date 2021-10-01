package net.corda.p2p.gateway.domino

import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.impl.LifecycleCoordinatorFactoryImpl
import net.corda.p2p.gateway.domino.DominoLifecycle.*
import net.corda.test.util.eventually
import net.corda.v5.base.util.seconds
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.util.UUID

class DominoLifecycleTest {

    @Test
    fun `test domino lifecycle`() {
        val lifecycleCoordinatorFactory = LifecycleCoordinatorFactoryImpl()
        val b = B2(lifecycleCoordinatorFactory)
        val c = C2(lifecycleCoordinatorFactory)
        val a = A2(lifecycleCoordinatorFactory, b, c)

        println("Starting A!")

        a.start()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Started)
            Assertions.assertThat(b.state).isEqualTo(State.Started)
            Assertions.assertThat(c.state).isEqualTo(State.Started)
        }

        println("Failing B!")

        b.failed()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(b.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(c.state).isEqualTo(State.StoppedByParent)
        }

        println("Failing C!")

        c.failed()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(b.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(c.state).isEqualTo(State.StoppedByParent)
        }

        println("B recovered!")

        b.recoverFromFailure()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Started)
            Assertions.assertThat(b.state).isEqualTo(State.Started)
            Assertions.assertThat(c.state).isEqualTo(State.Started)
        }
    }

}

class C2(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory): LeafDominoLifecycle(lifecycleCoordinatorFactory, "C-${UUID.randomUUID()}") {
    override fun startSequence() {}

    override fun stopSequence() {}

    fun failed() {
        gotError(RuntimeException("something went wrong!"))
    }

    fun recoverFromFailure() {
        if (state == State.StoppedDueToError) {
            start()
        }
    }

}

class B2(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory): LeafDominoLifecycle(lifecycleCoordinatorFactory, "B-${UUID.randomUUID()}") {
    override fun startSequence() {}

    override fun stopSequence() {}

    fun failed() {
        gotError(RuntimeException("something went wrong!"))
    }

    fun recoverFromFailure() {
        if (state == State.StoppedDueToError) {
            start()
        }
    }
}

class A2(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory, private val b: B2, private val c: C2): NonLeafDominoLifecycle(lifecycleCoordinatorFactory, "B-${UUID.randomUUID()}") {
    override fun children(): List<DominoLifecycle> = listOf(b, c)
}