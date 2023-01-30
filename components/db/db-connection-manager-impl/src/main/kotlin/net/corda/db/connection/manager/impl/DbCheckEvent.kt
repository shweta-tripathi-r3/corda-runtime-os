package net.corda.db.connection.manager.impl

import net.corda.lifecycle.TimerEvent

data class DbCheckEvent(override val key: String, val check: () -> Unit) : TimerEvent {
    fun run() = check()
}
