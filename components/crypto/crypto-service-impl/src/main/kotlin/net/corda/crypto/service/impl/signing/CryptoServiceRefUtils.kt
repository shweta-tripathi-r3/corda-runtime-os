package net.corda.crypto.service.impl.signing

import net.corda.crypto.persistence.SigningKeySaveContext
import net.corda.crypto.persistence.SigningPublicKeySaveContext
import net.corda.crypto.persistence.SigningWrappedKeySaveContext
import net.corda.crypto.persistence.SigningCachedKey
import net.corda.crypto.service.CryptoServiceRef
import net.corda.v5.cipher.suite.GeneratedKey
import net.corda.v5.cipher.suite.GeneratedPublicKey
import net.corda.v5.cipher.suite.GeneratedWrappedKey
import net.corda.v5.cipher.suite.KeyGenerationSpec
import net.corda.v5.cipher.suite.SigningAliasSpec
import net.corda.v5.cipher.suite.SigningWrappedSpec
import net.corda.v5.cipher.suite.schemes.KeyScheme
import net.corda.v5.cipher.suite.signing.EnhancedSigningData
import net.corda.v5.cipher.suite.signing.encode
import net.corda.v5.crypto.SignatureScheme
import net.corda.v5.crypto.exceptions.CryptoServiceException
import java.time.Instant

fun CryptoServiceRef.getSupportedSchemes(): List<String> =
    instance.supportedSchemes().map { it.codeName }

fun CryptoServiceRef.createWrappingKey(failIfExists: Boolean) {
    require(!masterKeyAlias.isNullOrBlank()) {
        "Wrapping key is not specified."
    }
    instance.createWrappingKey(masterKeyAlias!!, failIfExists, emptyMap())
}

fun CryptoServiceRef.generateKeyPair(
    alias: String?, context: Map<String, String>
): GeneratedKey =
    instance.generateKeyPair(
        KeyGenerationSpec(
            tenantId = tenantId,
            keyScheme = keyScheme,
            alias = alias,
            masterKeyAlias = masterKeyAlias,
            secret = aliasSecret
        ),
        context
    )

fun CryptoServiceRef.toSaveKeyContext(
    key: GeneratedKey,
    alias: String?,
    externalId: String?
): SigningKeySaveContext =
    when(key) {
        is GeneratedPublicKey -> SigningPublicKeySaveContext(
            key = key,
            alias = alias,
            keyScheme = keyScheme,
            category = category,
            externalId = externalId,
        )
        is GeneratedWrappedKey -> SigningWrappedKeySaveContext(
            key = key,
            masterKeyAlias = masterKeyAlias,
            externalId = externalId,
            alias = alias,
            keyScheme = keyScheme,
            category = category
        )
        else -> throw CryptoServiceException("Unknown key generation response: ${key::class.java.name}")
    }

fun CryptoServiceRef.sign(
    record: SigningCachedKey,
    keyScheme: KeyScheme,
    signatureScheme: SignatureScheme,
    data: ByteArray,
    context: Map<String, String>
): ByteArray {
    val spec = if(record.keyMaterial != null) {
        require(record.keyMaterial!!.isNotEmpty()) {
            "The key material is empty."
        }
        require(record.encodingVersion != null) {
            "The encoding version is missing."
        }
        SigningWrappedSpec(
            tenantId = tenantId,
            keyMaterial = record.keyMaterial!!,
            masterKeyAlias = record.masterKeyAlias,
            encodingVersion = record.encodingVersion!!,
            keyScheme = keyScheme,
            signatureScheme = signatureScheme
        )
    } else {
        require(!record.hsmAlias.isNullOrBlank()) {
            "The hsm assigned alias is missing."
        }
        SigningAliasSpec(
            tenantId = tenantId,
            hsmAlias = record.hsmAlias!!,
            keyScheme = keyScheme,
            signatureScheme = signatureScheme
        )
    }
    val signingData = EnhancedSigningData(
        timestamp = Instant.now(),
        signatureCodeName = signatureScheme.codeName,
        bytes = data
    )
    val signature = instance.sign(spec, signingData.encoded, context)
    return signingData.encode(signature)
}
