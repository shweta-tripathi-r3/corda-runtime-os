package net.corda.test.util.virtualnode.cpx.dsl

import java.util.UUID
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAuditEntity
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAuditKey
import net.corda.libs.cpi.datamodel.CpkDbChangeLogKey

fun cpkDbChangeLogAudit(init: CpkDbChangeLogAuditBuilder.() -> Unit): CpkDbChangeLogAuditEntity {
    val builder = CpkDbChangeLogAuditBuilder()
    init(builder)
    return builder.build()
}

class CpkDbChangeLogAuditBuilder(private var fileChecksumSupplier: () -> String? = { null }, private val randomUUID: UUID = UUID.randomUUID()) {

    private var filePath: String? = null
    private var changesetId: UUID? = null
    private var entityVersion: Int? = null
    private var isDeleted: Boolean? = null

    fun fileChecksum(value: String): CpkDbChangeLogAuditBuilder {
        fileChecksumSupplier = { value }
        return this
    }

    fun filePath(value: String): CpkDbChangeLogAuditBuilder {
        filePath = value
        return this
    }

    fun changesetId(value: UUID): CpkDbChangeLogAuditBuilder {
        changesetId = value
        return this
    }

    fun isDeleted(value: Boolean): CpkDbChangeLogAuditBuilder {
        isDeleted = value
        return this
    }

    fun entityVersion(value: Int): CpkDbChangeLogAuditBuilder {
        entityVersion = value
        return this
    }

    fun build(): CpkDbChangeLogAuditEntity {
        return CpkDbChangeLogAuditEntity(
            CpkDbChangeLogAuditKey(
                fileChecksumSupplier.invoke() ?: "file_checksum_$randomUUID",
            changesetId ?: UUID.randomUUID(),
            entityVersion ?: 0,
                filePath ?: "file_path_$randomUUID"
            ),
            "data_$randomUUID",
            isDeleted ?: false
        )
    }
}