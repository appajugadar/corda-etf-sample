package com.cts.bfs.etf.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.bfs.etf.corda.contract.EtfIssueContract;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.TradeType;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.BalanceHelper;
import com.cts.bfs.etf.corda.util.IdentityHelper;
import com.cts.bfs.etf.corda.util.SerilazationHelper;
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

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;

@InitiatingFlow
@StartableByRPC
public class APBuyEtfFLow extends FlowLogic<SignedTransaction> {

    private EtfTradeRequest etfTradeRequest;
    private String custodianName;


    public APBuyEtfFLow(EtfTradeRequest etfTradeRequest, String custodianName) {
        this.etfTradeRequest = etfTradeRequest;
        this.custodianName = custodianName;
        System.out.println("The input is " + etfTradeRequest + " for custodian " + custodianName);
    }

    private Party getPartyByName(String custodianName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
                .getAllIdentities();
        return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
    }

    protected Party getNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }

    @Suspendable
    public SignedTransaction call() throws FlowException {
        Party custodianParty = getPartyByName(custodianName);
        Party myParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

        System.out.println("The APBuyFLow is initiated time " + System.currentTimeMillis());

        EtfTradeState etfTradeState = null;
        for (EtfTradeState etfTradeState1 : new BalanceHelper().getBalance(getServiceHub(), "ISSUECASH")) {
            etfTradeState1.setFromParty(myParty);
            etfTradeState1.setToParty(custodianParty);
            etfTradeState1.setTradeType(TradeType.BUY.name());
            etfTradeState1.setEtfName(etfTradeRequest.getEtfName());
            etfTradeState1.setQuantity(etfTradeRequest.getQuantity());
            etfTradeState = etfTradeState1;
            break;
        }

        if (etfTradeState == null) {
            return null;
        }

        System.out.println("The APBuyFLow : sending etf buy trade request to custodian : ");

        FlowSession toPartySession = initiateFlow(getCustodian(custodianName));
        UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class, etfTradeState);
        EtfTradeState outPutValue = SerilazationHelper.getEtfTradeState(output);

        System.out.println("The APBuyFLow : received etf trade : " + outPutValue);

        SignedTransaction tx = persistEtfTrade(etfTradeState);

        System.out.println("The APBuyFLow end " + System.currentTimeMillis());

        return tx;
    }

    private SignedTransaction persistEtfTrade(EtfTradeState etfTradeState) throws FlowException {
        final Command<EtfIssueContract.Commands.SelfIssueEtf> txCommand = new Command<>(new EtfIssueContract.Commands.SelfIssueEtf(),
                etfTradeState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));


        System.out.println("Inside APBuyFlow persist trade");
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeState, SELF_ISSUE_ETF_CONTRACT_ID), txCommand);

        System.out.println("APBuyFLow flow verify tx");
        // Stage 3. verify tx

        // Verify that the transaction is valid.
        System.out.println("Before verify TX");
        txBuilder.verify(getServiceHub());
        System.out.println("Verified TX");

        // step 4 Sign the transaction.
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        System.out.println("Inside APBuyFLow flow finalize tx");
        // Stage 6. finalise tx;
        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        System.out.println("Persisted etftrade in APBuy vault");
        return notarisedTx;
    }

    private Party getCustodian(String custodianName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
                .getAllIdentities();

        return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
    }

    //@InitiatedBy(APBuyEtfFLow.class)
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
