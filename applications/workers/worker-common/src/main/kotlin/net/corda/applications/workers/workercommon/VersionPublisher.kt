package net.corda.applications.workers.workercommon

import net.corda.lifecycle.Lifecycle

/**
 * Publishes information about the current worker to a topic.
 */
interface VersionPublisher: Lifecycle {
    fun start(name: String)
}