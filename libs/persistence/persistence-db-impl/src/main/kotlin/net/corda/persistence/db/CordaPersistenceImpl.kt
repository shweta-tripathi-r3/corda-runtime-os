package net.corda.persistence.db

import com.zaxxer.hikari.HikariDataSource
import net.corda.persistence.api.CordaPersistence
import net.corda.persistence.api.MappedSchema
import net.corda.v5.base.util.contextLogger
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.slf4j.Logger
import java.sql.Connection
import java.sql.SQLException
import javax.persistence.EntityManager
import javax.persistence.PersistenceException

class CordaPersistenceImpl(
    private val dataSource: HikariDataSource,
    private val schemas: Set<MappedSchema>
) : CordaPersistence {
    private companion object {
        val logger: Logger = contextLogger()
    }

    override fun createEntityManager(
        bundleClassloader: ClassLoader,
//        entities: List<String>,
        schemas: List<MappedSchema>,
        connectionUrl: String,
        login: String,
        password: String
    ): EntityManager {
        logger.info("Creating session factory for schemas: $schemas")
        val serviceRegistry = BootstrapServiceRegistryBuilder().build()
        val metadataSources = MetadataSources(serviceRegistry)
        // We set a connection provider as the auto schema generation requires it.  The auto schema generation will not
        // necessarily remain and would likely be replaced by something like Liquibase.  For now it is very convenient though.
        // TODO: replace auto schema generation as it isn't intended for production use, according to Hibernate docs.
        val config = Configuration(metadataSources)
            .setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect")
            .setProperty("javax.persistence.validation.mode", "none") // ???
            .setProperty("hibernate.format_sql", "true")
            .setProperty("hibernate.hbm2ddl.auto", "update")
            .setProperty("hibernate.jdbc.time_zone", "UTC")
            .setProperty("hibernate.connection.isolation", Connection.TRANSACTION_READ_COMMITTED.toString())

//        entities.forEach {entity ->
//            config.addAnnotatedClass(bundleClassloader.loadClass(entity))
//        }
        schemas.forEach { schema ->
            // TODO: require mechanism to set schemaOptions (databaseSchema, tablePrefix) which are not global to session
            config.addAnnotatedClass(schema::class.java)
        }


        val sessionFactory = buildSessionFactory(config, metadataSources, "")
        logger.info("Created session factory for schemas: $schemas")
        return sessionFactory.createEntityManager()
    }

    @Suppress("Unused_Parameter")
    private fun buildSessionFactory(
        config: Configuration,
        metadataSources: MetadataSources,
        tablePrefix: String
    ): SessionFactory {
        config.standardServiceRegistryBuilder.applySettings(config.properties)
        @Suppress("DEPRECATION")
        val metadata = metadataSources.getMetadataBuilder(config.standardServiceRegistryBuilder.build())
//            .applyPhysicalNamingStrategy(
//                object : PhysicalNamingStrategyStandardImpl() {
//                    override fun toPhysicalTableName(name: Identifier?, context: JdbcEnvironment?): Identifier {
//                        val default = super.toPhysicalTableName(name, context)
//                        return Identifier.toIdentifier(tablePrefix + default.text, default.isQuoted)
//                    }
//                }
//            ).applyBasicType(CordaMaterializedBlobType, CordaMaterializedBlobType.name).
                // Register a tweaked version of `org.hibernate.type.MaterializedBlobType` that truncates logged messages.
                // to avoid OOM when large blobs might get logged.applyBasicType(CordaMaterializedBlobType, CordaMaterializedBlobType.name)
            .build()

        return metadata.sessionFactoryBuilder.run {
            allowOutOfTransactionUpdateOperations(true)
            applySecondLevelCacheSupport(false)
            applyQueryCacheSupport(false)
            enableReleaseResourcesOnCloseEnabled(true)
            build()
        }
    }

    override fun close() {
        dataSource.close()
    }
}