package net.corda.examples.obligation.flows;

import co.paralleluniverse.fibers.Suspendable;

import com.cts.bfs.etf.corda.flows.tracker.EtfProgressTracker;
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
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.examples.obligation.EtfAsset;
import net.corda.examples.obligation.EtfContract;
import net.corda.examples.obligation.EtfObligation;

import java.security.PublicKey;
import java.time.Duration;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class EtfBuyInitiatorFlow extends AbstractIssueFlow {

    private final ProgressTracker progressTracker = new ProgressTracker(
            EtfProgressTracker.INITIALISING, EtfProgressTracker.BUILDING, EtfProgressTracker.SIGNING, EtfProgressTracker.COLLECTING, EtfProgressTracker.FINALISING
    );

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    private Party fromParty;// AP

    private Party toParty;

    private EtfAsset etfAsset;

    private Amount<Currency> amount;

    private String partyRole;

    private boolean anonymous;

    public String getPartyRole() {
        return partyRole;
    }

    public EtfBuyInitiatorFlow (AbstractParty fromParty, AbstractParty toParty, EtfAsset etfAsset) {

        //TODO
        if(this.getOurIdentity().getName().getCommonName().toUpperCase().contains("AP")){
            partyRole="AP";
        }

        if(this.getOurIdentity().getName().getCommonName().toUpperCase().contains("CST")){
            partyRole="CST";
        }

        if(this.getOurIdentity().getName().getCommonName().toUpperCase().contains("DTCC")){
            partyRole="CLR";
        }

        this.etfAsset = etfAsset;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // Step 1. Initialisation.
        progressTracker.setCurrentStep(EtfProgressTracker.INITIALISING);

        EtfObligation obligation = createObligation();

     //   final Amount<Currency> amount = new Amount<Currency>(100,Currency.getInstance("GBP")); //TODO obtain amount from RPC

        final PublicKey ourSigningKey = obligation.getBorrower().getOwningKey();

        // Step 2. Building.
        progressTracker.setCurrentStep(EtfProgressTracker.BUILDING);
        final List<PublicKey> requiredSigners = obligation.getParticipantKeys();

        final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())
                .addOutputState(obligation, EtfContract.OBLIGATION_CONTRACT_ID)
                .addCommand(new EtfContract.Commands.BuyProposal(), requiredSigners)
                .setTimeWindow(getServiceHub().getClock().instant(), Duration.ofSeconds(30));

        // Step 3. Sign the transaction.
        progressTracker.setCurrentStep(EtfProgressTracker.SIGNING);
        final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, ourSigningKey);

        // Step 4. Get the counter-party signature.
        progressTracker.setCurrentStep(EtfProgressTracker.COLLECTING);
        final FlowSession lenderFlow = initiateFlow(toParty);
        final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                ptx,
                ImmutableSet.of(lenderFlow),
                ImmutableList.of(ourSigningKey),
                CollectSignaturesFlow.tracker())
        );


        //Step 5. invoke to party for next processing
        FlowSession toPartySession = initiateFlow(toParty);
        UntrustworthyData<EtfAsset> receivedData = toPartySession.sendAndReceive(EtfAsset.class, etfAsset);

        //EtfAsset receivedAsset = receivedData.getFromUntrustedWorld();

        // Step 5. Finalise the transaction.
        progressTracker.setCurrentStep(EtfProgressTracker.FINALISING);
        return subFlow(new FinalityFlow(stx, EtfProgressTracker.FINALISING.childProgressTracker()));

    }

    @Suspendable
    private EtfObligation createObligation() throws FlowException {
//
        if (anonymous) {
            final HashMap<Party, AnonymousParty> txKeys = subFlow(new SwapIdentitiesFlow(toParty));

            if (txKeys.size() != 2) {
                throw new IllegalStateException("Something went wrong when generating confidential identities.");
            } else if (!txKeys.containsKey(getOurIdentity())) {
                throw new FlowException("Couldn't create our conf. identity.");
            } else if (!txKeys.containsKey(toParty)) {
                throw new FlowException("Couldn't create lender's conf. identity.");
            }

            final AbstractParty anonymousLender = txKeys.get(toParty);
            final AbstractParty anonymousMe = txKeys.get(getOurIdentity());

            return new EtfObligation(etfAsset , anonymousLender, anonymousMe , new UniqueIdentifier());
        } else {
            return new EtfObligation(etfAsset, toParty, getOurIdentity(), new UniqueIdentifier());
        }
    }

    @InitiatedBy(EtfBuyInitiatorFlow.class)
    public static class Responder extends FlowLogic<SignedTransaction> {
        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx = subFlow(new ObligationBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            //TODO how to send back etfAsset received from ToParty to fromParty

            return waitForLedgerCommit(stx.getId());
        }
    }
}
