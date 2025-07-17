package com.example.deliverysystem.import_system;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ImportTable extends BaseActivity {
    private String vendorName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_page);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ImportTable.this, ImportItem.class);
            startActivity(intent);
        });
        vendorName = getIntent().getStringExtra("vendor_name");
    }
    @Override
    protected void onResume() {
        super.onResume();
        clearTable();
        getImportData();
    }

    private void getImportData() {
        ConnectDB.getImportRecords(vendorName, importList -> {
            DataSource.setImportRecords(importList);
            runOnUiThread(this::onImportDataReady);
        });
    }

    private void onImportDataReady() {
        List<ImportRecord> records = DataSource.getImportRecords();
        for (ImportRecord record : records) {
            addTableRow(
                    record.getImportId(), // 如果你有 id 欄位
                    record.getImportDate(),
                    record.getVendor(),
                    record.getProduct(),
                    record.getQuantity()
            );
        }
    }
    private void clearTable() {
        TableLayout tableLayout = findViewById(R.id.importTable);
        int childCount = tableLayout.getChildCount();

        // 從第1列開始移除（第0列通常是標題）
        if (childCount > 1) {
            tableLayout.removeViews(1, childCount - 1);
        }
    }

    private void addTableRow(int importId, String date, String vendorName, String itemName, String summaryAmount) {
        TableLayout tableLayout = findViewById(R.id.importTable);
        TableRow tableRow = new TableRow(this);

        // 共用樣式
        int textSize = 18;
        int padding = 8;

        // 建立 TextView 欄位
        TextView tableDate = createCell(date, 100, textSize, padding);
        TextView tableVendor = createCell(vendorName, 150, textSize, padding);
        TextView tableItem = createCell(itemName, 150, textSize, padding);
        TextView tableSum = createCell(summaryAmount, 100, textSize, padding);

        // 刪除按鈕
        AppCompatButton btnDelete = new AppCompatButton(this);
        btnDelete.setText("刪除");
        btnDelete.setTextSize(16f);
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setLayoutParams(new TableRow.LayoutParams(100, ViewGroup.LayoutParams.MATCH_PARENT));
        btnDelete.setBackgroundResource(R.drawable.table_button);
        // delete DB
        btnDelete.setOnClickListener(v -> {
            showDeleteDialog(vendorName, itemName, summaryAmount, () -> {
                // 確認刪除時執行
                ConnectDB.deleteImportRecordById(String.valueOf(importId), success -> {
                    if (success) {
                        tableLayout.removeView(tableRow); // 成功才刪除 UI row
                    } else {
                        Toast.makeText(this, "刪除失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // 加入所有欄位
        tableRow.addView(tableDate);
        tableRow.addView(tableVendor);
        tableRow.addView(tableItem);
        tableRow.addView(tableSum);
        tableRow.addView(btnDelete);
        tableLayout.addView(tableRow);
    }
    private TextView createCell(String text, int widthDp, int textSizeSp, int paddingDp) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSizeSp);
        tv.setTextColor(getResources().getColor(R.color.black));
        tv.setBackgroundResource(R.drawable.table_pattern);
        tv.setGravity(Gravity.CENTER);
        int widthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, getResources().getDisplayMetrics());
        tv.setLayoutParams(new TableRow.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT));
        int paddingPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, paddingDp, getResources().getDisplayMetrics());
        tv.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        return tv;
    }
    private void showDeleteDialog(String vendor, String product, String quantity, Runnable onConfirmDelete) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("確認刪除");

        String message = "\n\n"
                + "廠商：" + vendor + "\n"
                + "產品：" + product + "\n"
                + "數量：" + quantity;

        builder.setMessage(message);

        builder.setPositiveButton("確認", (dialog, which) -> {
            onConfirmDelete.run();  // 執行刪除動作
        });

        builder.setNegativeButton("取消", null);

        builder.show();
    }

}
