package net.cordapp.demo.utxo;

import net.corda.v5.application.persistence.PagedQuery;
import net.corda.v5.application.persistence.ParameterizedQuery;
import net.corda.v5.ledger.utxo.ContractState;
import net.corda.v5.ledger.utxo.UtxoLedgerService;

import java.util.List;

public class MyFlow {

    private UtxoLedgerService utxoLedgerService;

    public void method() {
        ParameterizedQuery<Integer> query = utxoLedgerService.query("FIND_BY_TEST_FIELD", Integer.class)
                .setParameter("testField", "value")
                .setParameter("participants", List.of("something"))
                .setParameter("contractStateType", ContractState.class.getName())
                .setParameter("in-memory-filter-parameter", "parameter")
                .setOffset(0)
                .setLimit(100);

        PagedQuery.ResultSet<Integer> resultSet = query.execute();

        processResultsWithApplicationLogic(resultSet.getResults());

        while (resultSet.getHasNextPage()) {
            resultSet = query.setOffset(resultSet.getNewOffset()).execute();
            processResultsWithApplicationLogic(resultSet.getResults());
        }

    }

    public void processResultsWithApplicationLogic(List<Integer> results) {
    }
}
