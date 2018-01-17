package com.cts.bfs.etf.corda.flows.tracker;

import net.corda.core.flows.CollectSignaturesFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.utilities.ProgressTracker;

public class EtfProgressTracker {

    public final static ProgressTracker.Step INITIALISING = new ProgressTracker.Step("Performing initial steps.");
    public final static ProgressTracker.Step BUILDING = new ProgressTracker.Step("Performing initial steps.");
    public final static ProgressTracker.Step SIGNING = new ProgressTracker.Step("Signing transaction.");
    public final static ProgressTracker.Step COLLECTING = new ProgressTracker.Step("Collecting counterparty signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    public final static ProgressTracker.Step FINALISING = new ProgressTracker.Step("Finalising transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };


}
