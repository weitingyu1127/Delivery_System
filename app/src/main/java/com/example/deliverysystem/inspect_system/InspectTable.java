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
import com.example.deliverysystem.utility.Patterns;
import com.example.deliverysystem.utility.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class InspectTable extends BaseActivity {
    /** 廠商類型(原or物)*/
    private String type;

    /** 進貨紀錄Table */
    ViewGroup inspectTable;
    
    /** 日期選擇 */
    TextView selectedDate;

    /** 選擇進貨地點 */
    private String currentPlace = "本廠"; // 預設本廠

    Map<String, Button> placeMap = new HashMap<>();
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 設定不同Table
        type = getIntent().getStringExtra("type");
        if ("物料".equals(type)) {
            setContentView(R.layout.inspect_material_table);
        } else if ("原料".equals(type)) {
            setContentView(R.layout.inspect_ingredient_table);
        }
        inspectTable = findViewById(R.id.inspectTable);

        // 日期選擇
        selectedDate = findViewById(R.id.date_text);
        ImageView calendarIcon = findViewById(R.id.calendar_icon);
        View.OnClickListener dateClickListener = v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    selectedDate.setText(String.format(Locale.TAIWAN, "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay));
                    selectedDate.setTextColor(getResources().getColor(R.color.black));
                    selectedDate.setTypeface(null, Typeface.NORMAL);
                },
                year, month, day
            );
            datePickerDialog.show();
        };
        selectedDate.setOnClickListener(dateClickListener);
        calendarIcon.setOnClickListener(dateClickListener);

        // 搜尋Button
        Button searchBtn = findViewById(R.id.btnSearch);
        searchBtn.setOnClickListener(v -> {
            Spinner vendorSp  = findViewById(R.id.spinnerVendor);
            Spinner productSp = findViewById(R.id.spinnerProduct);
            Spinner inspectorSp = findViewById(R.id.spinnerInspector);
            Spinner confirmerSp = findViewById(R.id.spinnerConfirmPerson);
            String vendor = vendorSp.getSelectedItemPosition() == 0 ? "" : vendorSp.getSelectedItem().toString();
            String product = productSp.getSelectedItemPosition() == 0 ? "" : productSp.getSelectedItem().toString();
            String inspector = inspectorSp.getSelectedItemPosition() == 0 ? "" : inspectorSp.getSelectedItem().toString();
            String confirmer = confirmerSp.getSelectedItemPosition() == 0 ? "" : confirmerSp.getSelectedItem().toString();
            String date = "選擇進貨日期".contentEquals(selectedDate.getText()) ? "" : selectedDate.getText().toString();
            filterRecords(type, vendor, product, inspector, confirmer, date, currentPlace);
        });

        // 地點Button
        Button btnA = findViewById(R.id.btn_place_A);
        Button btnB = findViewById(R.id.btn_place_B);
        Button btnC = findViewById(R.id.btn_place_C);
        btnA.setTag("本廠");
        btnB.setTag("倉庫");
        btnC.setTag("線西");
        placeMap.put("本廠", btnA);
        placeMap.put("倉庫", btnB);
        placeMap.put("線西", btnC);
        View.OnClickListener placeClick = v -> {
            currentPlace = (String) v.getTag();
            changePlace(currentPlace);
        };
        btnA.setOnClickListener(placeClick);
        btnB.setOnClickListener(placeClick);
        btnC.setOnClickListener(placeClick);
        changePlace(currentPlace);

        // 載入更多Btn
        loadButton();
        employeeSpinner(R.id.spinnerInspector, DataSource.getInspector(), "驗收人員","name");
        employeeSpinner(R.id.spinnerConfirmPerson, DataSource.getConfirmPerson(), "確認人員","name");
    }
    /** 重新加載 */
    protected void onResume() {
        super.onResume();
        getInspectData(true);
    }

    /** 取得驗收紀錄 */
    private void getInspectData(boolean firstLoad) {
        ConnectDB.loadInspectRecords(type, currentPlace, firstLoad, records -> {
            if (firstLoad) {
                // 第一次 or 切換地點
                DataSource.setInspectRecords(records);
                runOnUiThread(this::onInspectDataReady);
            } else {
                // 載入更多
                List<InspectRecord> all = DataSource.getInspectRecords();
                all.addAll(records);
                DataSource.setInspectRecords(all);
                runOnUiThread(() -> {
                    for (int i = all.size(); i < all.size(); i++) {
                        InspectRecord data = all.get(i);
                        addTableRow(
                                data.getImportId(),
                                data.getImportDate(),
                                data.getVendor(),
                                data.getProduct(),
                                data.getStandard(),
                                data.getPackageComplete(),
                                data.getVector(),
                                data.getPackageLabel(),
                                data.getQuantity(),
                                data.getValidDate(),
                                data.getPalletComplete(),
                                data.getCoa(),
                                data.getNote(),
                                data.getPlace(),
                                data.getInspectorStaff(),
                                data.getConfirmStaff(),
                                data.getOdor(),
                                data.getDegree(),
                                type
                        );
                    }
                });
            }
        });
    }

    /** 加載 Button */
    private void loadButton() {
        Button loadMoreBtn = findViewById(R.id.btnLoadMore);
        loadMoreBtn.setOnClickListener(v -> {
            getInspectData(false);
        });
    }

    /** 重新渲染驗收紀錄 */
    private void onInspectDataReady() {
        Tools.clearTable(inspectTable);
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

    /** 篩選 */
    private void filterRecords(String type, String vendor, String product, String inspector, String confirmer, String date, String place) {
        Tools.clearTable(inspectTable);
        ConnectDB.getFilteredInspectRecords(type, vendor, product, inspector, confirmer, date, place, records -> {
            DataSource.setInspectRecords(records);
            runOnUiThread(this::onInspectDataReady);
            selectedDate.setText("選擇進貨日期");
            selectedDate.setTextColor(Color.parseColor("#000000"));
        });
    }

    /** 下拉選單: 廠商 & 商品 */
    private void setupSpinner(String type) {
        Spinner vendorSpinner = findViewById(R.id.spinnerVendor);
        Spinner productSpinner = findViewById(R.id.spinnerProduct);

        // 取得該 type 對應的廠商
        List<String> vendorList = new ArrayList<>();
        vendorList.add("選擇廠商");

        for (Map.Entry<String, VendorInfo> entry : DataSource.getVendorProductMap().entrySet()) {
            if (type.equals(entry.getValue().getType())) {
                vendorList.add(entry.getKey());
            }
        }

        ArrayAdapter<String> vendorAdapter = createAdapter(vendorList);
        vendorSpinner.setAdapter(vendorAdapter);

        // 建立產品清單
        List<String> allProductList = new ArrayList<>();
        allProductList.add("選擇產品");

        Set<String> productSet = new LinkedHashSet<>();
        for (VendorInfo info : DataSource.getVendorProductMap().values()) {
            if (type.equals(info.getType())) {
                productSet.addAll(info.getProducts());
            }
        }
        allProductList.addAll(productSet);

        ArrayAdapter<String> productAdapter = createAdapter(allProductList);
        productSpinner.setAdapter(productAdapter);

        // 動態更新選單: 廠商 → 產品
        vendorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedVendor = vendorSpinner.getSelectedItem().toString();
                List<String> newProducts = new ArrayList<>();
                newProducts.add("選擇產品");
                if ("選擇廠商".equals(selectedVendor)) {
                    newProducts.addAll(productSet); // 所有產品
                } else {
                    newProducts.addAll(DataSource.getProductsByVendor(selectedVendor)); // 該廠商的產品
                }
                // 更新選單內容
                productAdapter.clear();
                productAdapter.addAll(newProducts);
                productAdapter.notifyDataSetChanged();
                productSpinner.setSelection(0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /** 下拉選單: 驗收 & 確認 */
    private void employeeSpinner(int spinnerId, List<Map<String, String>> data, String defaultLabel, String name) {
        Spinner spinner = findViewById(spinnerId);

        List<String> displayList = new ArrayList<>();
        displayList.add(defaultLabel);

        for (Map<String, String> item : data) {
            displayList.add(item.get(name));
        }

        ArrayAdapter<String> adapter = createAdapter(displayList);
        spinner.setAdapter(adapter);
    }

    /** 產生基本 ArrayAdapter */
    private ArrayAdapter<String> createAdapter(List<String> data) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    /** 更換地點button */
    private void changePlace(String place){
        Tools.selectPlace(this, place, placeMap);
        getInspectData(true);
    }

    /** 建立Table欄位 */
    private void addTableRow(String importId, String date, String vendor, String itemName, String spec, String packageConfirm, String vector,
                             String packageLabel, String amount, String validDate, String pallet, String COA, String note, String place, String inspector,
                             String confirmed, String odor, String degree, String type) {

        LinearLayout inspectTable = findViewById(R.id.inspectTable);
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
        View tablePackage = createCell("package", packageConfirm, checkIcon(packageConfirm), 80, textSize, padding);
        View tableVector = createCell("vector", vector, checkIcon(vector), 80, textSize, padding);
        View tableLabel = createCell("label", packageLabel, checkIcon(packageLabel), 100, textSize, padding);
        TextView tableAmount = createCell("amount", amount, 100, textSize, padding);
        View tableValidDate = createCell("validDate", validDate, checkIcon(validDate), 120, textSize, padding);
        View tablePallet = createCell("pallet", pallet, checkIcon(pallet), 80, textSize, padding);
        View tableCoa = createCell("coa", COA, checkIcon(COA), 80, textSize, padding);
        String noteText = (note == null || note.trim().isEmpty() || note.equalsIgnoreCase("null")) ? "" : note;
        TextView tableNote = createCell("note",noteText, 200, textSize, padding);
        TextView tablePlace = createCell("place",place, 80, textSize, padding);
        View inspectorView;

        // 驗收btn
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
            Bundle paramsInspector = buildParams(
                    "edit", type, importId, date, vendor, itemName, null, null, null, 
                    null, amount,null, null, null, null, place, null, 
                    null, null, null, "inspector"               
            );
            Tools.navigator(inspectorBtn, this, InspectDetail.class, false, paramsInspector);
            inspectorView = inspectorBtn;
        } else {
            inspectorView = createCell("inspector", inspector, 100, textSize, padding);
        }
        rowLayout.addView(tableDate);
        rowLayout.addView(tableVendor);
        rowLayout.addView(tableItem);
        rowLayout.addView(inspectorView);
        rowLayout.addView(tableSpec);
        rowLayout.addView(tablePackage);
        // 異味 & 溫度
        if("原料".equals(type)){
            View tableOdor = createCell("odor", odor, checkIcon(odor), 80, textSize, padding);
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

        // 確認btn
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
            // 組合參數
            Bundle params = buildParams(
                    "edit", type, importId, date,
                    vendor, itemName, spec,
                    packageConfirm, vector, packageLabel,
                    amount, validDate, pallet, COA,
                    note, place, inspector, confirmed,
                    odor, degree,"confirm"
            );

            Tools.navigator(confirmBtn, this, InspectDetail.class, true, params);
            confirmView = confirmBtn;
        } else {
            confirmView = createCell("confirmed",confirmed, 100, textSize, padding);
        }
        rowLayout.addView(confirmView);

        // 檢視btn
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
            Bundle paramsView = buildParams(
                    "view", type, importId, date,
                    vendor, itemName, spec,
                    packageConfirm, vector, packageLabel,
                    amount, validDate, pallet, COA,
                    note, place, inspector, confirmed,
                    odor, degree,
                    "confirm"
            );
            Tools.navigator(eyeIcon, this, InspectDetail.class, false, paramsView);
        });
        rowLayout.addView(eyeIcon);

        // 修改btn
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
            Bundle paramsEdit = buildParams(
                    "edit", type, importId, date,
                    vendor, itemName, spec,
                    packageConfirm, vector, packageLabel,
                    amount, validDate, pallet, COA,
                    note, place, inspector, confirmed,
                    odor, degree,
                    "confirm"
            );
            Tools.navigator(editIcon, this, InspectDetail.class, true, paramsEdit);
        });
        rowLayout.addView(editIcon);

        //  刪除btn
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
                    int qty = Integer.parseInt(amount.replaceAll("[^0-9]", ""));
                    ConnectDB.adjustQuantity(place, type, vendor, itemName, -qty, successAdj -> {
                        if (successAdj) {
                            ConnectDB.deleteImportRecordById(importId, success -> {
                                runOnUiThread(() -> {
                                    if (success) {
                                        Tools.showToast(this,"刪除成功，庫存已更新");
                                        getInspectData(true);
                                    } else {
                                        Tools.showToast(this,"刪除失敗");
                                    }
                                });
                            });
                        } else {
                            runOnUiThread(() ->
                                Tools.showToast(this,"刪除失敗：庫存不足")
                            );
                        }
                    });
                })
                .setNegativeButton("取消", null)
                .show();
        });
        inspectTable.addView(rowLayout);
    }

    private TextView createCell(String column, String text, int widthDp, int textSize, int padding) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(Color.BLACK);
        if ("note".equalsIgnoreCase(column)) {
            textView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL); 
        } else {
            textView.setGravity(Gravity.CENTER); 
        }
        textView.setPadding(padding, padding, padding, padding);
        textView.setTypeface(null, Typeface.NORMAL);
        int widthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, widthDp, getResources().getDisplayMetrics());

        textView.setLayoutParams(new LinearLayout.LayoutParams(widthPx, ViewGroup.LayoutParams.MATCH_PARENT));
        return textView;
    }

    /** (不)合格icon */
    private int checkIcon(String value) {
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

    private View createCell(String tag, String value, int iconRes, int widthDp, int textSize, int padding) {
        int widthPx  = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, getResources().getDisplayMetrics());
        int heightPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36,    getResources().getDisplayMetrics());

        if (iconRes != 0) {
            ImageView icon = new ImageView(this);
            icon.setImageResource(iconRes);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(widthPx, heightPx);
            lp.gravity = Gravity.CENTER_VERTICAL;
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
    /** 建立導轉參數 */
    private Bundle buildParams(
            String mode, String type, String importId, String date,
            String vendor, String itemName, String spec,
            String packageConfirm, String vector, String packageLabel,
            String amount, String validDate, String pallet, String COA,
            String note, String place, String inspector, String confirmed,
            String odor, String degree,
            String staff
    ) {
        Bundle bundle = new Bundle();
        bundle.putString("mode", mode);
        bundle.putString("type", type);
        bundle.putString("importId", importId);
        bundle.putString("date", date);
        bundle.putString("vendor", vendor);
        bundle.putString("itemName", itemName);
        bundle.putString("spec", spec);
        bundle.putString("packageConfirm", packageConfirm);
        bundle.putString("vector", vector);
        bundle.putString("packageLabel", packageLabel);
        bundle.putString("amount", amount);
        bundle.putString("validDate", validDate);
        bundle.putString("pallet", pallet);
        bundle.putString("COA", COA);
        bundle.putString("note", note);
        bundle.putString("place", place);
        bundle.putString("inspector", inspector);
        bundle.putString("confirmed", confirmed);
        if ("原料".equals(type)) {
            bundle.putString("odor", odor);
            bundle.putString("degree", degree);
        }
        bundle.putString("staff", staff);
        return bundle;
    }
}
