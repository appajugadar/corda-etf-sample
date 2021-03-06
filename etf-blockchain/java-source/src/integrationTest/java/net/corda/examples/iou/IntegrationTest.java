package net.corda.examples.iou;

import static net.corda.core.crypto.CryptoUtils.entropyToKeyPair;
import static net.corda.testing.TestConstants.getDUMMY_BANK_A;
import static net.corda.testing.TestConstants.getDUMMY_BANK_B;
import static net.corda.testing.TestConstants.getDUMMY_NOTARY;
import static net.corda.testing.driver.Driver.driver;

import java.math.BigInteger;
import java.security.KeyPair;
import java.util.List;
import java.util.Set;

import net.corda.core.identity.CordaX500Name;
import net.corda.testing.CoreTestUtils;
import net.corda.testing.driver.WebserverHandle;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.Party;
import net.corda.node.services.transactions.SimpleNotaryService;
import net.corda.nodeapi.internal.ServiceInfo;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(IntegrationTest.class);

    @Before
    public void setUp() {
        CoreTestUtils.setCordappPackages( "com.cts.bfs.etf.corda.contract");
    }

    @Test
    public void runDriverTest() {
        Party notary = getDUMMY_NOTARY();

        KeyPair DUMMY_AP1_KEY = entropyToKeyPair(BigInteger.valueOf(40));
        Party ap1 = new Party(new CordaX500Name("AP1", "London", "GB"), DUMMY_AP1_KEY.getPublic());

        KeyPair DUMMY_CUSTODIAN_KEY = entropyToKeyPair(BigInteger.valueOf(50));
        Party custodian1 = new Party(new CordaX500Name("CUSTODIAN1", "London", "GB"), DUMMY_CUSTODIAN_KEY.getPublic());

        Set<ServiceInfo> notaryServices = ImmutableSet.of(new ServiceInfo(SimpleNotaryService.Companion.getType(), null));

        driver(new DriverParameters().setIsDebug(true).setStartNodesInProcess(true), dsl -> {
            // This starts three nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            List<CordaFuture<NodeHandle>> handles = ImmutableList.of(
                    dsl.startNode(new NodeParameters().setProvidedName(notary.getName()).setAdvertisedServices(notaryServices)),
                    dsl.startNode(new NodeParameters().setProvidedName(ap1.getName())),
                    dsl.startNode(new NodeParameters().setProvidedName(custodian1.getName()))
            );

            try {
                NodeHandle notaryHandle = handles.get(0).get();
                NodeHandle nodeAHandle = handles.get(1).get();
                NodeHandle nodeBHandle = handles.get(2).get();

                // This test will call via the RPC proxy to find a party of another node to verify that the nodes have
                // started and can communicate. This is a very basic test, in practice tests would be starting flows,
                // and verifying the states in the vault and other important metrics to ensure that your CorDapp is working
                // as intended.

                CordaFuture<WebserverHandle> webHandleA = dsl.startWebserver(nodeAHandle);
                CordaFuture<WebserverHandle> webHandleB = dsl.startWebserver(nodeBHandle);

                log.info("Webserver A address: " + webHandleA.get().getListenAddress());

                dsl.waitForAllNodesToFinish();


                Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(ap1.getName()).getName(), ap1.getName());
                Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(custodian1.getName()).getName(), custodian1.getName());
                Assert.assertEquals(notaryHandle.getRpc().wellKnownPartyFromX500Name(notary.getName()).getName(), notary.getName());
            } catch (Exception e) {
                throw new RuntimeException("Caught exception during test", e);
            }

            return null;
        });
    }
}