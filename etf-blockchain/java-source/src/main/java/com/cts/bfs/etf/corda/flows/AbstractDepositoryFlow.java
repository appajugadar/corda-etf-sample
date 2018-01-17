package com.cts.bfs.etf.corda.flows;

import com.cts.bfs.etf.corda.state.EtfTradeState;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;

import java.util.HashSet;

abstract public class AbstractDepositoryFlow extends FlowLogic<String> {

    protected static HashSet<EtfTradeState> etfTradeBuyRequests = new HashSet<EtfTradeState>(); //
    protected static HashSet<EtfTradeState> etfTradeSellRequests = new HashSet<EtfTradeState>(); //
    private FlowSession flowSession;

    public AbstractDepositoryFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("Inside depositoryflow called by " + flowSession.getCounterparty());
    }

    public FlowSession getFlowSession() {
        return this.flowSession;
    }

}