package net.corda.p2p.gateway.domino

import net.corda.lifecycle.Lifecycle
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleStatus

/**
 * [start] is asynchronous, its completion does not mean the component is started.
 * This can be determined by observing for [LifecycleStatus.UP] events on its coordinator.
 *
 * [stop] is synchronous, its completion indicates the component is stopped.
 */
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