package com.cts.bfs.etf.corda.flows;

import com.cts.bfs.etf.corda.contract.EtfIssueContract;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.EtfTradeResponse;
import com.cts.bfs.etf.corda.model.TradeType;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.BalanceHelper;
import com.cts.bfs.etf.corda.util.IdentityHelper;
import com.cts.bfs.etf.corda.util.SerilazationHelper;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;

import java.util.Currency;
import java.util.stream.Collectors;

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;

@InitiatingFlow
@StartableByRPC
public class APSellEtfFLow extends FlowLogic<String> {

    private EtfTradeRequest etfTradeRequest;

    private String custodianName;

    public APSellEtfFLow(EtfTradeRequest etfTradeRequest, String custodianName) {
        this.etfTradeRequest = etfTradeRequest;
        this.custodianName = custodianName;
        System.out.println("The input is "+etfTradeRequest +" for custodian "+custodianName);
    }

    protected Party getNotary() {
        return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
    }

    @Suspendable
    public String call() throws FlowException {
        Party custodianParty = getCustodian(custodianName);
        Party myParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

        System.out.println("The APBuyFLow is initiated time " + System.currentTimeMillis());
        EtfTradeState etfTradeState =null;
        for(EtfTradeState etfTradeState1: new BalanceHelper().getBalance(getServiceHub(),"ISSUEETF")) {
            etfTradeState1.setFromParty(myParty);
            etfTradeState1.setToParty(custodianParty);
            etfTradeState1.setTradeType(TradeType.SELL.name());
            etfTradeState1.setAmount(new Amount<Currency>(etfTradeRequest.getAmount(), Currency.getInstance("GBP")));
            etfTradeState1.setQuantity(etfTradeRequest.getQuantity());
            etfTradeState = etfTradeState1;
            break;
        }

        if(etfTradeState==null){
            return "FAILED TO SELL AS NO ETF IN VAULT";
        }

        final Command<EtfIssueContract.Commands.EtfBuyCommand> txCommand = new Command<>(new EtfIssueContract.Commands.EtfBuyCommand(),
                ImmutableList.of(custodianParty,myParty).stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final Party notary = getNotary();
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeState, SELF_ISSUE_ETF_CONTRACT_ID), txCommand);

        FlowSession toPartySession = initiateFlow(getCustodian(custodianName));
        UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class, etfTradeState);

        EtfTradeState outPutValue = SerilazationHelper.getEtfTradeState(output);

        System.out.println("The APSellFLow : output from custodian : " + outPutValue);
        System.out.print("The APSellFLow end " + System.currentTimeMillis());

        return "SUCCESS";
    }

    private Party getCustodian(String custodianName) {
        Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
                .getAllIdentities();

        return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
    }

}

