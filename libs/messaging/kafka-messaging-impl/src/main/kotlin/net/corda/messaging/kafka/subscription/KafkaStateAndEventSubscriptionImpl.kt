package net.corda.messaging.kafka.subscription

import com.typesafe.config.Config
import net.corda.data.deadletter.StateAndEventDeadLetterRecord
import net.corda.messaging.api.exception.CordaMessageAPIFatalException
import net.corda.messaging.api.exception.CordaMessageAPIIntermittentException
import net.corda.messaging.api.processor.StateAndEventProcessor
import net.corda.messaging.api.records.Record
import net.corda.messaging.api.subscription.StateAndEventSubscription
import net.corda.messaging.api.subscription.listener.StateAndEventListener
import net.corda.messaging.kafka.producer.wrapper.CordaKafkaProducer
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.CONSUMER_CLOSE_TIMEOUT
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.CONSUMER_MAX_POLL_INTERVAL
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.CONSUMER_POLL_AND_PROCESS_RETRIES
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.CONSUMER_PROCESSOR_TIMEOUT
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.CONSUMER_THREAD_STOP_TIMEOUT
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.DEAD_LETTER_QUEUE_SUFFIX
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.KAFKA_PRODUCER
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.LISTENER_TIMEOUT
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.PRODUCER_CLIENT_ID
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.PRODUCER_CLOSE_TIMEOUT
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.PRODUCER_TRANSACTIONAL_ID
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.TOPIC_NAME
import net.corda.messaging.kafka.properties.KafkaProperties.Companion.TOPIC_PREFIX
import net.corda.messaging.kafka.publisher.CordaAvroSerializer
import net.corda.messaging.kafka.subscription.consumer.builder.StateAndEventBuilder
import net.corda.messaging.kafka.subscription.consumer.wrapper.ConsumerRecordAndMeta
import net.corda.messaging.kafka.subscription.consumer.wrapper.CordaKafkaConsumer
import net.corda.messaging.kafka.subscription.consumer.wrapper.asRecord
import net.corda.messaging.kafka.subscription.factory.SubscriptionMapFactory
import net.corda.messaging.kafka.utils.getEventsByBatch
import net.corda.messaging.kafka.utils.render
import net.corda.messaging.kafka.utils.tryGetFutureResult
import net.corda.schema.registry.AvroSchemaRegistry
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.base.util.debug
import net.corda.v5.base.util.trace
import net.corda.v5.base.util.uncheckedCast
import org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.slf4j.LoggerFactory
import java.lang.System.currentTimeMillis
import java.nio.ByteBuffer
import java.time.Clock
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class Topic(val prefix: String, val suffix: String) {
    val topic
        get() = prefix + suffix
}

