package net.corda.examples.obligation.flows;

import java.util.Currency;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfTradeRequest;
import net.corda.examples.obligation.EtfTradeResponse;


@InitiatedBy(CustodianSellEtfFlow.class)
@InitiatingFlow
public class DepositorySellEtfFlow extends AbstractDepositoryFlow {

    public DepositorySellEtfFlow(FlowSession flowSession) {
        super(flowSession);
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());
        AbstractDepositoryFlow.sellParty.put(flowSession.getCounterparty().getName().getOrganisation(), flowSession );
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The depository start "+System.currentTimeMillis());
        System.out.println("**In call method for depository flow");

        UntrustworthyData<EtfTradeRequest> inputFromAP = getFlowSession().receive(EtfTradeRequest.class); // Etf
        EtfTradeRequest input =  SerilazationHelper.getEtfTradeRequest(inputFromAP);
        System.out.println("**In call method for depository flow -->"+input);

        AbstractDepositoryFlow.etf.put("PNG", input.getQuantity());

        Amount<Currency> amount= null;

        for (String key : AbstractDepositoryFlow.cash.keySet()) {
            amount = AbstractDepositoryFlow.cash.get(key); //Output to Buyer AP
            System.out.println("**In call method for depository flow -->"+input);
        }

        if (amount != null) {
            System.out.println("**Found match for request -->"+input);
            EtfTradeResponse etfTradeResponse = new EtfTradeResponse(input.getToPartyName(),input.getEtfName(),input.getQuantity(),input.getAmount());
            getFlowSession().send(etfTradeResponse);
            System.out.println("**Sending response back to sellers custodian -->"+etfTradeResponse);
            for (String flowKey:             AbstractDepositoryFlow.buyParty.keySet()) {
                FlowSession flowSession1 = AbstractDepositoryFlow.buyParty.get(flowKey);
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