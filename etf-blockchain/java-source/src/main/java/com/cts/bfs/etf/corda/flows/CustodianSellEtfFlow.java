package com.cts.bfs.etf.corda.flows;

import com.cts.bfs.etf.corda.contract.EtfIssueContract;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.EtfTradeResponse;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.IdentityHelper;
import com.cts.bfs.etf.corda.util.SerilazationHelper;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;

import java.util.stream.Collectors;

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;
import static com.cts.bfs.etf.corda.util.SerilazationHelper.getEtfTradeState;

@InitiatedBy(APSellEtfFLow.class)
@InitiatingFlow
public class CustodianSellEtfFlow extends FlowLogic<String> {

	private String dipositoryName;

	private FlowSession flowSession;

	public CustodianSellEtfFlow(FlowSession flowSession) {
		this.flowSession = flowSession;
		this.dipositoryName = "DEPOSITORY";
		System.out.println("Inside custodian called by " + flowSession.getCounterparty());
	}

	@Suspendable
	public String call() throws FlowException {
		System.out.print("The custodian sell flow started at " + System.currentTimeMillis());

		UntrustworthyData<EtfTradeState> inputFromAP = flowSession.receive(EtfTradeState.class);
		EtfTradeState etfTradeStateFromAp = getEtfTradeState(inputFromAP);

		System.out.println("Custodian sell flow received input from AP " + etfTradeStateFromAp);

		etfTradeStateFromAp.setFromParty(getServiceHub().getMyInfo().getLegalIdentities().get(0));
		etfTradeStateFromAp.setToParty(getDipository(dipositoryName));

		System.out.println("**In call method for custodian flow -->" + etfTradeStateFromAp);

		Party custodianParty = getDipository(dipositoryName);
		Party myParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

		System.out.print("Sending trade for execution at depository" );

		FlowSession toPartySession = initiateFlow(getDipository(dipositoryName));
		UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class,
				etfTradeStateFromAp);
		EtfTradeState outputFromDepository = SerilazationHelper.getEtfTradeState(output);
		System.out.print("Received trade from depository after execution "+outputFromDepository );
		flowSession.send(outputFromDepository);
		//
/*

		final Command<EtfIssueContract.Commands.EtfBuyCommand> txCommand = new Command<>(new EtfIssueContract.Commands.EtfBuyCommand(),
				ImmutableList.of(custodianParty,myParty).stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
		final TransactionBuilder txBuilder = new TransactionBuilder(getNotary()).withItems(new StateAndContract(outputFromDepository, SELF_ISSUE_ETF_CONTRACT_ID),txCommand);
		txBuilder.verify(getServiceHub());
		final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
		final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(toPartySession), CollectSignaturesFlow.Companion.tracker()));


		System.out.println("**In call method for custodian flow output from depository-->" + outputFromDepository);
		//
		subFlow(new FinalityFlow(fullySignedTx));
*/



		System.out.print("The custodian Sell end " + System.currentTimeMillis());

		return " SELL-CUSTODIAN-SUCCESS ";
	}

	protected Party getNotary() {
		return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
	}


	private Party getDipository(String dipositoryName) {
		Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
				.getAllIdentities();
		return IdentityHelper.getPartyWithName(partyAndCertificates, dipositoryName);
	}
}
