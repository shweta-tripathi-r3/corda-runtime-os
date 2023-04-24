package net.corda.libs.cpi.datamodel.repository

import javax.persistence.EntityManager
import javax.persistence.LockModeType
import net.corda.libs.packaging.core.CpiIdentifier
import net.corda.libs.packaging.core.CpiMetadata
import net.corda.v5.crypto.SecureHash

interface CpiMetadataRepository {

    /**
     * @return null if not found
     */
    fun findById(em: EntityManager, cpiId: CpiIdentifier): CpiMetadata?

    /**
     * @return null if not found
     */
    fun findById(em: EntityManager, cpiId: CpiIdentifier, lockMode: LockModeType): CpiMetadata?

    /**
     * @return null if not found
     */
    fun findByNameAndSignerSummaryHash(em: EntityManager, name: String, signerSummaryHash: SecureHash): List<CpiMetadata>

    fun exist(em: EntityManager, cpiId: CpiIdentifier, lockMode: LockModeType): Boolean
}
