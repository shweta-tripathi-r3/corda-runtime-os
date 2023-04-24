package net.corda.messagebus.db.persistence

import net.corda.libs.configuration.SmartConfigImpl
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import javax.persistence.EntityManagerFactory

class DBAccessTest {
    @Test
    fun `DBAccess does not close entity manager factory on close`() {
        val config = SmartConfigImpl.empty()
        val emf = mock<EntityManagerFactory>()
        val dbAccess = DBAccess(emf, config)
        dbAccess.close()
        verify(emf, times(0)).close()
    }
}
