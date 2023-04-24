package net.corda.libs.cpi.datamodel.repository

import javax.persistence.EntityManager
import javax.persistence.LockModeType
import net.corda.crypto.core.parseSecureHash
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntity
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntityKey
import net.corda.libs.cpi.datamodel.entities.CpkMetadataEntity
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.libs.packaging.core.CpiMetadata
import net.corda.libs.packaging.core.CpkMetadata
import net.corda.v5.crypto.SecureHash

class CpiMetadataRepositoryImpl: CpiMetadataRepository {
    /**
     * @return null if not found
     */
    override fun findById(em: EntityManager, cpiId: CpiIdentifier): CpiMetadata? {
        return em.find(CpiMetadataEntity::class.java, cpiId.toEntity())?.toDto()
    }

    /**
     * @return null if not found
     */
    override fun findById(em: EntityManager, cpiId: CpiIdentifier, lockMode: LockModeType): CpiMetadata? {
        return em.find(CpiMetadataEntity::class.java, cpiId.toEntity(), lockMode)?.toDto()
    }

    override fun exist(em: EntityManager, cpiId: CpiIdentifier, lockMode: LockModeType): Boolean {
        return findById(em, cpiId, lockMode) != null
    }

    override fun findByNameAndSignerSummaryHash(em: EntityManager, name: String, signerSummaryHash: SecureHash): List<CpiMetadata> {
        return em.createQuery(
            "FROM ${CpiMetadataEntity::class.simpleName} c " +
                    "WHERE c.name = :cpiName " +
                    "AND c.signerSummaryHash = :cpiSignerSummaryHash",
            CpiMetadataEntity::class.java
        )
            .setParameter("cpiName", name)
            .setParameter("cpiSignerSummaryHash", signerSummaryHash.toString())
            .resultList.map { it.toDto() }
    }


    private fun CpiIdentifier.toEntity() =
        CpiMetadataEntityKey(name, version, signerSummaryHash.toString())

    private fun CpiMetadataEntity.toDto() =
        CpiMetadata(
            CpiIdentifier(name, version, parseSecureHash(signerSummaryHash)),
            parseSecureHash(fileChecksum),
            cpks.map { it.metadata.toDto() },
            groupPolicy,
            version = entityVersion,
            timestamp = insertTimestamp!!
        )

    // Todo: This should be moved elsewhere
    private fun CpkMetadataEntity.toDto() =
        CpkMetadata.fromJsonAvro(serializedMetadata)
}
