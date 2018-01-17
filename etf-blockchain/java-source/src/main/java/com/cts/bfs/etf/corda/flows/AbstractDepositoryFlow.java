package com.cts.bfs.etf.corda.flows;

import java.util.Currency;
import java.util.HashMap;
import java.util.HashSet;

import com.cts.bfs.etf.corda.state.EtfTradeState;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;

abstract public class AbstractDepositoryFlow extends FlowLogic<String> {

    private FlowSession flowSession;

    protected static HashSet<EtfTradeState> etfTradeBuyRequests = new HashSet<EtfTradeState> (); //
    protected static HashSet<EtfTradeState> etfTradeSellRequests = new HashSet<EtfTradeState> (); //

    public AbstractDepositoryFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside depositoryflow called by "+flowSession.getCounterparty());
    }

    public FlowSession getFlowSession() {
        return this.flowSession;
    }

}