package net.corda.sandbox.internal

import net.corda.crypto.testkit.SecureHashUtils.randomSecureHash
import net.corda.libs.packaging.core.CpkIdentifier
import net.corda.libs.packaging.core.CpkMetadata
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.osgi.framework.Bundle
import org.osgi.framework.Constants.SYSTEM_BUNDLE_ID
import org.osgi.framework.Version
import kotlin.math.abs
import kotlin.random.Random
import kotlin.random.Random.Default.nextLong

const val HASH_ALGORITHM = "SHA-256"
const val HASH_LENGTH = 32
const val PUBLIC_BUNDLE_NAME = "public_bundle_symbolic_name"
const val CPK_LIBRARY_BUNDLE_NAME = "cpk_library_bundle_symbolic_name"
const val CPK_MAIN_BUNDLE_NAME = "cpk_main_bundle_symbolic_name"
const val CORDA_CPK_CORDAPP_NAME = "cpk_cordapp_name"

val random = Random(0)

/** Generates a mock [Bundle] with [bundleSymbolicName] and [bundleLocation] that contains the given [klass]. */
fun mockBundle(
    bundleSymbolicName: String? = random.nextInt().toString(),
    klass: Class<*>? = null,
    bundleLocation: String = random.nextInt().toString()
) = mock<Bundle>().apply {
    val bundleVersion = Version.parseVersion("${abs(random.nextInt())}.${abs(random.nextInt())}")
    val id = if ("org.apache.felix.framework" == bundleSymbolicName) {
        SYSTEM_BUNDLE_ID
    } else {
        nextLong()
    }
    val description = "Bundle[BSN=$bundleSymbolicName, ID=$id]"
    whenever(bundleId).thenReturn(id)
    whenever(symbolicName).thenReturn(bundleSymbolicName)
    whenever(version).thenReturn(bundleVersion)
    whenever(loadClass(any())).then { answer ->
        val requestedClass = answer.arguments.single()
        if (klass?.name == requestedClass) klass else throw ClassNotFoundException()
    }
    whenever(location).thenReturn(bundleLocation)
    whenever(toString()).thenReturn(description)
}

/** Generates a mock CpkMetadata. */
fun mockCpkMeta(): CpkMetadata {
    val id = CpkIdentifier(CORDA_CPK_CORDAPP_NAME, "1.0", randomSecureHash())
    val hash = randomSecureHash()
    return mock<CpkMetadata>().apply {
        whenever(this.cpkId).thenReturn(id)
        whenever(this.fileChecksum).thenReturn(hash)
    }
}
