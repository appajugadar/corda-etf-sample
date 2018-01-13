package net.corda.examples.obligation;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import static net.corda.core.utilities.EncodingUtils.toBase58String;

@CordaSerializable
public class EtfObligation implements LinearState {

    private final EtfAsset etfAsset;
    private final AbstractParty lender;
    private final AbstractParty borrower;

    private final Amount<Currency> amount;

    private final UniqueIdentifier linearId;

    public EtfObligation(EtfAsset etfAsset, AbstractParty lender, AbstractParty borrower, UniqueIdentifier linearId) {
        this.etfAsset = etfAsset;
        this.amount = null;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }

    public EtfObligation(Amount<Currency> amount, AbstractParty lender, AbstractParty borrower, UniqueIdentifier linearId) {
        this.amount = amount;
        this.etfAsset = null;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = linearId;
    }


    public EtfObligation(EtfAsset eftAsset, AbstractParty lender, AbstractParty borrower, Amount<Currency> amount) {
        this.etfAsset = eftAsset;
        this.amount = amount;
        this.lender = lender;
        this.borrower = borrower;
        this.linearId = new UniqueIdentifier();
    }

    public EtfAsset getEtfAsset() {
        return etfAsset;
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public AbstractParty getLender() {
        return lender;
    }

    public AbstractParty getBorrower() {
        return borrower;
    }


    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(lender, borrower);
    }

    public EtfObligation pay(Amount<Currency> amountToPay) {
        return new EtfObligation(
                this.etfAsset,
                this.lender,
                this.borrower,
                this.linearId
        );
    }

    public EtfObligation pay(EtfAsset etfAsset) {
        return new EtfObligation(
                this.amount,
                this.lender,
                this.borrower,
                this.linearId
        );
    }
    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        String lenderString;
        if (this.lender instanceof Party) {
            lenderString = ((Party) lender).getName().getOrganisation();
        } else {
            PublicKey lenderKey = this.lender.getOwningKey();
            lenderString = toBase58String(lenderKey);
        }

        String borrowerString;
        if (this.borrower instanceof Party) {
            borrowerString = ((Party) borrower).getName().getOrganisation();
        } else {
            PublicKey borrowerKey = this.borrower.getOwningKey();
            borrowerString = toBase58String(borrowerKey);
        }

        return String.format("EtfObligation(%s): %s owes %s %s and has returned stock %s.",
                this.linearId, borrowerString, lenderString, this.amount, this.etfAsset);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EtfObligation)) {
            return false;
        }
        EtfObligation other = (EtfObligation) obj;
        return amount.equals(other.getAmount())
                && lender.equals(other.getLender())
                && borrower.equals(other.getBorrower())
                && etfAsset.equals(other.getEtfAsset())
                && linearId.equals(other.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, lender, borrower, etfAsset, linearId);
    }
}