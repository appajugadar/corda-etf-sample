package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfTradeRequest;
import net.corda.examples.obligation.EtfTradeResponse;

@InitiatingFlow
@StartableByRPC
public class APBuyEtfFLow extends FlowLogic<String> {
    EtfTradeRequest etfTradeRequest;

    String custodianName;

    public APBuyEtfFLow(EtfTradeRequest etfTradeRequest, String custodianName) {
        this.etfTradeRequest = etfTradeRequest;
        this.custodianName = custodianName;
        System.out.println("The input is "+etfTradeRequest +" for custodian "+custodianName);
    }
@Suspendable
    public String call() throws FlowException {

        System.out.println("The APSellFLow is initiated time "+System.currentTimeMillis());
        FlowSession toPartySession = initiateFlow(getCustodian(custodianName));
        UntrustworthyData<EtfTradeResponse> output =  toPartySession.sendAndReceive(EtfTradeResponse.class, etfTradeRequest);

         EtfTradeResponse outPutValue = output.unwrap(new UntrustworthyData.Validator<EtfTradeResponse, EtfTradeResponse>() {
            @Override
            public EtfTradeResponse validate(EtfTradeResponse data) throws FlowException {
                return data;
            }
        });

        System.out.println("The APSellFLow : output from custodian : "+outPutValue);
        System.out.print("The APSellFLow end "+System.currentTimeMillis());
        return "SUCCESS";
    }

    private Party getCustodian(String custodianName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService().getAllIdentities();

        for (PartyAndCertificate party : partyAndCertificates) {
            System.out.println("Party "+party.getParty());
            System.out.println("getName "+party.getParty().getName().getOrganisation());
            if (party.getName().getOrganisation().contains(custodianName)) {
                return party.getParty();
            }
        }
        return null;
    }

}

