package net.corda.lifecycle.domino.logic.events

import com.typesafe.config.Config
import net.corda.lifecycle.LifecycleEvent

data class NewConfigurationEvent(val changedKeys: Set<String>, val config: Map<String, Config>): LifecycleEvent