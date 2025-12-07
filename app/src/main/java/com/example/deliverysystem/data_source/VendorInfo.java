package com.example.deliverysystem.data_source;

import java.util.List;

public class VendorInfo {
    /** 產業 */
    private String industry;

    /** 類態(原or物) */
    private String type;

    /** 產品 */
    private List<String> products;

    public VendorInfo(String industry, String type, List<String> products) {
        this.industry = industry;
        this.type = type;
        this.products = products;
    }

    public String getIndustry() {
        return industry;
    }

    public String getType() {
        return type;
    }

    public List<String> getProducts() {
        return products;
    }
}
