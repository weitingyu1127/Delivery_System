package com.example.deliverysystem.import_system;

public class ImportRecord {
    private final String importId;
    private final String importDate;
    private final String vendor;
    private final String product;
    private final String quantity;
    private final String place;
    public ImportRecord(String import_id, String importDate, String vendor, String product, String quantity, String place) {
        this.importId = import_id;
        this.importDate = importDate;
        this.vendor = vendor;
        this.product = product;
        this.quantity = quantity;
        this.place = place;
    }
    public String getImportId() { return importId; }
    public String getImportDate() { return importDate; }
    public String getVendor() { return vendor; }
    public String getProduct() { return product; }
    public String getQuantity() { return quantity; }
    public String getPlace() { return place; }
}

