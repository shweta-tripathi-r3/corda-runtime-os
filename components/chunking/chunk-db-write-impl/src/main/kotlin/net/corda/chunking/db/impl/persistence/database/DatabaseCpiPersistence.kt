package net.corda.chunking.db.impl.persistence.database

import java.nio.file.Files
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.LockModeType
import net.corda.chunking.RequestId
import net.corda.chunking.db.impl.persistence.CpiPersistence
import net.corda.libs.cpi.datamodel.CpkDbChangeLog
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAudit
import net.corda.libs.cpi.datamodel.CpkFile
import net.corda.libs.cpi.datamodel.entities.CpiCpkEntity
import net.corda.libs.cpi.datamodel.entities.CpiCpkKey
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntity
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntityKey
import net.corda.libs.cpi.datamodel.entities.CpkMetadataEntity
import net.corda.libs.cpi.datamodel.repository.CpiMetadataRepository
import net.corda.libs.cpi.datamodel.repository.CpkDbChangeLogAuditRepositoryImpl
import net.corda.libs.cpi.datamodel.repository.CpkDbChangeLogRepositoryImpl
import net.corda.libs.cpi.datamodel.repository.CpkFileRepositoryImpl
import net.corda.libs.cpiupload.DuplicateCpiUploadException
import net.corda.libs.cpiupload.ValidationException
import net.corda.libs.packaging.Cpi
import net.corda.libs.packaging.Cpk
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.libs.packaging.core.CpiMetadata
import net.corda.membership.lib.grouppolicy.GroupPolicyParser
import net.corda.membership.lib.grouppolicy.GroupPolicyParser.Companion.isStaticNetwork
import net.corda.membership.network.writer.NetworkInfoWriter
import net.corda.orm.utils.transaction
import net.corda.v5.crypto.SecureHash
import org.slf4j.LoggerFactory

/**
 * This class provides some simple APIs to interact with the database for manipulating CPIs, CPKs and their associated
 * metadata.
 */
