package com.cts.bfs.etf.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.cts.bfs.etf.corda.contract.EtfIssueContract;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.cts.bfs.etf.corda.util.SerilazationHelper;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.UntrustworthyData;

import java.util.stream.Collectors;

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;


@InitiatedBy(CustodianSellEtfFlow.class)
@InitiatingFlow
public class DepositorySellEtfFlow extends AbstractDepositoryFlow {

    private FlowSession flowSession;

    public DepositorySellEtfFlow(FlowSession flowSession) {
        super(flowSession);
        this.flowSession = flowSession;
    }


    @Suspendable
    public String call() throws FlowException {
        System.out.println("The DepositorySellEtfFlow start " + System.currentTimeMillis());

        UntrustworthyData<EtfTradeState> inputFromCustodian = flowSession.receive(EtfTradeState.class); // Input is Cash
        EtfTradeState etfTradeStateCashInput = SerilazationHelper.getEtfTradeState(inputFromCustodian);
        etfTradeStateCashInput.setTradeStatus("UNMATCHED");
        System.out.println("DepositoryBuyEtfFlow got input from custodian " + etfTradeStateCashInput);

        if (etfTradeBuyRequests.size() > 0) {
            EtfTradeState etfSellState = (EtfTradeState) etfTradeBuyRequests.toArray()[0];
            etfTradeStateCashInput.setTradeStatus("MATCHED");
            etfTradeStateCashInput.setEtfName(etfSellState.getEtfName());
            etfTradeStateCashInput.setQuantity(etfSellState.getQuantity());
            etfTradeBuyRequests.remove(etfTradeStateCashInput);
            etfSellState.setTradeStatus("MATCHED");
            //	persistEtfTradeStateToVault(etfSellState);
            System.out.println("Sending back response to custodian as match found in vault");
            flowSession.send(etfSellState);
        } else {
            //wait to receive from seller flow
            etfTradeBuyRequests.add(etfTradeStateCashInput);
            UntrustworthyData<EtfTradeState> responseFromDepositorySellFlow = flowSession.receive(EtfTradeState.class);
            EtfTradeState etfTradeResponse = SerilazationHelper.getEtfTradeState(responseFromDepositorySellFlow);
            System.out.println("Sending back response to Cust1 as trade received from seller");
            etfTradeBuyRequests.remove(etfTradeStateCashInput);
            flowSession.send(etfTradeResponse);
        }
        System.out.println("The DepositorySellEtfFlow end ");
        return "SUCCESS";
    }

    private SignedTransaction persistEtfTradeStateToVault(EtfTradeState etfTradeStateCashInput) throws FlowException {
        final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        final Command<EtfIssueContract.Commands.SelfIssueEtf> txCommand = new Command<>(new EtfIssueContract.Commands.SelfIssueEtf(),
                etfTradeStateCashInput.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
        final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                .withItems(new StateAndContract(etfTradeStateCashInput, SELF_ISSUE_ETF_CONTRACT_ID), txCommand);
        final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
        return subFlow(new FinalityFlow(partSignedTx));
    }

}