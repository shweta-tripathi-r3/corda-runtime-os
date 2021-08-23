package net.corda.persistence.db

import com.codahale.metrics.MetricRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import net.corda.persistence.api.ConnectionConfig
import net.corda.persistence.api.MappedSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import java.nio.file.Paths
import java.time.Instant
import javax.persistence.Entity
import javax.persistence.EntityManager
import javax.persistence.Id
import javax.sql.DataSource

class CordaPersistenceTest {
    private lateinit var entityManager: EntityManager

    object TestSchemaFamily

    // TODO: Does it HAVE to be MappedSchema?
    object GoodSchema : MappedSchema(schemaFamily = TestSchemaFamily::class.java, version = 1, mappedTypes = listOf(
        State::class.java)) {
        @Entity
        class State(
            @Id
            var id: String
        )
    }

    @BeforeEach
    fun setup() {
        val hikariProperties = testDataSourceProperties("blah")
        val dataSource = createDataSource(hikariProperties) as HikariDataSource

        val customClassloader = Thread.currentThread().contextClassLoader
        val cordaPersistence = CordaPersistenceImpl(dataSource, setOf(GoodSchema))
//        entityManager = cordaPersistence.createEntityManager(customClassloader, listOf(GoodSchema::class.java.toString()), hikariProperties.url, hikariProperties.user, hikariProperties.password)
        entityManager = cordaPersistence.createEntityManager(customClassloader, listOf(GoodSchema), hikariProperties.url, hikariProperties.user, hikariProperties.password)
    }

    @Test
    fun `insert and extract using a CordaPersistence`() {
        val id = "state.${Instant.now()}"
        val obj = GoodSchema.State(id)

        entityManager.apply {
            transaction.begin()
            persist(obj)
            transaction.commit()
        }

        val result = entityManager.find(GoodSchema.State::class.java, id)

        assertThat(result).isNotNull()
        assertThat(result.id).isEqualTo(id)
    }

    private fun testDataSourceProperties(nodeName: String, inMem: Boolean = true): ConnectionConfig {
        val url = if (inMem) {
            "jdbc:h2:mem:${nodeName}_persistence:LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        } else {
            val dbPath = Paths.get("", "build", "mocknetworktestdb", nodeName).toAbsolutePath().resolve("persistence")
            "jdbc:h2:file:$dbPath;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
        return ConnectionConfig(
            "org.h2.jdbcx.JdbcDataSource",
            url,
            "sa",
            ""
        )
    }

    private fun createDataSource(connectionConfig: ConnectionConfig,
                                 registry: MetricRegistry? = null
    ): DataSource {
        val config = HikariConfig(connectionConfig)
        config.isAutoCommit = false
        return HikariDataSource(config).apply {
            if (registry != null) {
                metricRegistry = registry
            } else {
                LoggerFactory.getLogger(javaClass).warn("No MetricRegistry provided (via OSGi?) for $this.")
            }
        }
    }
}