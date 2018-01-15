package com.cts.bfs.etf.corda.util;

import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.EtfTradeResponse;

import net.corda.core.flows.FlowException;
import net.corda.core.utilities.UntrustworthyData;

public class SerilazationHelper {

    public static EtfTradeResponse getEtfTradeResponse(UntrustworthyData<EtfTradeResponse> output) throws FlowException {
        return output.unwrap(new UntrustworthyData.Validator<EtfTradeResponse, EtfTradeResponse>() {
            @Override
            public EtfTradeResponse validate(EtfTradeResponse data) throws FlowException {
                return data;
            }
        });
    }

    public static EtfTradeRequest getEtfTradeRequest(UntrustworthyData<EtfTradeRequest> inputFromAP) throws FlowException {
        return inputFromAP.unwrap(new UntrustworthyData.Validator<EtfTradeRequest, EtfTradeRequest>() {
            @Override
            public EtfTradeRequest validate(EtfTradeRequest data) throws FlowException {
                System.out.println("**In validate method for custodian flow received data "+data);
                return data;
            }
        });
    }

}
