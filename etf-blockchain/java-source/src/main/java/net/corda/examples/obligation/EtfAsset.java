package net.corda.examples.obligation;

import net.corda.core.serialization.CordaSerializable;

import java.util.Objects;

@CordaSerializable
public class EtfAsset {
    public long getQuantity() {
        return quantity;
    }

    public void setQuantity(long quantity) {
        this.quantity = quantity;
    }

    private String etfName;
    private long quantity;

    public String getEtfName() {
        return etfName;
    }

    public void setEtfName(String etfName) {
        this.etfName = etfName;
    }

    public EtfAsset(String etfName, int quantity) {
        this.etfName = etfName;
        this.quantity = quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EtfAsset etfAsset = (EtfAsset) o;
        return quantity == etfAsset.quantity &&
                Objects.equals(etfName, etfAsset.etfName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(etfName, quantity);
    }

    @Override
    public String toString() {
        return "EtfAsset{" +
                "etfName='" + etfName + '\'' +
                ", quantity=" + quantity +
                '}';
    }
}
