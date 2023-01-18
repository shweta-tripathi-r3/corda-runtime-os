package net.corda.applications.workers.workercommon

/**
 * Publishes information about the current worker to a topic.
 */
interface VersionPublisher {
    fun start(name: String)
    fun stop()
}