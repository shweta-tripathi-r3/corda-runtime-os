package net.corda.ledger.persistence.utxo.impl

import net.corda.ledger.common.data.transaction.SignedTransactionContainer
import net.corda.ledger.common.data.transaction.TransactionStatus
import net.corda.ledger.persistence.utxo.UtxoPersistenceService
import net.corda.ledger.persistence.utxo.UtxoRepository
import net.corda.ledger.persistence.utxo.UtxoTransactionReader
import net.corda.orm.utils.transaction
import net.corda.persistence.common.getEntityManagerFactory
import net.corda.sandboxgroupcontext.SandboxGroupContext
import net.corda.utilities.time.Clock
import net.corda.v5.application.crypto.DigestService
import net.corda.v5.crypto.DigestAlgorithmName
import net.corda.v5.ledger.common.transaction.CordaPackageSummary

class UtxoPersistenceServiceImpl constructor(
    private val sandbox: SandboxGroupContext,
    private val repository: UtxoRepository,
    private val sandboxDigestService: DigestService,
    private val utcClock: Clock
) : UtxoPersistenceService {

    override fun persistTransaction(transaction: UtxoTransactionReader) {
        val entityManger = sandbox.getEntityManagerFactory().createEntityManager()
        val nowUtc = utcClock.instant()

        entityManger.transaction { em ->
            val transactionIdString = transaction.id.toString()

            // Insert the Transaction
            repository.persistTransaction(
                em,
                transactionIdString,
                transaction.privacySalt.bytes,
                transaction.account,
                nowUtc
            )

            // Insert the Transactions components
            transaction.rawGroupLists.mapIndexed { groupIndex, leaves ->
                leaves.mapIndexed { leafIndex, data ->
                    repository.persistTransactionComponentLeaf(
                        em,
                        transactionIdString,
                        groupIndex,
                        leafIndex,
                        data,
                        sandboxDigestService.hash(data, DigestAlgorithmName.SHA2_256).toString(),
                        nowUtc
                    )
                }
            }

            // Insert the Transactions signatures
            transaction.signatures.forEachIndexed { index, digitalSignatureAndMetadata ->
                repository.persistTransactionSignature(
                    em,
                    transactionIdString,
                    index,
                    digitalSignatureAndMetadata,
                    nowUtc
                )
            }

            // Insert the transactions current status
            repository.persistTransactionStatus(
                em,
                transactionIdString,
                transaction.status,
                nowUtc
            )

            // Insert the CPK details liked to this transaction
            // TODOs: The CPK file meta does not exist yet, this will be implemented by
            // https://r3-cev.atlassian.net/browse/CORE-7626
        }
    }

    override fun persistTransactionIfDoesNotExist(
        transaction: SignedTransactionContainer,
        transactionStatus: TransactionStatus,
        account: String
    ): Pair<String?, List<CordaPackageSummary>> {
        val entityManger = sandbox.getEntityManagerFactory().createEntityManager()
        val nowUtc = utcClock.instant()

        return entityManger.transaction { em ->
            val transactionIdString = transaction.id.toString()

            val status = repository.findTransactionStatus(em, transactionIdString)

            if (status != null) {
                return@transaction status to emptyList()
            }

            // Insert the Transaction
            repository.persistTransaction(
                em,
                transactionIdString,
                transaction.wireTransaction.privacySalt.bytes,
                account,
                nowUtc
            )

            // Insert the Transactions components
            transaction.wireTransaction.componentGroupLists.mapIndexed { groupIndex, leaves ->
                leaves.mapIndexed { leafIndex, data ->
                    repository.persistTransactionComponentLeaf(
                        em,
                        transactionIdString,
                        groupIndex,
                        leafIndex,
                        data,
                        sandboxDigestService.hash(data, DigestAlgorithmName.SHA2_256).toString(),
                        nowUtc
                    )
                }
            }

            // Insert the Transactions signatures
            transaction.signatures.forEachIndexed { index, digitalSignatureAndMetadata ->
                repository.persistTransactionSignature(
                    em,
                    transactionIdString,
                    index,
                    digitalSignatureAndMetadata,
                    nowUtc
                )
            }

            // Insert the transactions current status
            repository.persistTransactionStatus(
                em,
                transactionIdString,
                transactionStatus,
                nowUtc
            )

            // Insert the CPK details liked to this transaction
            // TODOs: The CPK file meta does not exist yet, this will be implemented by
            // https://r3-cev.atlassian.net/browse/CORE-7626

            return null to emptyList()
        }
    }

    override fun updateStatus(id: String, transactionStatus: TransactionStatus) {
        sandbox.getEntityManagerFactory().createEntityManager().transaction { em ->
            repository.persistTransactionStatus(em, id, transactionStatus, utcClock.instant())
        }
    }

    override fun findTransaction(id: String, transactionStatus: TransactionStatus): SignedTransactionContainer? {
        return sandbox.getEntityManagerFactory().createEntityManager().transaction { em ->
            val status = repository.findTransactionStatus(em, id)
            if (status == transactionStatus.value) {
                repository.findTransaction(em, id)
            } else {
                null
            }
        }
    }
}
