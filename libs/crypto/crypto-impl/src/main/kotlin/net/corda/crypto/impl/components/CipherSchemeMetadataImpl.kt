package net.corda.crypto.impl.components

import net.corda.crypto.impl.schememetadata.ProviderMap
import net.corda.v5.cipher.suite.AlgorithmParameterSpecEncodingService
import net.corda.v5.cipher.suite.CipherSchemeMetadata
import net.corda.v5.cipher.suite.KeyEncodingService
import net.corda.v5.cipher.suite.schemes.AlgorithmParameterSpecSerializer
import net.corda.v5.cipher.suite.schemes.DigestScheme
import net.corda.v5.cipher.suite.schemes.ECDSA_SECP256K1_CODE_NAME
import net.corda.v5.cipher.suite.schemes.ECDSA_SECP256R1_CODE_NAME
import net.corda.v5.cipher.suite.schemes.EDDSA_ED25519_CODE_NAME
import net.corda.v5.cipher.suite.schemes.GOST3410_CODE_NAME
import net.corda.v5.cipher.suite.schemes.KeyScheme
import net.corda.v5.cipher.suite.schemes.RSA_CODE_NAME
import net.corda.v5.cipher.suite.schemes.SM2_CODE_NAME
import net.corda.v5.cipher.suite.schemes.SPHINCS256_CODE_NAME
import net.corda.v5.cipher.suite.schemes.SerializedAlgorithmParameterSpec
import net.corda.v5.crypto.CompositeKey
import net.corda.v5.crypto.ECDSA_SHA256
import net.corda.v5.crypto.ECDSA_SHA512
import net.corda.v5.crypto.EDDSA_ED25519_CUSTOM_SHA256
import net.corda.v5.crypto.EDDSA_ED25519_CUSTOM_SHA512
import net.corda.v5.crypto.EDDSA_ED25519_NONE
import net.corda.v5.crypto.GOST3410_GOST3411
import net.corda.v5.crypto.RSASSA_PSS_SHA256
import net.corda.v5.crypto.RSASSA_PSS_SHA512
import net.corda.v5.crypto.RSA_SHA256
import net.corda.v5.crypto.RSA_SHA512
import net.corda.v5.crypto.SM2_SM3
import net.corda.v5.crypto.SPHINCS256_SHA512
import net.corda.v5.crypto.SignatureScheme
import net.corda.v5.crypto.exceptions.CryptoServiceLibraryException
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.util.io.pem.PemReader
import org.osgi.service.component.annotations.Component
import java.io.StringReader
import java.io.StringWriter
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.PSSParameterSpec
import java.security.spec.X509EncodedKeySpec

@Component(
    service = [
        CipherSchemeMetadata::class,
        KeyEncodingService::class,
        AlgorithmParameterSpecEncodingService::class
    ]
)
@Suppress("TooManyFunctions")
class CipherSchemeMetadataImpl : CipherSchemeMetadata {
    companion object {
        val MESSAGE_DIGEST_TYPE: String = MessageDigest::class.java.simpleName

        private val DIGEST_CANDIDATES = listOf(
            "BLAKE2B-256",
            "BLAKE2B-384",
            "BLAKE2B-512",
            "BLAKE2S-256",
            "DSTU7564-256",
            "DSTU7564-384",
            "DSTU7564-512",
            "GOST3411",
            "GOST3411-2012-256",
            "GOST3411-2012-512",
            "KECCAK-256",
            "KECCAK-288",
            "KECCAK-384",
            "KECCAK-512",
            "RIPEMD256",
            "RIPEMD320",
            "SHA-256",
            "SHA-384",
            "SHA-512",
            "SHA-512/256",
            "SHA3-256",
            "SHA3-384",
            "SHA3-512",
            "SHAKE256-512",
            "SM3",
            "Skein-1024-1024",
            "Skein-1024-384",
            "Skein-1024-512",
            "Skein-256-256",
            "Skein-512-128",
            "Skein-512-160",
            "Skein-512-256",
            "Skein-512-384",
            "Skein-512-512",
            "TIGER",
            "WHIRLPOOL"
        )

        private val allowedDigestAlgorithmsForSignatureSchemeCreation = mapOf(
            "NONE" to "NONE",
            "SHA-256" to "SHA256",
            "SHA256" to "SHA256",
            "SHA-512" to "SHA512",
            "SHA512" to "SHA512",
            "SM3" to "SM3",
            "GOST3411" to "GOST3411"
        )
    }

