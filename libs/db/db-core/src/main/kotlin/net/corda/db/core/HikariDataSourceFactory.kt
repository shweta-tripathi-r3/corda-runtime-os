package net.corda.db.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

class HikariDataSourceFactory(
    private val hikariDataSourceFactory: (c: HikariConfig) -> CloseableDataSource = { c ->
        val uuid = getNewUuid()
        println("New HikariDataSource - $uuid")
        DataSourceWrapper(HikariDataSource(c), uuid)
    }
) : DataSourceFactory {


    /**
     * [HikariDataSource] wrapper that makes it [CloseableDataSource]
     */
    private class DataSourceWrapper(private val delegate: HikariDataSource, private val uuid: String):
        CloseableDataSource, DataSource by delegate {

        override fun getConnection(username: String?, password: String?): Connection {
            val uuid = getNewUuid()
            println("New Hikari connection - $uuid")

            val c = delegate.getConnection(username, password)
            return object : Connection by c {
                override fun close() {
                    println("Close Hikari connection - $uuid")
                    c.close()
                }
            }
        }

        override fun getConnection(): Connection {
            val uuid = getNewUuid()
            println("New Hikari connection - $uuid")
            connectionMap[uuid] = true

            val c = delegate.connection
            return object : Connection by c {
                override fun close() {
                    println("Close Hikari connection - $uuid")
                    connectionMap[uuid] = false
                    c.close()

                    println("total connections: ${connectionMap.count()}")
                    val count = connectionMap.filter { it.value }.count()
                    println("connections open: $count")
                }
            }
        }

        override fun close() {
            println("DataSourceWrapper.close() - $uuid")
            delegate.close()
        }
    }

    override fun create(
        driverClass: String,
        jdbcUrl: String,
        username: String,
        password: String,
        isAutoCommit: Boolean,
        maximumPoolSize: Int
    ): CloseableDataSource {
        val conf = HikariConfig()
        conf.driverClassName = driverClass
        conf.jdbcUrl = jdbcUrl
        conf.username = username
        conf.password = password
        conf.isAutoCommit = isAutoCommit
        conf.maximumPoolSize = maximumPoolSize
        return hikariDataSourceFactory(conf)
    }

    companion object {
        private val connectionMap: MutableMap<String, Boolean> = mutableMapOf()
    }
}

private fun getNewUuid() = UUID.randomUUID().toString()
