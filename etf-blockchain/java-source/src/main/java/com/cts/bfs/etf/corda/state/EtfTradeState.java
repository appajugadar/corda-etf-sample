package com.cts.bfs.etf.corda.state;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;
import java.util.List;

public class EtfTradeState implements LinearState {

    private final UniqueIdentifier linearId;
    private Party fromParty;
    private Party toParty;
    private String etfName;
    private Long quantity;
    private Amount<Currency> amount;
    private String tradeType;
    private String tradeStatus;

    public EtfTradeState(Party fromParty, Party toParty, String etfName, Long quantity, Amount<Currency> amount, String tradeType, UniqueIdentifier linearId) {
        this.fromParty = fromParty;
        this.toParty = toParty;
        this.etfName = etfName;
        this.quantity = quantity;
        this.amount = amount;
        this.tradeType = tradeType;
        this.linearId = linearId;
    }

    public Party getFromParty() {
        return fromParty;
    }

    public void setFromParty(Party fromParty) {
        this.fromParty = fromParty;
    }

    public Party getToParty() {
        return toParty;
    }

    public void setToParty(Party toParty) {
        this.toParty = toParty;
    }

    public String getEtfName() {
        return etfName;
    }

    public void setEtfName(String etfName) {
        this.etfName = etfName;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Amount<Currency> getAmount() {
        return amount;
    }

    public void setAmount(Amount<Currency> amount) {
        this.amount = amount;
    }

    public String getTradeStatus() {
        return tradeStatus;
    }

    public void setTradeStatus(String tradeStatus) {
        this.tradeStatus = tradeStatus;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Lists.newArrayList(Sets.newHashSet(fromParty, toParty));
    }

    @Override
    public String toString() {
        return "EtfTradeState{" +
                "fromParty=" + fromParty +
                ", toParty=" + toParty +
                ", etfName='" + etfName + '\'' +
                ", quantity=" + quantity +
                ", amount=" + amount +
                ", tradeType=" + tradeType +
                ", linearId=" + linearId +
                ", participants" + getParticipants() +
                '}';
    }
}
