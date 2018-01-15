package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfTradeRequest;
import net.corda.examples.obligation.EtfTradeResponse;

@InitiatedBy(APSellEtfFLow.class)
@InitiatingFlow
public class CustodianSellEtfFlow extends FlowLogic<String> {

    String dipositoryName;

    FlowSession flowSession;

    public CustodianSellEtfFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        this.dipositoryName = "DEPOSITORY";
        System.out.println("Inside custodian called by "+flowSession.getCounterparty());
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The custodian start "+System.currentTimeMillis());
        System.out.println("**In call method for custodian flow");
        UntrustworthyData<EtfTradeRequest> inputFromAP = flowSession.receive(EtfTradeRequest.class);


        EtfTradeRequest sendToDipository =  inputFromAP.unwrap(new UntrustworthyData.Validator<EtfTradeRequest, EtfTradeRequest>() {
            @Override
            public EtfTradeRequest validate(EtfTradeRequest data) throws FlowException {
                System.out.println("**In validate method for custodian flow received data "+data);
                return data;
            }
        });

        System.out.println("**In call method for custodian flow -->"+sendToDipository);

        FlowSession toPartySession = initiateFlow(getDipository(dipositoryName));
        UntrustworthyData<EtfTradeResponse> output =  toPartySession.sendAndReceive(EtfTradeResponse.class, sendToDipository);
        EtfTradeResponse out =  SerilazationHelper.getEtfTradeResponse(output);
        System.out.println("**In call method for custodian flow output from depository-->"+out);
        flowSession.send(out);

        System.out.print("The custodian end "+System.currentTimeMillis());
        return out +" (fromOutsideSendMethod) ";
    }

    private Party getDipository(String dipositoryName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService().getAllIdentities();

        for (PartyAndCertificate party : partyAndCertificates) {
            System.out.println("Party "+party.getParty());
            System.out.println("getName "+party.getParty().getName().getOrganisation());
            if (party.getName().getOrganisation().contains(dipositoryName)) {
                return party.getParty();
            }
        }
        return null;

    }
}
