package net.corda.libs.cpi.datamodel.tests

import net.corda.db.admin.impl.ClassloaderChangeLog
import net.corda.db.admin.impl.LiquibaseSchemaMigratorImpl
import net.corda.db.schema.DbSchema
import net.corda.db.testkit.DbUtils
import net.corda.libs.cpi.datamodel.CpiEntities
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAuditEntity
import net.corda.libs.cpi.datamodel.CpkDbChangeLogEntity
import net.corda.libs.cpi.datamodel.CpkDbChangeLogKey
import net.corda.libs.cpi.datamodel.getCpiChangelogsForGivenChangesetIds
import net.corda.libs.cpi.datamodel.findCurrentCpkChangeLogsForCpi
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.orm.EntityManagerConfiguration
import net.corda.orm.impl.EntityManagerFactoryFactoryImpl
import net.corda.orm.utils.transaction
import net.corda.orm.utils.use
import net.corda.v5.crypto.SecureHash
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import javax.persistence.EntityManager
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAuditKey
import net.corda.test.util.dsl.entities.cpx.cpi

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CpkDbChangeLogEntityTest {

    private val fakeId = UUID.randomUUID()
    private val dbConfig: EntityManagerConfiguration =
        DbUtils.getEntityManagerConfiguration("cpk_changelog_db")

    private fun transaction(callback: EntityManager.() -> Unit): Unit = EntityManagerFactoryFactoryImpl().create(
        "test_unit",
        CpiEntities.classes.toList(),
        dbConfig
    ).use { em ->
        em.transaction {
            it.callback()
        }
    }

    init {
        val dbChange = ClassloaderChangeLog(
            linkedSetOf(
                ClassloaderChangeLog.ChangeLogResourceFiles(
                    DbSchema::class.java.packageName,
                    listOf("net/corda/db/schema/config/db.changelog-master.xml"),
                    DbSchema::class.java.classLoader
                )
            )
        )
        dbConfig.dataSource.connection.use { connection ->
            LiquibaseSchemaMigratorImpl().updateDb(connection, dbChange)
        }
    }

    @AfterAll
    fun cleanUp() {
        dbConfig.close()
    }

    @Test
    fun `can persist changelogs`() {
        val (cpi, cpks) = TestObject.createCpiWithCpks(2)
        val cpk1 = cpks[0]
        val cpk2 = cpks[1]
        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk1.id.cpkFileChecksum, "master"),
            "master-content",
            fakeId
        )
        val changeLog2 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk2.id.cpkFileChecksum, "other"),
            "other-content",
            fakeId
        )

        transaction {
            persist(cpi)
            persist(changeLog1)
            persist(changeLog2)
            flush()
        }

        transaction {
            val loadedDbLogEntity = find(
                CpkDbChangeLogEntity::class.java,
                CpkDbChangeLogKey(cpk1.id.cpkFileChecksum, "master")
            )

            assertThat(changeLog1.content).isEqualTo(loadedDbLogEntity.content)
        }
    }

    @Test
    fun `can persist changelogs with audit`() {
        val (cpi, cpks) = TestObject.createCpiWithCpks()
        val cpk = cpks.first()
        val cpkFileChecksum = cpk.id.cpkFileChecksum
        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpkFileChecksum, "master"),
            "master-content",
            fakeId
        )

        val changeLog1Audit: CpkDbChangeLogAuditEntity = cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, changeLog1)

        transaction {
            persist(cpi)
            persist(changeLog1)
            flush()
            persist(changeLog1Audit)
            flush()
        }

        transaction {
            val loadedDbLogEntity = find(
                CpkDbChangeLogEntity::class.java,
                CpkDbChangeLogKey(cpkFileChecksum, "master")
            )
            val loadedDbLogAuditEntity = getEntireHistoryOfChangelogsForCpi(
                this,
                CpiIdentifier(
                    cpi.name,
                    cpi.version,
                    SecureHash.parse(cpi.signerSummaryHash)
                )
            ).singleOrNull()

            assertThat(loadedDbLogAuditEntity)
                .isNotEqualTo(null)
            assertThat(loadedDbLogEntity.id.cpkFileChecksum)
                .isEqualTo(loadedDbLogAuditEntity!!.id.cpkFileChecksum)
        }
    }

    /*
     * Find all the audit db changelogs for a CPI
     */
    fun getEntireHistoryOfChangelogsForCpi(
        entityManager: EntityManager,
        cpi: CpiIdentifier
    ): List<CpkDbChangeLogAuditEntity> = entityManager.createQuery(
        "FROM ${CpkDbChangeLogAuditEntity::class.simpleName}" +
                " WHERE cpiName = :name AND" +
                " cpiVersion = :version AND" +
                " cpiSignerSummaryHash = :signerSummaryHash",
        CpkDbChangeLogAuditEntity::class.java
    )
        .setParameter("name", cpi.name)
        .setParameter("version", cpi.version)
        .setParameter("signerSummaryHash", cpi.signerSummaryHash?.toString() ?: "")
        .resultList

    private fun cpkDbChangeLogAuditEntity(cpiName: String, cpiVersion: String, cpiSsh: String, changeLog: CpkDbChangeLogEntity):
            CpkDbChangeLogAuditEntity {
        return CpkDbChangeLogAuditEntity(
            CpkDbChangeLogAuditKey(
                cpiName,
                cpiVersion,
                cpiSsh,
                changeLog.id.cpkFileChecksum,
                changeLog.changesetId,
                changeLog.entityVersion,
                changeLog.id.filePath
            ),
            changeLog.content,
            changeLog.isDeleted
        )
    }

    @Test
    fun `can update changelogs and add new audit`() {
        val (cpi, cpks) = TestObject.createCpiWithCpks()
        val cpk = cpks.first()
        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk.id.cpkFileChecksum, "master"),
            "master-content",
            fakeId
        )

        val changeLog1Audit = cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, changeLog1)

        transaction {
            persist(cpi)
            persist(changeLog1)
            flush()
            persist(changeLog1Audit)
            flush()
        }

        transaction {
            val loadedDbLogEntity = find(
                CpkDbChangeLogEntity::class.java,
                CpkDbChangeLogKey(cpk.id.cpkFileChecksum, "master")
            )
            loadedDbLogEntity.isDeleted = true
            merge(loadedDbLogEntity)
            flush()
            persist(cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, changeLog1))
            flush()
        }

        transaction {
            val loadedDbLogAuditEntities = getEntireHistoryOfChangelogsForCpi(
                this,
                CpiIdentifier(
                    cpi.name,
                    cpi.version,
                    SecureHash.parse(cpi.signerSummaryHash)
                )
            ).sortedBy { it.insertTimestamp }

            assertThat(cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, changeLog1).id)
                .isNotEqualTo(loadedDbLogAuditEntities.first().id)
            assertThat(cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, changeLog1).id)
                .isEqualTo(loadedDbLogAuditEntities.last().id)
        }
    }

    @Test
    fun `can get changelogs based on changesetId`() {
        val changesetId1 = UUID.randomUUID()
        val (cpi, cpks) = TestObject.createCpiWithCpks(3)
        val changeset1 = (0..2).map {
            CpkDbChangeLogEntity(
                CpkDbChangeLogKey(cpks[it].id.cpkFileChecksum, "master-$it"),
                "master-content",
                changesetId1
            )
        }

        transaction {
            persist(cpi)
            changeset1.forEach { persist(it) }
            changeset1.forEach { persist(cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, it)) }
            flush()
        }

        val changesetId2 = UUID.randomUUID()
        val (cpi2, cpks2) = TestObject.createCpiWithCpks(3)
        val changeset2 = (0..2).map {
            CpkDbChangeLogEntity(
                CpkDbChangeLogKey(cpks2[it].id.cpkFileChecksum, "master-$it"),
                "master-content",
                changesetId2
            )
        }

        transaction {
            persist(cpi2)
            changeset2.forEach { persist(it) }
            changeset2.forEach { persist(cpkDbChangeLogAuditEntity(cpi.name, cpi.version, cpi.signerSummaryHash, it)) }
            flush()
        }

        transaction {
            val loadedDbLogAuditEntities = getCpiChangelogsForGivenChangesetIds(
                this,
                cpi.name,
                cpi.version,
                cpi.signerSummaryHash,
                setOf(changesetId1)
            ).sortedBy { it.insertTimestamp }

            assertThat(loadedDbLogAuditEntities.size).isEqualTo(3)
        }

        transaction {
            val loadedDbLogAuditEntities = getCpiChangelogsForGivenChangesetIds(
                this,
                cpi2.name,
                cpi2.version,
                cpi2.signerSummaryHash,
                setOf(changesetId2)
            ).sortedBy { it.insertTimestamp }

            assertThat(loadedDbLogAuditEntities.size).isEqualTo(3)
        }
    }

    @Test
    fun `when CPI is merged with new CPKs, old orphaned CPK changesets are no longer associated with the CPI`() {
        val changesetId1 = UUID.randomUUID()
        val cpiName = UUID.randomUUID().toString()
        val cpiVersion = UUID.randomUUID().toString()
        val cpiSsh = UUID.randomUUID().toString()
        val originalCpi = cpi {
            name(cpiName)
            version(cpiVersion)
            signerSummaryHash(cpiSsh)
            cpk { }
            cpk { }
            cpk { }
        }
        val originalChangelogs = originalCpi.cpks.mapIndexed { i, cpk ->
            CpkDbChangeLogEntity(
                CpkDbChangeLogKey(cpk.id.cpkFileChecksum, "master-$i"),
                "master-content",
                changesetId1
            )
        }

        transaction {
            persist(originalCpi)
            originalChangelogs.forEach {
                persist(it)
                persist(cpkDbChangeLogAuditEntity(originalCpi.name, originalCpi.version, originalCpi.signerSummaryHash, it))
            }
        }

        transaction {
            val loadedDbLogAuditEntities = getCpiChangelogsForGivenChangesetIds(
                this,
                cpiName,
                cpiVersion,
                cpiSsh,
                setOf(changesetId1)
            )

            assertThat(loadedDbLogAuditEntities.size).isEqualTo(3)
        }

        val changesetId2 = UUID.randomUUID()

        // This CPI has same PK as original, but different set of CPKs. When we merge this on top of original, we will see the old CpiCpk
        // relationships get purged (as a result of orphanRemoval). In the assertion we should see the correct set of dbChangeLogs returned
        val updatedCpi = cpi {
            name(originalCpi.name)
            version(originalCpi.version)
            signerSummaryHash(originalCpi.signerSummaryHash)
            cpk { }
            cpk { }
        }

        val newChangelogs = updatedCpi.cpks.mapIndexed { i, cpiCpk ->
            CpkDbChangeLogEntity(
                CpkDbChangeLogKey(cpiCpk.id.cpkFileChecksum, "master-$i"),
                "master-content",
                changesetId2
            )
        }

        transaction {
            merge(updatedCpi)
            newChangelogs.forEach {
                persist(it)
                persist(cpkDbChangeLogAuditEntity(updatedCpi.name, updatedCpi.version, updatedCpi.signerSummaryHash, it))
            }
        }

        transaction {
            val loadedDbLogAuditEntities = getCpiChangelogsForGivenChangesetIds(
                this,
                cpiName,
                cpiVersion,
                cpiSsh,
                setOf(changesetId2)
            )

            assertThat(loadedDbLogAuditEntities.size).isEqualTo(2)
        }
    }

    @Test
    fun `can persist changelogs to existing CPI`() {
        val (cpi, cpks) = TestObject.createCpiWithCpks()
        val cpk = cpks.first()
        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk.id.cpkFileChecksum, "master"),
            "master-content",
            fakeId
        )

        transaction {
            persist(cpi)
            flush()
        }

        transaction {
            persist(changeLog1)
            flush()
        }

        transaction {
            val loadedDbLogEntity = find(
                CpkDbChangeLogEntity::class.java,
                CpkDbChangeLogKey(cpk.id.cpkFileChecksum, "master")
            )

            assertThat(changeLog1.content).isEqualTo(loadedDbLogEntity.content)
        }
    }

    @Test
    fun `findCpkDbChangeLog returns all for cpk`() {
        val (cpi1, cpks1) = TestObject.createCpiWithCpks(2)
        val (cpi2, cpks2) = TestObject.createCpiWithCpks()
        val cpk1 = cpks1.first()
        val cpk1b = cpks1[1]
        val cpk2 = cpks2.first()

        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk1.id.cpkFileChecksum, "master"),
            """<databaseChangeLog xmlns="https://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd">

    <changeSet author="R3.Corda" id="test-migrations-v1.0">
        <createTable tableName="test">
             <column name="id" type="varchar(8)">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>
</databaseChangeLog>""",
            fakeId
        )
        val changeLog1b = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk1b.id.cpkFileChecksum, "master"),
            """<databaseChangeLog xmlns="https://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd">

    <changeSet author="R3.Corda" id="test-migrations-v1.0">
        <createTable tableName="test">
        <addColumn tableName="person" >
                <column name="is_active" type="varchar2(1)" defaultValue="Y" />  
            </addColumn>  
        </createTable>
    </changeSet>
</databaseChangeLog>""",
            fakeId
        )
        val changeLog2 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk1.id.cpkFileChecksum, "other"),
            "other-content",
            fakeId
        )
        val changeLog3 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk2.id.cpkFileChecksum, "master"),
            "master-content",
            fakeId
        )

        transaction {
            persist(cpi1)
            persist(cpi2)
            persist(changeLog1)
            persist(changeLog1b)
            persist(changeLog2)
            persist(changeLog3)
            flush()
            val changeLogs = findCurrentCpkChangeLogsForCpi(
                this,
                CpiIdentifier(cpi1.name, cpi1.version, SecureHash.parse(cpi1.signerSummaryHash))
            )
            assertThat(changeLogs.size).isEqualTo(3)
            assertThat(changeLogs.map { it.id }).containsAll(listOf(changeLog1.id, changeLog1b.id, changeLog2.id))
        }
    }

    @Test
    fun `findCpkDbChangeLog with no changelogs`() {
        val (cpi1, _) = TestObject.createCpiWithCpks(2)
        val (cpi2, _) = TestObject.createCpiWithCpks()

        transaction {
            persist(cpi1)
            persist(cpi2)
            flush()
            val changeLogs = findCurrentCpkChangeLogsForCpi(
                this,
                CpiIdentifier(cpi1.name, cpi1.version, SecureHash("SHA1", cpi1.signerSummaryHash.toByteArray()))
            )
            assertThat(changeLogs).isEmpty()
        }
    }
}
