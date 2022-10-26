package net.corda.crypto.client.impl

import net.corda.configuration.read.ConfigChangedEvent
import net.corda.configuration.read.ConfigurationReadService
import net.corda.crypto.client.CryptoOpsClient
import net.corda.crypto.client.CryptoOpsProxyClient
import net.corda.crypto.component.impl.AbstractConfigurableComponent
import net.corda.crypto.component.impl.DependenciesTracker
import net.corda.data.KeyValuePairList
import net.corda.data.crypto.wire.CryptoSignatureSpec
import net.corda.data.crypto.wire.CryptoSignatureWithKey
import net.corda.data.crypto.wire.CryptoSignatureWithSignatureSpec
import net.corda.data.crypto.wire.CryptoSigningKey
import net.corda.data.crypto.wire.CryptoSigningKeys
import net.corda.data.crypto.wire.ops.rpc.RpcOpsRequest
import net.corda.data.crypto.wire.ops.rpc.RpcOpsResponse
import net.corda.data.crypto.wire.ops.rpc.queries.CryptoKeyOrderBy
import net.corda.libs.configuration.helper.getConfig
import net.corda.lifecycle.LifecycleCoordinatorFactory
import net.corda.lifecycle.LifecycleCoordinatorName
import net.corda.messaging.api.publisher.RPCSender
import net.corda.messaging.api.publisher.factory.PublisherFactory
import net.corda.messaging.api.subscription.config.RPCConfig
import net.corda.schema.Schemas
import net.corda.schema.configuration.ConfigKeys.CRYPTO_CONFIG
import net.corda.schema.configuration.ConfigKeys.MESSAGING_CONFIG
import net.corda.v5.cipher.suite.CipherSchemeMetadata
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.crypto.DigitalSignature
import net.corda.v5.crypto.SecureHash
import net.corda.v5.crypto.SignatureSpec
import net.corda.v5.crypto.publicKeyId
import org.osgi.service.component.annotations.Activate
import org.osgi.service.component.annotations.Component
import org.osgi.service.component.annotations.Reference
import java.nio.ByteBuffer
import java.security.PublicKey

