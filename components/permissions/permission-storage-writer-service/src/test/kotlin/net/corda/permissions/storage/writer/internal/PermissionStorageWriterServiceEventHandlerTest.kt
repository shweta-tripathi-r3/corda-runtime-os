package net.corda.permissions.storage.writer.internal

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import net.corda.data.permissions.management.PermissionManagementRequest
import net.corda.data.permissions.management.PermissionManagementResponse
import net.corda.libs.configuration.SmartConfigFactory
import net.corda.libs.permissions.storage.writer.PermissionStorageWriterProcessor
import net.corda.libs.permissions.storage.writer.factory.PermissionStorageWriterProcessorFactory
import net.corda.lifecycle.LifecycleStatus
import net.corda.lifecycle.RegistrationStatusChangeEvent
import net.corda.lifecycle.StartEvent
import net.corda.lifecycle.StopEvent
import net.corda.messaging.api.config.toMessagingConfig
import net.corda.messaging.api.subscription.RPCSubscription
import net.corda.messaging.api.subscription.factory.SubscriptionFactory
import net.corda.permissions.storage.reader.PermissionStorageReaderService
import net.corda.schema.configuration.ConfigKeys.BOOT_CONFIG
import net.corda.schema.configuration.ConfigKeys.DB_CONFIG
import net.corda.schema.configuration.ConfigKeys.DB_PASS
import net.corda.schema.configuration.ConfigKeys.DB_USER
import net.corda.schema.configuration.ConfigKeys.JDBC_URL
import net.corda.schema.configuration.ConfigKeys.MESSAGING_CONFIG
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import javax.persistence.EntityManagerFactory
import net.corda.configuration.read.ConfigChangedEvent
import net.corda.configuration.read.ConfigurationReadService
import net.corda.db.connection.manager.DbConnectionManager
import net.corda.lifecycle.LifecycleCoordinator
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.lifecycle.RegistrationHandle
import net.corda.messaging.api.subscription.config.RPCConfig

class PermissionStorageWriterServiceEventHandlerTest {

    private val entityManagerFactory = mock<EntityManagerFactory>()
    private val entityManagerFactoryFactory = mock<() -> EntityManagerFactory> {
        on { this.invoke() }.doReturn(entityManagerFactory)
    }
    private val subscription = mock<RPCSubscription<PermissionManagementRequest, PermissionManagementResponse>>()
    private val subscriptionFactory = mock<SubscriptionFactory>().apply {
        whenever(createRPCSubscription(any(), any(), any<PermissionStorageWriterProcessor>())).thenReturn(subscription)
    }
    private val permissionStorageWriterProcessor = mock<PermissionStorageWriterProcessor>()
    private val permissionStorageWriterProcessorFactory = mock<PermissionStorageWriterProcessorFactory>().apply {
        whenever(create(any(), any())).thenReturn(permissionStorageWriterProcessor)
    }
    private val readerService = mock<PermissionStorageReaderService>().apply {
        whenever(permissionStorageReader).thenReturn(mock())
    }

    private val configurationReadService = mock<ConfigurationReadService>()
    private val handler = PermissionStorageWriterServiceEventHandler(
        subscriptionFactory,
        permissionStorageWriterProcessorFactory,
        readerService,
        configurationReadService,
        entityManagerFactoryFactory,
    )

    private val configFactory = SmartConfigFactory.create(ConfigFactory.empty())
    private val config = configFactory.create(
        ConfigFactory.empty()
            .withValue(
                DB_CONFIG,
                ConfigValueFactory.fromMap(
                    mapOf(
                        JDBC_URL to "dbUrl",
                        DB_USER to "dbUser",
                        DB_PASS to "dbPass"
                    )
                )
            )
    )

    private val bootstrapConfig = mapOf(
        BOOT_CONFIG to config,
        MESSAGING_CONFIG to configFactory.create(ConfigFactory.empty())
    )

    private val registrationHandle = mock<RegistrationHandle>()
    private val coordinator = mock<LifecycleCoordinator>().apply {
        whenever(followStatusChangesByName(
            setOf(
                LifecycleCoordinatorName.forComponent<PermissionStorageReaderService>(),
                LifecycleCoordinatorName.forComponent<ConfigurationReadService>(),
                LifecycleCoordinatorName.forComponent<DbConnectionManager>()
            )
        )).thenReturn(registrationHandle)
    }

    @Test
    fun `processing a START event follows and starts dependencies`() {
        assertNull(handler.registrationHandle)

        handler.processEvent(StartEvent(), coordinator)

        verify(coordinator).followStatusChangesByName(
            setOf(
                LifecycleCoordinatorName.forComponent<PermissionStorageReaderService>(),
                LifecycleCoordinatorName.forComponent<ConfigurationReadService>(),
                LifecycleCoordinatorName.forComponent<DbConnectionManager>()
            )
        )
        assertNotNull(handler.registrationHandle)
    }

    @Test
    fun `processing an UP event when the service is started registers for config updates`() {
        assertNull(handler.crsSub)

        whenever(configurationReadService.registerComponentForUpdates(
            coordinator,
            setOf(BOOT_CONFIG, MESSAGING_CONFIG)
        )).thenReturn(mock())

        handler.processEvent(StartEvent(), coordinator)
        handler.processEvent(RegistrationStatusChangeEvent(mock(), LifecycleStatus.UP), coordinator)
        handler.onConfigurationUpdated(bootstrapConfig.toMessagingConfig())

        assertNotNull(handler.crsSub)
    }

    @Test
    fun `processing an onConfigurationUpdated event creates and starts subscription`() {
        whenever(subscriptionFactory.createRPCSubscription(
            any<RPCConfig<PermissionManagementRequest, PermissionManagementResponse>>(),
            any(),
            any())
        ).thenReturn(subscription)

        handler.processEvent(
            ConfigChangedEvent(setOf(BOOT_CONFIG, MESSAGING_CONFIG), bootstrapConfig),
            coordinator
        )

        verify(subscription).start()
    }

    @Test
    fun `processing a stop event stops the permission storage writer`() {
        handler.processEvent(StartEvent(), mock())
        assertNull(handler.subscription)
        handler.processEvent(RegistrationStatusChangeEvent(mock(), LifecycleStatus.UP), mock())
        handler.onConfigurationUpdated(bootstrapConfig.toMessagingConfig())

        assertNotNull(handler.subscription)
        verify(subscription).start()
        handler.processEvent(StopEvent(), mock())
        assertNull(handler.subscription)
        verify(subscription).stop()
    }
}