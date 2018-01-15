package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfTradeRequest;
import net.corda.examples.obligation.EtfTradeResponse;

import java.util.Currency;
import java.util.Set;


@InitiatedBy(CustodianBuyEtfFlow.class)
@InitiatingFlow
public class DepositoryBuyEtfFlow extends DepositoryFlow {

    FlowSession flowSession;

    public DepositoryBuyEtfFlow(FlowSession flowSession) {
        super(flowSession);
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());
        DepositoryFlow.buyParty.put(flowSession.getCounterparty().getName().getOrganisation(), flowSession );
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The depository start "+System.currentTimeMillis());
        System.out.println("**In call method for depository flow");
        UntrustworthyData<EtfTradeRequest> inputFromAP = flowSession.receive(EtfTradeRequest.class); //Input is Cash
        EtfTradeRequest input =  SerilazationHelper.getEtfTradeRequest(inputFromAP);
        
        Currency currency = Currency.getInstance("GBP");
        DepositoryFlow.cash.put("GBP", new Amount<Currency>(input.getAmount(), currency));

        Set<String> etfset =  DepositoryFlow.etf.keySet();

        Integer etfQuantity = null;

        for (String key : etfset) {
            etfQuantity = DepositoryFlow.etf.get(key); //Output to Buyer AP
            System.out.println("**In call method for depository flow -->"+input);
        }


        if (etfQuantity != null) {
            System.out.println("**Found match for request -->"+input);
            EtfTradeResponse etfTradeResponse = new EtfTradeResponse(input.getToPartyName(),input.getEtfName(),etfQuantity,input.getAmount());
            System.out.println("**Sending response back to buyers custodian -->"+etfTradeResponse);
            flowSession.send(etfTradeResponse);
            for (String flowKey:             DepositoryFlow.sellParty.keySet()) {
                FlowSession flowSession1 = DepositoryFlow.sellParty.get(flowKey);
                System.out.println("**Sending response back to sellers custodian -->"+etfTradeResponse);
                if(flowSession1!=null)
                    flowSession1.send(etfTradeResponse);
                System.out.println("**In call method for depository flow -->"+input);
            }
        }else{
            //
            UntrustworthyData<EtfTradeResponse> responseFromDepositorySellFlow = this.getFlowSession().receive(EtfTradeResponse.class);
            EtfTradeResponse etfTradeResponse =  SerilazationHelper.getEtfTradeResponse(responseFromDepositorySellFlow);
            flowSession.send(etfTradeResponse);
        }

        System.out.print("The Depository end "+System.currentTimeMillis());
        return "**";

    }
}