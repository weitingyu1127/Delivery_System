package com.example.deliverysystem.import_system;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.utility.Tools;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ImportTable extends BaseActivity {
    /** 廠商名稱 */
    private String vendorName;
    /** 進貨紀錄 */
    private List<ImportRecord> importRecords = new ArrayList<>();
    /** 搜尋框 */
    private EditText searchInput;
    /** 清除搜尋按鈕 */
    private ImageView clearFilterBtn;
    /** 進貨Table */
    LinearLayout importTable;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_page);

        importTable= findViewById(R.id.importTable);
        vendorName = getIntent().getStringExtra("vendor_name");

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ImportTable.this, ImportItem.class);
            intent.putExtra("vendor_name",vendorName);
            startActivity(intent);
        });

        searchInput = findViewById(R.id.search_input);
        clearFilterBtn = findViewById(R.id.clear_btn);

        searchInput.setShowSoftInputOnFocus(false);
        searchInput.setOnClickListener(v -> {
            showDatePickerDialog();
        });

        clearFilterBtn.setOnClickListener(v -> {
            searchInput.setText("");
            filterTableByProduct("請選擇產品");
            getImportData();
        });
    }
    
    /** 重新載入 */
    @Override
    protected void onResume() {
        super.onResume();
        getImportData();
    }

    /** 取得進貨紀錄 */
    private void getImportData() {
        Tools.clearTable(importTable);
        ConnectDB.getImportRecords(vendorName, importList -> {
            DataSource.setImportRecords(importList);
            // 重新渲染
            runOnUiThread(this::onImportDataReady);
        });
    }

    /** 重新渲染 */
    private void onImportDataReady() {
        // 取得商品
        importRecords = DataSource.getImportRecords();
        Set<String> productSet = new LinkedHashSet<>();
        for (ImportRecord record : importRecords) {
            productSet.add(record.getProduct());
        }
        // 建立商品下拉選單
        List<String> spinnerProduct = new ArrayList<>(productSet);
        spinnerProduct.add(0, "請選擇產品"); // 選單預設值
        Spinner spinner = findViewById(R.id.product_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_pattern,
                R.id.spinner_text,
                spinnerProduct
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_pattern);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = spinnerProduct.get(position);
                filterTableByProduct(selectedProduct);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    /** 產品Filter */
    private void filterTableByProduct(String product) {
        Tools.clearTable(importTable);
        for (ImportRecord record : importRecords) {
            if (product.equals("請選擇產品")) {
                addRecordRow(record);
                continue;
            }
            if (record.getProduct().equals(product)) {
                addRecordRow(record);
            }
        }
    }

    /** 日期Filter */
    private void filterTableByDate(String date) {
        Tools.clearTable(importTable);
        for (ImportRecord record : importRecords) {
            if (date.equals("全部日期") || date.isEmpty()) {
                addRecordRow(record);
                continue;
            }
            if (record.getImportDate().equals(date)) {
                addRecordRow(record);
            }
        }
    }

    /** 刪除紀錄 */
    private void handleDelete(ImportRecord record) {
        showDeleteDialog(record.getVendor(), record.getProduct(), record.getQuantity(), () -> {
            int qty = Integer.parseInt(record.getQuantity().replaceAll("[^0-9]", ""));
            ConnectDB.adjustQuantity(record.getPlace(), record.getType(),
                record.getVendor(), record.getProduct(), -qty, successAdj -> {
                    if (successAdj) {
                        ConnectDB.deleteImportRecordById(record.getImportId(), success -> {
                            runOnUiThread(() -> {
                                if (success) {
                                    Tools.showToast(this,"刪除成功" );
                                    getImportData();
                                } else {
                                    Tools.showToast(this,"刪除失敗" );
                                }
                            });
                        });
                    } else {
                        runOnUiThread(() ->
                            Tools.showToast(this,"刪除失敗：庫存不足")
                        );
                    }
                });
        });
    }

    /** 進貨紀錄Table */
    private void addRecordRow(ImportRecord record) {
        List<String> cols = Arrays.asList(
            record.getType(),
            record.getImportDate(),
            record.getVendor(),
            record.getProduct(),
            record.getQuantity(),
            record.getPlace()
        );
        Tools.addTableRow(this, importTable, cols, () -> handleDelete(record));
    }

    /** 刪除彈窗 */
    private void showDeleteDialog(String vendor, String product, String quantity, Runnable onConfirmDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("確認刪除");
        String message = "廠商：" + vendor + "\n" + "產品：" + product + "\n" + "數量：" + quantity;
        builder.setMessage(message);
        builder.setPositiveButton("確認", (dialog, which) -> {
            onConfirmDelete.run();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /** 日期選擇 */
    private void showDatePickerDialog() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
            this,
            (view, selectedYear, selectedMonth, selectedDay) -> {
                String selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                searchInput.setText(selectedDate);
                filterTableByDate(selectedDate);
            },
            year, month, day
        );
        datePickerDialog.show();
    }
}
