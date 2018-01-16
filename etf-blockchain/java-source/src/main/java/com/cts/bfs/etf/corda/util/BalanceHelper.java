package com.cts.bfs.etf.corda.util;

import com.cts.bfs.etf.corda.state.EtfTradeState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.node.ServiceHub;

import java.util.ArrayList;
import java.util.List;

public class BalanceHelper {

   public List<EtfTradeState> getBalance(ServiceHub serviceHub, String type){
        List<StateAndRef<EtfTradeState>> etfTradeStatesQueryResp = serviceHub.getVaultService().queryBy(EtfTradeState.class).getStates();
        List<EtfTradeState> etfTradeStates = new ArrayList<>();
        for (StateAndRef<EtfTradeState> stateAndRef : etfTradeStatesQueryResp
                ) {
            EtfTradeState etfTradeState = stateAndRef.getState().getData();
            if(etfTradeState.getTradeType().equals(type)){
                etfTradeStates.add(stateAndRef.getState().getData());
            }
        }
        return etfTradeStates;
    }
}

