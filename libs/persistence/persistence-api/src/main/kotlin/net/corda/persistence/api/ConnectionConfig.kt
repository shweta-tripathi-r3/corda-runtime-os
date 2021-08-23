package net.corda.persistence.api

import java.util.*

/**
 * Simple class to contain the hikari properties and enforce that the four constructor parameters are
 * set as they're required by the persistence to start:
 *
 *      dataSourceClassName = <class to access the DB>
 *      dataSource.url = <jdbc url string>
 *      dataSource.user = <DB username>
 *      dataSource.password = <DB password>
 *
 */
class ConnectionConfig (
    dataSourceClassName: String,
    dataSourceUrl: String,
    dataSourceUser: String,
    dataSourcePassword: String,
) : Properties() {
    init {
        setProperty("dataSourceClassName", dataSourceClassName)
        setProperty("dataSource.url", dataSourceUrl)
        setProperty("dataSource.user", dataSourceUser)
        setProperty("dataSource.password", dataSourcePassword)
    }

    companion object {
        fun fromProperties(properties: Properties) : ConnectionConfig {
            return ConnectionConfig(
                properties.getProperty("dataSourceClassName"),
                properties.getProperty("dataSource.url"),
                properties.getProperty("dataSource.user"),
                properties.getProperty("dataSource.password")
            )
        }
    }

    // These aren't strictly needed but might be useful for access the must-have properties
    val dataSourceClassName
        get() = this["dataSourceClassName"].toString()

    val url
        get() = this["dataSource.url"].toString()

    val user
        get() = this["dataSource.user"].toString()

    val password
        get() = this["dataSource.password"].toString()
}