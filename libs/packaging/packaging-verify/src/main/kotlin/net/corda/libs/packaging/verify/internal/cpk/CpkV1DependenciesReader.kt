package net.corda.libs.packaging.verify.internal.cpk

import net.corda.libs.packaging.core.CpkType
import net.corda.libs.packaging.core.exception.DependencyMetadataException
import net.corda.libs.packaging.verify.internal.certSequenceHash
import net.corda.libs.packaging.verify.internal.sortedSequenceHash
import net.corda.v5.crypto.SecureHash
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXParseException
import java.io.InputStream
import java.lang.invoke.MethodHandles
import java.security.CodeSigner
import java.util.Base64
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.validation.SchemaFactory

/**
 * CPK format version 1 CPKDependencies XML file reader
 */
internal object CpkV1DependenciesReader {
    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    private const val CORDA_CPK_V1 = "corda-cpk-1.0.xsd"

    // The tags used in the XML of the file specifying a CPK's dependencies.
    private const val DEPENDENCY_TAG = "cpkDependency"
    private const val DEPENDENCY_NAME_TAG = "name"
    private const val DEPENDENCY_VERSION_TAG = "version"
    private const val DEPENDENCY_SIGNERS_TAG = "signers"
    private const val DEPENDENCY_TYPE_TAG = "type"
    private const val DEPENDENCY_SIGNER_TAG = "signer"
    private const val DEPENDENCY_SAME_SIGNER_TAG = "sameAsMe"

    /**
     * Reads CPK dependencies from [InputStream]
     * @param cpkName CPK's name used in re4porting errors
     * @param inputStream CPKDependencies [InputStream]
     * @param codeSigners CPK's code signers that will be used for "SameSignerAsMe" dependencies
     */
    fun readDependencies(
        cpkName: String,
        inputStream: InputStream,
        codeSigners: List<CodeSigner>
    ): List<CpkDependency> {

        val cpkSignersHash = codeSigners
            .mapTo(HashSet()) { it.signerCertPath.certificates.first() }
            .asSequence().certSequenceHash()

        try {
            // Validate against JSON Schema
            val cpkDependencyDocument: Document = run {
                // The CPK dependencies are specified as an XML file.
                val documentBuilder = cpkV1DocumentBuilderFactory.newDocumentBuilder()
                documentBuilder.setErrorHandler(XmlErrorHandler(cpkName))
                documentBuilder.parse(inputStream)
            }

            return ElementIterator(cpkDependencyDocument.getElementsByTagName(DEPENDENCY_TAG)).asSequence()
                .filter {
                    val dependencyType = it.getElementsByTagName(DEPENDENCY_TYPE_TAG)
                    dependencyType.length == 0 || CpkType.parse(dependencyType.item(0).textContent) != CpkType.CORDA_API
                }.map { el ->
                    val signers = el.getElementsByTagName(DEPENDENCY_SIGNERS_TAG).item(0) as Element
                    val signersHash: SecureHash =
                        if (signers.getElementsByTagName(DEPENDENCY_SAME_SIGNER_TAG).length == 1) {
                            cpkSignersHash
                        } else {
                            ElementIterator(signers.getElementsByTagName(DEPENDENCY_SIGNER_TAG))
                                .asSequence().map { signer ->
                                    val algorithm = signer.getAttribute("algorithm").trim()
                                    val hashData = Base64.getDecoder().decode(signer.textContent)
                                    SecureHash(algorithm, hashData)
                                }.sortedSequenceHash()
                        }
                    CpkSignersHashDependency(
                        el.textContent(DEPENDENCY_NAME_TAG),
                        el.textContent(DEPENDENCY_VERSION_TAG),
                        signersHash)
                }.toList()
        } catch (e: Exception) {
            throw DependencyMetadataException("Error reading CPK dependencies in CPK \"$cpkName\"", e)
        }
    }

    private fun Element.textContent(tagName: String): String =
        getElementsByTagName(tagName).item(0).textContent

    /**
     * [Iterator] for traversing every [Element] within a [NodeList].
     * Skips any [org.w3c.dom.Node] that is not also an [Element].
     */
    private class ElementIterator(private val nodes: NodeList) : Iterator<Element> {
        private var index = 0
        override fun hasNext() = index < nodes.length
        override fun next() = if (hasNext()) nodes.item(index++) as Element else throw NoSuchElementException()
    }

    private class XmlErrorHandler(private val cpkName: String) : ErrorHandler {
        override fun warning(ex: SAXParseException) {
            logger.warn(
                "Problem at (line {}, column {}) parsing CPK dependencies for {}: {}",
                ex.lineNumber, ex.columnNumber, cpkName, ex.message
            )
        }

        override fun error(ex: SAXParseException) {
            logger.error(
                "Error at (line {}, column {}) parsing CPK dependencies for {}: {}",
                ex.lineNumber, ex.columnNumber, cpkName, ex.message
            )
            throw DependencyMetadataException(ex.message ?: "", ex)
        }

        override fun fatalError(ex: SAXParseException) {
            logger.error(
                "Fatal error at (line {}, column {}) parsing CPK dependencies for {}: {}",
                ex.lineNumber, ex.columnNumber, cpkName, ex.message
            )
            throw ex
        }
    }

    private val cpkV1DocumentBuilderFactory = DocumentBuilderFactory.newInstance().also { dbf ->
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        dbf.disableProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA)
        dbf.disableProperty(XMLConstants.ACCESS_EXTERNAL_DTD)
        dbf.isExpandEntityReferences = false
        dbf.isIgnoringComments = true
        dbf.isNamespaceAware = true

        dbf.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).also { sf ->
            sf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            sf.disableProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA)
            sf.disableProperty(XMLConstants.ACCESS_EXTERNAL_DTD)
        }.newSchema(
            this::class.java.classLoader.getResource(CORDA_CPK_V1)
                ?: throw IllegalStateException("Corda CPK v1 schema missing")
        )
    }

    private fun DocumentBuilderFactory.disableProperty(propertyName: String) {
        try {
            setAttribute(propertyName, "")
        } catch (_: IllegalArgumentException) {
            // Property not supported.
        }
    }

    private fun SchemaFactory.disableProperty(propertyName: String) {
        try {
            setProperty(propertyName, "")
        } catch (_: SAXNotRecognizedException) {
            // Property not supported.
        }
    }
}