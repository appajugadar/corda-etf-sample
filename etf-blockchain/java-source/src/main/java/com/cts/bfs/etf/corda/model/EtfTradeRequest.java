package com.cts.bfs.etf.corda.model;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class EtfTradeRequest {

	private String toPartyName;
	private String etfName;
	private int quantity;
	private int amount;
	private TradeType tradeType;

    public EtfTradeRequest(String toPartyName, String etfName, int quantity, int amount, TradeType tradeType) {
        this.toPartyName = toPartyName;
        this.etfName = etfName;
        this.quantity = quantity;
        this.amount = amount;
        this.tradeType = tradeType;
    }

    public String getToPartyName() {
        return toPartyName;
    }

    public void setToPartyName(String toPartyName) {
        this.toPartyName = toPartyName;
    }

    public String getEtfName() {
        return etfName;
    }

    public void setEtfName(String etfName) {
        this.etfName = etfName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }
}
