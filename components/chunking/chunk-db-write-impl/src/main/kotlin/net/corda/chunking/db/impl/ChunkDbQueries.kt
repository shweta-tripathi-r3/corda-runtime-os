package net.corda.chunking.db.impl

import net.corda.chunking.Checksum
import net.corda.chunking.datamodel.ChunkEntity
import net.corda.chunking.toCorda
import net.corda.data.chunking.Chunk
import net.corda.orm.utils.transaction
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.crypto.SecureHash
import javax.persistence.EntityManagerFactory
import javax.persistence.NoResultException
import javax.persistence.NonUniqueResultException

/**
 * This class provides some simple methods to put chunks and retrieve the status
 * of any given request.
 *
 * If all chunks are received, then we can also calculate its checksum.
 */
internal class ChunkDbQueries(private val entityManagerFactory: EntityManagerFactory) {
    /** Persist a chunk */
    fun persist(chunk: Chunk) {
        // We put `null` into the data field if the array is empty, i.e. the final chunk
        // so that we can write easier queries for the parts expected and received.
        val data = if (chunk.data.array().isEmpty()) null else chunk.data.array()
        val cordaSecureHash = chunk.checksum?.toCorda()
        entityManagerFactory.createEntityManager().transaction {
            val entity = ChunkEntity(
                chunk.requestId,
                chunk.fileName,
                cordaSecureHash?.toString(),
                chunk.partNumber,
                chunk.offset,
                data
            )
            it.persist(entity)
        }
    }

    /** Return the parts we've received - i.e. chunks with non-zero bytes */
    fun partsReceived(requestId: String): Long {
        return entityManagerFactory.createEntityManager().transaction {
            it.createQuery(
                "SELECT count(c) FROM ${ChunkEntity::class.simpleName} c " +
                        "WHERE c.requestId = :requestId AND c.data != null"
            )
                .setParameter("requestId", requestId)
                .singleResult as Long
        }
    }

    /** Return the expected number of parts - i.e. the part number on the zero bytes chunk */
    fun partsExpected(requestId: String): Long {
        return entityManagerFactory.createEntityManager().transaction {
            try {
                (it.createQuery(
                    "SELECT c.partNumber FROM ${ChunkEntity::class.simpleName} c " +
                            "WHERE c.requestId = :requestId and c.data = null"
                )
                    .setParameter("requestId", requestId)
                    .singleResult as Int).toLong()
            } catch (ex: NoResultException) {
                0L
            } catch (ex: NonUniqueResultException) {
                throw ex
            }
        }
    }

    /** Return true if we've received all parts and the final zero-chunk part */
    fun haveReceivedAllParts(requestId: String): Boolean {
        // TODO - double check whether this needs to be transactional, possibly even part of the `persist` call.
        val partsExpected = partsExpected(requestId)
        if (partsExpected == 0L) return false
        val partsReceived = partsReceived(requestId)
        return partsReceived == partsExpected
    }

    /** Is the checksum for the given [requestId] valid? */
    fun checksumIsValid(requestId: String): Boolean {
        assert(haveReceivedAllParts(requestId)) // run with assertions on...
        return streamingChecksum(requestId)
        //return nastyInMemoryChecksum(entityManagerFactory, requestId)
    }

    private fun streamingChecksum(
        requestId: String
    ): Boolean {
        var expectedChecksum: SecureHash? = null
        var actualChecksum: ByteArray? = null


        entityManagerFactory.createEntityManager().transaction {
            val streamingResults = it.createQuery(
                "SELECT c FROM ${ChunkEntity::class.simpleName} c " +
                        "WHERE c.requestId = :requestId " +
                        "ORDER BY c.partNumber ASC",
                ChunkEntity::class.java
            )
                .setParameter("requestId", requestId)
                .resultStream

            val messageDigest = Checksum.getMessageDigest()

            streamingResults.forEach { e ->
                if (e.data == null) { // zero chunk
                    if (e.checksum == null) throw CordaRuntimeException("This shouldn't happen")
                    expectedChecksum = SecureHash.create(e.checksum!!)
                } else { // non-zero chunk
                    messageDigest.update(e.data!!)
                }
            }

            if (expectedChecksum == null) throw CordaRuntimeException("This shouldn't happen either")

            actualChecksum = messageDigest.digest()
        }

        return expectedChecksum!!.bytes.contentEquals(actualChecksum)
    }

    @Suppress("UNUSED")
    private fun nastyInMemoryChecksum(
        requestId: String
    ): Boolean {
        // In memory check - probably don't want to use this because it gets all the bytes of all the chunks
        // which in principle is a few Mb, until the case when it isn't, e.g. cpk with attachments or resources

        val entities = entityManagerFactory.createEntityManager().transaction {
            it.createQuery(
                "SELECT c FROM ${ChunkEntity::class.simpleName} c " +
                        "WHERE c.requestId = :requestId " +
                        "ORDER BY c.partNumber ASC",
                ChunkEntity::class.java
            )
                .setParameter("requestId", requestId)
                .resultList
        }

        val zeroChunk = entities.last()
        if (zeroChunk.data != null && zeroChunk.checksum != null) throw CordaRuntimeException("Last chunk should contain checksum")
        val expectedChecksum = SecureHash.create(zeroChunk.checksum!!)

        // Use same algorithm as the one we used to send the chunks.
        val messageDigest = Checksum.getMessageDigest()

        val nonZeroChunks = entities.dropLast(1)
        nonZeroChunks.forEach { messageDigest.update(it.data!!) }

        val actualChecksum = messageDigest.digest()

        return expectedChecksum.bytes.contentEquals(actualChecksum)
    }
}
