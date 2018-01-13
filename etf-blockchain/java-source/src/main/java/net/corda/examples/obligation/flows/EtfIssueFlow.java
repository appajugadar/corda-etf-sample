package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.confidential.SwapIdentitiesFlow;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.examples.obligation.*;
import net.corda.finance.flows.AbstractCashFlow;

import java.security.PublicKey;
import java.time.Duration;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
@InitiatingFlow
@StartableByRPC
public class EtfIssueFlow extends AbstractIssueFlow {

    private EtfAsset etfAsset;
    private OpaqueBytes issuerBankPartyRef;
    private Party lender;
    private final Boolean anonymous;

    private  TransactionResult result;

    public EtfIssueFlow(EtfAsset etfAsset, OpaqueBytes issuerBankPartyRef, Party lender,Boolean anonymous) {
        super();
        this.etfAsset = etfAsset;
        this.issuerBankPartyRef = issuerBankPartyRef;
        this.lender = lender;
        this.anonymous = anonymous;
    }

    private final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
    private final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    private final ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counterparty signature.") {
        @Override public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    private final ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
        @Override public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    private final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING
    );


    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Step 1. Initialisation.
        progressTracker.setCurrentStep(INITIALISING);
        final EtfObligation obligation = createObligation();
        final PublicKey ourSigningKey = obligation.getBorrower().getOwningKey();

        // Step 2. Building.
        progressTracker.setCurrentStep(BUILDING);
        final List<PublicKey> requiredSigners = obligation.getParticipantKeys();

        final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())
                .addOutputState(obligation, EtfContract.OBLIGATION_CONTRACT_ID)
                .addCommand(new EtfContract.Commands.Issue(), requiredSigners)
                .setTimeWindow(getServiceHub().getClock().instant(), Duration.ofSeconds(30));

        // Step 3. Sign the transaction.
        progressTracker.setCurrentStep(SIGNING);
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, ourSigningKey);

        // Step 4. Get the counter-party signature.
        progressTracker.setCurrentStep(COLLECTING);
        final FlowSession lenderFlow = initiateFlow(lender);
        final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                ptx,
                ImmutableSet.of(lenderFlow),
                ImmutableList.of(ourSigningKey),
                COLLECTING.childProgressTracker())
        );

        // Step 5. Finalise the transaction.
        progressTracker.setCurrentStep(FINALISING);
        return subFlow(new FinalityFlow(stx, FINALISING.childProgressTracker()));
    }

    @Suspendable
    private EtfObligation createObligation() throws FlowException {
        if (anonymous) {
            final HashMap<Party, AnonymousParty> txKeys = subFlow(new SwapIdentitiesFlow(lender));

            if (txKeys.size() != 2) {
                throw new IllegalStateException("Something went wrong when generating confidential identities.");
            } else if (!txKeys.containsKey(getOurIdentity())) {
                throw new FlowException("Couldn't create our conf. identity.");
            } else if (!txKeys.containsKey(lender)) {
                throw new FlowException("Couldn't create lender's conf. identity.");
            }

            final AbstractParty anonymousLender = txKeys.get(lender);
            final AbstractParty anonymousMe = txKeys.get(getOurIdentity());

            return new EtfObligation(etfAsset , anonymousLender, anonymousMe , new UniqueIdentifier());
        } else {
            return new EtfObligation(etfAsset, lender, getOurIdentity(), new UniqueIdentifier());
        }
    }

    @InitiatedBy(EtfIssueFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx = subFlow(new ObligationBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return waitForLedgerCommit(stx.getId());
        }
    }
}
