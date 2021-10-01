package net.corda.configuration.read.impl

import net.corda.configuration.read.ConfigurationHandler
import net.corda.libs.configuration.read.ConfigReader
import java.util.concurrent.ConcurrentHashMap

class ConfigurationHandlerStorage {

    private val handlers: MutableMap<CallbackHandle, Unit> = ConcurrentHashMap()

    // YIFT: Temporary hack - see https://r3-cev.atlassian.net/browse/CORE-2703
    @Volatile
    private var subscription: ConfigReader? = null

    private class CallbackHandle(
        private val callback: ConfigurationHandler,
        private val storage: ConfigurationHandlerStorage
    ) : AutoCloseable {

        private var handle: AutoCloseable? = null

        fun subscribe(subscription: ConfigReader) {
            handle?.close()
            handle = subscription.registerCallback(callback::onNewConfiguration)
        }

        fun unregister() {
            handle?.close()
            handle = null
        }

        override fun close() {
            storage.remove(this)
            handle?.close()
            handle = null
        }
    }

    private fun remove(handle: CallbackHandle) {
        handlers.remove(handle)
    }

    fun add(callback: ConfigurationHandler) : AutoCloseable {
        val handle = CallbackHandle(callback, this)
        handlers[handle] = Unit
        // YIFT: Temporary hack - see https://r3-cev.atlassian.net/browse/CORE-2703 -
        // we must add the handler before looking at the subscription
        val sub = subscription
        if (sub != null) {
            handle.subscribe(sub)
        }
        return handle
    }

    fun addSubscription(subscription: ConfigReader) {
        // YIFT: Temporary hack - see https://r3-cev.atlassian.net/browse/CORE-2703
        this.subscription = subscription
        handlers.keys.forEach {
            it.subscribe(subscription)
        }
    }

    fun removeSubscription() {
        handlers.keys.forEach {
            it.unregister()
        }
    }
}