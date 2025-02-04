package net.corda.virtualnode.write.db.impl.writer

import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import net.corda.crypto.core.parseSecureHash
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntity
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntityKey
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.libs.packaging.core.CpiMetadata
import net.corda.libs.packaging.core.CpkMetadata
import net.corda.orm.utils.transaction
import net.corda.orm.utils.use
import org.slf4j.LoggerFactory

/** Reads and writes CPIs, holding identities and virtual nodes to and from the cluster database. */
// TODO - remove this when moving to repository pattern for everything.
//  This will likely be done as part of CORE-8744
internal class VirtualNodeEntityRepository(
    private val entityManagerFactory: EntityManagerFactory
    ) : CpiEntityRepository {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        private const val SHORT_HASH_LENGTH: Int = 12
    }

    /** Reads CPI metadata from the database. */
    override fun getCpiMetadataByChecksum(cpiFileChecksum: String): CpiMetadata? {
        if (cpiFileChecksum.isBlank()) {
            log.warn("CPI file checksum cannot be empty")
            return null
        }

        if (cpiFileChecksum.length < SHORT_HASH_LENGTH) {
            log.warn("CPI file checksum must be at least $SHORT_HASH_LENGTH characters")
            return null
        }

        val cpiMetadataEntity = entityManagerFactory.transaction {
            val foundCpi = it.createQuery(
                "SELECT cpi FROM CpiMetadataEntity cpi " +
                    "WHERE upper(cpi.fileChecksum) like :cpiFileChecksum ",
                CpiMetadataEntity::class.java
            )
                .setParameter("cpiFileChecksum", "%${cpiFileChecksum.uppercase()}%")
                .resultList
            if (foundCpi.isNotEmpty()) foundCpi[0] else null
        } ?: return null

        return cpiMetadataEntity.toDto()
    }

    /** Reads CPI metadata from the database. */
    override fun getCPIMetadataByNameAndVersion(name: String, version: String): CpiMetadata? {
        val cpiMetadataEntity = entityManagerFactory.createEntityManager().use {
            it.transaction {
                it.createQuery(
                    "SELECT cpi FROM CpiMetadataEntity cpi " +
                            "WHERE cpi.name = :cpiName "+
                            "AND cpi.version = :cpiVersion ",
                    CpiMetadataEntity::class.java
                )
                    .setParameter("cpiName", name)
                    .setParameter("cpiVersion", version)
                    .singleResult
            }
        }

        return cpiMetadataEntity.toDto()
    }

    override fun getCPIMetadataById(em: EntityManager, id: CpiIdentifier): CpiMetadata? {
        return em.find(CpiMetadataEntity::class.java, id.toEntity())?.toDto()
    }

    private fun CpiIdentifier.toEntity(): CpiMetadataEntityKey =
        CpiMetadataEntityKey(name, version, signerSummaryHash.toString())

    private fun CpiMetadataEntity.toDto(): CpiMetadata {
        val cpiId = CpiIdentifier(name, version, parseSecureHash(signerSummaryHash))
        val fileChecksum = parseSecureHash(fileChecksum)
        val cpks = cpks.map { CpkMetadata.fromJsonAvro(it.metadata.serializedMetadata) }.toSet()
        return CpiMetadata(cpiId, fileChecksum, cpks, groupPolicy, entityVersion, insertTimestamp!!)
    }
}
