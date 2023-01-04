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
import javax.persistence.Version

/**
 * Representation of a DB ChangeLog (Liquibase) file associated with a CPK.
 */
@Entity
@Table(name = "cpk_db_change_log", schema = DbSchema.CONFIG)
class CpkDbChangeLogEntity(
    @EmbeddedId
    var id: CpkDbChangeLogKey,
    @Column(name = "content", nullable = false)
    val content: String,
) {
    // This structure does not distinguish the root changelogs from changelog include files
    // (or CSVs, which we do not need to support). So, to find the root, you need to look for a filename
    // convention. See the comment in the companion object of VirtualNodeDbChangeLog.
    // for the convention used when populating these records.
    @Version
    @Column(name = "entity_version", nullable = false)
    var entityVersion: Int = 0

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false

    // this TS is managed on the DB itself
    @Column(name = "insert_ts", insertable = false, updatable = false)
    val insertTimestamp: Instant? = null
}

/**
 * Composite primary key for a Cpk Change Log Entry.
 */
@Embeddable
data class CpkDbChangeLogKey(
    @Column(name = "cpk_file_checksum", nullable = false)
    var cpkFileChecksum: String,
    @Column(name = "file_path", nullable = false)
    val filePath: String,
    @Column(name = "changeset_id", nullable = false)
    val changesetId: UUID
) : Serializable

/*
 * Find all the db changelogs for a CPI
 */
fun findCurrentCpkChangeLogsForCpi(
    entityManager: EntityManager,
    cpi: CpiIdentifier
): List<CpkDbChangeLogEntity> {
    val allChangelogsForCPI = entityManager.createQuery(
        "SELECT changelog " +
                "FROM ${CpkDbChangeLogEntity::class.simpleName} AS changelog INNER JOIN " +
                "${CpiCpkEntity::class.simpleName} AS cpiCpk " +
                "ON changelog.id.cpkFileChecksum = cpiCpk.id.cpkFileChecksum " +
                "WHERE cpiCpk.id.cpiName = :name AND "+
                "      cpiCpk.id.cpiVersion = :version AND "+
                "      cpiCpk.id.cpiSignerSummaryHash = :signerSummaryHash AND "+
                "      changelog.isDeleted = FALSE " +
                "ORDER BY changelog.insertTimestamp DESC",
        CpkDbChangeLogEntity::class.java
    )
        .setParameter("name", cpi.name)
        .setParameter("version", cpi.version)
        .setParameter("signerSummaryHash", cpi.signerSummaryHash?.toString()?:"")
        .resultList

    if (allChangelogsForCPI.isEmpty()) return emptyList()

    // select out the most recent changelogs
    val mostRecentChangesetId = allChangelogsForCPI.first().id.changesetId
    return allChangelogsForCPI.filter { it.id.changesetId == mostRecentChangesetId }
}
