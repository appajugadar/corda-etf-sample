package com.cts.bfs.etf.corda.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class EtfTradeSchemaV1 extends MappedSchema {

    public EtfTradeSchemaV1() {
        super(EtfTradeSchema.class, 1, ImmutableList.of(PersistentEtfTrade.class));

    }


}
