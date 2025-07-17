package com.example.deliverysystem.data_source;

import java.util.List;

public class VendorInfo {
    private String industry;
    private String type;
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