class DatabaseCpiPersistence(
    private val entityManagerFactory: EntityManagerFactory,
    private val networkInfoWriter: NetworkInfoWriter,
    private val cpiMetadataRepository: CpiMetadataRepository
) : CpiPersistence {

    private companion object {
        val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
        val cpkDbChangeLogRepository = CpkDbChangeLogRepositoryImpl()
        val cpkDbChangeLogAuditRepository = CpkDbChangeLogAuditRepositoryImpl()
        val cpkFileRepository = CpkFileRepositoryImpl()
    }

    /**
     * Check if we already have a cpk persisted with this checksum
     *
     * @return true if checksum exists in database
     */
    override fun cpkExists(cpkChecksum: SecureHash): Boolean {
        return entityManagerFactory.createEntityManager().transaction { em ->
            cpkFileRepository.exists(em, cpkChecksum)
        }
    }

    override fun cpiExists(cpiId: CpiIdentifier): Boolean =
        entityManagerFactory.createEntityManager().transaction { em ->
            getCpiMetadata(em, cpiId) != null
        }

    override fun persistMetadataAndCpks(
        cpi: Cpi,
        cpiFileName: String,
        cpiFileChecksum: SecureHash,
        requestId: RequestId,
        groupId: String,
        changelogsExtractedFromCpi: List<CpkDbChangeLog>
    ): CpiMetadata {
        entityManagerFactory.createEntityManager().transaction { em ->
            val groupPolicy = getGroupPolicy(em, cpi)

            val cpiMetadataEntity = createCpiMetadataEntity(
                cpi,
                cpiFileName,
                cpiFileChecksum,
                requestId,
                groupId,
                groupPolicy,
                createCpiCpkRelationships(em, cpi)
            )

            val managedCpiMetadataEntity = em.merge(cpiMetadataEntity)

            persistNewCpkFileEntities(em, cpi.metadata.fileChecksum, cpi.cpks)

            persistNewChangelogs(em, changelogsExtractedFromCpi)

            return@persistMetadataAndCpks CpiMetadata(
                cpi.metadata.cpiId,
                cpiFileChecksum,
                cpi.cpks.map { it.metadata },
                managedCpiMetadataEntity.groupPolicy,
                version = managedCpiMetadataEntity.entityVersion,
                timestamp = managedCpiMetadataEntity.insertTimestamp!!
            )
        }
    }

    private fun createCpiCpkRelationships(em: EntityManager, cpi: Cpi): Set<CpiCpkEntity> {
        // Todo: Move the query below elsewhere
        // there may be some CPKs that already exist. We should load these first, then create CpiCpk associations for them (if necessary).
        val foundCpks = em.createQuery(
            "FROM ${CpkMetadataEntity::class.java.simpleName} cpk " +
                    "WHERE cpk.cpkFileChecksum IN :cpkFileChecksums",
            CpkMetadataEntity::class.java
        )
            .setParameter("cpkFileChecksums", cpi.cpks.map { it.metadata.fileChecksum.toString() })
            .resultList
            .associateBy { it.cpkFileChecksum }

        val (existingCpks, newCpks) = cpi.cpks.partition { it.metadata.fileChecksum.toString() in foundCpks.keys }

        val newCpiCpkRelationships = newCpks.map { thisCpk ->
            val cpkFileChecksum = thisCpk.metadata.fileChecksum.toString()
            CpiCpkEntity(
                CpiCpkKey(
                    cpi.metadata.cpiId.name,
                    cpi.metadata.cpiId.version,
                    cpi.metadata.cpiId.signerSummaryHash.toString(),
                    cpkFileChecksum
                ),
                thisCpk.originalFileName!!,
                CpkMetadataEntity(
                    cpkFileChecksum,
                    thisCpk.metadata.cpkId.name,
                    thisCpk.metadata.cpkId.version,
                    thisCpk.metadata.cpkId.signerSummaryHash.toString(),
                    thisCpk.metadata.manifest.cpkFormatVersion.toString(),
                    thisCpk.metadata.toJsonAvro()
                )
            )
        }

        check(foundCpks.keys.size == existingCpks.toSet().size)
        check(foundCpks.keys == existingCpks.map { it.metadata.fileChecksum.toString() }.toSet())

        val relationshipsForExistingCpks = existingCpks.map { thisCpk ->
            val cpkFileChecksum = thisCpk.metadata.fileChecksum.toString()
            val cpiCpkKey = CpiCpkKey(
                cpi.metadata.cpiId.name,
                cpi.metadata.cpiId.version,
                cpi.metadata.cpiId.signerSummaryHash.toString(),
                cpkFileChecksum.toString()
            )

            // cpiCpk relationship might already exist for this CPI, for example, if a force uploaded CPI doesn't change a CPK, otherwise
            // create a new one with the CpkMetadataEntity
            em.find(CpiCpkEntity::class.java, cpiCpkKey)
                ?: CpiCpkEntity(
                    CpiCpkKey(
                        cpi.metadata.cpiId.name,
                        cpi.metadata.cpiId.version,
                        cpi.metadata.cpiId.signerSummaryHash.toString(),
                        cpkFileChecksum
                    ),
                    thisCpk.originalFileName!!,
                    foundCpks[cpkFileChecksum]!!
                )
        }

        val totalCpiCpkRelationships = newCpiCpkRelationships + relationshipsForExistingCpks
        return totalCpiCpkRelationships.toSet()
    }

    /**
     * Update the changelogs in the db for cpi upload
     *
     * @property changelogsExtractedFromCpi: [List]<[CpkDbChangeLog]> a list of changelogs extracted from the force
     *  uploaded cpi.
     * @property em: [EntityManager] the entity manager from the call site. We reuse this for several operations as part
     *  of CPI upload
     *
     * @return [Boolean] indicating whether we actually updated any changelogs
     */
    private fun persistNewChangelogs(
        em: EntityManager,
        changelogsExtractedFromCpi: Collection<CpkDbChangeLog>
    ) {

        changelogsExtractedFromCpi.forEach { changelog ->
            log.info("Persisting changelog and audit for CPK: ${changelog.id.cpkFileChecksum}, ${changelog.id.filePath})")
            cpkDbChangeLogRepository.update(
                em,
                changelog
            )  // updating ensures any existing changelogs have isDeleted set to false
            cpkDbChangeLogAuditRepository.put(em, CpkDbChangeLogAudit(changeLog = changelog))
        }
    }

    override fun updateMetadataAndCpks(
        cpi: Cpi,
        cpiFileName: String,
        cpiFileChecksum: SecureHash,
        requestId: RequestId,
        groupId: String,
        changelogsExtractedFromCpi: Collection<CpkDbChangeLog>
    ): CpiMetadata {
        val cpiId = cpi.metadata.cpiId
        log.info("Performing updateMetadataAndCpks for: ${cpiId.name} v${cpiId.version}")

        // Perform update of CPI and store CPK along with its metadata
        entityManagerFactory.createEntityManager().transaction { em ->
            // We cannot delete old representation of CPI as there is FK constraint from `vnode_instance`
            val existingMetadataEntity = requireNotNull(
                getCpiMetadata(em, cpiId)
            ) {
                "Cannot find CPI metadata for ${cpiId.name} v${cpiId.version}"
            }

            val updatedMetadata = CpiMetadataEntity.create(
                id = existingMetadataEntity.cpiId,
                fileName = cpiFileName,
                fileChecksum = cpiFileChecksum,
                groupPolicy = existingMetadataEntity.groupPolicy!!,
                groupId = groupId,
                fileUploadRequestId = requestId,
                cpks = createCpiCpkRelationships(em, cpi)
            )

            val cpiMetadataEntity = em.merge(updatedMetadata)

            persistNewCpkFileEntities(em, cpiFileChecksum, cpi.cpks)

            persistNewChangelogs(em, changelogsExtractedFromCpi)

            return CpiMetadata(
                cpi.metadata.cpiId,
                cpiFileChecksum,
                cpi.cpks.map { it.metadata },
                cpiMetadataEntity.groupPolicy,
                version = cpiMetadataEntity.entityVersion,
                timestamp = cpiMetadataEntity.insertTimestamp!!
            )
        }
    }

    /**
     * @return null if not found
     */
    private fun findCpiMetadataEntityInTransaction(
        em: EntityManager,
        cpiId: CpiIdentifier
    ): CpiMetadataEntity? {

        val primaryKey = CpiMetadataEntityKey(
            cpiId.name,
            cpiId.version,
            cpiId.signerSummaryHash.toString()
        )

        return em.find(
            CpiMetadataEntity::class.java,
            primaryKey,
            // In case of force update, we want the entity to change regardless of whether the CPI being uploaded
            //  is identical to an existing.
            //  OPTIMISTIC_FORCE_INCREMENT means the version number will always be bumped.
            LockModeType.OPTIMISTIC_FORCE_INCREMENT
        )
    }

    override fun getGroupId(cpiId: CpiIdentifier): String? {
        entityManagerFactory.createEntityManager().transaction { em ->
            val groupPolicy = getCpiMetadata(em, cpiId)?.groupPolicy
                ?: return null
            return GroupPolicyParser.groupIdFromJson(groupPolicy)
        }
    }

    /**
     * For a given CPI, create the metadata entity required to insert into the database.
     *
     * @param cpi CPI object
     * @param cpiFileName original file name
     * @param checksum checksum/hash of the CPI
     * @param requestId the requestId originating from the chunk upload
     */
    @Suppress("LongParameterList")
    private fun createCpiMetadataEntity(
        cpi: Cpi,
        cpiFileName: String,
        checksum: SecureHash,
        requestId: RequestId,
        groupId: String,
        groupPolicy: String,
        cpiCpkEntities: Set<CpiCpkEntity>
    ): CpiMetadataEntity {
        val cpiMetadata = cpi.metadata

        return CpiMetadataEntity.create(
            id = cpiMetadata.cpiId,
            fileName = cpiFileName,
            fileChecksum = checksum,
            groupPolicy = groupPolicy,
            groupId = groupId,
            fileUploadRequestId = requestId,
            cpks = cpiCpkEntities
        )
    }

    private fun getCpiMetadata(em: EntityManager, cpiId: CpiIdentifier) =
        // In case of force update, we want the entity to change regardless of whether the CPI being uploaded
        //  is identical to an existing.
        //  OPTIMISTIC_FORCE_INCREMENT means the version number will always be bumped.
        cpiMetadataRepository.findById(em, cpiId, LockModeType.OPTIMISTIC_FORCE_INCREMENT)

    private fun persistNewCpkFileEntities(em: EntityManager, cpiFileChecksum: SecureHash, cpks: Collection<Cpk>) {
        val existingCpkMap =
            cpkFileRepository.findById(em, cpks.map { it.metadata.fileChecksum }).associateBy { it.fileChecksum }

        val (existingCpks, newCpks) = cpks.partition { it.metadata.fileChecksum in existingCpkMap.keys }

        check(existingCpks.toSet().size == existingCpkMap.keys.size)

        newCpks.forEach {
            cpkFileRepository.put(em, CpkFile(it.metadata.fileChecksum, Files.readAllBytes(it.path!!)))
        }

        if (existingCpks.isNotEmpty()) {
            log.info(
                "When persisting CPK files for CPI $cpiFileChecksum, ${existingCpks.size} file entities already" +
                        "existed with checksums ${existingCpkMap.keys.joinToString()}. No changes were made to these" +
                        "files."
            )
        }
    }

    override fun validateCanUpsertCpi(
        cpiId: CpiIdentifier,
        groupId: String,
        forceUpload: Boolean,
        requestId: String
    ) {
        val sameCPis = entityManagerFactory.createEntityManager().transaction { em ->
            cpiMetadataRepository.findByNameAndSignerSummaryHash(em, cpiId.name, cpiId.signerSummaryHash)
        }

        if (forceUpload) {
            if (!sameCPis.any { it.cpiId.version == cpiId.version }) {
                throw ValidationException("No instance of same CPI with previous version found", requestId)
            }

            if (GroupPolicyParser.groupIdFromJson(sameCPis.first().groupPolicy!!) != groupId) {
                throw ValidationException("Cannot force update a CPI with a different group ID", requestId)
            }
            // We can force-update this CPI because we found one with the same version
            return
        }

        // outside a force-update, anything goes except identical ID (name, signer and version)
        if (sameCPis.any { it.cpiId.version == cpiId.version }) {
            throw DuplicateCpiUploadException("CPI ${cpiId.name}, ${cpiId.version}, ${cpiId.signerSummaryHash} already exists.")
        }

        // NOTE: we may do additional validation here, such as validate that the group ID is not changing during a
        //  regular update. For now, just logging this as un-usual.
        if (sameCPis.any { GroupPolicyParser.groupIdFromJson(it.groupPolicy!!) != groupId }) {
            log.info(
                "CPI upload $requestId contains a CPI with the same name (${cpiId.name}) and " +
                        "signer (${cpiId.signerSummaryHash}) as an existing CPI, but a different Group ID."
            )
        }
    }

    /**
     * Process CPI network data and return the processed group policy.
     */
    private fun getGroupPolicy(
        em: EntityManager,
        cpi: Cpi
    ): String {
        val groupPolicy = cpi.metadata.groupPolicy!!
        return if (isStaticNetwork(groupPolicy)) {
            networkInfoWriter.parseAndPersistStaticNetworkInfo(em, cpi)
            networkInfoWriter.injectStaticNetworkMgm(em, groupPolicy)
        } else {
            groupPolicy
        }
    }
}
