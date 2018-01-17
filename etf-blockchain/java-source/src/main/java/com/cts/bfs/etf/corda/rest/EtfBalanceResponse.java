package com.cts.bfs.etf.corda.rest;

import com.cts.bfs.etf.corda.state.EtfTradeState;

import java.util.List;

public class EtfBalanceResponse {

    String etfName;
    String quantity;
    List<EtfTradeState> etfTradeStates;

    public EtfBalanceResponse(List<EtfTradeState> etfTradeStates) {
        this.etfTradeStates = etfTradeStates;
    }

    public List<EtfTradeState> getEtfTradeStates() {
        return etfTradeStates;
    }

    public void setEtfTradeStates(List<EtfTradeState> etfTradeStates) {
        this.etfTradeStates = etfTradeStates;
    }
}
