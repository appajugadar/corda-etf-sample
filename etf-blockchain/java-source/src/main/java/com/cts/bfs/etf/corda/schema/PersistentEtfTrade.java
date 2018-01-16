package com.cts.bfs.etf.corda.schema;

import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ETF_TRADE_STATES")
public class PersistentEtfTrade extends PersistentState {

    @Column(name = "from_party")
    private final String fromParty;

    @Column(name = "to_party")
    private final String toParty;

    @Column(name = "etf_name")
    private final String etfName;

    @Column(name = "quantity")
    private final int quantity;

    @Column(name = "amount")
    private final int amount;

    @Column(name = "trade_type")
    private final String tradeType;

    @Column(name = "linear_id")
    private final UUID linearId;

    public PersistentEtfTrade(String fromParty, String toParty, String etfName, int quantity,
                              int amount, String tradeType, UUID linearId) {
        System.out.println("fromParty "+fromParty+" toParty "+toParty+" etfName "+etfName+" quantity "+quantity+" amount "+amount+" tradeType "+tradeType+" linearId "+linearId);
        this.fromParty = fromParty;
        this.toParty = toParty;
        this.etfName = etfName;
        this.quantity = quantity;
        this.amount = amount;
        this.tradeType = tradeType;
        this.linearId = linearId;
    }

    public String getFromParty() {
        return fromParty;
    }

    public String getToParty() {
        return toParty;
    }

    public String getEtfName() {
        return etfName;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getAmount() {
        return amount;
    }

    public String getTradeType() {
        return tradeType;
    }

    public UUID getLinearId() {
        return linearId;
    }
}
