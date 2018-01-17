package com.cts.bfs.etf.corda.flows;

import com.cts.bfs.etf.corda.contract.CashIssueContract;
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
import com.google.common.collect.Sets;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.identity.PartyAndCertificate;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;

import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static com.cts.bfs.etf.corda.contract.EtfIssueContract.SELF_ISSUE_ETF_CONTRACT_ID;

@InitiatingFlow
@StartableByRPC
public class APBuyEtfFLow extends FlowLogic<String> {

	private EtfTradeRequest etfTradeRequest;
	private String custodianName;


	public APBuyEtfFLow(EtfTradeRequest etfTradeRequest, String custodianName) {
		this.etfTradeRequest = etfTradeRequest;
		this.custodianName = custodianName;
		System.out.println("The input is " + etfTradeRequest + " for custodian " + custodianName);
	}

	private Party getPartyByName(String custodianName) {
		Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
				.getAllIdentities();
		return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
	}

	protected Party getNotary() {
		return getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
	}

	@Suspendable
	public String call() throws FlowException {
		Party custodianParty = getPartyByName(custodianName);
		Party myParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);

		System.out.println("The APBuyFLow is initiated time " + System.currentTimeMillis());

		EtfTradeState etfTradeState =null;
		for(EtfTradeState etfTradeState1: new BalanceHelper().getBalance(getServiceHub(),"ISSUECASH")) {
			etfTradeState1.setFromParty(myParty);
			etfTradeState1.setToParty(custodianParty);
			etfTradeState1.setTradeType(TradeType.BUY.name());
			etfTradeState1.setEtfName(etfTradeRequest.getEtfName());
			etfTradeState1.setQuantity(etfTradeRequest.getQuantity());
			etfTradeState = etfTradeState1;
			break;
		}

		if(etfTradeState==null){
			return "FAILED TO BUY AS NO CASH IN VAULT";
		}

		System.out.println("The APBuyFLow : sending etf buy trade request to custodian : " );

		FlowSession toPartySession = initiateFlow(getCustodian(custodianName));
		UntrustworthyData<EtfTradeState> output = toPartySession.sendAndReceive(EtfTradeState.class, etfTradeState);
		EtfTradeState outPutValue = SerilazationHelper.getEtfTradeState(output);

		System.out.println("The APBuyFLow : received etf trade : " +outPutValue);
		/*System.out.println("The APBuyFLow : got resp to custodian : " );

		final Command<EtfIssueContract.Commands.EtfBuyCommand> txCommand = new Command<>(new EtfIssueContract.Commands.EtfBuyCommand(),
				ImmutableList.of(custodianParty,myParty).stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
		final TransactionBuilder txBuilder = new TransactionBuilder(getNotary()).withItems(new StateAndContract(outPutValue, SELF_ISSUE_ETF_CONTRACT_ID),txCommand);
		txBuilder.verify(getServiceHub());
		final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);
		final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(toPartySession), CollectSignaturesFlow.Companion.tracker()));

		System.out.println("The APBuyFLow : output from custodian : " + outPutValue);
		System.out.println("The APBuyFLow end " + System.currentTimeMillis());
		subFlow(new FinalityFlow(fullySignedTx));
		getLogger().info("Notarised TX");
		System.out.print("The APBuyFLow end " + System.currentTimeMillis());
*/
		System.out.println("The APBuyFLow end " + System.currentTimeMillis());

		getLogger().info("Notarised TX");


		return "SUCCESS";
	}

	private Party getCustodian(String custodianName) {
		Iterable<PartyAndCertificate> partyAndCertificates = this.getServiceHub().getIdentityService()
				.getAllIdentities();

        return IdentityHelper.getPartyWithName(partyAndCertificates, custodianName);
	}

	//@InitiatedBy(APBuyEtfFLow.class)
	public static class Acceptor extends FlowLogic<SignedTransaction> {

		private final FlowSession otherPartyFlow;

		public Acceptor(FlowSession otherPartyFlow) {
			this.otherPartyFlow = otherPartyFlow;
		}

		@Suspendable
		@Override
		public SignedTransaction call() throws FlowException {
			class SignTxFlow extends SignTransactionFlow {
				private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
					super(otherPartyFlow, progressTracker);
				}

				@Override
				protected void checkTransaction(SignedTransaction stx) {
					System.out.print("Inside check transaction for self issue etf");
				}
			}
			return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
		}
	}
}
