package net.corda.db.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.util.UUID
import javax.sql.DataSource

class HikariDataSourceFactory(

    private val hikariDataSourceFactory: (c: HikariConfig) -> CloseableDataSource = { c ->
        val uuid = UUID.randomUUID().toString()
        println("New HikariDataSource - $uuid")
        DataSourceWrapper(HikariDataSource(c), uuid)
    }
) : DataSourceFactory {
    /**
     * [HikariDataSource] wrapper that makes it [CloseableDataSource]
     */
    private class DataSourceWrapper(private val delegate: HikariDataSource, private val uuid: String):
        CloseableDataSource, DataSource by delegate {
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
}
