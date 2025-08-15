package com.example.deliverysystem.inspect_system;

import java.util.ArrayList;
import java.util.List;

public class InspectRecord {

    private String import_id;
    private String importDate, vendor, product, standard, packageComplete, vector,
            packageLabel, quantity, validDate, palletComplete, coa, note, place,
            inspectorStaff, confirmStaff;

    private String odor;
    private String degree;
    private List<String> images;
    public InspectRecord(String import_id, String importDate, String vendor, String product, String standard,
                         String packageComplete, String vector, String packageLabel,
                         String quantity, String validDate, String palletComplete,
                         String coa, String note, String place, List<String> images, String inspectorStaff, String confirmStaff, String odor, String degree) {
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
        this.note = note;
        this.place = place;
//        this.picture = picture;
        this.images = (images != null) ? images : new ArrayList<>(); // 避免 null
        this.inspectorStaff = inspectorStaff;
        this.confirmStaff = confirmStaff;
        this.odor = odor;
        this.degree = degree;
    }

    // ✅ Getter for all fields
    public String getImportId() { return import_id; }
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
    public String getNote() { return note; }
    public String getPlace() { return place; }
//    public String getPicture() { return picture; }
    // ★ 多張圖的取用
    public List<String> getImages() { return images; }

    // ★ 保留相容方法：回傳第一張（舊程式碼用到的 getPicture() 不用改呼叫點）
    public String getPicture() {
        return (images != null && !images.isEmpty()) ? images.get(0) : "";
    }

    public String getInspectorStaff() { return inspectorStaff; }
    public String getConfirmStaff() { return confirmStaff; }

    public String getOdor() { return odor; }
    public String getDegree() { return degree; }
}
