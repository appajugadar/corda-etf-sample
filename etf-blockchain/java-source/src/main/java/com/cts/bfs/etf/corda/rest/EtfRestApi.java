package com.cts.bfs.etf.corda.rest;

import com.cts.bfs.etf.corda.flows.APBuyEtfFLow;
import com.cts.bfs.etf.corda.flows.APSellEtfFLow;
import com.cts.bfs.etf.corda.flows.CashIssueFlow;
import com.cts.bfs.etf.corda.flows.EtfIssueFlow;
import com.cts.bfs.etf.corda.model.EtfAsset;
import com.cts.bfs.etf.corda.model.EtfTradeRequest;
import com.cts.bfs.etf.corda.model.TradeType;
import com.cts.bfs.etf.corda.state.EtfTradeState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static net.corda.finance.contracts.GetBalances.getCashBalances;

@Path("issue")
public class EtfRestApi {

    private static final Logger logger = LoggerFactory.getLogger(EtfRestApi.class);
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

    @GET
    @Path("cash-balances")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<Currency, Amount<Currency>> cashBalances() {
        return getCashBalances(rpcOps);
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
            for (StateAndRef<EtfTradeState> stateAndRef : etfTradeStatesQueryResp) {
                EtfTradeState etfTradeState = stateAndRef.getState().getData();

                if (etfTradeState.getTradeType().equals("ISSUEETF")) {
                    etfTradeState.setToParty(null);
                    etfTradeState.setFromParty(null);
                    etfTradeStates.add(etfTradeState);
                }
            }

            logger.info("etfTradeStates for checkEtfBalance size " + etfTradeStates.size());
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.writeValue(out, etfTradeStates);
            String json = new String(out.toByteArray());
            logger.info("etfTradeStates  json " + json);
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
                if (etfTradeState.getTradeType().equals("ISSUECASH")) {
                    etfTradeState.setToParty(null);
                    etfTradeState.setFromParty(null);
                    etfTradeStates.add(etfTradeState);
                }
            }
            logger.info("etfTradeStates for checkCashBalance size " + etfTradeStates.size());
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            mapper.writeValue(out, etfTradeStates);
            String json = new String(out.toByteArray());
            logger.info("etfTradeStates  json " + json);
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

        logger.info("quantity::" + quantity);
        logger.info("etfName::" + etfName);

        final List<Party> notaries = rpcOps.notaryIdentities();
        if (notaries.isEmpty()) {
            throw new IllegalStateException("Could not find a notary.");
        }
        // 2. Start flow and wait for response.
        try {

            rpcOps.startFlowDynamic(EtfIssueFlow.class, new EtfAsset(etfName, new Long(quantity)));
            final String msg = rpcOps.vaultQuery(EtfTradeState.class).getStates().get(0).getState().getData().toString();
            return Response.status(CREATED).entity("SUCCESS").build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity("SUCCESS").build();
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
            System.out.println("self issue completed");
            return Response.status(CREATED).entity("SUCCESS").build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("issue-etf-buy-sell")
    public Response issueETFBuySell(
            @QueryParam(value = "buysell") String buysell,
            @QueryParam(value = "counterparty") String counterparty,
            @QueryParam(value = "etfName") String etfName,
            @QueryParam(value = "currency") String currency,
            @QueryParam(value = "quantity") int quantity,
            @QueryParam(value = "amount") int amount) {

        logger.info("buysell::" + buysell);
        logger.info("counterparty::" + counterparty);
        logger.info("etfName::" + etfName);
        logger.info("currency::" + currency);
        logger.info("quantity::" + quantity);
        logger.info("amount::" + amount);

        if("BUY".equalsIgnoreCase(buysell)){

            Runnable th = new Runnable() {
                @Override
                public void run() {
                    EtfTradeRequest etfTradeRequest = new EtfTradeRequest(counterparty, etfName, Long.valueOf(quantity + ""), Long.valueOf(amount + ""), TradeType.BUY);
                    try {
                        logger.info("calling flow-->");
                        final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(APBuyEtfFLow.class, etfTradeRequest, "CUSTODIAN1");
                        logger.info("received resp from flow-->");
                        final SignedTransaction result = flowHandle.getReturnValue().get();
                        selfIssueEtf(etfTradeRequest.getQuantity().intValue(), etfTradeRequest.getEtfName());

                        EtfTradeState etfTradeRequest1 = (EtfTradeState ) result.getTx().getOutput(0);
                        selfIssueCash(- Integer.parseInt(etfTradeRequest1.getAmount().getQuantity()+""), etfTradeRequest1.getAmount().getToken().getCurrencyCode());
                        System.out.println("transaction to buy/selll completed");
                    } catch (Exception e) {
                        logger.info("",e);
                    }
                }
            };

            new Thread(th).start();
        }else{
            Runnable th = new Runnable() {
                @Override
                public void run() {
                    EtfTradeRequest etfTradeRequest = new EtfTradeRequest(counterparty, etfName, Long.valueOf(quantity + ""), Long.valueOf(amount + ""), TradeType.SELL);
                    try {
                        final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                                APSellEtfFLow.class, etfTradeRequest, "CUSTODIAN2");
                        final SignedTransaction result = flowHandle.getReturnValue().get();
                        selfIssueEtf(- etfTradeRequest.getQuantity().intValue(), etfTradeRequest.getEtfName());


                        EtfTradeState etfTradeRequest1 = (EtfTradeState ) result.getTx().getOutput(0);

                        selfIssueCash(Integer.parseInt(etfTradeRequest1.getAmount().getQuantity()+""), etfTradeRequest1.getAmount().getToken().getCurrencyCode());
                        System.out.println("transaction to buy/selll completed");

                    } catch (Exception e) {
                        logger.info("",e);
                    }
                }
            };

            new Thread(th).start();
        }

        return Response.status(CREATED).entity("Request placed Successfully").build();
    }



