package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfTradeRequest;
import net.corda.examples.obligation.EtfTradeResponse;

import java.util.Currency;


@InitiatedBy(CustodianBuyEtfFlow.class)
@InitiatingFlow
public class DepositorySellEtfFlow extends DepositoryFlow {

    FlowSession flowSession;

    public DepositorySellEtfFlow(FlowSession flowSession) {
        super(flowSession);
        this.flowSession = flowSession;
        System.out.println("**Inside depository called by "+flowSession.getCounterparty());
        DepositoryFlow.sellParty.put(flowSession.getCounterparty().getName().getOrganisation(), flowSession.getCounterparty() );
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The depository start "+System.currentTimeMillis());
        System.out.println("**In call method for depository flow");

        UntrustworthyData<EtfTradeRequest> inputFromAP = flowSession.receive(EtfTradeRequest.class); // Etf
        EtfTradeRequest input =  inputFromAP.unwrap(new UntrustworthyData.Validator<EtfTradeRequest, EtfTradeRequest>() {
            @Override
            public EtfTradeRequest validate(EtfTradeRequest data) throws FlowException {
                System.out.println("**In validate method for depository flow received data "+data);
                return data;
            }
        });

        System.out.println("**In call method for depository flow -->"+input);

        DepositoryFlow.etf.put("PNG", input.getQuantity());

        Amount<Currency> amount= null;

        for (String key : DepositoryFlow.cash.keySet()) {
            amount = DepositoryFlow.cash.get(key); //Output to Buyer AP
            System.out.println("**In call method for depository flow -->"+input);
        }

        System.out.println("**In call method for depository flow -->"+input);

        System.out.println("**In call method for depository flow -->"+input);

        EtfTradeResponse etfTradeResponse = new EtfTradeResponse(input.getToPartyName(),input.getEtfName(),input.getQuantity(),input.getAmount());

        if (amount != null) {
            flowSession.send(etfTradeResponse);
        }

        System.out.print("The Depository end "+System.currentTimeMillis());

        return "SUCCESS";

    }
}