package net.corda.libs.cpi.datamodel.tests

import net.corda.db.admin.impl.ClassloaderChangeLog
import net.corda.db.admin.impl.LiquibaseSchemaMigratorImpl
import net.corda.db.schema.DbSchema
import net.corda.db.testkit.DbUtils
import net.corda.libs.cpi.datamodel.CpiEntities
import net.corda.libs.cpi.datamodel.CpkDbChangeLogEntity
import net.corda.libs.cpi.datamodel.CpkDbChangeLogKey
import net.corda.libs.cpi.datamodel.findDbChangeLogForCpi
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
import javax.persistence.EntityManager




@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CpkDbChangeLogEntityTest {
    private val dbConfig: EntityManagerConfiguration =
        DbUtils.getEntityManagerConfiguration("cpk_changelog_db")

    private fun transaction( callback: EntityManager.() -> Unit ): Unit = EntityManagerFactoryFactoryImpl().create(
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
    private fun cleanUp() {
        dbConfig.close()
    }

    @Test
    fun `can persist changelogs`() {
        val (cpi, cpk) = TestObject.createCpiWithCpk()

        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk.metadata.id.cpkName, cpk.metadata.id.cpkVersion,
                cpk.metadata.id.cpkSignerSummaryHash, "master"),
            "master-checksum",
            "master-content"
        )
        val changeLog2 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk.metadata.id.cpkName, cpk.metadata.id.cpkVersion,
                cpk.metadata.id.cpkSignerSummaryHash, "other"),
            "other-checksum",
            "other-content"
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
                CpkDbChangeLogKey(cpk.metadata.id.cpkName, cpk.metadata.id.cpkVersion,
                    cpk.metadata.id.cpkSignerSummaryHash, "master")
            )

            assertThat(changeLog1.content).isEqualTo(loadedDbLogEntity.content)
        }
    }

    @Test
    fun `can persist changelogs to existing CPI`() {
        val (cpi, cpk) = TestObject.createCpiWithCpk()

        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk.metadata.id.cpkName, cpk.metadata.id.cpkVersion, cpk.metadata.id.cpkSignerSummaryHash, "master"),
            "master-checksum",
            "master-content"
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
                CpkDbChangeLogKey(cpk.metadata.id.cpkName, cpk.metadata.id.cpkVersion, cpk.metadata.id.cpkSignerSummaryHash, "master")
            )

            assertThat(changeLog1.content).isEqualTo(loadedDbLogEntity.content)
        }
    }

    @Test
    fun `findCpkDbChangeLog returns all for cpk`() {
        val (cpi1, cpk1) = TestObject.createCpiWithCpk()
        val (cpi2, cpk2) = TestObject.createCpiWithCpk()

        val changeLog1 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk1.metadata.id.cpkName, cpk1.metadata.id.cpkVersion, cpk1.metadata.id.cpkSignerSummaryHash, "master"),
            "master-checksum",
            """<databaseChangeLog xmlns="https://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="https://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://www.liquibase.org/xml/ns/dbchangelog https://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.3.xsd">

    <changeSet author="R3.Corda" id="test-migrations-v1.0">
        <createTable tableName="test_table_in_other_schema">
            <column name="id" type="INT">
                <constraints nullable="false"/>
            </column>
        </createTable>
    </changeSet>
</databaseChangeLog>"""
        )
        val changeLog2 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk1.metadata.id.cpkName, cpk1.metadata.id.cpkVersion, cpk1.metadata.id.cpkSignerSummaryHash, "other"),
            "other-checksum",
            "other-content"
        )
        val changeLog3 = CpkDbChangeLogEntity(
            CpkDbChangeLogKey(cpk2.metadata.id.cpkName, cpk2.metadata.id.cpkVersion, cpk2.metadata.id.cpkSignerSummaryHash, "master"),
            "master-checksum",
            "master-content"
        )

        transaction {
            persist(cpi1)
            persist(cpi2)
            persist(changeLog1)
            persist(changeLog2)
            persist(changeLog3)
            flush()
        }

        val changeLogs = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use { em ->
            em.findDbChangeLogForCpi(CpiIdentifier(cpi1.name, cpi1.version, SecureHash("SHA1", cpi1.signerSummaryHash.toByteArray())))
        }

        assertThat(changeLogs.size).isEqualTo(2)
        assertThat(changeLogs.map { it.id }).containsAll(listOf(changeLog1.id, changeLog2.id))
    }
}