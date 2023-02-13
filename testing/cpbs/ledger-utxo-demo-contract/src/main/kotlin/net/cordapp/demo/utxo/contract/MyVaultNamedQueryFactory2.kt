package net.cordapp.demo.utxo.contract

import net.corda.v5.ledger.utxo.VaultNamedQueryBuilderFactory2
import net.corda.v5.ledger.utxo.VaultNamedQueryFactory2

class MyVaultNamedQueryFactory2 : VaultNamedQueryFactory2 {

    override fun create(vaultNamedQueryBuilderFactory: VaultNamedQueryBuilderFactory2) {

        vaultNamedQueryBuilderFactory.create("FIND_ALL_THE_THINGS")
            .filter(MyVaultNamedQueryFilter::class.java)
            .map(MyVaultNamedQueryTransformer::class.java)
            .where("")
            .register()

        vaultNamedQueryBuilderFactory.create("FIND_BY_TEST_FIELD")
            .where("WHERE custom ->> 'TestUtxoState.testField' = ?")
            .filter(MyVaultNamedQueryFilter::class.java)
            .map(MyVaultNamedQueryTransformer::class.java)
            .register()
    }
}