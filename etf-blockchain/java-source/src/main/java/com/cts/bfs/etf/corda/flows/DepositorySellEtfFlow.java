package com.cts.bfs.etf.corda.flows;

import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import com.cts.bfs.etf.corda.contract.EtfIssueContract;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.EtfTradeResponse;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.BalanceHelper;
import com.cts.bfs.etf.corda.util.SerilazationHelper;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;


@InitiatedBy(CustodianSellEtfFlow.class)
@InitiatingFlow
public class DepositorySellEtfFlow extends FlowLogic<SignedTransaction> {

    private FlowSession flowSession;

    public DepositorySellEtfFlow(FlowSession flowSession) {
        this.flowSession = flowSession;
    }


    @Suspendable
    public SignedTransaction call() throws FlowException {
        System.out.print("The depository start " + System.currentTimeMillis());
        System.out.println("**In call method for depository flow");

        UntrustworthyData<EtfTradeState> inputFromCustodian = flowSession.receive(EtfTradeState.class); // Input is Cash
        EtfTradeState etfTradeStateEtfInput = SerilazationHelper.getEtfTradeState(inputFromCustodian);
        etfTradeStateEtfInput.setTradeStatus("UNMATCHED");

//Persist in depositories vault
        SignedTransaction partSignedTx = persistEtfTradeStateToVault(etfTradeStateEtfInput);

        List<EtfTradeState> etfTradeStates = new BalanceHelper().getBalance(getServiceHub(), "BUY");

        if(etfTradeStates.size() > 0){
            //send back matched trade to buyer
            EtfTradeState etfSellState = etfTradeStates.get(0);

            etfTradeStateEtfInput.setTradeStatus("MATCHED");
            etfTradeStateEtfInput.setAmount(etfTradeStates.get(0).getAmount());
            partSignedTx =persistEtfTradeStateToVault(etfTradeStateEtfInput);

            etfSellState.setTradeStatus("MATCHED");
            persistEtfTradeStateToVault(etfSellState);
            flowSession.send(etfSellState);
            //call buyer flow
        }else{
            //wait to receive from seller flow
            UntrustworthyData<EtfTradeState> responseFromDepositorySellFlow = flowSession.receive(EtfTradeState.class);
            EtfTradeState etfTradeResponse = SerilazationHelper.getEtfTradeState(responseFromDepositorySellFlow);
            flowSession.send(etfTradeResponse);
        }

        getLogger().info("completed depository buy flow");
        return partSignedTx;
    }

    private SignedTransaction persistEtfTradeStateToVault(EtfTradeState etfTradeStateCashInput) throws FlowException {
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final Command<EtfIssueContract.Commands.SelfIssueEtf> txCommand = new Command<>(new EtfIssueContract.Commands.SelfIssueEtf(),
                etfTradeStateCashInput.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeStateCashInput, SELF_ISSUE_ETF_CONTRACT_ID), txCommand);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        return  subFlow(new FinalityFlow(partSignedTx));
    }

}