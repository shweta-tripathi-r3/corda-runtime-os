package net.corda.libs.configuration.datamodel.tests

import net.corda.db.admin.impl.ClassloaderChangeLog
import net.corda.db.admin.impl.ClassloaderChangeLog.ChangeLogResourceFiles
import net.corda.db.admin.impl.LiquibaseSchemaMigratorImpl
import net.corda.db.schema.DbSchema
import net.corda.db.testkit.DbUtils
import net.corda.libs.cpi.datamodel.CpiEntities
import net.corda.libs.cpi.datamodel.CpiMetadataEntity
import net.corda.libs.virtualnode.datamodel.HoldingIdentityEntity
import net.corda.libs.virtualnode.datamodel.VirtualNodeEntities
import net.corda.libs.virtualnode.datamodel.VirtualNodeEntity
import net.corda.libs.virtualnode.datamodel.VirtualNodeEntityKey
import net.corda.orm.impl.EntityManagerFactoryFactoryImpl
import net.corda.orm.utils.transaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.persistence.EntityManagerFactory
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VirtualNodeEntitiesIntegrationTest {
    private val dbConfig = DbUtils.getEntityManagerConfiguration("virtual_node_db")
    private val entityManagerFactory: EntityManagerFactory

    private companion object {
        private const val MIGRATION_FILE_LOCATION = "net/corda/db/schema/config/db.changelog-master.xml"
        private val random = Random(0)
    }

    /**
     * Creates an in-memory database, applies the relevant migration scripts, and initialises
     * [entityManagerFactory].
     */
    init {
        val dbChange = ClassloaderChangeLog(
            linkedSetOf(
                ChangeLogResourceFiles(
                    DbSchema::class.java.packageName,
                    listOf(MIGRATION_FILE_LOCATION),
                    DbSchema::class.java.classLoader
                )
            )
        )
        dbConfig.dataSource.connection.use { connection ->
            LiquibaseSchemaMigratorImpl().updateDb(connection, dbChange)
        }
        entityManagerFactory = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            VirtualNodeEntities.classes.toList() + CpiEntities.classes.toList(),
            dbConfig
        )
    }

    @Suppress("Unused")
    @AfterAll
    private fun cleanup() {
        dbConfig.close()
        entityManagerFactory.close()
    }

    @Test
    fun `can persist and read back Holding Identity entity`() {
        val holdingIdentity = HoldingIdentityEntity(
            "0123456789AB", "a=b", "OU=LLC, O=Bob, L=Dublin, C=IE",
            "${random.nextInt()}", null, null, null, null, null
        )

        entityManagerFactory.createEntityManager().transaction { em ->
            em.persist(holdingIdentity)
        }

        assertEquals(
            holdingIdentity,
            entityManagerFactory.createEntityManager()
                .find(HoldingIdentityEntity::class.java, holdingIdentity.holdingIdentityId)
        )
    }

    @Test
    fun `can persist and read back Virtual Node entity`() {
        val cpiMetadata = CpiMetadataEntity(
            "Test CPI", "1.0", "CPI summary hash",
            "file", "1234567890", "group policy",
            "group ID", "request ID", emptySet()
        )
        val holdingIdentity = HoldingIdentityEntity(
            "0123456789AB", "a=b", "OU=LLC, O=Bob, L=Dublin, C=IE",
            "${random.nextInt()}", null, null, null, null, null
        )
        val virtualNode = VirtualNodeEntity(
            holdingIdentity, "Test CPI", "1.0",
            "CPI summary hash"
        )

        entityManagerFactory.createEntityManager().transaction { em ->
            em.persist(cpiMetadata)
            em.persist(virtualNode)
        }

        assertEquals(
            virtualNode,
            entityManagerFactory.createEntityManager().find(
                VirtualNodeEntity::class.java,
                VirtualNodeEntityKey(
                    virtualNode.holdingIdentity,
                    virtualNode.cpiName,
                    virtualNode.cpiVersion,
                    virtualNode.cpiSignerSummaryHash
                )
            )
        )
    }

    @Test
    fun `can persist and read back all Virtual Node entities`() {
        for (i in 1..2) {
            val cpiMetadata = CpiMetadataEntity(
                "Test CPI $i", "1.0", "CPI summary hash-$i",
                "file-$i", "1234567890-$i", "group policy",
                "group ID-$i", "request ID", emptySet()
            )
            val holdingIdentity = HoldingIdentityEntity(
                "0123456789A$i", "a=b$i", "OU=LLC, O=Bob$i, L=Dublin, C=IE",
                "${random.nextInt()}", null, null, null, null, null
            )
            val virtualNode = VirtualNodeEntity(
                holdingIdentity, cpiMetadata.name, cpiMetadata.version, cpiMetadata.signerSummaryHash
            )

            entityManagerFactory.createEntityManager().transaction { em ->
                em.persist(cpiMetadata)
                em.persist(virtualNode)
            }
        }

        val all = entityManagerFactory.createEntityManager().transaction { em ->
            em.createQuery(
                "FROM ${VirtualNodeEntity::class.simpleName} vnode_ "+
                        "INNER JOIN FETCH vnode_.holdingIdentity hid_ ",
                VirtualNodeEntity::class.java
            ).resultList
        }

        assertEquals(all.size, 2)
        // check this doesn't use a proxy and additional query:
        assertThat(all.first().holdingIdentity.x500Name).isNotNull
    }
}