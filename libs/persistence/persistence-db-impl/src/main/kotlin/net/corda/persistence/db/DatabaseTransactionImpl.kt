package net.corda.persistence.db

import net.corda.persistence.api.CordaPersistence
import net.corda.persistence.api.DatabaseTransaction
import org.hibernate.Session
import org.hibernate.Transaction
import java.sql.Connection
import java.sql.SQLException
import javax.persistence.EntityManager
import kotlin.jvm.Throws

class DatabaseTransactionImpl (
    private val entityManager: EntityManager,
    private val database: CordaPersistence
) :DatabaseTransaction {
    override val connection: Connection = createConnection()
    private fun createConnection() : Connection {
        return connection.apply {
            autoCommit = false
        }
    }

    private val sessionDelegate = lazy {
        val session = entityManager.withOptions().connection(connection).openSession()
        hibernateTransaction = session.beginTransaction()
        session
    }

    val session: Session by sessionDelegate
    private lateinit var hibernateTransaction: Transaction

    override fun commit() {
        if (sessionDelegate.isInitialized()) {
            hibernateTransaction.commit()
        }
        connection.commit()
    }

    @Throws(SQLException::class)
    override fun close() {
        if (sessionDelegate.isInitialized() && session.isOpen) {
            session.close()
        }
        connection.close()
    }
}