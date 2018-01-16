package com.cts.bfs.etf.corda.contract;
import net.corda.core.contracts.*;

import net.corda.core.transactions.LedgerTransaction;

public class EtfIssueContract implements Contract {

    public static final String SELF_ISSUE_ETF_CONTRACT_ID = "com.cts.bfs.etf.corda.contract.EtfIssueContract";

    public interface Commands extends CommandData {

        class SelfIssueEtf extends TypeOnlyCommandData implements com.cts.bfs.etf.corda.contract.EtfIssueContract.Commands {

        }


    }

    @Override
    public void verify(LedgerTransaction tx) {
        System.out.println("Inside contract. inputs "+tx.getInputs());
        System.out.println("Inside contract. outputs "+tx.getOutputs());
    }


}