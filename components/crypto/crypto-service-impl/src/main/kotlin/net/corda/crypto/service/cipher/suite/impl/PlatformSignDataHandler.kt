package net.corda.crypto.service.cipher.suite.impl

import net.corda.crypto.core.service.PlatformCipherSuiteMetadata
import net.corda.crypto.impl.cipher.suite.SignatureInstances
import net.corda.crypto.service.cipher.suite.SoftKeyMap
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.debug
import net.corda.v5.cipher.suite.handlers.signing.SignDataHandler
import net.corda.v5.cipher.suite.handlers.signing.SigningSpec
import net.corda.v5.cipher.suite.handlers.signing.SigningWrappedSpec
import net.corda.v5.cipher.suite.KeySchemeCapability
import net.corda.v5.cipher.suite.getParamsSafely

class PlatformSignDataHandler(
    private val suiteMetadata: PlatformCipherSuiteMetadata,
    private val keyMap: SoftKeyMap,
) : SignDataHandler {
    companion object {
        private val logger = contextLogger()
    }

    private val signatureInstances = SignatureInstances(suiteMetadata)

    override val rank: Int = 0

    override fun sign(
        spec: SigningSpec,
        data: ByteArray,
        metadata: ByteArray,
        context: Map<String, String>
    ): ByteArray {
        require(spec is SigningWrappedSpec) {
            "The service supports only ${SigningWrappedSpec::class.java}"
        }
        require(data.isNotEmpty()) {
            "Signing of an empty array is not permitted."
        }
        require(suiteMetadata.supportedSigningSchemes.containsKey(spec.keyScheme)) {
            "Unsupported key scheme: ${spec.keyScheme.codeName}"
        }
        require(spec.keyScheme.canDo(KeySchemeCapability.SIGN)) {
            "Key scheme: ${spec.keyScheme.codeName} cannot be used for signing."
        }
        logger.debug { "sign(spec=$spec)" }
        val privateKey = keyMap.unwrapPrivateKey(spec.publicKey, spec.keyMaterialSpec)
        return signatureInstances.withSignature(spec.keyScheme, spec.signatureSpec) { signature ->
            spec.signatureSpec.getParamsSafely()?.let { params -> signature.setParameter(params) }
            signature.initSign(privateKey, suiteMetadata.secureRandom)
            if(metadata.isNotEmpty()) {
                signature.update(metadata)
            }
            signature.update(data)
            signature.sign()
        }
    }
}