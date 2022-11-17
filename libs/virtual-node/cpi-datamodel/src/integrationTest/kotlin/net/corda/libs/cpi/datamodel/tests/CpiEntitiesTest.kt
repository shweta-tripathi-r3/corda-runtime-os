package net.corda.libs.cpi.datamodel.tests

import net.corda.db.admin.impl.ClassloaderChangeLog
import net.corda.db.admin.impl.LiquibaseSchemaMigratorImpl
import net.corda.db.schema.DbSchema
import net.corda.db.testkit.DbUtils
import net.corda.libs.cpi.datamodel.CpiCpkEntity
import net.corda.libs.cpi.datamodel.CpiCpkKey
import net.corda.libs.cpi.datamodel.CpiEntities
import net.corda.libs.cpi.datamodel.CpiMetadataEntity
import net.corda.libs.cpi.datamodel.CpiMetadataEntityKey
import net.corda.libs.cpi.datamodel.CpkFileEntity
import net.corda.libs.cpi.datamodel.CpkKey
import net.corda.libs.cpi.datamodel.findAllCpiMetadata
import net.corda.libs.cpi.datamodel.findCpkChecksumsNotIn
import net.corda.libs.cpi.datamodel.findCpkDataEntity
import net.corda.orm.EntityManagerConfiguration
import net.corda.orm.impl.EntityManagerFactoryFactoryImpl
import net.corda.orm.utils.transaction
import net.corda.orm.utils.use
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID
import javax.persistence.EntityManagerFactory
import kotlin.streams.toList

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CpiEntitiesIntegrationTest {
    private val dbConfig: EntityManagerConfiguration = DbUtils.getEntityManagerConfiguration("cpi_db")

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
    fun `can persist cpi and cpks`() {
        val cpiId = UUID.randomUUID()
        val cpkId = UUID.randomUUID().toString()
        val cpkSignerSummaryHash = TestObject.randomChecksumString()

        val cpkData = CpkFileEntity(
            CpkKey(cpkId, "1.2.3", cpkSignerSummaryHash),
            "cpk-checksum-$cpkId",
            ByteArray(2000),
        )
        val cpiCpk =
            TestObject.createCpiCpkEntity(
                "test-cpi-$cpiId", "1.0", "test-cpi-hash",
                cpkId, "1.2.3", cpkSignerSummaryHash,
                "test-cpk", cpkData.fileChecksum,
            )
        val cpi = TestObject.createCpi(cpiId, setOf(cpiCpk))

        EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use { em ->
            em.transaction {
                it.persist(cpi)
                it.persist(cpkData)
                it.flush()
            }
        }

        EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use {
            val loadedCpiEntity = it.find(
                CpiMetadataEntity::class.java,
                CpiMetadataEntityKey(cpi.name, cpi.version, cpi.signerSummaryHash)
            )

            assertThat(loadedCpiEntity.cpks).isEqualTo(cpi.cpks)
        }

    }

    @Test
    fun `can add cpk to cpi`() {
        val cpiId = UUID.randomUUID()
        val cpkId = UUID.randomUUID().toString()
        val cpkData = CpkFileEntity(
            CpkKey(cpkId, "cpkVer", "cpkSSH"),
            "cpk-checksum-$cpkId",
            ByteArray(2000),
        )
        val cpiCpk =
            TestObject.createCpiCpkEntity(
                "test-cpi-$cpiId", "1.0", "test-cpi-hash",
                cpkId, "1.2.3", TestObject.randomChecksumString(),
                "test-cpk", cpkData.fileChecksum,
            )
        val cpi = TestObject.createCpi(cpiId, setOf(cpiCpk))
        EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use { em ->
            em.transaction {
                it.persist(cpi)
                it.flush()
            }
        }

        // Create another CPK
        val cpkName2 = "test-cpk2"
        val cpkVer2 = "2.2.3"
        val cpk2SignerSummaryHash = TestObject.randomChecksumString()
        val cpkData2 = CpkFileEntity(
            CpkKey(cpkName2, cpkVer2, cpk2SignerSummaryHash),
            "cpk-checksum-${UUID.randomUUID()}",
            ByteArray(2000),
        )
        val cpkMetadataEntity2 = TestObject.createCpk(
            cpkData2.fileChecksum,
            cpkName2,
            cpkVer2,
            cpk2SignerSummaryHash,
        )

        EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use { em ->
            val  loadedCpiEntity = em.find(
                CpiMetadataEntity::class.java,
                CpiMetadataEntityKey(cpi.name, cpi.version, cpi.signerSummaryHash)
            )

            val updated = loadedCpiEntity.copy(
                cpks = loadedCpiEntity.cpks.plus(
                    CpiCpkEntity(
                        CpiCpkKey(
                            cpi.name,
                            cpi.version,
                            cpi.signerSummaryHash,
                            cpkMetadataEntity2.id.cpkName,
                            cpkMetadataEntity2.id.cpkVersion,
                            cpkMetadataEntity2.id.cpkSignerSummaryHash
                        ),
                        "test-cpk2.cpk",
                        cpkMetadataEntity2.cpkFileChecksum,
                        cpkMetadataEntity2
                    )
                ),
            )

            em.transaction {
                it.merge(updated)
                it.persist(cpkData2)
                it.flush()
            }
        }

        val loadedCpi = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use {
            it.find(
                CpiMetadataEntity::class.java,
                CpiMetadataEntityKey(cpi.name, cpi.version, cpi.signerSummaryHash)
            )
        }

        // assert the added one has been fetched eagerly.
        assertThat(loadedCpi.cpks.singleOrNull {
            it.metadata.cpkFileChecksum == cpkMetadataEntity2.cpkFileChecksum
        }).isNotNull
    }

    @Test
    fun `can have second cpi with shared cpk`() {
        val cpiId = UUID.randomUUID()
        val cpkId = UUID.randomUUID().toString()
        val cpk2Id = UUID.randomUUID().toString()
        val cpkVer = "1.2.3"
        val cpkSSH = TestObject.randomChecksumString()
        val cpkData = CpkFileEntity(
            CpkKey(cpkId, cpkVer, cpkSSH),
            "cpk-checksum-$cpkId",
            ByteArray(2000),
        )
        val cpiCpk1 =
            TestObject.createCpiCpkEntity(
                "test-cpi-$cpiId", "1.0", "test-cpi-hash",
                cpkId, cpkVer, cpkSSH,
                "test-cpk1", cpkData.fileChecksum,
            )
        val cpi1 = TestObject.createCpi(cpiId, setOf(cpiCpk1))

        EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use { em ->
            em.transaction {
                it.persist(cpi1)
                it.persist(cpkData)
                it.flush()
            }
        }

        val cpk2Ver = "2.2.3"
        val cpk2SSH = TestObject.randomChecksumString()
        // Create another CPK
        val cpkData2 = CpkFileEntity(
            CpkKey(cpk2Id, cpk2Ver, cpk2SSH),
            "cpk-checksum-${UUID.randomUUID()}",
            ByteArray(2000),
        )
        val cpiCpk2 =
            TestObject.createCpiCpkEntity(
                "test-cpi-$cpiId", "1.0", "test-cpi-hash",
                cpk2Id, cpk2Ver, cpk2SSH,
                "test-cpk2", cpkData2.fileChecksum,
            )
        val cpi2 = TestObject.createCpi(cpiId, setOf(cpiCpk1, cpiCpk2))
        EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use { em ->
            em.transaction {
                it.merge(cpi2)
                it.persist(cpkData2)
                it.flush()
            }
        }

        val loadedCpi = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        ).use {
            it.find(
                CpiMetadataEntity::class.java,
                CpiMetadataEntityKey(cpi2.name, cpi2.version, cpi2.signerSummaryHash)
            )
        }

        assertThat(loadedCpi.cpks.size).isEqualTo(2)
    }

    @Test
    fun `on findCpkChecksumsNotIn an empty set returns all results`() {
        val cpkChecksums = List(3) { TestObject.randomChecksumString() }

        val emFactory = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        )

        insertCpkChecksums(cpkChecksums, emFactory)

        val fetchedCpkChecksums =
            emFactory.transaction {
                it.findCpkChecksumsNotIn(emptyList())
            }

        assertThat(fetchedCpkChecksums).containsAll(cpkChecksums)
    }

    @Test
    fun `on findCpkChecksumsNotIn a checksum set returns all but this set`() {
        val cpkChecksums = List(3) { TestObject.randomChecksumString() }

        val emFactory = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        )

        insertCpkChecksums(cpkChecksums, emFactory)

        val fetchedCpkChecksums =
            emFactory.transaction {
                it.findCpkChecksumsNotIn(cpkChecksums.take(2))
            }

        assertThat(fetchedCpkChecksums).contains(cpkChecksums[2])
    }

    @Test
    fun `finds CPK data entity`() {
        val cpkChecksums = listOf(
            TestObject.randomChecksumString()
        )

        val emFactory = EntityManagerFactoryFactoryImpl().create(
            "test_unit",
            CpiEntities.classes.toList(),
            dbConfig
        )

        val insertedCpks = insertCpkChecksums(cpkChecksums, emFactory)

        val cpkDataEntity =
            emFactory.transaction {
                it.findCpkDataEntity(cpkChecksums[0])
            }

        assertThat(cpkDataEntity).isEqualTo(insertedCpks[0])
    }

    @Disabled("Pending resolution of: CORE-8145")
    @Test
    fun `what happens when it shares metadata`() {
        val emFactory = EntityManagerFactoryFactoryImpl().create(
            "test_unit_${UUID.randomUUID()}",
            CpiEntities.classes.toList(),
            dbConfig
        )
        val cpiId = UUID.randomUUID()
        val cpkName = "test-cpk-${UUID.randomUUID()}"
        val cpkVersion = "1.2.3"
        val cpkSSH = TestObject.randomChecksumString()
        val cpks = (1..2).associate {
            println(it)
            val cpkMetadataEntity =
                TestObject.createCpiCpkEntity(
                    "test-cpi-$cpiId", "1.0", "test-cpi-hash",
                    cpkName, cpkVersion, cpkSSH,
                    "test-cpk.cpk$it", TestObject.randomChecksumString(),
                )
            val cpkData = CpkFileEntity(
                CpkKey(cpkName, cpkVersion, cpkSSH),
                cpkMetadataEntity.cpkFileChecksum,
                ByteArray(2000),
            )
            cpkMetadataEntity to cpkData
        }
        val cpi = TestObject.createCpi(cpiId, cpks.keys)
        emFactory.use { em ->
            em.transaction {
                it.persist(cpi)
                cpks.values.forEach { cpkData ->
                    it.persist(cpkData)
                }
                it.flush()
            }
        }

        val cpisEagerlyLoaded = emFactory.use { em ->
            em.transaction {
                em.findAllCpiMetadata().toList() // toList here materialises the stream.
            }
            // closing the EntityManager validates that we haven't returned proxies but instead eagerly loaded all data
        }
        val cpiForThisTest = cpisEagerlyLoaded.single{ it.name == "test-cpi-$cpiId" }

        assertThat(cpiForThisTest.cpks.size).isEqualTo(2)
    }

    @Disabled("Pending resolution to: CORE-7629")
    @Test
    fun `findAllCpiMetadata properly streams through DB data`() {
        val emFactory = EntityManagerFactoryFactoryImpl().create(
            "test_unit_${UUID.randomUUID()}",
            CpiEntities.classes.toList(),
            dbConfig
        )

        val cpis = mutableListOf<CpiMetadataEntity>()

        assertSoftly { sa ->
            // Create CPIs First
            val cpiIds = (0..1).map {i ->
                val cpiId = UUID.randomUUID()
                val cpks = (1..2).associate {
                    println(it)
                    val cpkName = "test-cpk$it-${UUID.randomUUID()}"
                    val cpkVersion = "$i.2.3"
                    val cpkSSH = TestObject.randomChecksumString()
                    val cpkMetadataEntity =
                        TestObject.createCpiCpkEntity(
                            "test-cpi-$cpiId", "1.0", "test-cpi-hash",
                            cpkName, cpkVersion, cpkSSH,
                            "test-cpk.cpk$it", TestObject.randomChecksumString(),
                        )
                    val cpkData = CpkFileEntity(
                        CpkKey(cpkName, cpkVersion, cpkSSH),
                        cpkMetadataEntity.cpkFileChecksum,
                        ByteArray(2000),
                    )
                    cpkMetadataEntity to cpkData
                }
                sa.assertThat(cpks.size).isEqualTo(2)
                val cpi = TestObject.createCpi(cpiId, cpks.keys)
                cpis.add(cpi)
    
                emFactory.use { em ->
                    em.transaction {
                        it.persist(cpi)
                        cpks.values.forEach { cpkData ->
                            it.persist(cpkData)
                        }
                        it.flush()
                    }
                }
                return@map cpiId
            }
    
            // Find all - fetches eagerly
            println("**** [START] findAllCpiMetadata query as list ****")
            val cpisEagerlyLoaded = emFactory.use { em ->
                em.transaction {
                    em.findAllCpiMetadata().toList() // toList here materialises the stream.
                }
                // closing the EntityManager validates that we haven't returned proxies but instead eagerly loaded all data
            }
    
            sa.assertThat(cpisEagerlyLoaded.filter {
                cpiIds.contains(UUID.fromString(it.name.substringAfterLast("test-cpi-")))
            }.size).isEqualTo(2)
            cpisEagerlyLoaded.forEach {
                sa.assertThat(it.cpks.size).isEqualTo(2)
                it.cpks.forEach { cpkMetadataEntity ->
                    println("****       invoke metadata property ****")
                    sa.assertThat(cpkMetadataEntity.metadata).isNotNull
                }
            }
            println("**** [END] findAllCpiMetadata query as list ****")
    
            // Repeat the above, but consume from stream
            println("**** [START] findAllCpiMetadata query as stream ****")
            emFactory.use { em ->
                em.transaction {
                    val cpisLazyLoaded = em.findAllCpiMetadata()
    
                    var count = 0
                    cpisLazyLoaded.forEach {
                        if (cpiIds.contains(UUID.fromString(it.name.substringAfterLast("test-cpi-")))) {
                            count += 1
                            println("****       cpi has ${it.cpks.size} cpks: ****")
                            it.cpks.forEach { cpkMetadataEntity ->
                                println("****       invoke metadata property ****")
                                sa.assertThat(cpkMetadataEntity.metadata).isNotNull
                                println("****       ${cpkMetadataEntity.metadata.id}****")
                            }
                            sa.assertThat(it.cpks.size).isEqualTo(2)
                        }
                    }
                    sa.assertThat(count).isEqualTo(2)
                }
            }
        }
        println("**** [END] findAllCpiMetadata query as stream ****")
    }

    private fun insertCpkChecksums(cpkChecksums: List<String>, emFactory: EntityManagerFactory):
            List<CpkFileEntity> {
        val cpkFiles = mutableListOf<CpkFileEntity>()
        emFactory.transaction { em ->
            cpkChecksums.forEach {
                val cpkName = "file.cpk"
                val cpkVer = "1.2.3"
                val cpkSSH = TestObject.randomChecksumString()

                val cpkMeta = TestObject.createCpk(it, cpkName, cpkVer, cpkSSH)
                val cpkData = CpkFileEntity(
                    CpkKey(cpkName, cpkVer, cpkSSH),
                    it,
                    byteArrayOf(0x01, 0x02, 0x03)
                )

                em.persist(cpkMeta)
                em.persist(cpkData)
                cpkFiles.add(cpkData)
            }
        }
        return cpkFiles
    }
}
