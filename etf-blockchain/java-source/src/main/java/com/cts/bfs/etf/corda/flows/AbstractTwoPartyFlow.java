package com.cts.bfs.etf.corda.flows;

import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.IdentityHelper;
import com.cts.bfs.etf.corda.util.SerilazationHelper;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractTwoPartyFlow extends FlowLogic<SignedTransaction> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractTwoPartyFlow.class);
    protected final ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
    protected final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
    protected final ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
    protected final ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    protected final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };
    protected final ProgressTracker progressTracker = new ProgressTracker(
            INITIALISING, VERIFYING_TRANSACTION, BUILDING, SIGNING, GATHERING_SIGS, FINALISING_TRANSACTION
    );
    protected EtfTradeState etfTradeState;
    Party otherParty;
    Party myself;
    FlowSession flowSession;
    public AbstractTwoPartyFlow(String otherPartyName) {
        this.otherParty = getPartyByName(otherPartyName);
        this.myself = getServiceHub().getMyInfo().getLegalIdentities().get(0);
    }

    public AbstractTwoPartyFlow(FlowSession flowSession) {
        this.otherParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
        this.flowSession = flowSession;
        this.myself = getServiceHub().getMyInfo().getLegalIdentities().get(0);
    }

    public AbstractTwoPartyFlow() {
        this.otherParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
        this.myself = getServiceHub().getMyInfo().getLegalIdentities().get(0);
    }

    public FlowSession getFlowSession() {
        return flowSession;
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    protected abstract EtfTradeState getEtfTradeState();


    protected abstract void init();

    protected abstract TransactionBuilder buildTransaction();

    protected void verifyTransacton(TransactionBuilder txBuilder) throws FlowException {
        txBuilder.verify(getServiceHub());
    }


    protected SignedTransaction signTransaction(TransactionBuilder txBuilder) {
        return getServiceHub().signInitialTransaction(txBuilder);
    }

    protected SignedTransaction finalizeTransaction(SignedTransaction partSignedTx) throws FlowException {
        SignedTransaction notarisedTx = subFlow(new FinalityFlow(partSignedTx));
        return notarisedTx;
    }

    abstract protected void respondToInitiator(EtfTradeState etfTradeState);

    protected EtfTradeState callNextPartyFlow(EtfTradeState etfTradeState) throws FlowException {
        FlowSession toPartySession = initiateFlow(otherParty);
        UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class, etfTradeState);
        EtfTradeState outPutValue = SerilazationHelper.getEtfTradeState(output);
        return outPutValue;
    }

    protected abstract void obtainInput() throws FlowException;

    public Party getOtherParty() {
        return otherParty;
    }

    public void setOtherParty(Party otherParty) {
        this.otherParty = otherParty;
    }

    public Party getMyself() {
        return myself;
    }

    public void setMyself(Party myself) {
        this.myself = myself;
    }

    private Party getPartyByName(String custodianName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
                .getAllIdentities();
        return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
    }

    protected List<Party> getParticipants() {
        return ImmutableList.of(myself, otherParty);
    }

    protected Party getNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }
}
