package com.cts.bfs.etf.corda.flows;

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;
import static com.cts.bfs.etf.corda.util.SerilazationHelper.getEtfTradeRequest;
import static com.cts.bfs.etf.corda.util.SerilazationHelper.getEtfTradeState;

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
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;

import java.util.stream.Collectors;

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

        Party custodianParty = getDipository(dipositoryName);
        Party myParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

        final Command<EtfIssueContract.Commands.EtfBuyCommand> txCommand = new Command<>(new EtfIssueContract.Commands.EtfBuyCommand(),
                ImmutableList.of(custodianParty,myParty).stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final Party notary = getNotary();
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeStateFromAp, SELF_ISSUE_ETF_CONTRACT_ID), txCommand);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);


        FlowSession toPartySession = initiateFlow(getDipository(dipositoryName));
		UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class,
				etfTradeStateFromAp);
        EtfTradeState outputFromDepository = SerilazationHelper.getEtfTradeState(output);

		System.out.println("**In call method for custodian flow output from depository-->" + outputFromDepository);
		flowSession.send(outputFromDepository);

       final SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(toPartySession), CollectSignaturesFlow.Companion.tracker()));

        System.out.print("The APSellFLow end " + System.currentTimeMillis());
        SignedTransaction tx =  subFlow(new FinalityFlow(fullySignedTx));
        System.out.print("The custodian end " + System.currentTimeMillis());

		return " BUY-CUSTODIAN-SUCCESS ";
	}

	private Party getDipository(String dipositoryName) {
		Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
				.getAllIdentities();
		return IdentityHelper.getPartyWithName(partyAndCertificates, dipositoryName);
	}


    protected Party getNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }


    @InitiatedBy(CustodianBuyEtfFlow.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    System.out.print("Inside check transaction for self issue etf");
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
