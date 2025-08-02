package com.example.deliverysystem.import_system;

public class ImportRecord {
    private final String importId;
    private final String importDate;
    private final String vendor;
    private final String product;
    private final String quantity;

    public ImportRecord(String import_id, String importDate, String vendor, String product, String quantity) {
        this.importId = import_id;
        this.importDate = importDate;
        this.vendor = vendor;
        this.product = product;
        this.quantity = quantity;
    }
    public String getImportId() { return importId; }
    public String getImportDate() { return importDate; }
    public String getVendor() { return vendor; }
    public String getProduct() { return product; }
    public String getQuantity() { return quantity; }
}

