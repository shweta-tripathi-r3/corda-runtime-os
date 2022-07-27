package net.corda.ledger.common.impl.transactions

import net.corda.v5.ledger.common.transactions.TRANSACTION_META_DATA_CPK_IDENTIFIERS_KEY
import net.corda.v5.ledger.common.transactions.TRANSACTION_META_DATA_LEDGER_MODEL_KEY
import net.corda.v5.ledger.common.transactions.TRANSACTION_META_DATA_LEDGER_VERSION_KEY
import net.corda.v5.ledger.common.transactions.TransactionMetaData

//TODO(guarantee its serialization is deterministic)
class TransactionMetaDataImpl(
    private val properties: Map<String, Any>
    ) : TransactionMetaData {

    override operator fun get(key: String): Any? = properties[key]

    override val entries: Set<Map.Entry<String, Any>>
        get() = properties.entries

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is TransactionMetaDataImpl) return false
        if (this === other) return true
        return properties == other.properties
    }

    override fun hashCode(): Int = properties.hashCode()

    fun getLedgerModel(){
        this[TRANSACTION_META_DATA_LEDGER_MODEL_KEY]
    }
    fun getLedgerVersion(){
        this[TRANSACTION_META_DATA_LEDGER_VERSION_KEY]
    }
    fun cpkIdentifiers(){
        this[TRANSACTION_META_DATA_CPK_IDENTIFIERS_KEY]
    }
}