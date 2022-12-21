package net.corda.libs.cpi.datamodel

import net.corda.db.schema.DbSchema
import net.corda.libs.packaging.core.CpiIdentifier
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
    @Column(name = "cpk_file_checksum", nullable = false)
    val fileChecksum: String,
    @Column(name = "changeset_id", nullable = false)
    val changesetId: UUID,
    @Column(name = "entity_version", nullable = false)
    var entityVersion: Int,
    @Column(name = "file_path", nullable = false)
    val filePath: String,
) : Serializable

/*
 * Find all the audit db changelogs for a CPI
 */
fun findDbChangeLogAuditForCpi(
    entityManager: EntityManager,
    cpi: CpiIdentifier
): List<CpkDbChangeLogAuditEntity> = entityManager.createQuery(
    "SELECT changelog " +
        "FROM ${CpkDbChangeLogAuditEntity::class.simpleName} AS changelog INNER JOIN " +
        "${CpiCpkEntity::class.simpleName} AS cpi " +
        "ON changelog.id.fileChecksum = cpi.metadata.id.cpkFileChecksum " +
        "WHERE cpi.id.cpiName = :name AND " +
        "      cpi.id.cpiVersion = :version AND " +
        "      cpi.id.cpiSignerSummaryHash = :signerSummaryHash",
    CpkDbChangeLogAuditEntity::class.java
)
    .setParameter("name", cpi.name)
    .setParameter("version", cpi.version)
    .setParameter("signerSummaryHash", cpi.signerSummaryHash?.toString() ?: "")
    .resultList

/*
 * Find all the audit db changelogs for a CPI
 *
 *  lookup is chunked to prevent large list being passed as part of the IN clause
 */
fun findDbChangeLogAuditForCpi(
    entityManager: EntityManager,
    cpi: CpiIdentifier,
    changesetIds: Set<UUID>
): List<CpkDbChangeLogAuditEntity> = changesetIds.chunked(100) { changesetIdSlice ->
    entityManager.createQuery(
        "SELECT changelog " +
            "FROM ${CpkDbChangeLogAuditEntity::class.simpleName} AS changelog INNER JOIN " +
            "${CpiCpkEntity::class.simpleName} AS cpi " +
                "ON changelog.id.fileChecksum = cpi.metadata.id.cpkFileChecksum " +
            "WHERE cpi.id.cpiName = :name AND " +
            "      cpi.id.cpiVersion = :version AND " +
            "      cpi.id.cpiSignerSummaryHash = :signerSummaryHash AND " +
            "      changelog.id.changesetId IN :changesetIds",
        CpkDbChangeLogAuditEntity::class.java
    )
        .setParameter("name", cpi.name)
        .setParameter("version", cpi.version)
        .setParameter("signerSummaryHash", cpi.signerSummaryHash?.toString() ?: "")
        .setParameter("changesetIds", changesetIdSlice)
        .resultList
}.flatten()