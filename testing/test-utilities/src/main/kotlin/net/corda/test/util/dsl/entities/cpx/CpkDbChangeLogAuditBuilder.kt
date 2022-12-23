package net.corda.test.util.dsl.entities.cpx

import java.util.UUID
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAuditEntity
import net.corda.libs.cpi.datamodel.CpkDbChangeLogAuditKey

fun cpkDbChangeLogAudit(init: CpkDbChangeLogAuditBuilder.() -> Unit): CpkDbChangeLogAuditEntity {
    val builder = CpkDbChangeLogAuditBuilder()
    init(builder)
    return builder.build()
}

class CpkDbChangeLogAuditBuilder(
    private var cpiNameSupplier: () -> String? = { null },
    private var cpiVersionSupplier: () -> String? = { null },
    private var cpiSshSupplier: () -> String? = { null },
    private var fileChecksumSupplier: () -> String? = { null },
    private val randomUUID: UUID = UUID.randomUUID()
) {

    private var filePath: String? = null
    private var changesetId: UUID? = null
    private var entityVersion: Int? = null
    private var isDeleted: Boolean? = null

    fun cpiName(value: String): CpkDbChangeLogAuditBuilder {
        cpiNameSupplier = { value }
        return this
    }

    fun cpiVersion(value: String): CpkDbChangeLogAuditBuilder {
        cpiVersionSupplier = { value }
        return this
    }

    fun cpiSsh(value: String): CpkDbChangeLogAuditBuilder {
        cpiSshSupplier = { value }
        return this
    }

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
                cpiNameSupplier.invoke() ?: "cpiName_$randomUUID",
                cpiVersionSupplier.invoke() ?: "cpiVersion_$randomUUID",
                cpiSshSupplier.invoke() ?: "cpiSsh_$randomUUID",
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