package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.utilities.UntrustworthyData;

import java.util.Currency;
import java.util.HashMap;


@InitiatedBy(CustodianBuyEtfFlow.class)
@InitiatingFlow
public class DepositoryFlow extends FlowLogic<String> {

    FlowSession flowSession;

    static HashMap<String, Amount<Currency>> cash = new HashMap<String, Amount<Currency>> (); //

    static HashMap<String,Integer> etf = new HashMap<String, Integer> (); //

    static HashMap<String,Party> buyParty = new HashMap<String, Party>();

    static HashMap<String,Party> sellParty = new HashMap<String, Party>();

    public DepositoryFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());

    }

    public FlowSession getFlowSession() {
        return this.flowSession;
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The depository start "+System.currentTimeMillis());
        System.out.println("**In call method for depository flow");
        UntrustworthyData<String> inputFromAP = flowSession.receive(String.class);
        String input =  inputFromAP.unwrap(new UntrustworthyData.Validator<String, String>() {
            @Override
            public String validate(String data) throws FlowException {
                System.out.println("**In validate method for depository flow received data "+data);
                return data;
            }
        });
        System.out.println("**In call method for depository flow -->"+input);
        String output = "Depository Output:" + input;
        flowSession.send(output);
        System.out.print("The Depository end "+System.currentTimeMillis());
        return output+"**";

    }
}