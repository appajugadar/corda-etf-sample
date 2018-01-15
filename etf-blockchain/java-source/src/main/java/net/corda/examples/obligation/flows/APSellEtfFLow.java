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
public class APSellEtfFLow extends FlowLogic<String> {

    private EtfTradeRequest etfTradeRequest;

    private String custodianName;

    public APSellEtfFLow(EtfTradeRequest etfTradeRequest, String custodianName) {
        this.etfTradeRequest = etfTradeRequest;
        this.custodianName = custodianName;
        System.out.println("The input is "+etfTradeRequest +" for custodian "+custodianName);
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.println("The APSellFLow is initiated time "+System.currentTimeMillis());
        FlowSession toPartySession = initiateFlow(getCustodian(custodianName));
        UntrustworthyData<EtfTradeResponse> output =  toPartySession.sendAndReceive(EtfTradeResponse.class, etfTradeRequest);
        EtfTradeResponse outPutValue = SerilazationHelper.getEtfTradeResponse(output);
        System.out.println("The APSellFLow : output from custodian : "+outPutValue);
        System.out.print("The APSellFLow end "+System.currentTimeMillis());
        return "SUCCESS";
    }

    private Party getCustodian(String custodianName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService().getAllIdentities();
        return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
    }

}

