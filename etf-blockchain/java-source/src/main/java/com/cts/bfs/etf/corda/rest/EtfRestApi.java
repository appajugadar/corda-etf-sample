package com.cts.bfs.etf.corda.rest;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static net.corda.finance.contracts.GetBalances.getCashBalances;

import java.util.Currency;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.cts.bfs.etf.corda.flows.APBuyEtfFLow;
import com.cts.bfs.etf.corda.flows.APSellEtfFLow;
import com.cts.bfs.etf.corda.flows.EtfIssueFlow;
import com.cts.bfs.etf.corda.model.EtfAsset;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.TradeType;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.google.common.collect.ImmutableMap;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;

@Path("issue")
public class EtfRestApi {

    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    public EtfRestApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myIdentity = rpcOps.nodeInfo().getLegalIdentities().get(0);
    }

    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Party> me() {
        return ImmutableMap.of("me", myIdentity);
    }

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<String>> peers() {
        return ImmutableMap.of("peers", rpcOps.networkMapSnapshot()
                .stream()
                .filter(nodeInfo -> nodeInfo.getLegalIdentities().get(0) != myIdentity)
                .map(it -> it.getLegalIdentities().get(0).getName().getOrganisation())
                .collect(toList()));
    }
/*

    @GET
    @Path("owed-per-currency")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Currency, Long> owedPerCurrency() {
        return rpcOps.vaultQuery(Obligation.class).getStates()
                .stream()
                .filter(it -> it.getState().getData().getLender() != myIdentity)
                .map(it -> it.getState().getData().getAmount())
                .collect(groupingBy(
                        Amount::getToken, summingLong(Amount::getQuantity)
                ));
    }

    @GET
    @Path("obligations")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<Obligation>> obligations() {
        return rpcOps.vaultQuery(Obligation.class).getStates();
    }

    @GET
    @Path("cash")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<Cash.State>> cash() {
        return rpcOps.vaultQuery(Cash.State.class).getStates();
    }
*/

    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Currency, Amount<Currency>> cashBalances() {
        return getCashBalances(rpcOps);
    }
  
    @GET
    @Path("buy-etf-from-party")
    public Response initiateBuyEtf(
            @QueryParam(value = "toPartyName") String toPartyName,
            @QueryParam(value = "etfName") String etfName,
            @QueryParam(value = "quantity") int quantity,
            @QueryParam(value = "buyamount") int buyAmount) {
        EtfTradeRequest etfTradeRequest = new EtfTradeRequest(toPartyName, etfName, quantity, buyAmount, TradeType.BUY);
        try {
            final FlowHandle<String> flowHandle = rpcOps.startFlowDynamic(APBuyEtfFLow.class,etfTradeRequest,"CUSTODIAN1");
            final String result = flowHandle.getReturnValue().get();
            return Response.status(CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("sell-etf-to-party")
    public Response initiateSellEtf(
            @QueryParam(value = "toPartyName") String toPartyName,
            @QueryParam(value = "etfName") String etfName,
            @QueryParam(value = "quantity") int quantity, @QueryParam(value = "sellamount") int sellAmount) {

        EtfTradeRequest etfTradeRequest = new EtfTradeRequest(toPartyName, etfName, quantity, sellAmount, TradeType.SELL);
        try {
            final FlowHandle<String> flowHandle = rpcOps.startFlowDynamic(
                    APSellEtfFLow.class,etfTradeRequest,"CUSTODIAN2");
            final String result = flowHandle.getReturnValue().get();
            return Response.status(CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
        }

/*

    @GET
    @Path("self-issue-cash")
    public Response selfIssueCash(
            @QueryParam(value = "amount") int amount,
            @QueryParam(value = "currency") String currency) {

        // 1. Prepare issue request.
        final Amount<Currency> issueAmount = new Amount<>((long) amount * 100, Currency.getInstance(currency));
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        final Party notary = notaries.get(0);
        final OpaqueBytes issueRef = OpaqueBytes.of(new byte[1]);
        final CashIssueFlow.IssueRequest issueRequest = new CashIssueFlow.IssueRequest(issueAmount, issueRef, notary);

        // 2. Start flow and wait for response.
        try {
            final FlowHandle<AbstractCashFlow.Result> flowHandle = rpcOps.startFlowDynamic(CashIssueFlow.class, issueRequest);
            final AbstractCashFlow.Result result = flowHandle.getReturnValue().get();
            final String msg = result.getStx().getTx().getOutputStates().get(0).toString();
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
*/



    @GET
    @Path("self-issue-etf")
    public Response selfIssueEtf(
            @QueryParam(value = "quantity") int quantity,
            @QueryParam(value = "etfName") String etfName) {
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        // 2. Start flow and wait for response.
        try {
            final FlowHandle<SignedTransaction>  transactionFlowHandle = rpcOps.startFlowDynamic(EtfIssueFlow.class, new EtfAsset(etfName, quantity));
            transactionFlowHandle.getReturnValue().get();
            final String msg = rpcOps.vaultQuery(EtfTradeState.class).getStates().get(0).getState().getData().toString();


            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}