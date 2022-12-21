package net.corda.test.util.virtualnode.cpx.dsl

import java.lang.Exception
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
    internal var cpiName: String? = null
    internal var cpiVersion: String? = null
    internal var cpiSsh: String? = null

    // cpk
    internal var name: String? = null
    internal var version: String? = null
    internal var signerSummaryHash: String? = null

    // cpicpk
    internal var fileName: String? = null
    internal var metadata: CpkMetadataBuilder? = null
    internal var cpkFileChecksum: String? = null

    //metadata
    internal var formatVersion: String? = null
    internal var serializedMetadata: String? = null

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

    fun cpkFileChecksum(value: String): CpiCpkBuilder {
        cpkFileChecksum = value
        return this
    }

    fun serializedMetadata(value: String): CpiCpkBuilder {
        serializedMetadata = value
        return this
    }

    fun supplyCpkFileChecksum() = cpkFileChecksum

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
                cpiNameSupplier.invoke() ?: throw Exception("CpiCpkBuilder.cpiNameSupplier is mandatory"),
                cpiVersionSupplier.invoke() ?: throw Exception("CpiCpkBuilder.cpiVersionSupplier is mandatory"),
                cpiSshSupplier.invoke() ?: throw Exception("CpiCpkBuilder.cpiSshSupplier is mandatory"),
                supplyCpkFileChecksum()!!
            ),
            fileName ?: "cpk_filename_$randomId",
            cpk
        )
    }
}