package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfTradeRequest;
import net.corda.examples.obligation.EtfTradeResponse;

import java.util.Currency;


@InitiatedBy(CustodianSellEtfFlow.class)
@InitiatingFlow
public class DepositorySellEtfFlow extends DepositoryFlow {

    FlowSession flowSession;

    public DepositorySellEtfFlow(FlowSession flowSession) {
        super(flowSession);
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());
        DepositoryFlow.sellParty.put(flowSession.getCounterparty().getName().getOrganisation(), flowSession );
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The depository start "+System.currentTimeMillis());
        System.out.println("**In call method for depository flow");

        UntrustworthyData<EtfTradeRequest> inputFromAP = flowSession.receive(EtfTradeRequest.class); // Etf
        EtfTradeRequest input =  SerilazationHelper.getEtfTradeRequest(inputFromAP);
        System.out.println("**In call method for depository flow -->"+input);

        DepositoryFlow.etf.put("PNG", input.getQuantity());

        Amount<Currency> amount= null;

        for (String key : DepositoryFlow.cash.keySet()) {
            amount = DepositoryFlow.cash.get(key); //Output to Buyer AP
            System.out.println("**In call method for depository flow -->"+input);
        }

        if (amount != null) {
            System.out.println("**Found match for request -->"+input);
            EtfTradeResponse etfTradeResponse = new EtfTradeResponse(input.getToPartyName(),input.getEtfName(),input.getQuantity(),input.getAmount());
            flowSession.send(etfTradeResponse);
            System.out.println("**Sending response back to sellers custodian -->"+etfTradeResponse);
            for (String flowKey:             DepositoryFlow.buyParty.keySet()) {
                FlowSession flowSession1 = DepositoryFlow.buyParty.get(flowKey);
                System.out.println("**Sending response back to buyers custodian -->"+etfTradeResponse);
                if(flowSession1!=null)
                    flowSession1.send(etfTradeResponse);
                System.out.println("**In call method for depository flow -->"+input);
            }
        }

        System.out.print("The Depository end "+System.currentTimeMillis());

        return "SUCCESS";

    }
}