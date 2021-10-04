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
            Assertions.assertThat(a.state).isEqualTo(State.Created)
            Assertions.assertThat(b.state).isEqualTo(State.Created)
            Assertions.assertThat(c.state).isEqualTo(State.Created)
        }

        println("B has started!")

        b.correctConfigurationArrived()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Created)
            Assertions.assertThat(b.state).isEqualTo(State.Started)
            Assertions.assertThat(c.state).isEqualTo(State.Created)
        }

        println("B gets wrong configuration!")

        b.incorrectConfigurationArrived()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Created)
            Assertions.assertThat(b.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(c.state).isEqualTo(State.StoppedByParent)
        }

        println("C has started!")

        c.correctConfigurationArrived()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Created)
            Assertions.assertThat(b.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(c.state).isEqualTo(State.StoppedByParent)
        }

        println("B gets correct configuration!")

        b.correctConfigurationArrived()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Started)
            Assertions.assertThat(b.state).isEqualTo(State.Started)
            Assertions.assertThat(c.state).isEqualTo(State.Started)
        }

        println("C failed!")

        c.failed()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.StoppedDueToError)
            Assertions.assertThat(b.state).isEqualTo(State.StoppedByParent)
            Assertions.assertThat(c.state).isEqualTo(State.StoppedDueToError)
        }

        println("C recovered!")

        c.recoverFromFailure()

        eventually(1.seconds) {
            Assertions.assertThat(a.state).isEqualTo(State.Started)
            Assertions.assertThat(b.state).isEqualTo(State.Started)
            Assertions.assertThat(c.state).isEqualTo(State.Started)
        }
    }

}

class C2(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory): LeafDominoLifecycle(lifecycleCoordinatorFactory, UUID.randomUUID().toString()) {

    private var correctConfigHasArrived = false

    override fun startSequence() {
        println("Start component's resources")
        if (correctConfigHasArrived) {
            hasStarted()
        }
    }

    override fun stopSequence() {
        println("Stopping component's resources")
    }

    fun correctConfigurationArrived() {
        println("Correct configuration arrived")
        correctConfigHasArrived = true
        hasStarted()
    }

    fun incorrectConfigurationArrived() {
        println("Bad configuration arrived")
        correctConfigHasArrived = false
        failed("Incorrect configuration!")
    }

    fun failed(error: String = "Something went wrong!") {
        gotError(RuntimeException(error))
    }

    fun recoverFromFailure() {
        hasStarted()
    }

}

class B2(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory): LeafDominoLifecycle(lifecycleCoordinatorFactory, UUID.randomUUID().toString()) {

    private var correctConfigHasArrived = false

    override fun startSequence() {
        println("Start component's resources")
        if (correctConfigHasArrived) {
            hasStarted()
        }
    }

    override fun stopSequence() {
        println("Stopping component's resources")
    }

    fun correctConfigurationArrived() {
        println("Correct configuration arrived")
        correctConfigHasArrived = true
        hasStarted()
    }

    fun incorrectConfigurationArrived() {
        println("Bad configuration arrived")
        correctConfigHasArrived = false
        failed("Incorrect configuration!")
    }

    fun failed(error: String = "Something went wrong!") {
        gotError(RuntimeException(error))
    }

    fun recoverFromFailure() {
        hasStarted()
    }
}

class A2(lifecycleCoordinatorFactory: LifecycleCoordinatorFactory, private val b: B2, private val c: C2): NonLeafDominoLifecycle(lifecycleCoordinatorFactory, UUID.randomUUID().toString()) {
    override fun children(): List<DominoLifecycle> = listOf(b, c)
}