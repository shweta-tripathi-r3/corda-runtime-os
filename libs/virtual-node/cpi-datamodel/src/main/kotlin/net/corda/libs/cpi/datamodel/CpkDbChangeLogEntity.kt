package net.corda.libs.cpi.datamodel

import net.corda.db.schema.DbSchema
import net.corda.libs.cpi.datamodel.CpkDbChangeLogEntity.Companion.QUERY_FIND_FOR_CPI
import net.corda.libs.packaging.core.CpiIdentifier
import java.io.Serializable
import java.nio.charset.Charset
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.EmbeddedId
import javax.persistence.Entity
import javax.persistence.EntityManager
import javax.persistence.NamedQuery
import javax.persistence.Table
import javax.persistence.Version

/**
 * Representation of a DB ChangeLog (Liquibase) file associated with a CPK.
 */
@Entity
@Table(name = "cpk_db_change_log", schema = DbSchema.CONFIG)
@NamedQuery(
    name= QUERY_FIND_FOR_CPI,
    query="SELECT NEW CpkDbChangeLogEntity(d.id, d.fileChecksum, d.content) " +
            "FROM CpkDbChangeLogEntity AS d INNER JOIN " +
            "CpiCpkEntity AS i " +
            "ON d.id.cpkName = i.metadata.id.cpkName AND d.id.cpkVersion = i.id.cpkVersion AND " +
            "   d.id.cpkSignerSummaryHash = i.id.cpkSignerSummaryHash "+
            "WHERE i.id.cpiName = :name AND "+
            "      i.id.cpiVersion = :version AND "+
            "      i.id.cpiSignerSummaryHash = :signerSummaryHash")
class CpkDbChangeLogEntity(
    @EmbeddedId
    var id: CpkDbChangeLogKey,
    @Column(name = "cpk_file_checksum", nullable = false, unique = true)
    val fileChecksum: String,
    @Column(name = "content", nullable = false)
    val content: String,
) {
    companion object {
        const val QUERY_FIND_FOR_CPI = "CpkDbChangeLogEntity.findDbChangeLogForCpi"
    }
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
    @Column(name = "cpk_name", nullable = false)
    var cpkName: String,
    @Column(name = "cpk_version", nullable = false)
    var cpkVersion: String,
    @Column(name = "cpk_signer_summary_hash", nullable = false)
    var cpkSignerSummaryHash: String,
    @Column(name = "file_path", nullable = false)
    val filePath: String,
) : Serializable


/*
 * Find all the db changelogs for a CPI
 */
fun EntityManager.findDbChangeLogForCpi(
    cpi: CpiIdentifier
): List<CpkDbChangeLogEntity> = createNamedQuery(
    QUERY_FIND_FOR_CPI,
    CpkDbChangeLogEntity::class.java
)
    .setParameter("name", cpi.name)
    .setParameter("version", cpi.version)
    .setParameter("signerSummaryHash", cpi.signerSummaryHash?.bytes?.toString(Charset.defaultCharset())?:"")
    .resultList
