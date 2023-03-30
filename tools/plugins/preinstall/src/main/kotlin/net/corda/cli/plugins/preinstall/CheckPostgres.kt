package net.corda.cli.plugins.preinstall


import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.fabric8.kubernetes.api.model.Secret
import io.fabric8.kubernetes.client.KubernetesClient
import io.fabric8.kubernetes.client.KubernetesClientBuilder
import io.fabric8.kubernetes.client.KubernetesClientException
import picocli.CommandLine
import java.io.File

import picocli.CommandLine.Parameters
import picocli.CommandLine.Option

data class SecretValues(
    @JsonProperty("valueFrom")
    val valueFrom: SecretValues?,
    @JsonProperty("secretKeyRef")
    val secretKeyRef: SecretValues?,
    @JsonProperty("value")
    val value: String?,
    @JsonProperty("key")
    val key: String?,
    @JsonProperty("name")
    val name: String?
)

data class Credentials(
    @JsonProperty("username")
    val username: SecretValues,
    @JsonProperty("password")
    val password: SecretValues
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Cluster(
    @JsonProperty("cluster")
    val cluster: Credentials
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class DB(
    @JsonProperty("db")
    val db: Cluster
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Bootstrap(
    @JsonProperty("bootstrap")
    val bootstrap: DB
)

@CommandLine.Command(name = "check-postgres", description = ["Check that the postgres DB is up and that the credentials work."])
class CheckPostgres : Runnable {

    @Parameters(index = "0", description = ["The yaml file containing either the username and password value, " +
            "or valueFrom.secretKeyRef.key fields for Postgres."])
    lateinit var path: String

    @Option(names = ["-n", "--namespace"], description = ["The namespace in which to look for the secrets"])
    var namespace: String? = null

    @Option(names = ["-v", "--verbose"], description = ["Display additional information when checking resources"])
    var verbose: Boolean = false

    @Option(names = ["-d", "--debug"], description = ["Show information about limit calculation for debugging purposes"])
    var debug: Boolean = false

    companion object LogLevel {
        const val ERROR: Int = 0
        const val INFO: Int = 1
        const val DEBUG: Int = 2
        const val WARN: Int = 3
    }

    // used for logging
    private fun log(s: String, level: Int) {
        when (level) {
            ERROR -> println("[ERROR] $s")
            INFO -> if (verbose) println("[INFO] $s")
            DEBUG -> if (debug) println("[DEBUG] $s")
            WARN -> println("[WARN] $s")

        }
    }

    private fun getSecret(secretName: String, secretKey: String): String? {
        return try {
            val client: KubernetesClient = KubernetesClientBuilder().build()
            val secret: Secret = client.secrets().inNamespace(namespace!!).withName(secretName).get()
            secret.data[secretKey]
        } catch (e: KubernetesClientException) {
            log("Could not read secret $secretName with key $secretKey.", ERROR)
            null
        }
    }

    override fun run() {
        log("Working Directory = ${System.getProperty("user.dir")}\n", INFO)
        val file = File(path)

        if(!file.isFile) {
            log("File does not exist", ERROR)
            return
        }

        lateinit var yaml: Bootstrap
        try {
            val mapper: ObjectMapper = YAMLMapper()
            yaml = mapper.readValue(file, Bootstrap::class.java)
        }
        catch ( e: ValueInstantiationException) {
            log("Could not parse the YAML file at $path.", ERROR)
            return
        }

        val userSecretKey: String? = yaml.bootstrap.db.cluster.username.valueFrom?.secretKeyRef?.key
        val userSecretName: String? = yaml.bootstrap.db.cluster.username.valueFrom?.secretKeyRef?.name
        var username: String? = yaml.bootstrap.db.cluster.username.value

        username = username ?: run {
            if (namespace == null) {
                log("No namespace has been specified. If the username is supposed to be in a secret, " +
                    "specify a namespace with -n or --namespace.", ERROR)
                return
            }
            if (userSecretKey == null)  {
                log("Username secret key could not be found.", ERROR)
                return
            }
            if (userSecretName == null) {
                log("Username secret name could not be found.", ERROR)
                return
            }
            getSecret(userSecretName, userSecretKey) ?: return
        }

        val passwordSecretKey: String? = yaml.bootstrap.db.cluster.password.valueFrom?.secretKeyRef?.key
        val passwordSecretName: String? = yaml.bootstrap.db.cluster.password.valueFrom?.secretKeyRef?.name
        var password: String? = yaml.bootstrap.db.cluster.password.value

        password = password ?: run {
            if (namespace == null) {
                log("No namespace has been specified. If the password is supposed to be in a secret, " +
                        "specify a namespace with -n or --namespace.", ERROR)
                return
            }
            if (passwordSecretKey == null)  {
                log("Password secret key could not be found.", ERROR)
                return
            }
            if (passwordSecretName == null) {
                log("Password secret name could not be found.", ERROR)
                return
            }
            getSecret(passwordSecretName, passwordSecretKey) ?: return
        }

        println("User: $username Password: $password")
    }

}