package net.corda.libs.packaging.verify.internal.cpk

import net.corda.libs.packaging.core.exception.DependencyMetadataException
import net.corda.libs.packaging.verify.TestUtils.CODE_SIGNER_ALICE
import net.corda.libs.packaging.verify.internal.codeSignersHash
import net.corda.libs.packaging.verify.internal.sortedSequenceHash
import net.corda.v5.crypto.SecureHash
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Base64

class CpkV1DependencyReaderTest {
    private fun hash(base64Hash: String, algorithm: String) =
        SecureHash(algorithm, Base64.getDecoder().decode(base64Hash))

    @Test
    fun `parses dependencies correctly`() {
        val dependenciesDocument = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cpkDependencies xmlns="urn:corda-cpk">
                <cpkDependency>
                    <name>net.acme.contract</name>
                    <version>1.0.0</version>
                    <signers>
                        <signer algorithm="SHA-256">qlnYKfLKj931q+pA2BX5N+PlTlcrZbk7XCFq5llOfWs=</signer>
                    </signers>
                </cpkDependency>
                <cpkDependency>
                    <name>com.example.helloworld.hello-world-cpk-one</name>
                    <version>2.0.0</version>
                    <signers>
                        <sameAsMe/>
                    </signers>
                </cpkDependency>
            </cpkDependencies>
        """.trimIndent().byteInputStream()

        val codeSigners = listOf(CODE_SIGNER_ALICE)
        val dependencies = CpkV1DependenciesReader.readDependencies(
            "testCpk.cpk", dependenciesDocument, codeSigners)

        val expectedDependencies = listOf(
            CpkSignersHashDependency(
                "net.acme.contract",
                "1.0.0",
                sequenceOf(hash("qlnYKfLKj931q+pA2BX5N+PlTlcrZbk7XCFq5llOfWs=", "SHA-256"))
                .sortedSequenceHash()),

            CpkSignersHashDependency(
            "com.example.helloworld.hello-world-cpk-one",
            "2.0.0",
                codeSigners.codeSignersHash())
        )

        assertEquals(expectedDependencies, dependencies)
    }

    @Test
    fun `parses empty dependencies correctly`() {
        val dependenciesDocument = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cpkDependencies xmlns="urn:corda-cpk">
            </cpkDependencies>
        """.trimIndent().byteInputStream()

        val codeSigners = listOf(CODE_SIGNER_ALICE)
        val dependencies = CpkV1DependenciesReader.readDependencies("testCpk.cpk", dependenciesDocument, codeSigners)

        assertEquals(emptyList<CpkDependency>(), dependencies)
    }

    @Test
    fun `throws if dependency document doesn't conform to schema (missing name)`() {
        // version is missing
        val dependenciesDocument = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cpkDependencies xmlns="urn:corda-cpk">
                <cpkDependency>
                    <version>1.0.0</version>
                    <signers>
                        <signer algorithm="SHA-256">qlnYKfLKj931q+pA2BX5N+PlTlcrZbk7XCFq5llOfWs=</signer>
                    </signers>
                </cpkDependency>
            </cpkDependencies>
        """.trimIndent().byteInputStream()

        val codeSigners = listOf(CODE_SIGNER_ALICE)
        val exception = assertThrows<DependencyMetadataException> {
            CpkV1DependenciesReader.readDependencies("testCpk.cpk", dependenciesDocument, codeSigners)
        }
        assertNotNull(exception.cause)
        assertNotNull(exception.cause!!.message)
        assertTrue(exception.cause!!.message!!.contains("The content of element 'cpkDependency' is not complete."))
    }

    @Test
    fun `throws if dependency document doesn't conform to schema (missing version)`() {
        // version is missing
        val dependenciesDocument = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cpkDependencies xmlns="urn:corda-cpk">
                <cpkDependency>
                    <name>net.acme.contract</name>
                    <signers>
                        <signer algorithm="SHA-256">qlnYKfLKj931q+pA2BX5N+PlTlcrZbk7XCFq5llOfWs=</signer>
                    </signers>
                </cpkDependency>
            </cpkDependencies>
        """.trimIndent().byteInputStream()

        val codeSigners = listOf(CODE_SIGNER_ALICE)
        val exception = assertThrows<DependencyMetadataException> {
            CpkV1DependenciesReader.readDependencies("testCpk.cpk", dependenciesDocument, codeSigners)
        }
        assertNotNull(exception.cause)
        assertNotNull(exception.cause!!.message)
        assertTrue(exception.cause!!.message!!.contains("The content of element 'cpkDependency' is not complete."))
    }
}