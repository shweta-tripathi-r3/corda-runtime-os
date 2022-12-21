package net.corda.test.util.virtualnode.cpx.dsl

import java.util.UUID
import net.corda.libs.cpi.datamodel.CpiCpkEntity
import net.corda.libs.cpi.datamodel.CpiCpkKey
import net.corda.libs.cpi.datamodel.CpkMetadataEntity

class CpiCpkBuilder(
    private var cpiNameSupplier: () -> String?,
    private var cpiVersionSupplier: () -> String?,
    private var cpiSshSupplier: () -> String?,
    private val randomId: UUID = UUID.randomUUID()
) {

    constructor(
        cpk: CpkMetadataBuilder,
        cpiNameSupplier: () -> String?,
        cpiVersionSupplier: () -> String?,
        cpiSshSupplier: () -> String?,
    ) : this(cpiNameSupplier, cpiVersionSupplier, cpiSshSupplier) {
        name = cpk.cpkName
        version = cpk.cpkVersion
        signerSummaryHash = cpk.cpkSignerSummaryHash
        formatVersion = cpk.formatVersion
        serializedMetadata = cpk.serializedMetadata
        metadata = cpk
        cpiName = cpiNameSupplier.invoke()
        cpiVersion = cpiVersionSupplier.invoke()
        cpiSsh = cpiSshSupplier.invoke()
        cpkFileChecksum = cpk.fileChecksumSupplier.invoke()
    }

    // cpi
    private var cpiName: String? = null
    private var cpiVersion: String? = null
    private var cpiSsh: String? = null

    // cpk
    private var name: String? = null
    private var version: String? = null
    private var signerSummaryHash: String? = null

    // cpicpk
    private var fileName: String? = null
    private var metadata: CpkMetadataBuilder? = null
    private var cpkFileChecksum: String? = null

    //metadata
    private var formatVersion: String? = null
    private var serializedMetadata: String? = null

    fun name(value: String): CpiCpkBuilder {
        name = value
        return this
    }

    fun version(value: String): CpiCpkBuilder {
        version = value
        return this
    }

    fun signerSummaryHash(value: String): CpiCpkBuilder {
        signerSummaryHash = value
        return this
    }

    fun fileName(value: String): CpiCpkBuilder {
        fileName = value
        return this
    }

    fun metadata(init: CpkMetadataBuilder.() -> Unit): CpiCpkBuilder {
        val cpkMetadata = CpkMetadataBuilder(::supplyCpkFileChecksum, randomId)
        init(cpkMetadata)
        metadata = cpkMetadata
        return this
    }

    fun metadata(value: CpkMetadataBuilder): CpiCpkBuilder {
        metadata = value
        return this
    }

    fun fileChecksum(value: String): CpiCpkBuilder {
        cpkFileChecksum = value
        return this
    }

    fun serializedMetadata(value: String): CpiCpkBuilder {
        serializedMetadata = value
        return this
    }

    fun supplyCpkFileChecksum() = cpkFileChecksum

    @Suppress("ThrowsCount")
    fun build(): CpiCpkEntity {
        if (cpkFileChecksum == null) cpkFileChecksum = "cpk_file_checksum_$randomId"
        val cpk: CpkMetadataEntity = metadata?.build() ?: CpkMetadataBuilder(::supplyCpkFileChecksum, randomId)
            .cpkName(name)
            .cpkVersion(version)
            .cpkSignerSummaryHash(signerSummaryHash)
            .formatVersion(formatVersion)
            .serializedMetadata(serializedMetadata)
            .build()

        return CpiCpkEntity(
            CpiCpkKey(
                cpiNameSupplier.invoke() ?: throw DslException("CpiCpkBuilder.cpiNameSupplier is mandatory"),
                cpiVersionSupplier.invoke() ?: throw DslException("CpiCpkBuilder.cpiVersionSupplier is mandatory"),
                cpiSshSupplier.invoke() ?: throw DslException("CpiCpkBuilder.cpiSshSupplier is mandatory"),
                supplyCpkFileChecksum()!!
            ),
            fileName ?: "cpk_filename_$randomId",
            cpk
        )
    }
}