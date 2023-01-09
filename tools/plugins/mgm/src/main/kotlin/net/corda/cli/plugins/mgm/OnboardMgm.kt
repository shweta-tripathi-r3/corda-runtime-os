package net.corda.cli.plugins.mgm

import kong.unirest.Unirest
import kong.unirest.json.JSONArray
import kong.unirest.json.JSONObject
import net.corda.cli.plugins.packaging.CreateCpiV2
import net.corda.cli.plugins.packaging.signing.SigningOptions
import net.corda.crypto.test.certificates.generation.toPem
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.jar.Manifest

@Command(
    name = "mgm",
    description = [
        "Onboard MGM member.",
        "This sub command should only be used in for internal development",
    ]
)
class OnboardMgm : Runnable, BaseOnboard() {
    @Option(
        names = ["--cpb-name"],
        description = ["The name of the CPB. Default to random UUID"]
    )
    var cpbName: String = UUID.randomUUID().toString()

    @Option(
        names = ["--x500-name"],
        description = ["The X500 name of the MGM. Default to a random name"]
    )
    override var x500Name: String = "O=Mgm, L=London, C=GB, OU=${UUID.randomUUID()}"

    private val dotCordaDir = File(File(System.getProperty("user.home")), ".corda")

    @Option(
        names = ["--save-group-policy-as"],
        description = ["Location to save the group policy file (default to ~/.corda/gp/groupPolicy.json)"]
    )
    var groupPolicyFile: File = File(File(dotCordaDir, "gp"), "groupPolicy.json")

    @CommandLine.Mixin
    var signingOptions = SigningOptions()

    private val groupPolicy by lazy {
        mapOf(
            "fileFormatVersion" to 1,
            "groupId" to "CREATE_ID",
            "registrationProtocol" to "net.corda.membership.impl.registration.dynamic.mgm.MGMRegistrationService",
            "synchronisationProtocol" to "net.corda.membership.impl.synchronisation.MgmSynchronisationServiceImpl"
        ).let { groupPolicyMap ->
            ByteArrayOutputStream().use { outputStream ->
                json.writeValue(outputStream, groupPolicyMap)
                outputStream.toByteArray()
            }
        }
    }
    private val cordaVersion by lazy {
        val manifest = OnboardMgm::class.java.classLoader
            .getResource("META-INF/MANIFEST.MF")
            ?.openStream()
            ?.use {
                Manifest(it)
            }
        manifest?.mainAttributes?.getValue("Bundle-Version") ?: "5.0.0.0-SNAPSHOT"
    }

    private val jar: ByteArray by lazy {

        val gpFile = File.createTempFile("gp", "tmp", dotCordaDir).apply { deleteOnExit() }
        val cpiFile = File.createTempFile("cpi", "tmp", dotCordaDir).apply { deleteOnExit() }

        gpFile.writeBytes(groupPolicy)

        CreateCpiV2().apply {
            groupPolicyFileName = gpFile.absolutePath
            cpiName = cpbName
            cpiVersion = cordaVersion
            cpiUpgrade = false
            outputFileName = cpiFile.absolutePath
            signingOptions = this@OnboardMgm.signingOptions
        }.run()

        cpiFile.readBytes()
    }

    private fun saveGroupPolicy() {
        val response = Unirest.get("/mgm/$holdingId/info").asString()
        groupPolicyFile.parentFile.mkdirs()
        json.writerWithDefaultPrettyPrinter()
            .writeValue(
                groupPolicyFile,
                json.readTree(response.bodyOrThrow())
            )
        println("Group policy file created at $groupPolicyFile")
    }

    private val tlsTrustRoot by lazy {
        ca.caCertificate.toPem()
    }

    override val registrationContext by lazy {
        mapOf(
            "corda.session.key.id" to sessionKeyId,
            "corda.ecdh.key.id" to ecdhKeyId,
            "corda.group.protocol.registration"
                to "net.corda.membership.impl.registration.dynamic.member.DynamicMemberRegistrationService",
            "corda.group.protocol.synchronisation"
                to "net.corda.membership.impl.synchronisation.MemberSynchronisationServiceImpl",
            "corda.group.protocol.p2p.mode" to "Authenticated_Encryption",
            "corda.group.key.session.policy" to "Distinct",
            "corda.group.pki.session" to "NoPKI",
            "corda.group.pki.tls" to "Standard",
            "corda.group.tls.version" to "1.3",
            "corda.endpoints.0.connectionURL" to p2pUrl,
            "corda.endpoints.0.protocolVersion" to "1",
            "corda.group.truststore.tls.0" to tlsTrustRoot,
        )
    }

    override val cpiFileChecksum by lazy {
        val existingHash = Unirest.get("/cpi")
            .asJson()
            .bodyOrThrow()
            .let {
                val cpis = it.`object`.get("cpis") as JSONArray
                cpis
                    .filterIsInstance<JSONObject>()
                    .firstOrNull { cpi ->
                        cpi.get("groupPolicy").toString().contains("CREATE_ID")
                    }?.get("cpiFileChecksum")
            }
        if (existingHash is String) return@lazy existingHash

        uploadCpi(jar.inputStream(), cpbName)
    }
    override fun run() {
        println("This sub command should only be used in for internal development")
        println("On-boarding MGM member $x500Name")

        setupClient()

        createTlsKeyIdNeeded()

        disableClrChecks()

        register()

        setupNetwork()

        println("MGM Member $x500Name was onboarded")

        saveGroupPolicy()
    }
}
