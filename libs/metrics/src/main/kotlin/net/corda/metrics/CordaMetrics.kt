package net.corda.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import java.util.concurrent.atomic.AtomicInteger
import io.micrometer.core.instrument.Tag as micrometerTag


object CordaMetrics {
    sealed class Metric<T: Meter> (
        val metricsName: String,
        private val meter: (String, Iterable<micrometerTag>) -> T) {

        fun builder(): MeterBuilder<T> {
            return MeterBuilder(this.metricsName, this.meter)
        }

        // NOTE: please ensure the metric names adhere to the conventions described on https://micrometer.io/docs/concepts#_naming_meters

        /**
         * Number of HTTP Requests.
         */
        object HttpRequestCount : Metric<Counter>("http.server.request", Metrics::counter)
        /**
         * HTTP Requests time.
         */
        object HttpRequestTime : Metric<Timer>("http.server.request.time", CordaMetrics::timer)
        /**
         * Time it took to create the sandbox
         */
        object SandboxCreateTime : Metric<Timer>("sandbox.create.time", CordaMetrics::timer)

        /**
         * Time it took to execute a message pattern processor
         */
        object MessageProcessorTime : Metric<Timer>("messaging.processor.time", CordaMetrics::timer)

        /**
         * The size of batches of messages received in a poll from the message bus.
         */
        object MessageBatchSize : Metric<DistributionSummary>("messaging.batch.size", Metrics::summary)

        /**
         * The time taken to commit a processed batch of messages back to the bus.
         */
        object MessageCommitTime : Metric<Timer>("messaging.commit.time", CordaMetrics::timer)

        /**
         * The time blocking inside a poll call waiting for messages from the bus.
         */
        object MessagePollTime : Metric<Timer>("messaging.poll.time", CordaMetrics::timer)

        /**
         * Time it took for a flow to complete sucessfully or to error.
         */
        object FlowRunTime : Metric<Timer>("flow.run.time", CordaMetrics::timer)

        /**
         * Metric for flow fiber serialization.
         */
        object FlowFiberSerializationTime : Metric<Timer>("flow.fiber.serialization.time", Metrics::timer)

        /**
         * Metric for flow fiber deserialization.
         */
        object FlowFiberDeserializationTime : Metric<Timer>("flow.fiber.deserialization.time", Metrics::timer)

        /**
         * Number of outbound peer-to-peer data messages sent.
         */
        object OutboundMessageCount: Metric<Counter>("p2p.message.outbound", Metrics::counter)

        /**
         * Number of outbound peer-to-peer data messages replayed.
         */
        object OutboundMessageReplayCount: Metric<Counter>("p2p.message.outbound.replayed", Metrics::counter)

        /**
         * Time it took for an outbound peer-to-peer message to be delivered end-to-end (from initial processing to acknowledgement).
         */
        object OutboundMessageDeliveryLatency: Metric<Timer>("p2p.message.outbound.latency", CordaMetrics::timer)

        /**
         * Number of outbound peer-to-peer data messages that were discarded because their TTL expired.
         */
        object OutboundMessageTtlExpired: Metric<Counter>("p2p.message.outbound.expired", Metrics::counter)

        /**
         * Number of inbound peer-to-peer data messages received.
         */
        object InboundMessageCount: Metric<Counter>("p2p.message.inbound", Metrics::counter)

        /**
         * Number of outbound peer-to-peer sessions that timed out (indicating communication issues with peers).
         */
        object OutboundSessionTimeoutCount: Metric<Counter>("p2p.session.outbound.timeout", Metrics::counter)

        /**
         * Number of outbound peer-to-peer sessions.
         */
        object OutboundSessionCount: Metric<SettableGauge>("p2p.session.outbound", CordaMetrics::settableGauge)

        /**
         * Number of inbound peer-to-peer sessions.
         */
        object InboundSessionCount: Metric<SettableGauge>("p2p.session.inbound", CordaMetrics::settableGauge)

        /**
         * The time taken from requesting a uniqueness check to a response being received from the perspective of
         * a client (requesting) node.
         */
        object LedgerUniquenessClientRunTime: Metric<Timer>("ledger.uniqueness.client.run.time", CordaMetrics::timer)

        /**
         * The overall time for the uniqueness checker to process a batch, inclusive of all sub-batches.
         */
        object UniquenessCheckerBatchExecutionTime: Metric<Timer>("uniqueness.checker.batch.execution.time", CordaMetrics::timer)

        /**
         * The number of requests in a batch processed by the uniqueness checker.
         */
        object UniquenessCheckerBatchSize: Metric<DistributionSummary>("uniqueness.checker.batch.size", Metrics::summary)

        /**
         * The time for the uniqueness checker to process a sub-batch, i.e. a partition of a batch segregated
         * by notary virtual node holding identity.
         */
        object UniquenessCheckerSubBatchExecutionTime: Metric<Timer>("uniqueness.checker.subbatch.execution.time", CordaMetrics::timer)

        /**
         * The number of requests in a sub-batch processed by the uniqueness checker.
         */
        object UniquenessCheckerSubBatchSize: Metric<DistributionSummary>("uniqueness.checker.subbatch.size", Metrics::summary)

        /**
         * Cumulative number of requests processed by the uniqueness checker, irrespective of batch.
         */
        object UniquenessCheckerRequestCount: Metric<Counter>("uniqueness.checker.request.count", Metrics::counter)

        /**
         * The overall execution time for a (uniqueness checker) backing store session, which includes retrieving
         * uniqueness database connection details, getting a database connection, as well as all database operations
         * (both read and write) carried out within a session context.
         */
        object UniquenessBackingStoreSessionExecutionTime: Metric<Timer>(
            "uniqueness.backingstore.session.execution.time", CordaMetrics::timer)

        /**
         * The execution time for a transaction within the context of a backing store session, which excludes
         * retrieving uniqueness database connection details and getting a database connection. If a transaction
         * needs to be retried due to database exceptions, then the execution time covers the cumulative duration
         * of all retry attempts.
         */
        object UniquenessBackingStoreTransactionExecutionTime: Metric<Timer>(
            "uniqueness.backingstore.transaction.execution.time", CordaMetrics::timer)

        /**
         * Cumulative number of errors raised by the backing store when executing a transaction. This is incremented
         * regardless of whether an expected or unexpected error is raised, and is incremented on each retry.
         */
        object UniquenessBackingStoreTransactionErrorCount: Metric<Counter>(
            "uniqueness.backingstore.transaction.error.count", Metrics::counter)

        /**
         * The number of attempts that were made by the backing store before a transaction ultimately succeeded. In
         * the event that a transaction was unsuccessful due to reaching the maximum number of attempts, this metric
         * is not updated.
         */
        object UniquenessBackingStoreTransactionAttempts: Metric<DistributionSummary>(
            "uniqueness.backingstore.transaction.attempts", Metrics::summary)

        /**
         * The time taken by the backing store to commit a transaction (i.e. write) to the database. Only updated if
         * data is written to the database, so is not cumulative across retry attempts for a given transaction.
         */
        object UniquenessBackingStoreDbCommitTime: Metric<Timer>("uniqueness.backingstore.db.commit.time", CordaMetrics::timer)

        /**
         * The time taken by the backing store to perform a single read operation from the database.
         */
        object UniquenessBackingStoreDbReadTime: Metric<Timer>("uniqueness.backingstore.db.read.time", CordaMetrics::timer)
    }