    private val paramSpecSerializers = mapOf<String, AlgorithmParameterSpecSerializer<out AlgorithmParameterSpec>>(
        PSSParameterSpec::class.java.name to PSSParameterSpecSerializer()
    )

    override fun findKeyScheme(algorithm: AlgorithmIdentifier): KeyScheme =
        algorithmMap[normaliseAlgorithmIdentifier(algorithm)]
            ?: throw IllegalArgumentException("Unrecognised algorithm: ${algorithm.algorithm.id}")

    @Suppress("UNCHECKED_CAST")
    override fun serialize(params: AlgorithmParameterSpec): SerializedAlgorithmParameterSpec {
        val clazz = params::class.java.name
        val serializer = paramSpecSerializers[clazz] as? AlgorithmParameterSpecSerializer<AlgorithmParameterSpec>
            ?: throw IllegalArgumentException("$clazz is not supported.")
        return SerializedAlgorithmParameterSpec(
            clazz = clazz,
            bytes = serializer.serialize(params)
        )
    }

    override fun findKeyScheme(key: PublicKey): KeyScheme {
        val keyInfo = SubjectPublicKeyInfo.getInstance(key.encoded)
        return findKeyScheme(keyInfo.algorithm)
    }

    private val providerMap by lazy(LazyThreadSafetyMode.PUBLICATION) { ProviderMap(this) }

    override val keySchemes: List<KeyScheme> = listOf(
        providerMap.RSA_SHA256,
        providerMap.ECDSA_SECP256K1_SHA256,
        providerMap.ECDSA_SECP256R1_SHA256,
        providerMap.EDDSA_ED25519_NONE,
        providerMap.SPHINCS256_SHA512,
        providerMap.SM2_SM3,
        providerMap.GOST3410_GOST3411,
        providerMap.COMPOSITE_KEY
    )

    private val algorithmMap: Map<AlgorithmIdentifier, KeyScheme> = keySchemes.flatMap { scheme ->
        scheme.algorithmOIDs.map { identifier -> identifier to scheme }
    }.toMap()

    override val digestSchemes: List<DigestScheme> = providerMap.providers.values
        .flatMap { it.services }
        .filter {
            it.type.equals(MESSAGE_DIGEST_TYPE, true)
                    && !CipherSchemeMetadata.BANNED_DIGESTS.contains(it.algorithm)
                    && DIGEST_CANDIDATES.contains(it.algorithm)
        }
        .map { DigestScheme(algorithmName = it.algorithm, providerName = it.provider.name) }
        .distinctBy { it.algorithmName }

    override val providers: Map<String, Provider> get() = providerMap.providers

    override val secureRandom: SecureRandom get() = providerMap.secureRandom

    override val signatureSchemes: List<SignatureScheme> = listOf(
        RSA_SHA256,
        RSA_SHA512,
        RSASSA_PSS_SHA256,
        RSASSA_PSS_SHA512,
        ECDSA_SHA256,
        ECDSA_SHA512,
        EDDSA_ED25519_NONE,
        EDDSA_ED25519_CUSTOM_SHA256,
        EDDSA_ED25519_CUSTOM_SHA512,
        SPHINCS256_SHA512,
        SM2_SM3,
        GOST3410_GOST3411
    )

    override fun createSignatureScheme(digestAlgorithm: String, key: PublicKey): SignatureScheme {
        require(allowedDigestAlgorithmsForSignatureSchemeCreation.containsKey(digestAlgorithm)) {
            "The $digestAlgorithm is not supported to create ${SignatureScheme::class.simpleName}"
        }
        val digest = allowedDigestAlgorithmsForSignatureSchemeCreation[digestAlgorithm]
        val keyScheme = findKeyScheme(key)
        if(digestAlgorithm.equals("NONE", ignoreCase = true)) {
            require(keyScheme.codeName == EDDSA_ED25519_CODE_NAME) {
                "The digest NONE is supported only for EdDSA keys"
            }
        }
        val signatureScheme = when(keyScheme.codeName) {
            RSA_CODE_NAME -> signatureSchemes.firstOrNull {
                it.signatureName.equals("${digest}withRSA", true)
            }
            EDDSA_ED25519_CODE_NAME -> {
                if(digestAlgorithm.equals("NONE", true)) {
                    signatureSchemes.firstOrNull {
                        it.codeName == EDDSA_ED25519_NONE.codeName
                    }
                } else {
                    signatureSchemes.firstOrNull {
                        it.codeName == EDDSA_ED25519_NONE.codeName &&
                                ((digest == "SHA256" && it.customDigestName?.name == "SHA-256")
                                        || (digest == "SHA512" && it.customDigestName?.name == "SHA-512"))
                    }
                }
            }
            ECDSA_SECP256K1_CODE_NAME -> signatureSchemes.firstOrNull {
                it.signatureName.equals("${digest}withECDSA", true)
            }
            ECDSA_SECP256R1_CODE_NAME -> signatureSchemes.firstOrNull {
                it.signatureName.equals("${digest}withECDSA", true)
            }
            SM2_CODE_NAME -> signatureSchemes.firstOrNull {
                it.signatureName.equals("${digest}withSM2", true)
            }
            GOST3410_CODE_NAME -> signatureSchemes.firstOrNull {
                it.signatureName.equals("${digest}withSM2", true)
            }
            SPHINCS256_CODE_NAME -> signatureSchemes.firstOrNull {
                it.signatureName.equals("${digest}withSM2", true)
            }
            else -> throw IllegalArgumentException("The ${keyScheme.codeName} is not supported")
        }
        require(signatureScheme != null) {
            "The signature scheme $digest is not supported for the ${keyScheme.codeName}"
        }
        return signatureScheme
    }

