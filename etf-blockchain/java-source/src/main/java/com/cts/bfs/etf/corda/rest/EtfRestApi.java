package com.cts.bfs.etf.corda.rest;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static net.corda.finance.contracts.GetBalances.getCashBalances;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
import com.cts.bfs.etf.corda.flows.CashIssueFlow;
import com.cts.bfs.etf.corda.flows.EtfIssueFlow;
import com.cts.bfs.etf.corda.model.EtfAsset;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.TradeType;
import com.cts.bfs.etf.corda.schema.EtfTradeSchemaV1;
import com.cts.bfs.etf.corda.schema.PersistentEtfTrade;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("issue")
public class EtfRestApi {

    private final CordaRPCOps rpcOps;
    private final Party myIdentity;

    private static final Logger logger = LoggerFactory.getLogger(EtfRestApi.class);

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
        logger.info("initiateBuyEtf -->");
        EtfTradeRequest etfTradeRequest = new EtfTradeRequest(toPartyName, etfName, Long.valueOf(quantity+""), Long.valueOf(buyAmount+""), TradeType.BUY);
        try {
            logger.info("calling flow-->");
            final FlowHandle<String> flowHandle = rpcOps.startFlowDynamic(APBuyEtfFLow.class,etfTradeRequest,"CUSTODIAN1");
            logger.info("received resp from flow-->");
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

        logger.info("initiateSellEtf -->");
        EtfTradeRequest etfTradeRequest = new EtfTradeRequest(toPartyName, etfName, Long.valueOf(quantity+""), Long.valueOf(sellAmount+""), TradeType.SELL);
        try {
            final FlowHandle<String> flowHandle = rpcOps.startFlowDynamic(
                    APSellEtfFLow.class,etfTradeRequest,"CUSTODIAN2");
            final String result = flowHandle.getReturnValue().get();
            return Response.status(CREATED).entity(result).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
        }

    @GET
    @Path("checkEtfBalance")
    public Response checkEtfBalance() {
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        // 2. Start flow and wait for response.
        try {
            logger.info("query etfTradeStates for checkEtfBalance");
            List<StateAndRef<EtfTradeState>> etfTradeStatesQueryResp = rpcOps.vaultQuery(EtfTradeState.class).getStates();
            List<EtfTradeState> etfTradeStates = new ArrayList<>();
            for (StateAndRef<EtfTradeState> stateAndRef : etfTradeStatesQueryResp
                 ) {
                EtfTradeState etfTradeState = stateAndRef.getState().getData();
                if(etfTradeState.getTradeType().equals("ISSUEETF")){
                    etfTradeStates.add(stateAndRef.getState().getData());
                }
            }
            logger.info("etfTradeStates for checkEtfBalance size "+etfTradeStates.size());
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.writeValue(out, etfTradeStates);
            String json = new String(out.toByteArray());
            logger.info("etfTradeStates  json "+json);
            return Response.status(CREATED).entity(json).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    @GET
    @Path("checkCashBalance")
    public Response checkCashBalance() {
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        // 2. Start flow and wait for response.
        try {
            logger.info("query etfTradeStates for checkEtfBalance");
            List<StateAndRef<EtfTradeState>> etfTradeStatesQueryResp = rpcOps.vaultQuery(EtfTradeState.class).getStates();
            List<EtfTradeState> etfTradeStates = new ArrayList<>();
            for (StateAndRef<EtfTradeState> stateAndRef : etfTradeStatesQueryResp
                    ) {
                EtfTradeState etfTradeState = stateAndRef.getState().getData();
                if(etfTradeState.getTradeType().equals("ISSUECASH")){
                    etfTradeStates.add(stateAndRef.getState().getData());
                }
            }
            logger.info("etfTradeStates for checkCashBalance size "+etfTradeStates.size());
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.writeValue(out, etfTradeStates);
            String json = new String(out.toByteArray());
            logger.info("etfTradeStates  json "+json);
            return Response.status(CREATED).entity(json).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

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
            rpcOps.startFlowDynamic(EtfIssueFlow.class, new EtfAsset(etfName, new Long(quantity)));
            final String msg = rpcOps.vaultQuery(EtfTradeState.class).getStates().get(0).getState().getData().toString();
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }


    @GET
    @Path("self-issue-cash")
    public Response selfIssueCash(
            @QueryParam(value = "amount") int amount,
            @QueryParam(value = "currency") String currency) {
        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        // 2. Start flow and wait for response.
        try {
            rpcOps.startFlowDynamic(CashIssueFlow.class, new Amount<Currency>(amount, Currency.getInstance(currency)));
            final String msg = rpcOps.vaultQuery(EtfTradeState.class).getStates().get(0).getState().getData().toString();
            return Response.status(CREATED).entity(msg).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }
}