    /**
     * Metric tag names
     *
     * NOTE: please ensure the metric names adhere to the convensions described on
     * https://micrometer.io/docs/concepts#_naming_meters
     */
    enum class Tag(val value: String) {
        /**
         * Address for which the metric is applicable.
         */
        Address("address"),
        /**
         * Type of the SandboxGroup to which the metric applies.
         */
        SandboxGroupType("sandbox.type"),
        /**
         * Source of metric.
         */
        WorkerType("worker.type"),
        /**
         * Virtual Node for which the metric is applicable.
         */
        VirtualNode("virtualnode"),

        /**
         * Message pattern type for which the metric is applicable.
         */
        MessagePatternType("messagepattern.type"),

        /**
         * The name of the operation performed
         */
        OperationName("operation.name"),

        /**
         * Message pattern clientId for which the metric is applicable.
         */
        MessagePatternClientId("messagepattern.clientid"),

        /**
         * Flow class for which the metric is applicable.
         */
        FlowClass("class"),


        /**
         * Flow Id for which the metric is applicable.
         */
        FlowId("id"),

        /**
         * The status of the operation. Can be used to indicate whether an operation was successful or failed.
         */
        OperationStatus("status"),

        /**
         * The source virtual node in peer-to-peer communication.
         */
        SourceVirtualNode("virtualnode.source"),

