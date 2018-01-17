package com.cts.bfs.etf.corda.flows;

import com.cts.bfs.etf.corda.state.EtfTradeState;
import net.corda.core.flows.FlowLogic;
import net.corda.core.transactions.SignedTransaction;

import java.util.HashSet;

public abstract class AbstractApFlow extends FlowLogic<SignedTransaction> {
    public static HashSet<EtfTradeState> etfTradeBuyRequests = new HashSet<EtfTradeState>(); //
    public static HashSet<EtfTradeState> etfTradeSellRequests = new HashSet<EtfTradeState>(); //

}
