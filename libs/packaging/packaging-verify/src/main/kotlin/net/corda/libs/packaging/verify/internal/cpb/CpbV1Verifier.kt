package net.corda.libs.packaging.verify.internal.cpb

import net.corda.libs.packaging.JarReader
import net.corda.libs.packaging.PackagingConstants.CPB_FORMAT_ATTRIBUTE
import net.corda.libs.packaging.PackagingConstants.CPB_NAME_ATTRIBUTE
import net.corda.libs.packaging.PackagingConstants.CPB_VERSION_ATTRIBUTE
import net.corda.libs.packaging.PackagingConstants.CPI_GROUP_POLICY_ENTRY
import net.corda.libs.packaging.PackagingConstants.CPK_FILE_EXTENSION
import net.corda.libs.packaging.core.exception.DependencyResolutionException
import net.corda.libs.packaging.core.exception.PackagingException
import net.corda.libs.packaging.verify.internal.cpi.GroupPolicy
import net.corda.libs.packaging.verify.internal.cpk.AvailableCpk
import net.corda.libs.packaging.verify.internal.cpk.CpkV1Verifier
import net.corda.libs.packaging.verify.internal.cpk.FileHashCalculator
import net.corda.libs.packaging.verify.internal.firstOrThrow
import net.corda.libs.packaging.verify.internal.requireAttribute
import net.corda.libs.packaging.verify.internal.requireAttributeValueIn
import java.util.jar.Manifest

/**
 * Verifies CPB format 1.0
 */
class CpbV1Verifier internal constructor (private val packageType: String, jarReader: JarReader): CpbVerifier {
    private val name = jarReader.jarName
    private val manifest: Manifest = jarReader.manifest
    private val cpkVerifiers: List<CpkV1Verifier>
    private val cpkHashCalculators: List<FileHashCalculator>
    private val groupPolicy: GroupPolicy

    constructor (jarReader: JarReader): this("CPB", jarReader)

    init {
        val (cpkVerifiers, cpkHashCalculators) = jarReader.entries.filter(::isCpk).map {
            Pair(
                CpkV1Verifier(JarReader("$name/${it.name}", it.createInputStream(), jarReader.trustedCerts)),
                FileHashCalculator(it::createInputStream))
        }.unzip()
        this.cpkVerifiers = cpkVerifiers
        this.cpkHashCalculators = cpkHashCalculators
        groupPolicy = jarReader.entries.filter(::isGroupPolicy).map { GroupPolicy() }
            .firstOrThrow(PackagingException("Group policy not found in $packageType \"$name\""))
    }

    private fun isCpk(entry: JarReader.Entry): Boolean {
        return entry.name.let {
            it.indexOf('/') == -1 &&
            it.endsWith(CPK_FILE_EXTENSION, ignoreCase = true)
        }
    }

    private fun isGroupPolicy(entry: JarReader.Entry): Boolean =
        entry.name.endsWith(CPI_GROUP_POLICY_ENTRY, ignoreCase = true)

    private fun verifyManifest() {
        with (manifest) {
            requireAttributeValueIn(CPB_FORMAT_ATTRIBUTE, null, "1.0")
            requireAttribute(CPB_NAME_ATTRIBUTE)
            requireAttribute(CPB_VERSION_ATTRIBUTE)
        }
    }

    private fun verifyCpkDependencies() {
        val availableCpks = cpkVerifiers.zip(cpkHashCalculators).map { (cpk, cpkHashCalculator) ->
            AvailableCpk(cpk.bundleName(), cpk.bundleVersion(), cpkHashCalculator, cpk.codeSigners) }

        // Multiple versions of the same CorDapp not allowed
        val nonUniqueNames = availableCpks.map { it.name }.let { names -> names - names.toSet() }.toSet()
        if (nonUniqueNames.isNotEmpty())
            throw PackagingException("CorDapp(s) $nonUniqueNames appear more than once in $packageType \"$name\"")

        // Check that all dependencies are satisfied
        cpkVerifiers.forEach { cpk ->
            cpk.dependencies.firstOrNull { !it.satisfied(availableCpks) }?.let {
                throw DependencyResolutionException("CPK \"${cpk.name}\" has unsatisfied dependency: $it")
            }
        }
    }

    private fun verifyCpks() {
        if (cpkVerifiers.isEmpty())
            throw PackagingException("None CPK found in $packageType \"$name\"")

        cpkVerifiers.forEach { it.verify() }

        verifyCpkDependencies()
    }

    override fun verify() {
        verifyManifest()
        verifyCpks()
        groupPolicy.verify()
    }
}