    override fun findKeyScheme(codeName: String): KeyScheme =
        keySchemes.firstOrNull { it.codeName == codeName }
            ?: throw IllegalArgumentException("Unrecognised scheme code name: $codeName")

    override fun findKeyFactory(scheme: KeyScheme): KeyFactory = providerMap.keyFactories[scheme]

    override fun decodePublicKey(encodedKey: ByteArray): PublicKey = try {
        val subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(encodedKey)
        val scheme = findKeyScheme(subjectPublicKeyInfo.algorithm)
        val keyFactory = providerMap.keyFactories[scheme]
        keyFactory.generatePublic(X509EncodedKeySpec(encodedKey))
    } catch (e: CryptoServiceLibraryException) {
        throw e
    } catch (e: Throwable) {
        throw CryptoServiceLibraryException("Failed to decode public key", e)
    }

    override fun decodePublicKey(encodedKey: String): PublicKey = try {
        val pemContent = parsePemContent(encodedKey)
        val publicKeyInfo = SubjectPublicKeyInfo.getInstance(pemContent)
        val converter = getJcaPEMKeyConverter(publicKeyInfo)
        val publicKey = converter.getPublicKey(publicKeyInfo)
        toSupportedPublicKey(publicKey)
    } catch (e: CryptoServiceLibraryException) {
        throw e
    } catch (e: Throwable) {
        throw CryptoServiceLibraryException("Failed to decode public key", e)
    }

    override fun deserialize(params: SerializedAlgorithmParameterSpec): AlgorithmParameterSpec {
        val serializer = paramSpecSerializers[params.clazz]
            ?: throw IllegalArgumentException("${params.clazz} is not supported.")
        return serializer.deserialize(params.bytes)
    }

    override fun encodeAsString(publicKey: PublicKey): String = try {
        objectToPem(publicKey)
    } catch (e: CryptoServiceLibraryException) {
        throw e
    } catch (e: Throwable) {
        throw CryptoServiceLibraryException("Failed to encode public key", e)
    }

    override fun toSupportedPublicKey(key: PublicKey): PublicKey {
        return when {
            key::class.java.`package` == BCECPublicKey::class.java.`package` -> key
            key is CompositeKey -> key
            else -> decodePublicKey(key.encoded)
        }
    }

    private fun getJcaPEMKeyConverter(publicKeyInfo: SubjectPublicKeyInfo): JcaPEMKeyConverter {
        val scheme = findKeyScheme(publicKeyInfo.algorithm)
        val converter = JcaPEMKeyConverter()
        converter.setProvider(providers[scheme.providerName])
        return converter
    }

    private fun objectToPem(obj: Any): String =
        StringWriter().use { strWriter ->
            JcaPEMWriter(strWriter).use { pemWriter ->
                pemWriter.writeObject(obj)
            }
            return strWriter.toString()
        }

    private fun parsePemContent(pem: String): ByteArray =
        StringReader(pem).use { strReader ->
            return PemReader(strReader).use { pemReader ->
                pemReader.readPemObject().content
            }
        }

    private fun normaliseAlgorithmIdentifier(id: AlgorithmIdentifier): AlgorithmIdentifier =
        if (id.parameters is DERNull) {
            AlgorithmIdentifier(id.algorithm, null)
        } else {
            id
        }
}
