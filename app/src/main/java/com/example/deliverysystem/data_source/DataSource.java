package com.example.deliverysystem.data_source;

import com.example.deliverysystem.import_system.ImportRecord;
import com.example.deliverysystem.inspect_system.InspectRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataSource {
//    public static final String[] UNITS = {
//            "公克", "公斤", "頓"
//    };

    // 產品
    private static Map<String, VendorInfo> vendorProductMap = new LinkedHashMap<>();

    public static Map<String, VendorInfo> getVendorProductMap() {
        return vendorProductMap;
    }

    public static void setVendorProductMap(Map<String, VendorInfo> map) {
        vendorProductMap.clear();
        vendorProductMap.putAll(map);
    }

    // ✅ 抓出所有供應商名稱
    public static List<String> getVendor() {
        return new ArrayList<>(vendorProductMap.keySet());
    }

    // ✅ 根據供應商抓產品
    public static List<String> getProductsByVendor(String vendor) {
        VendorInfo info = vendorProductMap.get(vendor);
        return info != null ? info.getProducts() : new ArrayList<>();
    }

    // 職員
    private static List<String> inspectors = new ArrayList<>();
    private static List<String> confirmPerson = new ArrayList<>();
    public static List<String> getInspector() {
        return inspectors;
    }
    public static void setInspectors(List<String> list) {
        inspectors.clear();
        inspectors.addAll(list);
    }
    public static List<String> getConfirmPerson() {
        return confirmPerson;
    }
    public static void setConfirmPersons(List<String> list) {
        confirmPerson.clear();
        confirmPerson.addAll(list);
    }


    //權限密碼
    private static List<String> passwords = new ArrayList<>();
    public static void setPasswords(List<String> list) {
        passwords.clear();
        passwords.addAll(list);
    }

    public static List<String> getPasswords() {
        return passwords;
    }

    // 驗收table
    private static List<InspectRecord> inspectRecords = new ArrayList<>();
    public static void setInspectRecords(List<InspectRecord> records) {
        inspectRecords = records;
    }
    public static List<InspectRecord> getInspectRecords() {
        return inspectRecords;
    }

    // 進貨table
    private static List<ImportRecord> importRecords = new ArrayList<>();

    public static void setImportRecords(List<ImportRecord> records) {
        importRecords = records;
    }

    public static List<ImportRecord> getImportRecords() {
        return importRecords;
    }

}
