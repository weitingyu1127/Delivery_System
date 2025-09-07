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
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class InspectTable extends BaseActivity {
    private String type;
    private String currentPlace = "本廠";
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = getIntent().getStringExtra("type");
        if ("物料".equals(type)) {
            setContentView(R.layout.inspect_material_table);
        } else if ("原料".equals(type)) {
            setContentView(R.layout.inspect_ingredient_table);
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
            Spinner vendorSp    = findViewById(R.id.spinnerVendor);
            Spinner productSp   = findViewById(R.id.spinnerProduct);
            Spinner inspectorSp = findViewById(R.id.spinnerInspector);
            Spinner confirmerSp = findViewById(R.id.spinnerConfirmPerson);
            TextView dateTv     = findViewById(R.id.date_text);
            String vendor    = vendorSp.getSelectedItemPosition()    == 0 ? "" : vendorSp.getSelectedItem().toString();
            String product   = productSp.getSelectedItemPosition()   == 0 ? "" : productSp.getSelectedItem().toString();
            String inspector = inspectorSp.getSelectedItemPosition() == 0 ? "" : inspectorSp.getSelectedItem().toString();
            String confirmer = confirmerSp.getSelectedItemPosition() == 0 ? "" : confirmerSp.getSelectedItem().toString();
            String date      = "選擇進貨日期".contentEquals(dateTv.getText()) ? "" : dateTv.getText().toString();

            fetchFilteredRecords(type, vendor, product, inspector, confirmer, date, currentPlace);
        });

        Button btnA = findViewById(R.id.btn_place_A);
        Button btnB = findViewById(R.id.btn_place_B);
        Button btnC = findViewById(R.id.btn_place_C);

        View.OnClickListener placeClick = v -> {
            if (v.getId() == R.id.btn_place_A) {
                selectPlace("本廠", btnA, btnB, btnC);
            } else if (v.getId() == R.id.btn_place_B) {
                selectPlace("倉庫", btnB, btnA, btnC);
            } else if (v.getId() == R.id.btn_place_C) {
                selectPlace("線西", btnC, btnA, btnB);
            }
        };
        btnA.setOnClickListener(placeClick);
        btnB.setOnClickListener(placeClick);
        btnC.setOnClickListener(placeClick);
        selectPlace("本廠", btnA, btnB, btnC);

    }

    protected void onResume() {
        super.onResume();
        getInspectData();
    }
    private void getInspectData() {
        ConnectDB.getInspectRecords(type, currentPlace, inspectList -> {
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
                        record.getPlace(),
                        record.getInspectorStaff(),
                        record.getConfirmStaff(),
                        record.getOdor(),
                        record.getDegree(),
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
                        record.getPlace(),
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
        tableLayout.removeAllViews();
    }
    private void fetchFilteredRecords(String type, String vendor, String product, String inspector, String confirmer, String date, String place) {
        clearTable();
        ConnectDB.getFilteredInspectRecords(type, vendor, product, inspector, confirmer, date, place, records -> {
            DataSource.setInspectRecords(records);
            runOnUiThread(this::onInspectDataReady);
            TextView textSelectedDate = findViewById(R.id.date_text);
            textSelectedDate.setText("選擇進貨日期");
            textSelectedDate.setTextColor(Color.parseColor("#000000"));
        });
    }
    private void setupSpinner(String type) {
        Spinner vendorSpinner = findViewById(R.id.spinnerVendor);
        Spinner productSpinner = findViewById(R.id.spinnerProduct);

        List<String> vendorList = new ArrayList<>();
        vendorList.add("選擇廠商");

        for (Map.Entry<String, VendorInfo> entry : DataSource.getVendorProductMap().entrySet()) {
            if (type.equals(entry.getValue().getType())) {
                vendorList.add(entry.getKey());
            }
        }

        ArrayAdapter<String> vendorAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, vendorList
        );
        vendorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vendorSpinner.setAdapter(vendorAdapter);

        Set<String> allProductsSet = new LinkedHashSet<>();
        for (VendorInfo info : DataSource.getVendorProductMap().values()) {
            if (type.equals(info.getType())) {
                allProductsSet.addAll(info.getProducts());
            }
        }
        List<String> allProducts = new ArrayList<>();
        allProducts.add("選擇產品");
        allProducts.addAll(allProductsSet);

        ArrayAdapter<String> productAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, allProducts);
        productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        productSpinner.setAdapter(productAdapter);

        vendorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVendor = vendorSpinner.getSelectedItem().toString();

                Set<String> uniq = new LinkedHashSet<>();
                if ("選擇廠商".equals(selectedVendor)) {
                    for (VendorInfo info : DataSource.getVendorProductMap().values()) {
                        if (type.equals(info.getType())) {
                            uniq.addAll(info.getProducts());
                        }
                    }
                } else {
                    uniq.addAll(DataSource.getProductsByVendor(selectedVendor));
                }

                List<String> updated = new ArrayList<>();
                updated.add("選擇產品");
                updated.addAll(uniq);

                productAdapter.clear();
                productAdapter.addAll(updated);
                productAdapter.notifyDataSetChanged();
                productSpinner.setSelection(0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupSpinnerData(R.id.spinnerInspector, DataSource.getInspector(), "", "inspect");
        setupSpinnerData(R.id.spinnerConfirmPerson, DataSource.getConfirmPerson(), "", "confirm");
    }

    private void setupSpinnerData(int spinnerId, List<Map<String, String>> data, String selectedId, String type) {
        Spinner spinner = findViewById(spinnerId);
        setupSpinnerAdapter(spinner, data, selectedId, type);
    }
    private void setupSpinnerAdapter(Spinner spinner, List<Map<String, String>> data, String selectedId, String type) {
        List<String> spinnerDisplayList = new ArrayList<>();
        if ("inspect".equals(type)) {
            spinnerDisplayList.add("驗收人員");
        } else {
            spinnerDisplayList.add("確認人員");
        }

        // 把 Map 中的 name 加到顯示清單
        for (Map<String, String> emp : data) {
            spinnerDisplayList.add(emp.get("name"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, spinnerDisplayList
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (selectedId != null && !selectedId.trim().isEmpty()) {
            int position = -1;
            for (int i = 0; i < data.size(); i++) {
                if (selectedId.equals(data.get(i).get("id"))) {
                    position = i + 1; // +1 因為第0項是提示文字
                    break;
                }
            }
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }
    private void addTableRow(String importId, String date, String vendor, String itemName, String spec, String packageConfirm, String vector,
                             String packageLabel, String amount, String validDate, String pallet, String COA, String note, String place, String inspector,
                             String confirmed, String odor, String degree, String type) {
        LinearLayout tableLayout = findViewById(R.id.inspectTable);

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setBaselineAligned(false);
        rowLayout.setGravity(Gravity.CENTER_VERTICAL);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(20, 10, 20, 10);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int textSize = 16;
        int padding = 8;

        TextView tableDate = createCell("date", date, 120, textSize, padding);
        TextView tableVendor = createCell("vendor", vendor, 120, textSize, padding);
        TextView tableItem = createCell("item", itemName, 250, textSize, padding);
        TextView tableSpec = createCell("spec", spec, 250, textSize, padding);
        View tablePackage = createIconOrTextCell("package", packageConfirm, toCheckIconRes(packageConfirm), 80, textSize, padding);
        View tableVector = createIconOrTextCell("vector", vector, toCheckIconRes(vector), 80, textSize, padding);
        View tableLabel = createIconOrTextCell("label", packageLabel, toCheckIconRes(packageLabel), 100, textSize, padding);
        TextView tableAmount = createCell("amount", amount, 100, textSize, padding);
        View tableValidDate = createIconOrTextCell("validDate", validDate, toCheckIconRes(validDate), 120, textSize, padding);
        View tablePallet = createIconOrTextCell("pallet", pallet, toCheckIconRes(pallet), 80, textSize, padding);
        View tableCoa = createIconOrTextCell("coa", COA, toCheckIconRes(COA), 80, textSize, padding);

        String noteText = (note == null || note.trim().isEmpty() || note.equalsIgnoreCase("null")) ? "" : note;
        TextView tableNote = createCell("note",noteText, 200, textSize, padding);
        TextView tablePlace = createCell("place",place, 80, textSize, padding);
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
            inspectorBtn.setOnClickListener(v -> {
                openInspectDetail(
                        "edit", type, importId, date,
                        vendor, itemName, null,
                        null, null, null,
                        amount, null, null, null,
                        null, place, null, null,
                        null, null, "inspector"
                );
            });
            inspectorView = inspectorBtn;
        } else {
            inspectorView = createCell("inspector", inspector, 100, textSize, padding);
        }
        // 加入所有欄位到該列
        rowLayout.addView(tableDate);
        rowLayout.addView(tableVendor);
        rowLayout.addView(tableItem);
        rowLayout.addView(inspectorView);
        rowLayout.addView(tableSpec);
        rowLayout.addView(tablePackage);
        if("原料".equals(type)){
            View tableOdor = createIconOrTextCell("odor", odor, toCheckIconRes(odor), 80, textSize, padding);
            rowLayout.addView(tableOdor);
        }
        rowLayout.addView(tableVector);
        if("原料".equals(type)){
            String degreeText = (degree == null || degree.trim().isEmpty() || degree.equalsIgnoreCase("null")) ? "" : degree;
            TextView tableDegree = createCell("degree",degreeText, 110, textSize, padding);
            rowLayout.addView(tableDegree);
        }
        rowLayout.addView(tableLabel);
        rowLayout.addView(tableAmount);
        rowLayout.addView(tableValidDate);
        rowLayout.addView(tablePallet);
        rowLayout.addView(tableCoa);
        rowLayout.addView(tableNote);
        rowLayout.addView(tablePlace);

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
            confirmBtn.setOnClickListener(v -> {
                LayoutInflater inflater = LayoutInflater.from(InspectTable.this);
                View dialogView = inflater.inflate(R.layout.dialog_password, null);
                EditText editPassword = dialogView.findViewById(R.id.editPassword);

                new AlertDialog.Builder(InspectTable.this)
                        .setTitle("密碼驗證")
                        .setView(dialogView)
                        .setPositiveButton("確定", (dialog, which) -> {
                            String password = editPassword.getText().toString().trim();
                            if (DataSource.getPasswords().contains(password)) {
                                openInspectDetail(
                                        "edit", type, importId, date,
                                        vendor, itemName, spec,
                                        packageConfirm, vector, packageLabel,
                                        amount, validDate, pallet, COA,
                                        note, place, inspector, confirmed,
                                        odor, degree, "confirm"
                                );
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

        rowLayout.addView(confirmView);

        // 檢視（eye icon）
        ImageView eyeIcon = new ImageView(this);
        eyeIcon.setImageResource(R.drawable.ic_eye);
        eyeIcon.setContentDescription("檢視細節");
        eyeIcon.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams eyeIconParams = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        eyeIconParams.setMargins(10, 0, 10, 0);
        eyeIconParams.gravity = Gravity.CENTER_VERTICAL;
        eyeIcon.setLayoutParams(eyeIconParams);

        eyeIcon.setOnClickListener(v -> {
            openInspectDetail(
                    "view", type, importId, date,
                    vendor, itemName, spec,
                    packageConfirm, vector, packageLabel,
                    amount, validDate, pallet, COA,
                    note, place, inspector, confirmed,
                    odor, degree, "confirm"
            );
        });
        rowLayout.addView(eyeIcon);

        ImageView editIcon = new ImageView(this);
        editIcon.setImageResource(R.drawable.ic_edit);
        editIcon.setContentDescription("編輯");
        editIcon.setPadding(padding, padding, padding, padding);
        LinearLayout.LayoutParams editIconParams = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        editIconParams.setMargins(10, 0, 10, 0);
        editIconParams.gravity = Gravity.CENTER_VERTICAL;
        editIcon.setLayoutParams(editIconParams);
        editIcon.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(InspectTable.this);
            View dialogView = inflater.inflate(R.layout.dialog_password, null);
            EditText editPassword = dialogView.findViewById(R.id.editPassword);

            new AlertDialog.Builder(InspectTable.this)
                    .setTitle("密碼驗證")
                    .setView(dialogView)
                    .setPositiveButton("確定", (dialog, which) -> {
                        String password = editPassword.getText().toString().trim();
                        if (DataSource.getPasswords().contains(password)) {
                            openInspectDetail(
                                    "edit", type, importId, date,
                                    vendor, itemName, spec,
                                    packageConfirm, vector, packageLabel,
                                    amount, validDate, pallet, COA,
                                    note, place, inspector, confirmed,
                                    odor, degree, "confirm"
                            );
                        } else {
                            Toast.makeText(InspectTable.this, "密碼錯誤", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        rowLayout.addView(editIcon);

        ImageView deleteIcon = new ImageView(this);
        deleteIcon.setImageResource(R.drawable.ic_delete);
        deleteIcon.setContentDescription("刪除");
        deleteIcon.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams deleteIconParams = new LinearLayout.LayoutParams(
                (int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics()),
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        deleteIconParams.setMargins(10, 0, 10, 0);
        eyeIconParams.gravity = Gravity.CENTER_VERTICAL;
        deleteIcon.setLayoutParams(deleteIconParams);
        rowLayout.addView(deleteIcon);
        deleteIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("刪除確認")
                    .setMessage("進貨資料一併刪除")
                    .setPositiveButton("確認", (dialog, which) -> {
                        ConnectDB.deleteImportRecordById(importId, success -> {
                            if (success) {
                                Toast.makeText(this, "刪除成功", Toast.LENGTH_SHORT).show();
                                getInspectData();
                            } else {
                                runOnUiThread(() ->
                                        Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show()
                                );
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

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

    private int toCheckIconRes(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return 0;
        }
        switch (value) {
            case "0":
                return R.drawable.ic_cross;
            case "1":
                return R.drawable.ic_check;
            default:
                return 0;
        }
    }

    private View createIconOrTextCell(String tag, String value, int iconRes, int widthDp, int textSize, int padding) {
        int widthPx  = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, getResources().getDisplayMetrics());
        int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36,    getResources().getDisplayMetrics());

        if (iconRes != 0) {
            ImageView icon = new ImageView(this);
            icon.setImageResource(iconRes);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, heightPx);
            lp.gravity = Gravity.CENTER_VERTICAL;          // 👈 讓圖示在列中垂直置中
            icon.setLayoutParams(lp);
            icon.setPadding(padding, padding, padding, padding);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setAdjustViewBounds(true);
            icon.setTag(tag);
            return icon;
        } else {
            return createCell(tag, value, widthDp, textSize, padding);
        }
    }

    private void selectPlace(String place, Button selected, Button other1, Button other2) {
        currentPlace = place;
        styleSelected(selected);
        styleUnselected(other1);
        styleUnselected(other2);
        getInspectData();
    }

    private void styleSelected(Button btn) {
        btn.setBackgroundResource(R.drawable.btn_orange);
        btn.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void styleUnselected(Button btn) {
        btn.setBackgroundResource(R.drawable.btn_white);
        btn.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void openInspectDetail(
            String mode, String type, String importId, String date,
            String vendor, String itemName, String spec,
            String packageConfirm, String vector, String packageLabel,
            String amount, String validDate, String pallet, String COA,
            String note, String place, String inspector, String confirmed,
            String odor, String degree, String staff) {

        Intent intent = new Intent(InspectTable.this, InspectDetail.class);
        intent.putExtra("mode", mode);
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
        intent.putExtra("place", place);
        intent.putExtra("inspector", inspector);
        intent.putExtra("confirmed", confirmed);

        if ("原料".equals(type)) {
            intent.putExtra("odor", odor);
            intent.putExtra("degree", degree);
            if ("view".equals(mode)) {
                intent.putExtra("view", true); // 只有 view 模式才需要
            }
        }

        intent.putExtra("staff", staff);
        startActivity(intent);
    }

}
