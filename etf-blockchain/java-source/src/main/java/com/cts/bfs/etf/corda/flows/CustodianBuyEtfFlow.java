package com.cts.bfs.etf.corda.flows;

import static com.cts.bfs.etf.corda.util.SerilazationHelper.getEtfTradeRequest;
import static com.cts.bfs.etf.corda.util.SerilazationHelper.getEtfTradeState;

import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.EtfTradeResponse;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.IdentityHelper;
import com.cts.bfs.etf.corda.util.SerilazationHelper;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.utilities.UntrustworthyData;

@InitiatedBy(APBuyEtfFLow.class)
@InitiatingFlow
public class CustodianBuyEtfFlow extends FlowLogic<String> {

	private String dipositoryName;

	private FlowSession flowSession;

	public CustodianBuyEtfFlow(FlowSession flowSession) {
		this.flowSession = flowSession;
		this.dipositoryName = "DEPOSITORY";

		System.out.println("**Inside custodian called by " + flowSession.getCounterparty());
	}

	@Suspendable
	public String call() throws FlowException {
		System.out.print("The custodian start " + System.currentTimeMillis());
		System.out.println("**In call method for custodian flow");

		UntrustworthyData<EtfTradeState> inputFromAP = flowSession.receive(EtfTradeState.class);

		EtfTradeState etfTradeStateFromAp = getEtfTradeState(inputFromAP);
        etfTradeStateFromAp.setFromParty(getServiceHub().getMyInfo().getLegalIdentities().get(0));
        etfTradeStateFromAp.setToParty(getDipository(dipositoryName));

        System.out.println("**In call method for custodian flow -->" + etfTradeStateFromAp);

		FlowSession toPartySession = initiateFlow(getDipository(dipositoryName));
		UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class,
				etfTradeStateFromAp);
        EtfTradeState outputFromDepository = SerilazationHelper.getEtfTradeState(output);

		System.out.println("**In call method for custodian flow output from depository-->" + outputFromDepository);
		flowSession.send(outputFromDepository);

		System.out.print("The custodian end " + System.currentTimeMillis());

		return " BUY-CUSTODIAN-SUCCESS ";
	}

	private Party getDipository(String dipositoryName) {
		Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
				.getAllIdentities();
		return IdentityHelper.getPartyWithName(partyAndCertificates, dipositoryName);
	}
}
