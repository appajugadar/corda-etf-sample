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
        DepositoryFlow.buyParty.put(flowSession.getCounterparty().getName().getOrganisation(), flowSession.getCounterparty() );
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The depository start "+System.currentTimeMillis());
        System.out.println("**In call method for depository flow");
        UntrustworthyData<EtfTradeRequest> inputFromAP = flowSession.receive(EtfTradeRequest.class); //Input is Cash
        EtfTradeRequest input =  inputFromAP.unwrap(new UntrustworthyData.Validator<EtfTradeRequest, EtfTradeRequest>() {
            @Override
            public EtfTradeRequest validate(EtfTradeRequest data) throws FlowException {
                System.out.println("**In validate method for depository flow received data "+data);
                return data;
            }
        });

        Currency currency = Currency.getInstance("GBP");
        DepositoryFlow.cash.put("GBP", new Amount<Currency>(input.getAmount(), currency));

        Set<String> etfset =  DepositoryFlow.etf.keySet();

        Integer etfQuantity = null;

        for (String key : etfset) {
            etfQuantity = DepositoryFlow.etf.get(key); //Output to Buyer AP
            System.out.println("**In call method for depository flow -->"+input);
        }

        System.out.println("**In call method for depository flow -->"+input);

        System.out.println("**In call method for depository flow -->"+input);
        String output = "Depository Output:" + etfQuantity;

        EtfTradeResponse etfTradeResponse = new EtfTradeResponse(input.getToPartyName(),input.getEtfName(),etfQuantity,input.getAmount());

        if (etfQuantity != null) {
            flowSession.send(etfTradeResponse);
        }

        System.out.print("The Depository end "+System.currentTimeMillis());
        return "**";

    }
}