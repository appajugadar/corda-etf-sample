package com.cts.bfs.etf.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.bfs.etf.corda.contract.CashIssueContract;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Currency;
import java.util.stream.Collectors;

import static com.cts.bfs.etf.corda.contract.CashIssueContract.SELF_ISSUE_CASH_CONTRACT_ID;

@StartableByRPC
@InitiatingFlow
public class CashIssueFlow extends AbstractIssueFlow {

    private static final Logger logger = LoggerFactory.getLogger(CashIssueFlow.class);
    private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };
    private final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING, VERIFYING_TRANSACTION, BUILDING, SIGNING, GATHERING_SIGS, FINALISING_TRANSACTION
    );
    private Amount<Currency> amount;

    public CashIssueFlow(Amount<Currency> amount) {
        super();
        this.amount = amount;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        logger.info("Inside CashIssue flow call init");
        // Step 1. Initialisation.
        progressTracker.setCurrentStep(INITIALISING);
        EtfTradeState etfTradeState = new EtfTradeState(
                getServiceHub().getMyInfo().getLegalIdentities().get(0),
                getServiceHub().getMyInfo().getLegalIdentities().get(0),
                null,
                null,
                amount, "ISSUECASH",
                new UniqueIdentifier());

        logger.info("etfTradeState -->> " + etfTradeState);

        final Command<CashIssueContract.Commands.SelfIssueCash> txCommand = new Command<>(new CashIssueContract.Commands.SelfIssueCash(),
                etfTradeState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));

        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);


        logger.info("Inside EtfIssue flow BUILDING tx");
        // Step 2. build tx.
        progressTracker.setCurrentStep(BUILDING);
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeState, SELF_ISSUE_CASH_CONTRACT_ID), txCommand);

        logger.info("Inside EtfIssue flow verify tx");
        // Stage 3. verify tx
        progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
        // Verify that the transaction is valid.
        getLogger().info("Before verify TX");
        txBuilder.verify(getServiceHub());

        getLogger().info("Verified TX");

        // step 4 Sign the transaction.
        progressTracker.setCurrentStep(SIGNING);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

        getLogger().info("Signed TX");

        System.out.println("Inside EtfIssue flow finalize tx");
        // Stage 6. finalise tx;
        progressTracker.setCurrentStep(FINALISING_TRANSACTION);

        // Notarise and record the transaction in both parties' vaults.
        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        getLogger().info("Notarised TX");
        return notarisedTx;
    }

    @InitiatedBy(CashIssueFlow.class)
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
                    /*requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an IOU transaction.", output instanceof IOUState);
                        IOUState iou = (IOUState) output;
                        require.using("I won't accept IOUs with a value over 100.", iou.getValue() <= 100);
                        return null;
                    });*/
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }

}
