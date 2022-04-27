package net.corda.crypto.impl.components

import net.corda.crypto.impl.SignatureInstances
import net.corda.v5.cipher.suite.CipherSchemeMetadata
import net.corda.v5.crypto.DigestService
import net.corda.v5.crypto.SignatureVerificationService
import net.corda.v5.crypto.signing.EnhancedSigningData
import net.corda.v5.crypto.signing.decodeDigitalSignature
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

    override fun verify(publicKey: PublicKey, signature: ByteArray, clearData: ByteArray) {
        val sig = signature.decodeDigitalSignature()
        val signatureScheme = schemeMetadata.signatureSchemes.firstOrNull {
            it.codeName == sig.signatureCodeName
        }
        val keyScheme = schemeMetadata.findKeyScheme(publicKey)
        require(signatureScheme != null) {
            "Unsupported key/algorithm for codeName: ${sig.signatureCodeName}"
        }
        require(sig.signature.isNotEmpty()) {
            "Signature data is empty!"
        }
        require(clearData.isNotEmpty()) {
            "Clear data is empty, nothing to verify!"
        }
        val signingData = EnhancedSigningData(
            timestamp = sig.timestamp,
            signatureCodeName = sig.signatureCodeName,
            bytes = clearData
        )
        val signingBytes = signatureScheme.getSigningData(hashingService, signingData.encoded)
        val result = if (signatureScheme.precalculateHash && publicKey.algorithm.equals("RSA", true)) {
            val cipher = Cipher.getInstance(
                signatureScheme.signatureName,
                schemeMetadata.providers.getValue(keyScheme.providerName)
            )
            cipher.init(Cipher.DECRYPT_MODE, publicKey)
            cipher.doFinal(sig.signature).contentEquals(signingBytes)
        } else {
            signatureInstances.withSignature(signatureScheme) {
                if(signatureScheme.params != null) {
                    it.setParameter(signatureScheme.params)
                }
                it.initVerify(publicKey)
                it.update(signingBytes)
                it.verify(sig.signature)
            }
        }
        if (!result) {
            throw SignatureException("Signature Verification failed!")
        }
    }
}