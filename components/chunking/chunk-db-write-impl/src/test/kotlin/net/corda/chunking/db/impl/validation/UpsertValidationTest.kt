package net.corda.chunking.db.impl.validation

import net.corda.chunking.db.impl.persistence.database.DatabaseCpiPersistence
import net.corda.libs.cpi.datamodel.entities.CpiMetadataEntity
import net.corda.libs.cpiupload.DuplicateCpiUploadException
import net.corda.libs.cpiupload.ValidationException
import net.corda.v5.crypto.SecureHash
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.EntityTransaction
import javax.persistence.TypedQuery

class UpsertValidationTest {

    private fun createMockEntityManagerFactory(queryResult: List<CpiMetadataEntity>): EntityManagerFactory {
        val query = mock<TypedQuery<CpiMetadataEntity>>() {
            on { it.setParameter(any<String>(), any<String>()) }.doReturn(it)
            on { it.resultList }.doReturn(queryResult)
        }
        val tx = mock<EntityTransaction>()
        val mockEntityManager = mock<EntityManager>() {
            on { it.createQuery(any(), any<Class<CpiMetadataEntity>>()) } doReturn(query)
            on { it.transaction } doReturn (tx)
        }
        return mock<EntityManagerFactory>() {
            on {  it.createEntityManager() } doReturn (mockEntityManager)
        }
    }
    @Test
    fun `succeeds with unique cpi`() {
        val requiredName = "aaa"
        val requiredVersion = "1.0"
        val hash = "DUMMY:1234567890"
        val groupId = "ABC"

        val p = DatabaseCpiPersistence(createMockEntityManagerFactory(emptyList()))

        assertDoesNotThrow {
            p.validateCanUpsertCpi(requiredName, hash, requiredVersion, groupId, false, "id")
        }
    }

    @Test
    fun `succeeds force upload when version exact`() {
        val requiredName = "aaa"
        val requiredVersion = "1.0"
        val hash = "DUMMY:1234567890"
        val groupId = "ABC"

        val meta = mock<CpiMetadataEntity> {
            on { name }.doReturn("")
            on { it.version }.doReturn(requiredVersion)
            on { signerSummaryHash }.doReturn(SecureHash("SHA1", "DUMMY:ABCDEFGH".toByteArray()).toString() )
            on { fileChecksum }.doReturn(SecureHash("SHA1", "DUMMY:abcdefgh".toByteArray()).toString() )
            on { this.groupId }.doReturn(groupId )
            on { this.groupPolicy }.doReturn("{}" )
        }

        val p = DatabaseCpiPersistence(createMockEntityManagerFactory(listOf(meta)))

        assertDoesNotThrow {
            p.validateCanUpsertCpi(requiredName, hash, requiredVersion, groupId, true, "id")
        }
    }

    @Test
    fun `fails force upload when version correct but groupId different to previous`() {
        val requiredName = "aaa"
        val requiredVersion = "1.0"
        val hash = "DUMMY:1234567890"
        val groupId = "ABC"

        val meta = mock<CpiMetadataEntity> {
            on { name }.doReturn("")
            on { it.version }.doReturn(requiredVersion)
            on { signerSummaryHash }.doReturn(SecureHash("SHA1", "DUMMY:ABCDEFGH".toByteArray()).toString() )
            on { fileChecksum }.doReturn(SecureHash("SHA1", "DUMMY:abcdefgh".toByteArray()).toString() )
            on { this.groupId }.doReturn("foo" )
            on { this.groupPolicy }.doReturn("{}" )
        }

        val p = DatabaseCpiPersistence(createMockEntityManagerFactory(listOf(meta)))

        assertThrows<ValidationException> {
            p.validateCanUpsertCpi(requiredName, hash, requiredVersion, groupId, true, "id")
        }
    }

    @Test
    fun `fails force upload when version is different`() {
        val requiredName = "aaa"
        val requiredVersion = "1.0"
        val hash = "DUMMY:1234567890"
        val groupId = "ABC"

        val meta = mock<CpiMetadataEntity> {
            on { name }.doReturn("")
            on { version }.doReturn("2.0")
            on { signerSummaryHash }.doReturn(SecureHash("SHA1", "DUMMY:ABCDEFGH".toByteArray()).toString() )
            on { fileChecksum }.doReturn(SecureHash("SHA1", "DUMMY:abcdefgh".toByteArray()).toString() )
            on { this.groupId }.doReturn("" )
            on { this.groupPolicy }.doReturn("{}" )
        }

        val p = DatabaseCpiPersistence(createMockEntityManagerFactory(listOf(meta)))

        assertThrows<ValidationException> {
            p.validateCanUpsertCpi(requiredName, hash, requiredVersion, groupId, true, "id")
        }
    }

    @Test
    fun `succeeds upload when version different`() {
        val requiredName = "aaa"
        val requiredVersion = "1.0"
        val hash = "DUMMY:1234567890"
        val groupId = "ABC"

        val meta = mock<CpiMetadataEntity> {
            on { name }.doReturn("")
            on { version }.doReturn("2.0")
            on { signerSummaryHash }.doReturn(SecureHash("SHA1", "DUMMY:ABCDEFGH".toByteArray()).toString() )
            on { fileChecksum }.doReturn(SecureHash("SHA1", "DUMMY:abcdefgh".toByteArray()).toString() )
            on { this.groupId }.doReturn("" )
            on { this.groupPolicy }.doReturn("{}" )
        }

        val p = DatabaseCpiPersistence(createMockEntityManagerFactory(listOf(meta)))

        assertDoesNotThrow {
            p.validateCanUpsertCpi(requiredName, hash, requiredVersion, groupId, false, "id")
        }
    }

    @Test
    fun `fails upload when version is same`() {
        val requiredName = "aaa"
        val requiredVersion = "1.0"
        val hash = SecureHash("SHA1", "DUMMY:1234567890".toByteArray()).toString()
        val groupId = "ABC"

        val meta = mock<CpiMetadataEntity> {
            on { name }.doReturn("")
            on { version }.doReturn(requiredVersion)
            on { signerSummaryHash }.doReturn(SecureHash("SHA1", "DUMMY:ABCDEFGH".toByteArray()).toString() )
            on { fileChecksum }.doReturn(SecureHash("SHA1", "DUMMY:abcdefgh".toByteArray()).toString() )
            on { this.groupId }.doReturn("" )
            on { this.groupPolicy }.doReturn("{}" )
        }

        val p = DatabaseCpiPersistence(createMockEntityManagerFactory(listOf(meta)))

        assertThrows<DuplicateCpiUploadException> {
            p.validateCanUpsertCpi(requiredName, hash, requiredVersion, groupId, false, "id")
        }
    }
}
