package net.corda.entityprocessor.impl.tests

import net.corda.cpiinfo.read.CpiInfoReadService
import net.corda.data.flow.FlowKey
import net.corda.data.virtualnode.EntityRequest
import net.corda.data.virtualnode.PersistEntity
import net.corda.db.admin.LiquibaseSchemaMigrator
import net.corda.db.admin.impl.ClassloaderChangeLog
import net.corda.db.messagebus.testkit.DBSetup
import net.corda.entityprocessor.impl.internal.EntityMessageProcessor
import net.corda.entityprocessor.impl.internal.EntitySandboxContextTypes
import net.corda.entityprocessor.impl.internal.EntitySandboxServiceImpl
import net.corda.entityprocessor.impl.tests.components.VirtualNodeService
import net.corda.entityprocessor.impl.tests.fake.FakeDbConnectionManager
import net.corda.entityprocessor.impl.tests.helpers.BasicMocks
import net.corda.messaging.api.records.Record
import net.corda.orm.JpaEntitiesSet
import net.corda.orm.utils.use
import net.corda.sandboxgroupcontext.getObjectByKey
import net.corda.testing.sandboxes.SandboxSetup
import net.corda.testing.sandboxes.fetchService
import net.corda.testing.sandboxes.lifecycle.EachTestLifecycle
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.base.util.contextLogger
import net.corda.virtualnode.VirtualNodeInfo
import net.corda.virtualnode.read.VirtualNodeInfoReadService
import net.corda.virtualnode.toAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.osgi.test.common.annotation.InjectBundleContext
import org.osgi.test.common.annotation.InjectService
import org.osgi.test.junit5.context.BundleContextExtension
import org.osgi.test.junit5.service.ServiceExtension
import java.nio.ByteBuffer
import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * To use Postgres rather than in-memory (HSQL):
 *
 *     docker run --rm --name test-instance -e POSTGRES_PASSWORD=password -p 5432:5432 postgres
 *
 *     gradlew integrationTest -PpostgresPort=5432
 */
