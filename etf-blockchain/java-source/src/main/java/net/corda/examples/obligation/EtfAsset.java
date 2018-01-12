package net.corda.examples.obligation;

public class EtfAsset {
    private String etfName;
    private long quantity;

    public EtfAsset(String etfName, int quantity) {
        this.etfName = etfName;
        this.quantity = quantity;
    }
}
