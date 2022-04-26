package net.corda.crypto.impl.components

import net.corda.crypto.impl.SignatureInstances
import net.corda.v5.cipher.suite.CipherSchemeMetadata
import net.corda.v5.crypto.DigestService
import net.corda.v5.crypto.SignatureVerificationService
import net.corda.v5.crypto.signing.EnhancedSignedData
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.security.PublicKey
import java.security.SignatureException
import javax.crypto.Cipher

@Component(service = [SignatureVerificationService::class])
class SignatureVerificationServiceImpl @Activate constructor(
    @Reference(service = CipherSchemeMetadata::class)
    private val schemeMetadata: CipherSchemeMetadata,
    @Reference(service = DigestService::class)
    private val hashingService: DigestService
) : SignatureVerificationService {

    private val signatureInstances = SignatureInstances(schemeMetadata.providers)

    override fun verify(publicKey: PublicKey, signedData: EnhancedSignedData) {
        val signatureScheme = schemeMetadata.signatureSchemes.firstOrNull {
            it.codeName == signedData.signature.signatureCodeName
        }
        val keyScheme = schemeMetadata.findKeyScheme(publicKey)
        require(signatureScheme != null) {
            "Unsupported key/algorithm for codeName: ${signedData.signature.signatureCodeName}"
        }
        require(signedData.signature.signature.isNotEmpty()) {
            "Signature data is empty!"
        }
        require(signedData.bytes.isNotEmpty()) {
            "Clear data is empty, nothing to verify!"
        }
        val signingData = signatureScheme.getSigningData(hashingService, signedData.bytes)
        val result = if (signatureScheme.precalculateHash && publicKey.algorithm.equals("RSA", true)) {
            val cipher = Cipher.getInstance(
                signatureScheme.signatureName,
                schemeMetadata.providers.getValue(keyScheme.providerName)
            )
            cipher.init(Cipher.DECRYPT_MODE, publicKey)
            cipher.doFinal(signedData.signature.signature).contentEquals(signingData)
        } else {
            signatureInstances.withSignature(signatureScheme) {
                if(signatureScheme.params != null) {
                    it.setParameter(signatureScheme.params)
                }
                it.initVerify(publicKey)
                it.update(signingData)
                it.verify(signedData.bytes)
            }
        }
        if (!result) {
            throw SignatureException("Signature Verification failed!")
        }
    }
}