@Suppress("TooManyFunctions")
@Component(service = [CryptoOpsClient::class, CryptoOpsProxyClient::class])
class CryptoOpsClientComponent @Activate constructor(
    @Reference(service = LifecycleCoordinatorFactory::class)
    coordinatorFactory: LifecycleCoordinatorFactory,
    @Reference(service = PublisherFactory::class)
    private val publisherFactory: PublisherFactory,
    @Reference(service = CipherSchemeMetadata::class)
    private val schemeMetadata: CipherSchemeMetadata,
    @Reference(service = ConfigurationReadService::class)
    configurationReadService: ConfigurationReadService
) : AbstractConfigurableComponent<CryptoOpsClientComponent.Impl>(
    coordinatorFactory = coordinatorFactory,
    myName = LifecycleCoordinatorName.forComponent<CryptoOpsClient>(),
    configurationReadService = configurationReadService,
    upstream = DependenciesTracker.Default(
        setOf(
            LifecycleCoordinatorName.forComponent<ConfigurationReadService>()
        )
    ),
    configKeys = setOf(MESSAGING_CONFIG, CRYPTO_CONFIG)
), CryptoOpsClient, CryptoOpsProxyClient {
    companion object {
        const val CLIENT_ID = "crypto.ops.rpc.client"
        const val GROUP_NAME = "crypto.ops.rpc.client"
    }

    override fun createActiveImpl(event: ConfigChangedEvent): Impl =
        Impl(publisherFactory, schemeMetadata, event)

    override fun getSupportedSchemes(tenantId: String, category: String): List<String> =
        impl.ops.getSupportedSchemes(tenantId, category)

    override fun filterMyKeys(tenantId: String, candidateKeys: Collection<PublicKey>): Collection<PublicKey> =
        impl.ops.filterMyKeys(tenantId, candidateKeys)

    override fun generateKeyPair(
        tenantId: String,
        category: String,
        alias: String,
        scheme: String,
        context: Map<String, String>
    ): PublicKey =
        impl.ops.generateKeyPair(tenantId, category, alias, scheme, context)

    override fun generateKeyPair(
        tenantId: String,
        category: String,
        alias: String,
        externalId: String,
        scheme: String,
        context: Map<String, String>
    ): PublicKey =
        impl.ops.generateKeyPair(tenantId, category, alias, externalId, scheme, context)

    override fun freshKey(
        tenantId: String,
        category: String,
        scheme: String,
        context: Map<String, String>
    ): PublicKey =
        impl.ops.freshKey(tenantId, category, scheme, context)

    override fun freshKey(
        tenantId: String,
        category: String,
        externalId: String,
        scheme: String,
        context: Map<String, String>
    ): PublicKey =
        impl.ops.freshKey(tenantId, category, externalId, scheme, context)

    override fun sign(
        tenantId: String,
        publicKey: PublicKey,
        signatureSpec: SignatureSpec,
        data: ByteArray,
        context: Map<String, String>
    ): DigitalSignature.WithKey =
        impl.ops.sign(tenantId, publicKey, signatureSpec, data, context)

    override fun sign(
        tenantId: String,
        publicKey: PublicKey,
        digest: DigestAlgorithmName,
        data: ByteArray,
        context: Map<String, String>
    ): DigitalSignature.WithKey {
        val signatureSpec = schemeMetadata.inferSignatureSpec(publicKey, digest)
        require(signatureSpec != null) {
            "Failed to infer the signature spec for key=${publicKey.publicKeyId()} " +
                    " (${schemeMetadata.findKeyScheme(publicKey).codeName}:${digest.name})"
        }
        return impl.ops.sign(tenantId, publicKey, signatureSpec, data, context)
    }

    override fun lookup(
        tenantId: String,
        skip: Int,
        take: Int,
        orderBy: CryptoKeyOrderBy,
        filter: Map<String, String>
    ): List<CryptoSigningKey> =
        impl.ops.lookup(
            tenantId = tenantId,
            skip = skip,
            take = take,
            orderBy = orderBy,
            filter = filter
        )

    override fun lookup(tenantId: String, ids: List<String>): List<CryptoSigningKey> =
        impl.ops.lookup(
            tenantId = tenantId,
            ids = ids
        )

    override fun filterMyKeysProxy(tenantId: String, candidateKeys: Iterable<ByteBuffer>): CryptoSigningKeys =
        impl.ops.filterMyKeysProxy(tenantId, candidateKeys)

    override fun signProxy(
        tenantId: String,
        publicKey: ByteBuffer,
        signatureSpec: CryptoSignatureSpec,
        data: ByteBuffer,
        context: KeyValuePairList
    ): CryptoSignatureWithKey = impl.ops.signProxy(tenantId, publicKey, signatureSpec, data, context)

    override fun signProxy(
        tenantId: String,
        publicKey: ByteBuffer,
        data: ByteBuffer,
        context: KeyValuePairList
    ): CryptoSignatureWithSignatureSpec {
        // must infer signature spec from public key alone
        val publicKeyDecoded = schemeMetadata.decodePublicKey(publicKey.array())
        // TODO should the inferring of signature spec take place here or maybe deeper at
        //  `SignRpcCommandHandler`. `CipherSchemeMetadata` (which has `inferSignatureSpec` API) is also available over there.
        //  Doing it here means we are only exposing the new Avro API (`CryptoSignatureWithSignatureSpec`) here
        //  and not to potential other paths leading to `SignRpcCommand` processing.
        //  Less importantly also means we can fail and return faster.
        val signatureSpec = schemeMetadata.inferSignatureSpec(publicKeyDecoded)
        require(signatureSpec != null) {
            "Failed to infer the signature spec for key=${publicKeyDecoded.publicKeyId()} " +
                    " (${schemeMetadata.findKeyScheme(publicKeyDecoded).codeName})"
        }

        val signatureSpecAvro = CryptoSignatureSpec(
            signatureSpec.signatureName, null, null
        )

        // TODO this should be changed to directly call and get `CryptoSignatureWithSignatureSpec` answer from Kafka,
        //  In that case it 'd make more sense to infer to `SignatureSpec` on the other side.
        val cryptoSignatureWithKey = signProxy(tenantId, publicKey, signatureSpecAvro, data, context)

        return CryptoSignatureWithSignatureSpec(
            cryptoSignatureWithKey.bytes,
            // TODO To be changed to avro `CryptoSignatureSpec` type.
            signatureSpec.signatureName,
            // TODO fix below dummy value with SHA-256 hashing public key
            SecureHash.parse("SHA-256:6D1687C143DF792A011A1E80670A4E4E0C25D0D87A39514409B1ABFC2043581F").let {
                net.corda.data.crypto.SecureHash(
                    it.algorithm,
                    ByteBuffer.wrap(it.bytes)
                )
            }
        )
    }

    override fun createWrappingKey(
        hsmId: String,
        failIfExists: Boolean,
        masterKeyAlias: String,
        context: Map<String, String>
    ) = impl.ops.createWrappingKey(hsmId, failIfExists, masterKeyAlias, context)

    override fun deriveSharedSecret(
        tenantId: String,
        publicKey: PublicKey,
        otherPublicKey: PublicKey,
        context: Map<String, String>
    ): ByteArray = impl.ops.deriveSharedSecret(tenantId, publicKey, otherPublicKey, context)

    class Impl(
        publisherFactory: PublisherFactory,
        schemeMetadata: CipherSchemeMetadata,
        event: ConfigChangedEvent
    ) : AbstractImpl {
        private val sender: RPCSender<RpcOpsRequest, RpcOpsResponse> = publisherFactory.createRPCSender(
            RPCConfig(
                groupName = GROUP_NAME,
                clientName = CLIENT_ID,
                requestTopic = Schemas.Crypto.RPC_OPS_MESSAGE_TOPIC,
                requestType = RpcOpsRequest::class.java,
                responseType = RpcOpsResponse::class.java
            ),
            event.config.getConfig(MESSAGING_CONFIG)
        ).also { it.start() }

        val ops: CryptoOpsClientImpl = CryptoOpsClientImpl(
            schemeMetadata = schemeMetadata,
            sender = sender
        )

        override val downstream: DependenciesTracker = DependenciesTracker.Default(setOf(sender.subscriptionName))

        override fun close() {
            sender.close()
        }
    }
}
