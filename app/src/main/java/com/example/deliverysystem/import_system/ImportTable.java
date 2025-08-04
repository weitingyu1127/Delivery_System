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
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ImportTable extends BaseActivity {
    private String vendorName;
    private List<ImportRecord> fullRecords = new ArrayList<>();
    private EditText searchInput;
    private ImageView clearFilterBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_page);

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
    @Override
    protected void onResume() {
        super.onResume();
        getImportData();
    }

    private void getImportData() {
        clearTable();
        ConnectDB.getImportRecords(vendorName, importList -> {
            DataSource.setImportRecords(importList);
            runOnUiThread(this::onImportDataReady);
        });
    }

    private void onImportDataReady() {
        fullRecords = DataSource.getImportRecords();

        Set<String> productSet = new LinkedHashSet<>();
        for (ImportRecord record : fullRecords) {
            productSet.add(record.getProduct());
        }

        List<String> spinnerProductList = new ArrayList<>(productSet);
        spinnerProductList.add(0, "請選擇產品");

        Spinner spinner = findViewById(R.id.product_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_pattern,
                R.id.spinner_text,
                spinnerProductList
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_pattern);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selectedProduct = spinnerProductList.get(position);
                filterTableByProduct(selectedProduct);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        // 4. 預設顯示全部
        filterTableByProduct("請選擇產品");
    }

    private void filterTableByProduct(String product) {
        clearTable();
        for (ImportRecord record : fullRecords) {
            if (product.equals("請選擇產品") || record.getProduct().equals(product)) {
                addTableRow(
                        record.getImportId(),
                        record.getImportDate(),
                        record.getVendor(),
                        record.getProduct(),
                        record.getQuantity()
                );
            }
        }
    }
    private void filterTableByDate(String date) {
        clearTable();
        for (ImportRecord record : fullRecords) {
            if (record.getImportDate().equals(date)) {
                addTableRow(
                        record.getImportId(),
                        record.getImportDate(),
                        record.getVendor(),
                        record.getProduct(),
                        record.getQuantity()
                );
            }
        }
    }
    private void clearTable() {
        LinearLayout tableLayout = findViewById(R.id.importTable);
        tableLayout.removeAllViews();
    }

    private void addTableRow(String importId, String date, String vendorName, String itemName, String summaryAmount) {
        LinearLayout tableLayout = findViewById(R.id.importTable);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(12, 15, 12, 15);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int textSize = 16;
        int padding = 8;

        // 建立 Cell 方法（TextView）
        TextView tableDate = createCell(date, 0.5f, textSize, padding);
        TextView tableVendor = createCell(vendorName, 0.5f, textSize, padding);
        TextView tableItem = createCell(itemName, 0.5f, textSize, padding);
        TextView tableSum = createCell(summaryAmount, 0.5f, textSize, padding);

        LinearLayout btnContainer = new LinearLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f);
        btnContainer.setLayoutParams(containerParams);
        btnContainer.setGravity(Gravity.CENTER); // 讓內部 button 置中

        AppCompatButton btnDelete = new AppCompatButton(this);
        btnDelete.setText("刪除");
        btnDelete.setTextSize(14f);
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setBackgroundResource(R.drawable.btn_orange);

        // 設定固定大小（例如 84x34 dp）
        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 84, getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(width, height);
        btnDelete.setLayoutParams(btnParams);

        // 放入容器
        btnContainer.addView(btnDelete);

        btnDelete.setOnClickListener(v -> {
            showDeleteDialog(vendorName, itemName, summaryAmount, () -> {
                ConnectDB.deleteImportRecordById(String.valueOf(importId), success -> {
                    if (success) {
                        tableLayout.removeView(rowLayout);
                    } else {
                        Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // 加入所有欄位到該列
        rowLayout.addView(tableDate);
        rowLayout.addView(tableVendor);
        rowLayout.addView(tableItem);
        rowLayout.addView(tableSum);
        rowLayout.addView(btnContainer);

        // 建立分隔線
        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 1);
        dividerParams.setMargins(12, 0, 12, 0);
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(Color.parseColor("#999999"));

        // 加入列與分隔線
        tableLayout.addView(rowLayout);
        tableLayout.addView(divider);
    }

    private TextView createCell(String text, float weight, int textSize, int padding) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(padding, padding, padding, padding);
        textView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        return textView;
    }

    private void showDeleteDialog(String vendor, String product, String quantity, Runnable onConfirmDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("確認刪除");

        String message =
                "廠商：" + vendor + "\n"
                + "產品：" + product + "\n"
                + "數量：" + quantity;

        builder.setMessage(message);

        builder.setPositiveButton("確認", (dialog, which) -> {
            onConfirmDelete.run();  // 執行刪除動作
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

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
                    filterTableByDate(selectedDate);  // 🔸依日期過濾
                },
                year, month, day
        );
        datePickerDialog.show();
    }

}