@Suppress("TooManyFunctions", "LongParameterList")
class KafkaStateAndEventSubscriptionImpl<K : Any, S : Any, E : Any>(
    private val config: Config,
    private val mapFactory: SubscriptionMapFactory<K, Pair<Long, S>>,
    private val builder: StateAndEventBuilder<K, S, E>,
    private val processor: StateAndEventProcessor<K, S, E>,
    private val avroSchemaRegistry: AvroSchemaRegistry,
    private val stateAndEventListener: StateAndEventListener<K, S>? = null,
    private val clock: Clock = Clock.systemUTC()
) : StateAndEventSubscription<K, S, E>, ConsumerRebalanceListener {

    companion object {
        private const val STATE_CONSUMER = "stateConsumer"
        private const val EVENT_CONSUMER = "eventConsumer"
        private const val STATE_TOPIC_NAME = "$STATE_CONSUMER.$TOPIC_NAME"
        private const val EVENT_GROUP_ID = "$EVENT_CONSUMER.$GROUP_ID_CONFIG"
        private val EVENT_CONSUMER_THREAD_STOP_TIMEOUT = CONSUMER_THREAD_STOP_TIMEOUT.replace("consumer", "eventConsumer")
        private val EVENT_CONSUMER_CLOSE_TIMEOUT = CONSUMER_CLOSE_TIMEOUT.replace("consumer", "eventConsumer")
        private val EVENT_CONSUMER_POLL_AND_PROCESS_RETRIES = CONSUMER_POLL_AND_PROCESS_RETRIES.replace("consumer", "eventConsumer")
        //short timeout for poll of paused partitions when waiting for processor to finish
        private val PAUSED_POLL_TIMEOUT = Duration.ofMillis(100)
        private const val MIN_THREADS = 1
    }

    private val executor = Executors.newScheduledThreadPool(MIN_THREADS) { runnable ->
        val thread = Thread(runnable)
        thread.isDaemon = true
        thread
    }

    private val log = LoggerFactory.getLogger(
        "${config.getString(EVENT_GROUP_ID)}.${config.getString(PRODUCER_TRANSACTIONAL_ID)}"
    )

    private lateinit var producer: CordaKafkaProducer
    private lateinit var eventConsumer: CordaKafkaConsumer<K, E>
    private lateinit var stateConsumer: CordaKafkaConsumer<K, S>
    private val currentStates: MutableMap<Int, MutableMap<K, Pair<Long, S>>> = mutableMapOf()

    @Volatile
    private var stopped = false
    private val lock = ReentrantLock()
    private var consumeLoopThread: Thread? = null
    private var nextPollIntervalCutoff: Long = 0

    private val cordaAvroSerializer = CordaAvroSerializer<Any>(avroSchemaRegistry)

    private val topicPrefix = config.getString(TOPIC_PREFIX)
    private val eventTopic = Topic(topicPrefix, config.getString(TOPIC_NAME))
    private val stateTopic = Topic(topicPrefix, config.getString(STATE_TOPIC_NAME))
    private val groupName = config.getString(EVENT_GROUP_ID)
    private val producerClientId: String = config.getString(PRODUCER_CLIENT_ID)
    private val consumerThreadStopTimeout = config.getLong(EVENT_CONSUMER_THREAD_STOP_TIMEOUT)
    private val producerCloseTimeout = Duration.ofMillis(config.getLong(PRODUCER_CLOSE_TIMEOUT))
    private val consumerCloseTimeout = Duration.ofMillis(config.getLong(EVENT_CONSUMER_CLOSE_TIMEOUT))
    private val consumerPollAndProcessMaxRetries = config.getLong(EVENT_CONSUMER_POLL_AND_PROCESS_RETRIES)
    private val maxPollInterval = config.getLong(CONSUMER_MAX_POLL_INTERVAL.replace("consumer", "eventConsumer"))

    private val processorTimeout = config.getLong(CONSUMER_PROCESSOR_TIMEOUT.replace("consumer", "eventConsumer"))
    private val initialProcessorTimeout = maxPollInterval / 4
    private val listenerTimeout = config.getLong(LISTENER_TIMEOUT)
    private val deadLetterQueueSuffix = config.getString(DEAD_LETTER_QUEUE_SUFFIX)

    // When syncing up new partitions gives us the (partition, endOffset) for a given new partition
    private val statePartitionsToSync: MutableMap<Int, Long> = ConcurrentHashMap<Int, Long>()

    /**
     * Is the subscription running.
     */
    override val isRunning: Boolean
        get() {
            return !stopped
        }

    override fun start() {
        log.debug { "Starting subscription with config:\n${config.render()}" }
        lock.withLock {
            if (consumeLoopThread == null) {
                stopped = false
                consumeLoopThread = thread(
                    start = true,
                    isDaemon = true,
                    contextClassLoader = null,
                    name = "state/event processing thread $groupName-($stateTopic.$eventTopic)",
                    priority = -1,
                    block = ::runConsumeLoop
                )
            }
        }
    }

    override fun stop() {
        if (!stopped) {
            val thread = lock.withLock {
                stopped = true
                val threadTmp = consumeLoopThread
                consumeLoopThread = null
                threadTmp
            }
            thread?.join(consumerThreadStopTimeout)
        }
    }

    /**
     * This is not guaranteed to be thread-safe!
     */
    override fun getValue(key: K): S? {
        currentStates.forEach {
            val state = it.value[key]
            if (state != null) {
                return state.second
            }
        }

        return null
    }

    private fun getStatesForPartition(partitionId: Int): Map<K, S> {
        return currentStates[partitionId]?.map { state -> Pair(state.key, state.value.second) }?.toMap() ?: mapOf()
    }

    @Suppress("TooGenericExceptionCaught")
    fun runConsumeLoop() {
        var attempts = 0
        while (!stopped) {
            attempts++
            try {
                producer = builder.createProducer(config.getConfig(KAFKA_PRODUCER))
                stateConsumer = builder.createStateConsumer(config.getConfig(STATE_CONSUMER), processor.keyClass, processor.stateValueClass)
                eventConsumer =
                    builder.createEventConsumer(config.getConfig(EVENT_CONSUMER), processor.keyClass, processor.eventValueClass, this)
                validateConsumers()

                eventConsumer.subscribeToTopic()

                while (!stopped) {
                    updateStates(true)
                    processEvents()
                }
            } catch (ex: Exception) {
                when (ex) {
                    is CordaMessageAPIIntermittentException -> {
                        log.warn(
                            "Failed to read and process records from topic $eventTopic, group $groupName, " +
                                    "producerClientId $producerClientId. Attempts: $attempts. Recreating " +
                                    "consumer/producer and Retrying.", ex
                        )
                    }
                    else -> {
                        log.error(
                            "Failed to read and process records from topic $eventTopic, group $groupName, " +
                                    "producerClientId $producerClientId. Attempts: $attempts. Closing " +
                                    "subscription.", ex
                        )
                        stop()
                    }
                }
            } finally {
                producer.close(producerCloseTimeout)
                eventConsumer.close(consumerCloseTimeout)
                stateConsumer.close(consumerCloseTimeout)
            }
        }
        producer.close(producerCloseTimeout)
        eventConsumer.close(consumerCloseTimeout)
        stateConsumer.close(consumerCloseTimeout)
    }

    private fun validateConsumers() {
        val statePartitions = stateConsumer.getPartitions(stateTopic.topic, Duration.ofSeconds(consumerThreadStopTimeout))
        val eventPartitions = eventConsumer.getPartitions(eventTopic.topic, Duration.ofSeconds(consumerThreadStopTimeout))
        if (statePartitions.size != eventPartitions.size) {
            val errorMsg = "Mismatch between state and event partitions."
            log.debug {
                errorMsg + "\n" +
                        "state: ${statePartitions.joinToString()}\n" +
                        "event: ${eventPartitions.joinToString()}"
            }
            throw CordaRuntimeException(errorMsg)
        }

        val deadLetterQueueExists = eventConsumer.listTopics().keys.contains("${eventTopic.topic}$deadLetterQueueSuffix")
        if (!deadLetterQueueExists) {
            val errorMsg = "No dead letter queue exists for the event topic $eventTopic"
            log.debug(errorMsg)
            throw CordaRuntimeException(errorMsg)
        }
    }

    /**
     *  This rebalance is called for the event consumer, though most of the work is to ensure the state consumer
     *  keeps up
     */
    override fun onPartitionsAssigned(newEventPartitions: Collection<TopicPartition>) {
        log.debug { "Updating state partitions to match new event partitions: $newEventPartitions" }
        val newStatePartitions = newEventPartitions.toStateTopics()
        val statePartitions = stateConsumer.assignment() + newStatePartitions
        stateConsumer.assign(statePartitions)
        stateConsumer.seekToBeginning(newStatePartitions)

        // Initialise the housekeeping here but the sync and updates
        // will be handled in the normal poll cycle
        val syncablePartitions = filterSyncablePartitions(newStatePartitions)
        log.debug { "Syncing the following new state partitions: $syncablePartitions" }
        statePartitionsToSync.putAll(syncablePartitions)
        eventConsumer.pause(syncablePartitions.map { TopicPartition(eventTopic.topic, it.first) })

        statePartitions.forEach {
            currentStates.computeIfAbsent(it.partition()) { mapFactory.createMap() }
        }
    }

    private fun filterSyncablePartitions(newStatePartitions: List<TopicPartition>): List<Pair<Int, Long>> {
        val beginningOffsets = stateConsumer.beginningOffsets(newStatePartitions)
        val endOffsets = stateConsumer.endOffsets(newStatePartitions)
        return newStatePartitions.mapNotNull {
            val beginningOffset = beginningOffsets[it] ?: 0
            val endOffset = endOffsets[it] ?: 0
            if (beginningOffset < endOffset) {
                Pair(it.partition(), endOffset)
            } else {
                null
            }
        }
    }

    /**
     *  This rebalance is called for the event consumer, though most of the work is to ensure the state consumer
     *  keeps up
     */
    override fun onPartitionsRevoked(removedEventPartitions: Collection<TopicPartition>) {
        log.debug { "Updating state partitions to match removed event partitions: $removedEventPartitions" }
        val removedStatePartitions = removedEventPartitions.toStateTopics()
        val statePartitions = stateConsumer.assignment() - removedStatePartitions
        stateConsumer.assign(statePartitions)
        for (topicPartition in removedStatePartitions) {
            val partitionId = topicPartition.partition()
            statePartitionsToSync.remove(partitionId)

            currentStates[partitionId]?.let { partitionStates ->
                stateAndEventListener?.let { listener ->
                    waitForFunctionToFinish(
                        { listener.onPartitionLost(getStatesForPartition(partitionId)) },
                        listenerTimeout,
                        "StateAndEventListener timed out for onPartitionLost operation on partition $topicPartition"
                    )
                }

                mapFactory.destroyMap(partitionStates)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processEvents() {
        var attempts = 0
        var pollAndProcessSuccessful = false
        while (!pollAndProcessSuccessful && !stopped) {
            try {
                val polledEvents = eventConsumer.poll()
                nextPollIntervalCutoff = getNextPollIntervalCutoff()
                for (batch in getEventsByBatch(polledEvents)) {
                    tryProcessBatchOfEvents(batch)
                }
                pollAndProcessSuccessful = true
            } catch (ex: Exception) {
                when (ex) {
                    is CordaMessageAPIIntermittentException -> {
                        attempts++
                        handleProcessEventRetries(attempts, ex)
                    }
                    else -> {
                        throw CordaMessageAPIFatalException(
                            "Failed to process records from topic $eventTopic, group $groupName, producerClientId $producerClientId. " +
                                    "Fatal error occurred.", ex
                        )
                    }
                }
            }
        }
    }

    private fun tryProcessBatchOfEvents(events: List<ConsumerRecordAndMeta<K, E>>) {
        val outputRecords = mutableListOf<Record<*, *>>()
        val updatedStates: MutableMap<Int, MutableMap<K, S?>> = mutableMapOf()

        log.trace { "Processing events(size: ${events.size})" }
        for (event in events) {
            resetPollInterval()
            processEvent(event, outputRecords, updatedStates)
        }

        producer.beginTransaction()
        producer.sendRecords(outputRecords)
        producer.sendRecordOffsetsToTransaction(eventConsumer, events.map { it.record })
        producer.tryCommitTransaction()
        log.trace { "Processing of events(size: ${events.size}) complete" }

        onProcessorStateUpdated(updatedStates)
    }

    private fun processEvent(
        event: ConsumerRecordAndMeta<K, E>,
        outputRecords: MutableList<Record<*, *>>,
        updatedStates: MutableMap<Int, MutableMap<K, S?>>
    ) {
        log.trace { "Processing event: $event" }
        val key = event.record.key()
        val state = getValue(key)
        val partitionId = event.record.partition()
        val thisEventUpdates = getUpdatesForEvent(state, event)

        if (thisEventUpdates == null) {
            log.error("Sending event: $event, and state: $state to dead letter queue. Processor failed to complete.")
            outputRecords.add(generateDeadLetterRecord(event.record, state))
            outputRecords.add(Record(stateTopic.suffix, key, null))
            updatedStates.computeIfAbsent(partitionId) { mutableMapOf() }[key] = null
        } else {
            outputRecords.addAll(thisEventUpdates.responseEvents)
            val updatedState = thisEventUpdates.updatedState
            outputRecords.add(Record(stateTopic.suffix, key, updatedState))
            updatedStates.computeIfAbsent(partitionId) { mutableMapOf() }[key] = updatedState
            log.trace { "Completed event: $event" }
        }
    }

    private fun getUpdatesForEvent(state: S?, event: ConsumerRecordAndMeta<K, E>): StateAndEventProcessor.Response<S>? {
        val processorFuture: CompletableFuture<StateAndEventProcessor.Response<S>> =
            CompletableFuture.supplyAsync({ processor.onNext(state, event.asRecord()) }, executor)

        var thisEventUpdates: StateAndEventProcessor.Response<S>? =
            uncheckedCast(tryGetFutureResult(processorFuture, getInitialProcessorTimeout(initialProcessorTimeout)))

        if (thisEventUpdates == null) {
            log.trace { "Initial processor timeout on event: $event. Pausing partitions and waiting. " }
            pauseEventConsumerAndWaitForFutureToFinish(processorFuture, processorTimeout)
            if (processorFuture.isDone) {
                thisEventUpdates = uncheckedCast(tryGetFutureResult(processorFuture))
                log.trace { "Finished waiting to process event: $event" }
            } else {
                log.error("Cancelling processor. Failed to finish within the time limit for state: $state and event: $event")
                processorFuture.cancel(true)
            }
        }

        return thisEventUpdates
    }

    /**
     * Don't allow initial processor timeout to go past the poll interval cutoff point
     */
    private fun getInitialProcessorTimeout(initialProcessorTimeout: Long): Long {
        return if ((currentTimeMillis() + initialProcessorTimeout) > nextPollIntervalCutoff) {
            nextPollIntervalCutoff - currentTimeMillis()
        } else {
            initialProcessorTimeout
        }
    }

    private fun waitForFunctionToFinish(function: () -> Unit, timeout: Long, timeoutErrorMessage: String) {
        val future: CompletableFuture<*> = CompletableFuture.supplyAsync({ function() }, executor)
        tryGetFutureResult(future)

        if (!future.isDone) {
            pauseEventConsumerAndWaitForFutureToFinish(future, timeout)
        }

        if (!future.isDone) {
            future.cancel(true)
            log.error(timeoutErrorMessage)
        }
    }

    private fun pauseEventConsumerAndWaitForFutureToFinish(future: CompletableFuture<*>, timeout: Long) {
        val assignment = eventConsumer.assignment() - eventConsumer.paused()
        eventConsumer.pause(assignment)
        val maxWaitTime = currentTimeMillis() + timeout
        var done = future.isDone

        while (!done && (maxWaitTime > currentTimeMillis())) {
            eventConsumer.poll(PAUSED_POLL_TIMEOUT)
            nextPollIntervalCutoff = getNextPollIntervalCutoff()
            updateStates(false)
            done = future.isDone
        }

        eventConsumer.resume(assignment)
    }

    private fun getNextPollIntervalCutoff() : Long {
        return currentTimeMillis() + (maxPollInterval/2)
    }

    private fun resetPollInterval() {
        if (currentTimeMillis() > nextPollIntervalCutoff) {
            val assignment = eventConsumer.assignment() - eventConsumer.paused()
            eventConsumer.pause(assignment)
            eventConsumer.poll(PAUSED_POLL_TIMEOUT)
            stateConsumer.poll(PAUSED_POLL_TIMEOUT)
            nextPollIntervalCutoff = getNextPollIntervalCutoff()
            eventConsumer.resume(assignment)
        }
    }

    private fun generateDeadLetterRecord(event: ConsumerRecord<K, E>, state: S?): Record<*, *> {
        val stateBytes = if (state != null) ByteBuffer.wrap(cordaAvroSerializer.serialize(stateTopic.topic, state)) else null
        val eventBytes = ByteBuffer.wrap(cordaAvroSerializer.serialize(eventTopic.topic, event.value()))
        return Record(eventTopic.topic + deadLetterQueueSuffix, event.key(),
            StateAndEventDeadLetterRecord(stateBytes, eventBytes)
        )
    }

    private fun onProcessorStateUpdated(updatedStates: MutableMap<Int, MutableMap<K, S?>>) {
        val updatedStatesByKey = mutableMapOf<K, S?>()
        updatedStates.forEach { (partitionId, states) ->
            for (entry in states) {
                val key = entry.key
                val value = entry.value
                val currentStatesByPartition = currentStates.computeIfAbsent(partitionId) { mapFactory.createMap() }
                if (value != null) {
                    updatedStatesByKey[key] = value
                    currentStatesByPartition[key] = Pair(clock.instant().toEpochMilli(), value)
                } else {
                    updatedStatesByKey[key] = null
                    currentStatesByPartition.remove(key)
                }
            }
        }

        stateAndEventListener?.let { listener ->
            waitForFunctionToFinish(
                { listener.onPostCommit(updatedStatesByKey) },
                listenerTimeout,
                "StateAndEventListener timed out on onPostCommit operation for updated states $updatedStatesByKey"
            )
        }

    }

    private fun updateStates(syncPartitions: Boolean) {
        if (stateConsumer.assignment().isEmpty()) {
            log.trace { "State consumer has no partitions assigned." }
            return
        }

        val partitionsSynced = mutableSetOf<TopicPartition>()
        val states = stateConsumer.poll()
        for (state in states) {
            log.trace { "Updating state: $state" }
            updateInMemoryState(state)

            if (syncPartitions) {
                partitionsSynced.addAll(getSyncedEventPartitions())
            }
        }

        if (partitionsSynced.isNotEmpty()) {
            resumeConsumerAndExecuteListener(partitionsSynced)
        }
    }

    private fun getSyncedEventPartitions() : Set<TopicPartition> {
        val partitionsSynced = mutableSetOf<TopicPartition>()
        for (partition in statePartitionsToSync) {
            val partitionId = partition.key
            val stateTopicPartition = TopicPartition(stateTopic.topic, partitionId)
            val stateConsumerPollPosition = stateConsumer.position(stateTopicPartition)
            val endOffset = partition.value
            if (stateConsumerPollPosition >= endOffset) {
                log.trace {
                    "State partition $stateTopicPartition is now up to date. Poll position $stateConsumerPollPosition, recorded " +
                            "end offset $endOffset"
                }
                statePartitionsToSync.remove(partitionId)
                partitionsSynced.add(TopicPartition(eventTopic.topic, partitionId))
            }
        }

        return partitionsSynced
    }

    private fun resumeConsumerAndExecuteListener(partitionsSynced: Set<TopicPartition>) {
        log.debug { "State consumer is up to date for $partitionsSynced.  Resuming event feed." }
        eventConsumer.resume(partitionsSynced)

        stateAndEventListener?.let { listener ->
            for (partition in partitionsSynced) {
                waitForFunctionToFinish(
                    { listener.onPartitionSynced(getStatesForPartition(partition.partition())) },
                    listenerTimeout,
                    "StateAndEventListener timed out for onPartitionSynced operation on partition $partition"
                )
            }
        }
    }

    private fun updateInMemoryState(state: ConsumerRecordAndMeta<K, S>) {
        currentStates[state.record.partition()]?.compute(state.record.key()) { _, currentState ->
            if (currentState == null || currentState.first <= state.record.timestamp()) {
                if (state.record.value() == null) {
                    // Removes this state from the map
                    null
                } else {
                    // Replaces/adds the new state
                    Pair(state.record.timestamp(), state.record.value())
                }
            } else {
                // Keeps the old state
                currentState
            }
        }
    }

    /**
     * Handle retries for event processing.
     * Reset [eventConsumer] position and retry poll and process of eventRecords
     * Retry a max of [consumerPollAndProcessMaxRetries] times.
     * If [consumerPollAndProcessMaxRetries] is exceeded then throw a [CordaMessageAPIIntermittentException]
     */
    private fun handleProcessEventRetries(
        attempts: Int,
        ex: Exception
    ) {
        if (attempts <= consumerPollAndProcessMaxRetries) {
            log.warn(
                "Failed to process record from topic $eventTopic, group $groupName, " +
                        "producerClientId $producerClientId. " +
                        "Retrying poll and process. Attempts: $attempts."
            )
            eventConsumer.resetToLastCommittedPositions(OffsetResetStrategy.EARLIEST)
        } else {
            val message = "Failed to process records from topic $eventTopic, group $groupName, " +
                    "producerClientId $producerClientId. " +
                    "Attempts: $attempts. Max reties exceeded."
            log.warn(message, ex)
            throw CordaMessageAPIIntermittentException(message, ex)
        }
    }

    private fun TopicPartition.toStateTopic() = TopicPartition(stateTopic.topic, partition())
    private fun Collection<TopicPartition>.toStateTopics(): List<TopicPartition> = map { it.toStateTopic() }

}
