package com.example.deliverysystem.import_system;

public class ImportRecord {
    // TODO 待確認是否要DocId
    private String docId;
    /** 進貨編號 */
    private final String importId;
    /** 進貨日期 */
    private final String importDate;
    /** 進貨廠商 */
    private final String vendor;
    /** 進貨產品 */
    private final String product;
    /** 進貨數量 */
    private final String quantity;
    /** 進貨地點 */
    private final String place;
    /** 進貨類型 */
    private final String type;
    public ImportRecord(String docId, String import_id, String importDate, String vendor, String product, String quantity, String place, String type) {
        this.docId = docId;
        this.importId = import_id;
        this.importDate = importDate;
        this.vendor = vendor;
        this.product = product;
        this.quantity = quantity;
        this.place = place;
        this.type = type;
    }
    // TODO 待確認是否要DocId
    public String getDocId() { return docId; }
    public String getImportId() { return importId; }
    public String getImportDate() { return importDate; }
    public String getVendor() { return vendor; }
    public String getProduct() { return product; }
    public String getQuantity() { return quantity; }
    public String getPlace() { return place; }
    public String getType() { return type; }
}

