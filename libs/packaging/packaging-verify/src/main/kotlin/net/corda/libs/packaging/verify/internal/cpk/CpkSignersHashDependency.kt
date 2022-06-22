package net.corda.libs.packaging.verify.internal.cpk

import net.corda.libs.packaging.verify.internal.codeSignersHash
import net.corda.v5.crypto.SecureHash

/** CPK dependency matched by hash of code signers */
internal data class CpkSignersHashDependency (
    override val name: String,
    override val version: String,
    val codeSignersHash: SecureHash
): CpkDependency {
    override fun satisfied(cpks: List<AvailableCpk>): Boolean {
        return cpks.any {
            it.name == name &&
            it.version == version &&
            it.codeSigners.codeSignersHash() == codeSignersHash
        }
    }
}