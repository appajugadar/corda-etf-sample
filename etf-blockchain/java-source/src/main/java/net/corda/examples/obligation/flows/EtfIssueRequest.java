package net.corda.examples.obligation.flows;


import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.examples.obligation.EtfAsset;

@CordaSerializable
class EtfIssueRequest {

    EtfAsset etfAsset;
    OpaqueBytes issueRef ;
    Party notary;

    public EtfIssueRequest(EtfAsset etfAsset,
            OpaqueBytes issueRef ,
            Party notary) {
        super();
        this.etfAsset = etfAsset;
        this.issueRef = issueRef;
        this.notary = notary;

    }
}


