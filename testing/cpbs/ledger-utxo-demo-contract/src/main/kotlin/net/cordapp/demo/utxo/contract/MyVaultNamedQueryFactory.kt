package net.cordapp.demo.utxo.contract

import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.VaultNamedQuery
import net.corda.v5.ledger.utxo.VaultNamedQueryBuilderFactory
import net.corda.v5.ledger.utxo.VaultNamedQueryFactory
import net.corda.v5.ledger.utxo.VaultNamedQueryFilter
import net.corda.v5.ledger.utxo.VaultNamedQueryTransformer

class MyVaultNamedQueryFactory : VaultNamedQueryFactory {

    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory): List<VaultNamedQuery> {
        return listOf(
            vaultNamedQueryBuilderFactory.create("FIND_ALL_THE_THINGS")
                .filter(MyVaultNamedQueryFilter::class.java)
                .map(MyVaultNamedQueryTransformer::class.java)
                .whereJson(""),
            vaultNamedQueryBuilderFactory.create("FIND_BY_TEST_FIELD")
                .filter(MyVaultNamedQueryFilter::class.java)
                .map(MyVaultNamedQueryTransformer::class.java)
                .whereJson("WHERE custom ->> 'TestUtxoState.testField' = ?"),
            // could probably remove the WHERE and custom ->> at this rate
            // need to keep functionality open for selects in the future still though
            vaultNamedQueryBuilderFactory.create("FIND_WITH_CORDA_COLUMNS")
                .filter(MyVaultNamedQueryFilter::class.java)
                .map(MyVaultNamedQueryTransformer::class.java)
                .whereJson(
                    "WHERE custom ->> 'TestUtxoState.testField' = :testField " +
                            "AND custom ->> 'Corda.participants' IN :participants " +
                            "AND custom ? :contractStateType"
                )
        )
    }
}

class MyVaultNamedQueryFilter : VaultNamedQueryFilter<ContractState> {

    override fun filter(state: ContractState): Boolean {
        return true
    }
}

class MyVaultNamedQueryTransformer : VaultNamedQueryTransformer<ContractState, MyPojo> {

    override fun transform(state: ContractState): MyPojo {
        return MyPojo()
    }
}

class MyPojo

class AnotherVaultNamedQueryFactory : VaultNamedQueryFactory {

    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory): List<VaultNamedQuery> {
        return listOf(
            vaultNamedQueryBuilderFactory.create("FIND_ALL_THE_THINGS_AGAIN").whereJson(""),
            vaultNamedQueryBuilderFactory.create("FIND_BY_TEST_FIELD_AGAIN").whereJson("WHERE custom ->> 'testField' = ?")
        )
    }
}