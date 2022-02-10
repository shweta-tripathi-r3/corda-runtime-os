package net.corda.applications.workers.db.test

import net.corda.applications.workers.db.DBWorker
import net.corda.applications.workers.workercommon.HealthMonitor
import net.corda.libs.configuration.SmartConfig
import net.corda.osgi.api.Shutdown
import net.corda.processors.db.DBProcessor
import net.corda.schema.configuration.ConfigKeys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.osgi.framework.Bundle

/**
 * Tests handling of command-line arguments for the [DBWorker].
 *
 * Since the behaviour is almost identical across workers, we do not have equivalent tests for the other worker types.
 */
class ConfigTests {

    @Test
    fun `instance ID, topic prefix, messaging params, database params and additional params are passed through to the processor`() {
        val processor = DummyProcessor()
        val dbWorker = DBWorker(processor, DummyShutdown(), DummyHealthMonitor())
        val args = arrayOf(
            FLAG_INSTANCE_ID, VAL_INSTANCE_ID,
            FLAG_TOPIC_PREFIX, VALUE_TOPIC_PREFIX,
            FLAG_MSG_PARAM, "$MSG_KEY_ONE=$MSG_VAL_ONE",
            FLAG_DB_PARAM, "$DB_KEY_ONE=$DB_VAL_ONE",
            FLAG_ADDITIONAL_PARAM, "$CUSTOM_KEY_ONE=$CUSTOM_VAL_ONE",
        )
        dbWorker.startup(args)
        val config = processor.config!!

        val expectedKeys = setOf(
            KEY_INSTANCE_ID,
            KEY_TOPIC_PREFIX,
            "$CUSTOM_CONFIG_PATH.$CUSTOM_KEY_ONE",
            "$MSG_CONFIG_PATH.$MSG_KEY_ONE",
            "${ConfigKeys.DB_CONFIG}.$DB_KEY_ONE"
        )
        val actualKeys = config.entrySet().map { entry -> entry.key }.toSet()
        assertEquals(expectedKeys, actualKeys)

        assertEquals(VAL_INSTANCE_ID.toInt(), config.getAnyRef(KEY_INSTANCE_ID))
        assertEquals(VALUE_TOPIC_PREFIX, config.getAnyRef(KEY_TOPIC_PREFIX))
        assertEquals(MSG_VAL_ONE, config.getAnyRef("$MSG_CONFIG_PATH.$MSG_KEY_ONE"))
        assertEquals(DB_VAL_ONE, config.getAnyRef("${ConfigKeys.DB_CONFIG}.$DB_KEY_ONE"))
        assertEquals(CUSTOM_VAL_ONE, config.getAnyRef("$CUSTOM_CONFIG_PATH.$CUSTOM_KEY_ONE"))
    }

    @Test
    fun `other params are not passed through to the processor`() {
        val processor = DummyProcessor()
        val dbWorker = DBWorker(processor, DummyShutdown(), DummyHealthMonitor())
        val args = arrayOf(
            FLAG_DISABLE_MONITOR,
            FLAG_MONITOR_PORT, "9999"
        )
        dbWorker.startup(args)
        val config = processor.config!!

        // Instance ID and topic prefix are always present, with default values if none are provided.
        val expectedKeys = setOf(KEY_INSTANCE_ID, KEY_TOPIC_PREFIX)
        val actualKeys = config.entrySet().map { entry -> entry.key }.toSet()
        assertEquals(expectedKeys, actualKeys)
    }

    @Test
    fun `defaults are provided for instance ID and topic prefix`() {
        val processor = DummyProcessor()
        val dbWorker = DBWorker(processor, DummyShutdown(), DummyHealthMonitor())
        val args = arrayOf<String>()
        dbWorker.startup(args)
        val config = processor.config!!

        val expectedKeys = setOf(KEY_INSTANCE_ID, KEY_TOPIC_PREFIX)
        val actualKeys = config.entrySet().map { entry -> entry.key }.toSet()
        assertEquals(expectedKeys, actualKeys)

        // The default for instance ID is randomly generated, so its value can't be tested for.
        assertEquals(DEFAULT_TOPIC_PREFIX, config.getAnyRef(KEY_TOPIC_PREFIX))
    }

    @Test
    fun `multiple messaging params can be provided`() {
        val processor = DummyProcessor()
        val dbWorker = DBWorker(processor, DummyShutdown(), DummyHealthMonitor())
        val args = arrayOf(
            FLAG_MSG_PARAM, "$MSG_KEY_ONE=$MSG_VAL_ONE",
            FLAG_MSG_PARAM, "$MSG_KEY_TWO=$MSG_VAL_TWO"
        )
        dbWorker.startup(args)
        val config = processor.config!!

        assertEquals(MSG_VAL_ONE, config.getAnyRef("$MSG_CONFIG_PATH.$MSG_KEY_ONE"))
        assertEquals(MSG_VAL_TWO, config.getAnyRef("$MSG_CONFIG_PATH.$MSG_KEY_TWO"))
    }

    @Test
    fun `multiple database params can be provided`() {
        val processor = DummyProcessor()
        val dbWorker = DBWorker(processor, DummyShutdown(), DummyHealthMonitor())
        val args = arrayOf(
            FLAG_DB_PARAM, "$DB_KEY_ONE=$DB_VAL_ONE",
            FLAG_DB_PARAM, "$DB_KEY_TWO=$DB_VAL_TWO"
        )
        dbWorker.startup(args)
        val config = processor.config!!

        assertEquals(DB_VAL_ONE, config.getAnyRef("${ConfigKeys.DB_CONFIG}.$DB_KEY_ONE"))
        assertEquals(DB_VAL_TWO, config.getAnyRef("${ConfigKeys.DB_CONFIG}.$DB_KEY_TWO"))
    }

    @Test
    fun `multiple additional params can be provided`() {
        val processor = DummyProcessor()
        val dbWorker = DBWorker(processor, DummyShutdown(), DummyHealthMonitor())
        val args = arrayOf(
            FLAG_ADDITIONAL_PARAM, "$CUSTOM_KEY_ONE=$CUSTOM_VAL_ONE",
            FLAG_ADDITIONAL_PARAM, "$CUSTOM_KEY_TWO=$CUSTOM_VAL_TWO"
        )
        dbWorker.startup(args)
        val config = processor.config!!

        assertEquals(CUSTOM_VAL_ONE, config.getAnyRef("$CUSTOM_CONFIG_PATH.$CUSTOM_KEY_ONE"))
        assertEquals(CUSTOM_VAL_TWO, config.getAnyRef("$CUSTOM_CONFIG_PATH.$CUSTOM_KEY_TWO"))
    }

    /** A [DBProcessor] that stores the passed-in config in [config] for inspection. */
    private class DummyProcessor : DBProcessor {
        var config: SmartConfig? = null

        override fun start(bootConfig: SmartConfig) {
            this.config = bootConfig
        }

        override fun stop() = throw NotImplementedError()
    }

    /** A no-op [Shutdown]. */
    private class DummyShutdown : Shutdown {
        override fun shutdown(bundle: Bundle) = Unit
    }

    /** A no-op [HealthMonitor]. */
    private class DummyHealthMonitor : HealthMonitor {
        override fun listen(port: Int) = Unit
        override fun stop() = throw NotImplementedError()
    }
}