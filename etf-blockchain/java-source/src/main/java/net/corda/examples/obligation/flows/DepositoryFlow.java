package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.utilities.UntrustworthyData;

import java.util.Currency;
import java.util.HashMap;

abstract public class DepositoryFlow extends FlowLogic<String> {

    FlowSession flowSession;

    static HashMap<String, Amount<Currency>> cash = new HashMap<String, Amount<Currency>> (); //

    static HashMap<String,Integer> etf = new HashMap<String, Integer> (); //

    static HashMap<String,FlowSession> buyParty = new HashMap<String, FlowSession>();

    static HashMap<String,FlowSession> sellParty = new HashMap<String, FlowSession>();

    public DepositoryFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());

    }

    public FlowSession getFlowSession() {
        return this.flowSession;
    }

}