        /**
         * The destination virtual node in peer-to-peer communication.
         */
        DestinationVirtualNode("virtualnode.destination"),

        /**
         * The membership group within which peer-to-peer communication happens.
         */
        MembershipGroup("group"),

        /**
         * The type of a peer-to-peer message.
         */
        MessageType("message.type"),

        /**
         * The subsystem that sends or receives a peer-to-peer message from the network layer.
         */
        MessagingSubsystem("subsystem"),

        /**
         * Type of result returned. Currently used by uniqueness client and checker to indicate
         * successful vs failed results.
         */
        ResultType("result.type"),

        /**
         * Boolean value indicating whether the metric relates to a duplicate request. Used by the
         * uniqueness checker to indicate when a request has been seen before and the original
         * result is being returned.
         */
        IsDuplicate("duplicate"),

        /**
         * Type of error raised in failure cases
         */
        ErrorType("error.type")
    }

    val registry: CompositeMeterRegistry = Metrics.globalRegistry

    /**
     * Configure the Metrics Registry
     *
     * @param workerType Type of Worker, will be tagged to each metric.
     * @param registry Registry instance
     */
    fun configure(workerType: String, registry: MeterRegistry) {
        this.registry.add(registry)
        this.registry.config()
            .meterFilter(object : MeterFilter {
                override fun map(id: Meter.Id): Meter.Id {
                    // prefix all metrics with `corda`, except standard JVM and Process metrics
                    @Suppress("ComplexCondition")
                    if(
                        id.name.startsWith("corda") ||
                        id.name.startsWith("jvm") ||
                        id.name.startsWith("system") ||
                        id.name.startsWith("process")) {
                        return id
                    }
                    return id.withName("corda." + id.name)
                }
            })
            .commonTags(Tag.WorkerType.value, workerType)
    }

    fun settableGauge(name: String, tags: Iterable<micrometerTag>): SettableGauge {
        val gaugeValue = AtomicInteger()
        val gauge = Gauge.builder(name, gaugeValue, Number::toDouble)
            .tags(tags)
            .register(registry)
        return SettableGauge(gauge, gaugeValue)
    }

    fun timer(name: String, tags: Iterable<micrometerTag>): Timer {
        return Timer.builder(name)
            .publishPercentiles(0.50, 0.95, 0.99)
            .tags(tags)
            .register(registry)
    }

    class MeterBuilder<T: Meter>(
        val name: String,
        val func: (String, Iterable<micrometerTag>) -> T
    ) {
        val allTags: MutableList<io.micrometer.core.instrument.Tag> = mutableListOf()

        /**
         * Tag the metric with the Holding ID short hash for the Virtual Node.
         */
        fun forVirtualNode(holdingId: String): MeterBuilder<T> {
            return withTag(Tag.VirtualNode, holdingId)
        }

        fun withTag(key: Tag, value: String): MeterBuilder<T> {
            allTags.add(io.micrometer.core.instrument.Tag.of(key.value, value))
            return this
        }

        fun build(): T {
            return func(name, allTags)
        }
    }
}
