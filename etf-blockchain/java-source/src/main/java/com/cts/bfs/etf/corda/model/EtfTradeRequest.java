package com.cts.bfs.etf.corda.model;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class EtfTradeRequest {

	private String toPartyName;
	private String etfName;
	private Long quantity;
	private Long amount;
	private TradeType tradeType;

    public EtfTradeRequest(String toPartyName, String etfName, Long quantity, Long amount, TradeType tradeType) {
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

    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public void setTradeType(TradeType tradeType) {
        this.tradeType = tradeType;
    }
}
