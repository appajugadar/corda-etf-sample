package com.cts.bfs.etf.corda.flows;

import java.util.Currency;
import java.util.HashMap;

import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;

abstract public class AbstractDepositoryFlow extends FlowLogic<String> {

    private FlowSession flowSession;

    protected static HashMap<String, Amount<Currency>> cash = new HashMap<String, Amount<Currency>> (); //

    protected static HashMap<String,Integer> etf = new HashMap<String, Integer> (); //

    protected static HashMap<String,FlowSession> buyParty = new HashMap<String, FlowSession>();

    protected static HashMap<String,FlowSession> sellParty = new HashMap<String, FlowSession>();

    public AbstractDepositoryFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());
    }

    public FlowSession getFlowSession() {
        return this.flowSession;
    }

}