package net.corda.libs.cpi.datamodel

import net.corda.db.schema.DbSchema
import java.io.Serializable
import java.time.Instant
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.EntityManager
import javax.persistence.Table

/**
 * Audit representation of a DB ChangeLog (Liquibase) file associated with a CPK.
 */
@Entity
@Table(name = "cpk_db_change_log_audit", schema = DbSchema.CONFIG)
class CpkDbChangeLogAuditEntity(
    @EmbeddedId
    var id: CpkDbChangeLogAuditKey,
    @Column(name = "content", nullable = false)
    val content: String,
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false
) {
    // this TS is managed on the DB itself
    @Column(name = "insert_ts", insertable = false, updatable = false)
    val insertTimestamp: Instant? = null
}

@Embeddable
data class CpkDbChangeLogAuditKey(
    @Column(name = "cpi_name", nullable = false)
    var cpiName: String,
    @Column(name = "cpi_version", nullable = false)
    var cpiVersion: String,
    @Column(name = "cpi_signer_summary_hash", nullable = false)
    var cpiSignerSummaryHash: String,
    @Column(name = "cpk_file_checksum", nullable = false)
    val cpkFileChecksum: String,
    @Column(name = "changeset_id", nullable = false)
    val changesetId: UUID,
    @Column(name = "entity_version", nullable = false)
    var entityVersion: Int,
    @Column(name = "file_path", nullable = false)
    val filePath: String,
) : Serializable

fun changelogAuditEntriesForGivenChangesetIds(entityManager: EntityManager, changesetIds: Set<UUID>): List<CpkDbChangeLogAuditEntity> {
    return changesetIds.chunked(100) { changesetIdSlice ->
        entityManager.createQuery(
            "FROM ${CpkDbChangeLogAuditEntity::class.simpleName}" +
                    " WHERE id.changesetId IN :changesetIds",
            CpkDbChangeLogAuditEntity::class.java
        )
            .setParameter("changesetIds", changesetIdSlice)
            .resultList
    }.flatten()
}