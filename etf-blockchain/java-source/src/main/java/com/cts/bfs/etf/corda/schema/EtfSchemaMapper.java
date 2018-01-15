package com.cts.bfs.etf.corda.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class EtfSchemaMapper extends MappedSchema{
    
    public EtfSchemaMapper() {
        super(EtfSchema.class, 1, ImmutableList.of(EtfSchema.class));
    }

}
