package com.example.deliverysystem.inspect_system;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;
import com.example.deliverysystem.setting_system.SettingMain;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InspectTable extends BaseActivity {
    private String type;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getIntent().getStringExtra("type");
        if ("物料".equals(type)) {
            setContentView(R.layout.inspect_material_table); // 原料的 layout
        } else if ("原料".equals(type)) {
            setContentView(R.layout.inspect_ingredient_table);  // 物料的 layout
        }

        TextView textSelectedDate = findViewById(R.id.date_text);
        ImageView calendarIcon = findViewById(R.id.calendar_icon);

        View.OnClickListener dateClickListener = v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // 補零處理，selectedMonth + 1 是因為從 0 開始
                        String selectedDate = String.format(Locale.TAIWAN, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);

                        textSelectedDate.setText(selectedDate);
                        textSelectedDate.setTextColor(getResources().getColor(R.color.black));
                        textSelectedDate.setTypeface(null, Typeface.NORMAL);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        };
        // 點擊文字或圖示都可以觸發日期選擇器
        textSelectedDate.setOnClickListener(dateClickListener);
        calendarIcon.setOnClickListener(dateClickListener);

        Button searchBtn = findViewById(R.id.btnSearch);
        searchBtn.setOnClickListener(v -> {
            String vendor = ((Spinner) findViewById(R.id.spinnerVendor)).getSelectedItem().toString();
            String product = ((Spinner) findViewById(R.id.spinnerProduct)).getSelectedItem().toString();
            String inspector = ((Spinner) findViewById(R.id.spinnerInspector)).getSelectedItem().toString();
            String confirmer = ((Spinner) findViewById(R.id.spinnerConfirmPerson)).getSelectedItem().toString();
            String date = ((TextView) findViewById(R.id.date_text)).getText().toString();  // yyyy-MM-dd
            if (vendor.equals("廠商")) vendor = "";
            if (product.equals("產品")) product = "";
            if (inspector.equals("驗收人員")) inspector = "";
            if (confirmer.equals("確認人員")) confirmer = "";
            if (date.equals("選擇日期")) date = "";
            fetchFilteredRecords(type, vendor, product, inspector, confirmer, date);
        });
        getInspectData();
    }

    protected void onResume() {
        super.onResume();
        getInspectData();
    }
    private void getInspectData() {
        ConnectDB.getInspectRecords(type, inspectList -> {
            DataSource.setInspectRecords(inspectList);
            runOnUiThread(this::onInspectDataReady);
        });
    }
    private void onInspectDataReady() {
        clearTable();
        setupSpinner(type);
        List<InspectRecord> records = DataSource.getInspectRecords();
        for (InspectRecord record : records) {
            if ("原料".equals(type)) {
                addTableRow(
                        record.getImportId(),
                        record.getImportDate(),
                        record.getVendor(),
                        record.getProduct(),
                        record.getStandard(),
                        record.getPackageComplete(),
                        record.getVector(),
                        record.getPackageLabel(),
                        record.getQuantity(),
                        record.getValidDate(),
                        record.getPalletComplete(),
                        record.getCoa(),
                        record.getNote(),
                        record.getPicture(),
                        record.getInspectorStaff(),
                        record.getConfirmStaff(),
                        record.getOdor(),         // ✅ 新增：異味 (Boolean)
                        record.getDegree(),       // ✅ 新增：溫度 (nteger)
                        type
                );
            } else {
                addTableRow(
                        record.getImportId(),
                        record.getImportDate(),
                        record.getVendor(),
                        record.getProduct(),
                        record.getStandard(),
                        record.getPackageComplete(),
                        record.getVector(),
                        record.getPackageLabel(),
                        record.getQuantity(),
                        record.getValidDate(),
                        record.getPalletComplete(),
                        record.getCoa(),
                        record.getNote(),
                        record.getPicture(),
                        record.getInspectorStaff(),
                        record.getConfirmStaff(),
                        null,
                        null,
                        type
                );
            }
        }
    }

    private void clearTable() {
        ViewGroup tableLayout = findViewById(R.id.inspectTable);
        tableLayout.removeAllViews(); // 清空子 View（表格列）
    }
    private void fetchFilteredRecords(String type, String vendor, String product, String inspector, String confirmer, String date) {
        clearTable();
        ConnectDB.getFilteredInspectRecords(type, vendor, product, inspector, confirmer, date, records -> {
            DataSource.setInspectRecords(records);
            Log.d("records:",records.toString());
            runOnUiThread(this::onInspectDataReady);
            TextView textSelectedDate = findViewById(R.id.date_text);
            textSelectedDate.setText("選擇日期");
            textSelectedDate.setTextColor(Color.parseColor("#000000"));
        });
    }
    private void setupSpinner(String type) {
        Spinner vendorSpinner = findViewById(R.id.spinnerVendor);
        Spinner productSpinner = findViewById(R.id.spinnerProduct);

        // ✅ 建立 vendor 清單，僅取指定 type 的 vendor
        List<String> vendorList = new ArrayList<>();
        vendorList.add("廠商");

        for (Map.Entry<String, VendorInfo> entry : DataSource.getVendorProductMap().entrySet()) {
            if (type.equals(entry.getValue().getType())) {
                vendorList.add(entry.getKey()); // 廠商名稱
            }
        }

        ArrayAdapter<String> vendorAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, vendorList
        );
        vendorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vendorSpinner.setAdapter(vendorAdapter);

        // ✅ 預設載入所有該 type 的產品
        List<String> allProducts = new ArrayList<>();
        allProducts.add("產品");

        for (VendorInfo info : DataSource.getVendorProductMap().values()) {
            if (type.equals(info.getType())) {
                allProducts.addAll(info.getProducts());
            }
        }

        ArrayAdapter<String> productAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, allProducts
        );
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productSpinner.setAdapter(productAdapter);

        // ✅ 廠商選擇時更新產品
        vendorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVendor = vendorSpinner.getSelectedItem().toString();
                List<String> updatedProducts = new ArrayList<>();
                updatedProducts.add("產品");

                if ("廠商".equals(selectedVendor)) {
                    for (VendorInfo info : DataSource.getVendorProductMap().values()) {
                        if (type.equals(info.getType())) {
                            updatedProducts.addAll(info.getProducts());
                        }
                    }
                } else {
                    updatedProducts.addAll(DataSource.getProductsByVendor(selectedVendor));
                }

                productAdapter.clear();
                productAdapter.addAll(updatedProducts);
                productAdapter.notifyDataSetChanged();
                productSpinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 其他 spinner 初始化
        setupSpinnerData(R.id.spinnerInspector, DataSource.getInspector(), "", "inspect");
        setupSpinnerData(R.id.spinnerConfirmPerson, DataSource.getConfirmPerson(), "", "confirm");
    }

    private void setupSpinnerData(int spinnerId, List<String> data, String selectedData, String type) {
        Spinner spinner = findViewById(spinnerId);
        setupSpinnerAdapter(spinner, data, selectedData, type);
    }

    private void setupSpinnerAdapter(Spinner spinner, List<String> data, String selectedData, String type) {
        List<String> spinnerData = new ArrayList<>();
        if (type == "inspect"){
            spinnerData.add("驗收人員");
        }else {
            spinnerData.add("確認人員");
        }
        spinnerData.addAll(data);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerData
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (selectedData != null && !selectedData.trim().isEmpty()) {
            int position = adapter.getPosition(selectedData);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }
    private void addTableRow(String importId, String date, String vendor, String itemName, String spec, String packageConfirm, String vector,
                             String packageLabel, String amount, String validDate, String pallet, String COA, String note, String picture, String inspector,
                             String confirmed, String odor, String degree, String type) {
        LinearLayout tableLayout = findViewById(R.id.inspectTable);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(20, 10, 20, 10);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int textSize = 16;
        int padding = 8;

        // 建立 Cell 方法（TextView）
        TextView tableDate = createCell("date", date, 120, textSize, padding);
        TextView tableVendor = createCell("vendor", vendor, 120, textSize, padding);
        TextView tableItem = createCell("item", itemName, 100, textSize, padding);
        TextView tableSpec = createCell("spec", spec, 100, textSize, padding);

        TextView tablePackage = createCell("package", toCheckSymbol(packageConfirm), 80, textSize, padding);
        TextView tableVector = createCell("vector", toCheckSymbol(vector), 80, textSize, padding);
        TextView tableLabel = createCell("label", toCheckSymbol(packageLabel), 100, textSize, padding);
        TextView tableAmount = createCell("amount", amount, 100, textSize, padding); // 數值無需轉換
        TextView tableValidDate = createCell("validDate", toCheckSymbol(validDate), 120, textSize, padding);
        TextView tablePallet = createCell("pallet", toCheckSymbol(pallet), 80, textSize, padding);
        TextView tableCoa = createCell("coa", toCheckSymbol(COA), 80, textSize, padding);

        String noteText = (note == null || note.trim().isEmpty() || note.equalsIgnoreCase("null")) ? "" : note;
        TextView tableNote = createCell("note",noteText, 200, textSize, padding);

        // 加入所有欄位到該列
        rowLayout.addView(tableDate);
        rowLayout.addView(tableVendor);
        rowLayout.addView(tableItem);
        rowLayout.addView(tableSpec);
        rowLayout.addView(tablePackage);
        if("原料".equals(type)){
            TextView tableOdor = createCell("odor",toCheckSymbol(odor), 80, textSize, padding);
            rowLayout.addView(tableOdor);
        }
        rowLayout.addView(tableVector);
        if("原料".equals(type)){
            String degreeText = (degree == null || degree.trim().isEmpty() || degree.equalsIgnoreCase("null")) ? "" : degree + "°C";
            TextView tableDegree = createCell("degree",degreeText, 80, textSize, padding);
            rowLayout.addView(tableDegree);
        }
        rowLayout.addView(tableLabel);
        rowLayout.addView(tableAmount);
        rowLayout.addView(tableValidDate);
        rowLayout.addView(tablePallet);
        rowLayout.addView(tableCoa);
        rowLayout.addView(tableNote);

        View inspectorView;
        if (inspector == null || inspector.trim().isEmpty() || inspector.equalsIgnoreCase("null")) {
            Button inspectorBtn = new Button(this);
             inspectorBtn.setText("驗收");
            inspectorBtn.setBackgroundResource(R.drawable.btn_blue);
            inspectorBtn.setTextSize(textSize);
            inspectorBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
            inspectorBtn.setPadding(padding, padding, padding, padding);
            inspectorBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics()),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            // 可選：加入點擊事件
            inspectorBtn.setOnClickListener(v -> {
                Intent intent = new Intent(InspectTable.this, InspectDetail.class);
                intent.putExtra("mode", "edit");
                intent.putExtra("type", type);
                intent.putExtra("importId", importId);
                intent.putExtra("date", date);
                intent.putExtra("vendor", vendor);
                intent.putExtra("itemName", itemName);
                intent.putExtra("amount", amount);
                intent.putExtra("staff", "inspector");
                startActivity(intent);
            });
            inspectorView = inspectorBtn;
        } else {
            inspectorView = createCell("inspector", inspector, 100, textSize, padding);
        }

        View confirmView;
        if (confirmed == null || confirmed.trim().isEmpty() || confirmed.equalsIgnoreCase("null")) {
            Button confirmBtn = new Button(this);
            confirmBtn.setText("確認");
            confirmBtn.setBackgroundResource(R.drawable.btn_green);
            confirmBtn.setTextSize(textSize);
            confirmBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
            confirmBtn.setPadding(padding, padding, padding, padding);
            confirmBtn.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics()),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            // 可選：加入點擊事件
            confirmBtn.setOnClickListener(v -> {
                LayoutInflater inflater = LayoutInflater.from(InspectTable.this);
                View dialogView = inflater.inflate(R.layout.dialog_password, null);
                EditText editPassword = dialogView.findViewById(R.id.editPassword);

                new AlertDialog.Builder(InspectTable.this) // 🔧 修正 this
                        .setTitle("密碼驗證")
                        .setView(dialogView)
                        .setPositiveButton("確定", (dialog, which) -> {
                            String password = editPassword.getText().toString().trim();
                            if (DataSource.getPasswords().contains(password)) {
                                // 驗證成功後才跳轉
                                Intent intent = new Intent(InspectTable.this, InspectDetail.class);
                                intent.putExtra("mode", "edit");
                                intent.putExtra("type", type);
                                intent.putExtra("importId", importId);
                                intent.putExtra("date", date);
                                intent.putExtra("vendor", vendor);
                                intent.putExtra("itemName", itemName);
                                intent.putExtra("spec", spec);
                                intent.putExtra("packageConfirm", packageConfirm);
                                intent.putExtra("vector", vector);
                                intent.putExtra("packageLabel", packageLabel);
                                intent.putExtra("amount", amount);
                                intent.putExtra("validDate", validDate);
                                intent.putExtra("pallet", pallet);
                                intent.putExtra("COA", COA);
                                intent.putExtra("note", note);
                                intent.putExtra("picture", picture);
                                intent.putExtra("inspector", inspector);
                                intent.putExtra("confirmed", confirmed);
                                if ("原料".equals(type)) {
                                    intent.putExtra("odor", odor);
                                    intent.putExtra("degree", degree);
                                }
                                intent.putExtra("staff", "confirm");
                                startActivity(intent);
                            } else {
                                Toast.makeText(InspectTable.this, "密碼錯誤", Toast.LENGTH_SHORT).show(); // 🔧 修正 this
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            });
            confirmView = confirmBtn;
        } else {
            confirmView = createCell("confirmed",confirmed, 100, textSize, padding);
        }

        rowLayout.addView(inspectorView);
        rowLayout.addView(confirmView);

        // 加入 icon（eye icon）
        ImageView eyeIcon = new ImageView(this);
        eyeIcon.setImageResource(R.drawable.ic_eye);
        eyeIcon.setContentDescription("檢視細節");
        eyeIcon.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        iconParams.setMargins(10, 0, 10, 0);
        eyeIcon.setLayoutParams(iconParams);

        eyeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(InspectTable.this, InspectDetail.class);
            intent.putExtra("mode", "view");
            intent.putExtra("type", type);
            intent.putExtra("importId", importId);
            intent.putExtra("date", date);
            intent.putExtra("vendor", vendor);
            intent.putExtra("itemName", itemName);
            intent.putExtra("spec", spec);
            intent.putExtra("packageConfirm", packageConfirm);
            intent.putExtra("vector", vector);
            intent.putExtra("packageLabel", packageLabel);
            intent.putExtra("amount", amount);
            intent.putExtra("validDate", validDate);
            intent.putExtra("pallet", pallet);
            intent.putExtra("COA", COA);
            intent.putExtra("note", note);
            intent.putExtra("picture", picture);
            intent.putExtra("inspector", inspector);
            intent.putExtra("confirmed", confirmed);
            if ("原料".equals(type)) {
                intent.putExtra("odor", odor);
                intent.putExtra("degree", degree);
            }
            intent.putExtra("staff", "confirm");
            startActivity(intent);
        });

        rowLayout.addView(eyeIcon);

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

    private TextView createCell(String column, String text, int widthDp, int textSize, int padding) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(Color.BLACK);
        if ("note".equalsIgnoreCase(column)) {
            textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); // 左對齊 & 垂直置中
        } else {
            textView.setGravity(Gravity.CENTER); // 其他欄位居中
        }
        textView.setPadding(padding, padding, padding, padding);
        textView.setTypeface(null, Typeface.NORMAL);
        int widthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, widthDp, getResources().getDisplayMetrics());

        textView.setLayoutParams(new LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT));
        return textView;
    }

    private String toCheckSymbol(String value) {
        if (value == null || value.trim().isEmpty()|| value.equalsIgnoreCase("null")) {
            return "";
        }
        switch (value) {
            case "0":
                return "✘";
            case "1":
                return "✔";
            default:
                return value;
        }
    }
}
