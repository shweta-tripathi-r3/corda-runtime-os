package net.corda.persistence.api

import java.sql.Connection
import java.sql.SQLException

interface DatabaseTransaction {
    val connection: Connection

    @Throws(SQLException::class)
    fun commit()

    @Throws(SQLException::class)
    fun close()
}