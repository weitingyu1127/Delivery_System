package com.example.deliverysystem.data_source;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.Nullable;

import com.example.deliverysystem.import_system.ImportRecord;
import com.example.deliverysystem.inspect_system.InspectRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataSource {

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
    public static String getTypeByVendor(String vendor) {
        VendorInfo info = vendorProductMap.get(vendor);
        return info != null ? info.getType() : "";
    }
    // 職員
    private static List<Map<String,String>> confirmPerson = new ArrayList<>();
    private static List<Map<String,String>> inspectors     = new ArrayList<>();
    public static List<Map<String,String>> getConfirmPerson() { return confirmPerson; }
    public static List<Map<String,String>> getInspector()     { return inspectors; }

    public static void setConfirmPersons(List<Map<String,String>> data) { confirmPerson = data; }
    public static void setInspectors(List<Map<String,String>> data)     { inspectors = data; }

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

    // ✅ 單位 (unit)
    private static List<String> units = new ArrayList<>();

    public static void setUnits(List<String> list) {
        units.clear();

        // 如果傳進來的只有一個大字串 → 自動拆分
        if (list.size() == 1 && list.get(0).startsWith("[") && list.get(0).endsWith("]")) {
            String joined = list.get(0).substring(1, list.get(0).length() - 1); // 去掉 []
            String[] splitUnits = joined.split(",\\s*"); // 依逗號切割
            for (String u : splitUnits) {
                units.add(u.trim());
            }
        } else {
            units.addAll(list);
        }

        Log.d("DataSource", "Units set: " + units);
    }

    public static List<String> getUnits() {
        return units;
    }

    public static void setupUnitSpinner(Context context, Spinner spinner, @Nullable String defaultValue) {
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                units   // ✅ 直接用 List<String>
        );
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(unitAdapter);

        // 如果有指定預設值，就幫你選中
        if (defaultValue != null && units.contains(defaultValue)) {
            int index = units.indexOf(defaultValue);
            spinner.setSelection(index);
        }
    }

}
