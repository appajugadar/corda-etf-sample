package net.corda.examples.obligation.flows;

import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.transactions.SignedTransaction;

@CordaSerializable
public class TransactionResult {
    private SignedTransaction stx;
    private AbstractParty recipient;

    public TransactionResult(SignedTransaction stx, AbstractParty recipient) {
        this.stx = stx;
        this.recipient = recipient;
    }
}
