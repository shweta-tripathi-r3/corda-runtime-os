package net.corda.virtualnode.write.db.impl.writer

import com.typesafe.config.ConfigFactory
import net.corda.crypto.core.ShortHash
import net.corda.data.virtualnode.VirtualNodeCreateRequest
import net.corda.db.admin.LiquibaseSchemaMigrator
import net.corda.db.connection.manager.DbConnectionManager
import net.corda.db.connection.manager.VirtualNodeDbType
import net.corda.db.connection.manager.VirtualNodeDbType.VAULT
import net.corda.db.connection.manager.VirtualNodeDbType.UNIQUENESS
import net.corda.db.connection.manager.VirtualNodeDbType.CRYPTO
import net.corda.db.connection.manager.createDbConfig
import net.corda.db.core.DbPrivilege
import net.corda.db.core.DbPrivilege.DDL
import net.corda.db.core.DbPrivilege.DML
import net.corda.schema.configuration.DatabaseConfig
import net.corda.virtualnode.write.db.impl.VirtualNodesDbAdmin
import java.security.SecureRandom

/**
 * A factory for [VirtualNodeDb]s.
 */
internal class VirtualNodeDbFactoryImpl(
    private val dbConnectionManager: DbConnectionManager,
    private val virtualNodesDbAdmin: VirtualNodesDbAdmin,
    private val schemaMigrator: LiquibaseSchemaMigrator
) : VirtualNodeDbFactory {
    private val smartConfigFactory = dbConnectionManager.clusterConfig.factory
    private val adminJdbcUrl = dbConnectionManager.clusterConfig.getString(DatabaseConfig.JDBC_URL)

    companion object {
        private const val ddlMaxPoolSize = 1
        private const val dmlMaxPoolSize = 1
        private const val passwordLength = 64
        private val passwordSource = (('0'..'9') + ('A'..'Z') + ('a'..'z')).toCharArray()
        private val random = SecureRandom()
    }

    /**
     * Creates [VirtualNodeDb]s using connection configurations from virtual node creation request
     *
     * @param holdingIdentityShortHash Holding identity ID (short hash)
     * @param request Virtual node creation request
     *
     * @return map of [VirtualNodeDbType]s to [VirtualNodeDb]s
     */
    override fun createVNodeDbs(
        holdingIdentityShortHash: ShortHash,
        request: VirtualNodeCreateRequest
    ): Map<VirtualNodeDbType, VirtualNodeDb> {
        with(request) {
            return mapOf(
                Pair(
                    VAULT,
                    createVNodeDb(VAULT, holdingIdentityShortHash, vaultDdlConnection, vaultDmlConnection)
                ),
                Pair(
                    CRYPTO,
                    createVNodeDb(CRYPTO, holdingIdentityShortHash, cryptoDdlConnection, cryptoDmlConnection)
                ),
                Pair(
                    UNIQUENESS,
                    createVNodeDb(
                        UNIQUENESS,
                        holdingIdentityShortHash,
                        uniquenessDdlConnection,
                        uniquenessDmlConnection
                    )
                )
            )
        }
    }

    /**
     * Creates [VirtualNodeDb] with DDL and DML connections from provided connection configurations. If DML connection
     * configuration is not provided, cluster connections are created.
     *
     * @param dbType Virtual node database type
     * @param holdingIdentityShortHash Holding identity ID (short hash)
     * @param ddlConfig DDL connection configuration
     * @param dmlConfig DML connection configuration
     */
    private fun createVNodeDb(
        dbType: VirtualNodeDbType,
        holdingIdentityShortHash: ShortHash,
        ddlConfig: String?,
        dmlConfig: String?
    ): VirtualNodeDb {
        val connectionsProvided = !dmlConfig.isNullOrBlank()
        val dbConnections =
            if (connectionsProvided) {
                mapOf(
                    Pair(DDL, ddlConfig?.let { createConnection(dbType, holdingIdentityShortHash, DDL, ddlConfig) }),
                    Pair(DML, dmlConfig?.let { createConnection(dbType, holdingIdentityShortHash, DML, dmlConfig) })
                )
            } else {
                mapOf(
                    Pair(DDL, createClusterConnection(dbType, holdingIdentityShortHash, DDL)),
                    Pair(DML, createClusterConnection(dbType, holdingIdentityShortHash, DML))
                )
            }
        return VirtualNodeDbImpl(
            !connectionsProvided,
            dbConnections,
            dbType,
            holdingIdentityShortHash,
            virtualNodesDbAdmin,
            dbConnectionManager,
            schemaMigrator
        )
    }

    /**
     * Creates [DbConnectionImpl] from provided connection configuration
     *
     * @param dbType Virtual node database type
     * @param holdingIdentityShortHash Holding identity ID (short hash)
     * @param dbPrivilege Database privilege
     * @param config Connection configuration
     *
     * @return [DbConnectionImpl] created from provided connection configuration
     */
    private fun createConnection(
        dbType: VirtualNodeDbType,
        holdingIdentityShortHash: ShortHash,
        dbPrivilege: DbPrivilege,
        config: String
    ): DbConnectionImpl {
        with(dbType) {
            return DbConnectionImpl(
                getConnectionName(holdingIdentityShortHash),
                dbPrivilege,
                config.toSmartConfig(),
                getConnectionDescription(dbPrivilege, holdingIdentityShortHash)
            )
        }
    }

    /**
     * Creates cluster [DbConnectionImpl]
     *
     * @param dbType Virtual node database type
     * @param holdingIdentityShortHash Holding identity ID (short hash)
     * @param dbPrivilege Database privilege
     *
     * @return created cluster [DbConnectionImpl]
     */
    private fun createClusterConnection(
        dbType: VirtualNodeDbType,
        holdingIdentityShortHash: ShortHash,
        dbPrivilege: DbPrivilege
    ): DbConnectionImpl {
        with(dbType) {
            val user = createUsername(dbPrivilege, holdingIdentityShortHash)
            val password = generatePassword()
            val maxPoolSize = when (dbPrivilege) {
                DDL -> ddlMaxPoolSize
                DML -> dmlMaxPoolSize
            }

            // Add reWriteBatchedInserts JDBC parameter for uniqueness db to enable Hibernate batching
            var jdbcUrl = virtualNodesDbAdmin.createJdbcUrl(getSchemaName(holdingIdentityShortHash))
            if (dbType == UNIQUENESS && jdbcUrl.startsWith("jdbc:postgresql")) {
                jdbcUrl += "&reWriteBatchedInserts=true"
            }

            // TODO support for CharArray passwords in SmartConfig
            val config = createDbConfig(
                smartConfigFactory, user, password.concatToString(),
                jdbcUrl = jdbcUrl,
                maxPoolSize = maxPoolSize,
                key = "corda-vault-$holdingIdentityShortHash-database-password"
            )
            return DbConnectionImpl(
                getConnectionName(holdingIdentityShortHash),
                dbPrivilege,
                config,
                getConnectionDescription(dbPrivilege, holdingIdentityShortHash)
            )
        }
    }

    /**
     * Generates random DB password
     */
    private fun generatePassword(): CharArray {
        val password = CharArray(passwordLength)
        for (i in 0 until passwordLength) password[i] = passwordSource[random.nextInt(passwordSource.size)]
        return password
    }

    /**
     * Creates a SmartConfig from configuration String
     *
     * @return SmartConfig created from configuration String
     */
    private fun String.toSmartConfig() = smartConfigFactory.create(ConfigFactory.parseString(this))
}