    @GET
    @Path("sell-etf-to-party")
    public Response initiateSellEtf(
            @QueryParam(value = "toPartyName") String toPartyName,
            @QueryParam(value = "etfName") String etfName,
            @QueryParam(value = "quantity") int quantity, @QueryParam(value = "sellamount") int sellAmount) {

        logger.info("initiateSellEtf -->");

        Runnable th = new Runnable() {
            @Override
            public void run() {
                EtfTradeRequest etfTradeRequest = new EtfTradeRequest(toPartyName, etfName, Long.valueOf(quantity + ""), Long.valueOf(sellAmount + ""), TradeType.SELL);
                try {
                    final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(
                            APSellEtfFLow.class, etfTradeRequest, "CUSTODIAN2");
                    final SignedTransaction result = flowHandle.getReturnValue().get();
                    selfIssueEtf(- etfTradeRequest.getQuantity().intValue(), etfTradeRequest.getEtfName());


                    EtfTradeState etfTradeRequest1 = (EtfTradeState ) result.getTx().getOutput(0);

                    selfIssueCash(Integer.parseInt(etfTradeRequest1.getAmount().getQuantity()+""), etfTradeRequest1.getAmount().getToken().getCurrencyCode());


                } catch (Exception e) {
                    logger.info("",e);
                }
            }
        };

        new Thread(th).start();

        return Response.status(CREATED).entity("Request sent. Please check later").build();
    }
    @GET
    @Path("buy-etf-from-party")
    public Response initiateBuyEtf(
            @QueryParam(value = "toPartyName") String toPartyName,
            @QueryParam(value = "etfName") String etfName,
            @QueryParam(value = "quantity") int quantity,
            @QueryParam(value = "buyamount") int buyAmount) {
        logger.info("initiateBuyEtf -->");

        Runnable th = new Runnable() {
            @Override
            public void run() {
                EtfTradeRequest etfTradeRequest = new EtfTradeRequest(toPartyName, etfName, Long.valueOf(quantity + ""), Long.valueOf(buyAmount + ""), TradeType.BUY);
                try {
                    logger.info("calling flow-->");
                    final FlowHandle<SignedTransaction> flowHandle = rpcOps.startFlowDynamic(APBuyEtfFLow.class, etfTradeRequest, "CUSTODIAN1");
                    logger.info("received resp from flow-->");
                    final SignedTransaction result = flowHandle.getReturnValue().get();
                    selfIssueEtf(etfTradeRequest.getQuantity().intValue(), etfTradeRequest.getEtfName());

                    EtfTradeState etfTradeRequest1 = (EtfTradeState ) result.getTx().getOutput(0);
                    selfIssueCash(- Integer.parseInt(etfTradeRequest1.getAmount().getQuantity()+""), etfTradeRequest1.getAmount().getToken().getCurrencyCode());
                } catch (Exception e) {
                    logger.info("",e);
                }
            }
        };

        new Thread(th).start();
        return Response.status(CREATED).entity("Request sent. Please check later").build();
    }


}