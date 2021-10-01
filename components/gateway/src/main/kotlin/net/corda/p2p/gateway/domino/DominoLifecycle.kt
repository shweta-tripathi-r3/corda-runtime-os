package net.corda.p2p.gateway.domino

import net.corda.lifecycle.Lifecycle
import net.corda.lifecycle.LifecycleCoordinator

interface DominoLifecycle: Lifecycle {
    enum class State {
        Created,
        Started,
        StoppedDueToError,
        StoppedByParent
    }

    fun state(): State

    fun coordinator(): LifecycleCoordinator
}