@ExtendWith(ServiceExtension::class, BundleContextExtension::class, DBSetup::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersistenceServiceInternalTests {
    companion object {
        const val TOPIC = "pretend-topic"
        private val logger = contextLogger()
    }

    @InjectService
    lateinit var lbm: LiquibaseSchemaMigrator

    @RegisterExtension
    private val lifecycle = EachTestLifecycle()

    private lateinit var virtualNode: VirtualNodeService
    private lateinit var cpiInfoReadService: CpiInfoReadService
    private lateinit var virtualNodeInfoReadService: VirtualNodeInfoReadService

    @BeforeAll
    fun setup(
        @InjectService(timeout = 1000)
        sandboxSetup: SandboxSetup,
        @InjectBundleContext
        bundleContext: BundleContext,
        @TempDir
        testDirectory: Path
    ) {
        logger.info("Setup test (test Directory: $testDirectory)")
        sandboxSetup.configure(bundleContext, testDirectory)
        lifecycle.accept(sandboxSetup) { setup ->
            virtualNode = setup.fetchService(timeout = 1000)
            cpiInfoReadService = setup.fetchService(timeout = 1000)
            virtualNodeInfoReadService = setup.fetchService(timeout = 1000)
        }
    }

    //@Test
//    fun `persist`() {
//        val virtualNodeInfo = virtualNode.load(Resources.EXTENDABLE_CPB)
//
//        val entitySandboxService =
//            EntitySandboxServiceImpl(
//                virtualNode.sandboxGroupContextComponent,
//                cpiInfoReadService,
//                virtualNodeInfoReadService,
//                BasicMocks.dbConnectionManager(),
//                BasicMocks.componentContext()
//            )
//
//        val sandbox = entitySandboxService.get(virtualNodeInfo.holdingIdentity)
//
//        val serializer = sandbox.getObjectByKey<SerializationService>(EntitySandboxContextTypes.SANDBOX_SERIALIZER)
//        assertThat(serializer).isNotNull
//
//        val expectedDog = Dog(UUID.randomUUID(), "rover", Instant.now(), "me")
//        logger.info("Persisting $expectedDog")
//        val dogBytes = serializer!!.serialize(expectedDog)
//
//        val persistenceService = PersistenceServiceInternal(entitySandboxService)
//        val payload = PersistEntity(ByteBuffer.wrap(dogBytes.bytes))
//
//        val entityManager = BasicMocks.entityManager()
//
//        persistenceService.persist(serializer, entityManager, payload)
//
//        Mockito.verify(entityManager).persist(Mockito.any())
//
//    }

    //@Test
//    fun `persist via message processor`() {
//        val virtualNodeInfo = virtualNode.load(Resources.EXTENDABLE_CPB)
//        val entitySandboxService =
//            EntitySandboxServiceImpl(
//                virtualNode.sandboxGroupContextComponent,
//                cpiInfoReadService,
//                virtualNodeInfoReadService,
//                BasicMocks.dbConnectionManager(),
//                BasicMocks.componentContext()
//            )
//
//        val sandbox = entitySandboxService.get(virtualNodeInfo.holdingIdentity)
//        val serializer = sandbox.getObjectByKey<SerializationService>(EntitySandboxContextTypes.SANDBOX_SERIALIZER)
//
//        val request = dogRequest(UUID.randomUUID(), virtualNodeInfo, serializer!!)
//        val processor = EntityMessageProcessor(entitySandboxService)
//
//        val requestId = UUID.randomUUID().toString() // just needs to be something unique.
//        val records = listOf(Record(TOPIC, requestId, request))
//        val responses = processor.onNext(records)
//
//        assertThat(responses.size).isEqualTo(1)
//        val response = processor.onNext(records).first().value as EntityResponse
//        assertThat(response.requestId).isEqualTo(requestId)
//    }

//    fun `persist using two different sandboxes`() {
//        val virtualNodeInfoOne = virtualNode.load(Resources.EXTENDABLE_CPB)
//        val virtualNodeInfoTwo = virtualNode.load(Resources.EXTENDABLE_CPB)
//
//        // should be able to test sandboxes in here, such as entities in one can't be persisted by two
//    }

    @Test
    fun `persist to an actual database`() {
        val virtualNodeInfo = virtualNode.load(Resources.EXTENDABLE_CPB)

        val dogDbConnection = Pair(virtualNodeInfo.vaultDmlConnectionId, "dogs-node")
        val dbConnectionManager = FakeDbConnectionManager(listOf(dogDbConnection))

        // TEST persist outside sandbox
//        val dog2 = Dog(UUID.randomUUID(), "rover", Instant.now(), "me")
//        dbConnectionManager.createEntityManagerFactory(
//            dogDbConnection.first, JpaEntitiesSet.create("Test", setOf(Dog::class.java))).transaction {
//            it.persist(dog2)
//        }
//
//        val r = dbConnectionManager.createEntityManagerFactory(
//            dogDbConnection.first, JpaEntitiesSet.create(dogDbConnection.second, setOf(Dog::class.java))
//        ).createEntityManager().use {
//            it.find(Dog::class.java, dog2.id)
//        }
//
//        assertThat(r).isNotNull

        // set up sandbox
        val entitySandboxService =
            EntitySandboxServiceImpl(
                virtualNode.sandboxGroupContextComponent,
                cpiInfoReadService,
                virtualNodeInfoReadService,
                dbConnectionManager,
                BasicMocks.componentContext()
            )

        val sandbox = entitySandboxService.get(virtualNodeInfo.holdingIdentity)
        val serializer = sandbox.getObjectByKey<SerializationService>(EntitySandboxContextTypes.SANDBOX_SERIALIZER)!!

        // migrate DB schema

        val dogClass = sandbox.sandboxGroup.loadClassFromMainBundles("net.corda.testing.cpks.dogs.Dog")

        val cl = ClassloaderChangeLog(
            linkedSetOf(
                ClassloaderChangeLog.ChangeLogResourceFiles(
                    dogClass.packageName, listOf("migration/db.changelog-master.xml"),
                    classLoader = dogClass.classLoader
                )
            )
        )

        lbm.updateDb(dbConnectionManager.getDataSource(dogDbConnection.first).connection, cl)

        // more test
//        val dog3 = Dog(UUID.randomUUID(), "rover", Instant.now(), "me")
//        val dogBytes = serializer.serialize(dog3)
//        val dogAgain = serializer.deserialize(dogBytes.bytes, Any::class.java)
//        dbConnectionManager.createEntityManagerFactory(
//            dogDbConnection.first, JpaEntitiesSet.create("Test", setOf(Dog::class.java))).transaction {
//            it.persist(dogAgain)
//        }
//
//        val r2 = dbConnectionManager.createEntityManagerFactory(
//            dogDbConnection.first, JpaEntitiesSet.create(dogDbConnection.second, setOf(Dog::class.java))
//        ).createEntityManager().use {
//            it.find(Dog::class.java, dog3.id)
//        }
//        assertThat(r2).isNotNull
        //

        // request persist
        val dogId = UUID.randomUUID()
        val request = dogRequest(dogId, virtualNodeInfo, serializer, dogClass)
        val processor = EntityMessageProcessor(entitySandboxService)
        val requestId = UUID.randomUUID().toString() // just needs to be something unique.
        val records = listOf(Record(TOPIC, requestId, request))
        val responses = processor.onNext(records)

        // assert persisted
        assertThat(responses.size).isEqualTo(1)

        val queryResults = dbConnectionManager.createEntityManagerFactory(
            dogDbConnection.first, JpaEntitiesSet.create(dogDbConnection.second, setOf(dogClass))
        ).createEntityManager().use {
            it.find(dogClass, dogId)
        }

        assertThat(queryResults).isNotNull
        logger.info("Woof ${queryResults}")
    }


    private fun dogRequest(
        dogId: UUID,
        virtualNodeInfo: VirtualNodeInfo,
        serializer: SerializationService,
        dogClass: Class<*>
    ): EntityRequest {
        val flowKey = FlowKey(UUID.randomUUID().toString(), virtualNodeInfo.holdingIdentity.toAvro())
        val dogCtor = dogClass.getDeclaredConstructor(UUID::class.java, String::class.java, Instant::class.java, String::class.java)
        val expectedDog = dogCtor.newInstance(dogId, "rover", Instant.now(), "me")
        val dogBytes = serializer.serialize(expectedDog)
        val persistEntity = PersistEntity(ByteBuffer.wrap(dogBytes.bytes))
        logger.info("Entity Request - flow: $flowKey, entity: $expectedDog")
        return EntityRequest(Instant.now(), flowKey, persistEntity)
    }
}
