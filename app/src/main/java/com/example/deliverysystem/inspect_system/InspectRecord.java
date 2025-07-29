package com.example.deliverysystem.inspect_system;

public class InspectRecord {

    private int import_id;
    private String importDate, vendor, product, standard, packageComplete, vector,
            packageLabel, quantity, validDate, palletComplete, coa,
            inspectorStaff, confirmStaff;

    private String odor;
    private String degree;

    public InspectRecord(int import_id, String importDate, String vendor, String product, String standard,
                         String packageComplete, String vector, String packageLabel,
                         String quantity, String validDate, String palletComplete,
                         String coa, String inspectorStaff, String confirmStaff, String odor, String degree) {
        this.import_id = import_id;
        this.importDate = importDate;
        this.vendor = vendor;
        this.product = product;
        this.standard = standard;
        this.packageComplete = packageComplete;
        this.vector = vector;
        this.packageLabel = packageLabel;
        this.quantity = quantity;
        this.validDate = validDate;
        this.palletComplete = palletComplete;
        this.coa = coa;
        this.inspectorStaff = inspectorStaff;
        this.confirmStaff = confirmStaff;
        this.odor = odor;
        this.degree = degree;
    }

    // ✅ Getter for all fields
    public int getImportId() { return import_id; }
    public String getImportDate() { return importDate; }
    public String getVendor() { return vendor; }
    public String getProduct() { return product; }
    public String getStandard() { return standard; }
    public String getPackageComplete() { return packageComplete; }
    public String getVector() { return vector; }
    public String getPackageLabel() { return packageLabel; }
    public String getQuantity() { return quantity; }
    public String getValidDate() { return validDate; }
    public String getPalletComplete() { return palletComplete; }
    public String getCoa() { return coa; }
    public String getInspectorStaff() { return inspectorStaff; }
    public String getConfirmStaff() { return confirmStaff; }

    public String getOdor() { return odor; }
    public String getDegree() { return degree; }
}
