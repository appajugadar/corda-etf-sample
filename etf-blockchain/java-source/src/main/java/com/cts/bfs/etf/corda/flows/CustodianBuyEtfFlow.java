package com.cts.bfs.etf.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.IdentityHelper;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;

import static com.cts.bfs.etf.corda.util.SerilazationHelper.getEtfTradeState;

@InitiatedBy(APBuyEtfFLow.class)
@InitiatingFlow
public class CustodianBuyEtfFlow extends FlowLogic<String> {

    private String dipositoryName;

    private FlowSession flowSession;

    public CustodianBuyEtfFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
        this.dipositoryName = "DEPOSITORY";
        System.out.println("Inside custodian called by " + flowSession.getCounterparty());
    }

    @Suspendable
    public String call() throws FlowException {
        System.out.print("The custodian start " + System.currentTimeMillis());
        System.out.println("**In call method for custodian flow");

        UntrustworthyData<EtfTradeState> inputFromAP = flowSession.receive(EtfTradeState.class);
        EtfTradeState etfTradeStateFromAp = getEtfTradeState(inputFromAP);
        System.out.println("**In call method for custodian flow");

        etfTradeStateFromAp.setFromParty(getServiceHub().getMyInfo().getLegalIdentities().get(0));
        etfTradeStateFromAp.setToParty(getDipository(dipositoryName));

        System.out.println("**In call method for custodian flow -->" + etfTradeStateFromAp);

        Party custodianParty = getDipository(dipositoryName);
        Party myParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

//
        UntrustworthyData<EtfTradeState> output = flowSession.sendAndReceive(EtfTradeState.class, etfTradeStateFromAp);
        /*EtfTradeState outputFromDepository = SerilazationHelper.getEtfTradeState(output);
        flowSession.send(outputFromDepository);

        final Command<EtfIssueContract.Commands.EtfBuyCommand> txCommand = new Command<>(new EtfIssueContract.Commands.EtfBuyCommand(),
                ImmutableList.of(custodianParty,myParty).stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(getNotary()).withItems(new StateAndContract(etfTradeStateFromAp, SELF_ISSUE_ETF_CONTRACT_ID),txCommand);

*//*
        final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(flowSession), CollectSignaturesFlow.Companion.tracker()));
        System.out.println("**In call method for custodian flow output from depository-->" + etfTradeStateFromAp);

*//*
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        txBuilder.verify(getServiceHub());
        subFlow(new FinalityFlow(partSignedTx));*/
        System.out.print("Sending back buyed etf to AP ");
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


    public static class CustodianSignatureAcceptorFlow extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public CustodianSignatureAcceptorFlow(FlowSession otherPartyFlow) {
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
