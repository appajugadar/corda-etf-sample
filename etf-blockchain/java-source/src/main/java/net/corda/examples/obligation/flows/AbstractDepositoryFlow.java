package net.corda.examples.obligation.flows;

import java.util.Currency;
import java.util.HashMap;

import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;

abstract public class AbstractDepositoryFlow extends FlowLogic<String> {

    private FlowSession flowSession;

    static HashMap<String, Amount<Currency>> cash = new HashMap<String, Amount<Currency>> (); //

    static HashMap<String,Integer> etf = new HashMap<String, Integer> (); //

    static HashMap<String,FlowSession> buyParty = new HashMap<String, FlowSession>();

    static HashMap<String,FlowSession> sellParty = new HashMap<String, FlowSession>();

    public AbstractDepositoryFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());

    }

    public FlowSession getFlowSession() {
        return this.flowSession;
    }

}