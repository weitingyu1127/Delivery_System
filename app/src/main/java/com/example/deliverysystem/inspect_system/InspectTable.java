package com.example.deliverysystem.inspect_system;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.AppCompatButton;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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

        TextView textSelectedDate = findViewById(R.id.textselectedDate);
        ImageView calendarIcon = findViewById(R.id.calendarIcon);

        View.OnClickListener dateClickListener = v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String selectedDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                        textSelectedDate.setText(selectedDate);
                        textSelectedDate.setTextColor(getResources().getColor(R.color.gray));
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
            String date = ((TextView) findViewById(R.id.textselectedDate)).getText().toString();  // yyyy-MM-dd
            if (vendor.equals("請選擇")) vendor = "";
            if (product.equals("請選擇")) product = "";
            if (inspector.equals("請選擇")) inspector = "";
            if (confirmer.equals("請選擇")) confirmer = "";
            if (date.equals("")) date = "";
            fetchFilteredRecords(type, vendor, product, inspector, confirmer, date);
        });
        getInspectData();
    }

    protected void onResume() {
        super.onResume();
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
        Log.d("addtableRow", records.toString());

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
        TableLayout tableLayout = findViewById(R.id.inspectTable);
        tableLayout.removeAllViews();
    }
    private void fetchFilteredRecords(String type, String vendor, String product, String inspector, String confirmer, String date) {
        ConnectDB.getFilteredInspectRecords(type, vendor, product, inspector, confirmer, date, records -> {
            DataSource.setInspectRecords(records);
            runOnUiThread(this::onInspectDataReady);
            TextView textSelectedDate = findViewById(R.id.textselectedDate);
            textSelectedDate.setText("");  // 或 "請選擇" 等預設字串
        });
    }
    private void setupSpinner(String type) {
        Spinner vendorSpinner = findViewById(R.id.spinnerVendor);
        Spinner productSpinner = findViewById(R.id.spinnerProduct);

        // ✅ 建立 vendor 清單，僅取指定 type 的 vendor
        List<String> vendorList = new ArrayList<>();
        vendorList.add("請選擇");

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
        allProducts.add("請選擇");

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
                updatedProducts.add("請選擇");

                if ("請選擇".equals(selectedVendor)) {
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
        setupSpinnerData(R.id.spinnerInspector, DataSource.getInspector(), "");
        setupSpinnerData(R.id.spinnerConfirmPerson, DataSource.getConfirmPerson(), "");
    }

    private void setupSpinnerData(int spinnerId, List<String> data, String selectedData) {
        Spinner spinner = findViewById(spinnerId);
        setupSpinnerAdapter(spinner, data, selectedData);
    }

    private void setupSpinnerData(View dialogView, int spinnerId, List<String> data, String selectedData) {
        Spinner spinner = dialogView.findViewById(spinnerId);
        setupSpinnerAdapter(spinner, data, selectedData);
    }

    // 共同邏輯封裝
    private void setupSpinnerAdapter(Spinner spinner, List<String> data, String selectedData) {
        List<String> spinnerData = new ArrayList<>();
        spinnerData.add("請選擇");      // 預設項目
        spinnerData.addAll(data);       // 加入實際資料

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
    private void addTableRow(int importId, String date, String vendor, String itemName, String spec, String packageConfirm, String vector,
                             String packageLabel, String amount, String validDate, String pallet, String COA, String inspector, String confirmed, String odor, String degree, String type) {
        TableLayout tableLayout = findViewById(R.id.inspectTable);
        TableRow tableRow = new TableRow(this);

        // 共用樣式
        int textSize = 18;
        int padding = 8;

        // 建立 TextView 欄位
        TextView deliveredDate = createCell(date, 100, textSize, padding);
        TextView productVendor = createCell(vendor, 90, textSize, padding);
        TextView productItem = createCell(itemName, 180, textSize, padding);
        TextView productSpec = createCell(spec, 70, textSize, padding);

        TextView packageComplete = createCell("null".equals(packageConfirm) ? "" : "1".equals(packageConfirm) ? "✓" : "✗", 60, textSize, padding);
        TextView vectorCheck = createCell( "null".equals(vector) ? "" : "1".equals(vector) ? "✓" : "✗", 50, textSize, padding);
        int widthDp;
        if ("原料".equals(type)){
            widthDp = 80;
        }else{
            widthDp = 100;
        };
        TextView labelCheck = createCell("null".equals(packageLabel) ? "" : "1".equals(packageLabel) ? "✓" : "✗", widthDp, textSize, padding);
        TextView quantity = createCell(amount, 50, textSize, padding);
        TextView expireDate = createCell("null".equals(validDate) ? "" : validDate, 120, textSize, padding);
        TextView palletCheck = createCell("null".equals(pallet) ? "" : "1".equals(pallet) ? "✓" : "✗", 60, textSize, padding);
        TextView coaCheck = createCell("null".equals(COA) ? "" : "1".equals(COA) ? "✓" : "✗", 50, textSize, padding);
        Log.d("odor", odor);
        Log.d("degree", degree);
        TextView odorCheck = createCell("null".equals(odor) ? "" : "1".equals(odor) ? "✓" : "✗", 50, textSize, padding);
        TextView degreeDisplay = createCell("null".equals(degree) ? "" : degree + "°C", 70, textSize, padding);

        // 加入所有欄位
        tableRow.addView(deliveredDate);
        tableRow.addView(productVendor);
        tableRow.addView(productItem);
        tableRow.addView(productSpec);
        tableRow.addView(packageComplete);
        if ("原料".equals(type)){
            tableRow.addView(odorCheck);
        }
        tableRow.addView(vectorCheck);
        if ("原料".equals(type)){
            tableRow.addView(degreeDisplay);
        }
        tableRow.addView(labelCheck);
        tableRow.addView(quantity);
        tableRow.addView(expireDate);
        tableRow.addView(palletCheck);
        tableRow.addView(coaCheck);

        if (inspector != null && !inspector.trim().isEmpty()) {
            // 顯示驗收人員名稱（TextView 取代驗收按鈕）
            TextView tvInspector = createCell(inspector, 50, 16, 8);
            tableRow.addView(tvInspector);
        } else {
            // 建立 驗收按鈕
            AppCompatButton btnInspect = new AppCompatButton(this);
            btnInspect.setText("驗收");
            btnInspect.setTextSize(16f);
            btnInspect.setTextColor(Color.WHITE);
            btnInspect.setLayoutParams(new TableRow.LayoutParams(50, ViewGroup.LayoutParams.MATCH_PARENT));
            btnInspect.setBackgroundResource(R.drawable.table_button);
            btnInspect.setOnClickListener(v -> showInspectDialog(type, importId, date, itemName, spec, packageConfirm, vector, packageLabel,
                    amount, validDate, pallet, COA, inspector, confirmed, odor, degree, false));
            tableRow.addView(btnInspect);
        }

        if (confirmed != null && !confirmed.trim().isEmpty()) {
            // 顯示確認人員名稱（TextView 取代確認按鈕）
            TextView tvConfirmed = createCell(confirmed, 50, 16, 8);
            tableRow.addView(tvConfirmed);
        } else {
            // 建立 確認按鈕
            AppCompatButton btnConfirm = new AppCompatButton(this);
            btnConfirm.setText("確認");
            btnConfirm.setTextSize(16f);
            btnConfirm.setTextColor(Color.WHITE);
            btnConfirm.setLayoutParams(new TableRow.LayoutParams(50, ViewGroup.LayoutParams.MATCH_PARENT));
            btnConfirm.setBackgroundResource(R.drawable.table_button);
            btnConfirm.setOnClickListener(v -> showPasswordDialog(type, importId, date, itemName, spec, packageConfirm, vector, packageLabel,
                    amount, validDate, pallet, COA, inspector, confirmed, odor, degree));
            tableRow.addView(btnConfirm);
        }
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
    private void showInspectDialog(String type, int importId, String date, String itemName, String spec, String packageConfirm, String vector, String packageLabel,
              String amount, String validDate, String pallet, String COA, String inspector, String confirmed, String odorCheck, String degreeDisplay, boolean confirm) {

        View dialogView = LayoutInflater.from(this).inflate(R.layout.inspect_dialog, null);
        setupSpinnerData(dialogView, R.id.inspector, DataSource.getInspector(), inspector);
        setupSpinnerData(dialogView, R.id.confirmPerson, DataSource.getConfirmPerson(), confirmed);

        // 顯示/隱藏 -> 驗收button
        if (confirm){
            LinearLayout extraLayout = dialogView.findViewById(R.id.confirmLayout);
            extraLayout.setVisibility(View.VISIBLE);
        }
        LinearLayout odorLayout = dialogView.findViewById(R.id.odorLayout);
        LinearLayout degreeLayout = dialogView.findViewById(R.id.degreeLayout);

        if (!"原料".equals(type)) {
            odorLayout.setVisibility(View.GONE);
            degreeLayout.setVisibility(View.GONE);
        }

        // 更新dialog資訊
        EditText editValidDate = dialogView.findViewById(R.id.validDate);
        if (validDate != null && !validDate.toLowerCase().contains("null") && !validDate.trim().isEmpty()) {
            editValidDate.setText(validDate.trim());
        } else {
            editValidDate.setText("");
        }

        editValidDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    InspectTable.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        final String finalDate = selectedYear + "-" + (selectedMonth + 1) + "-" + selectedDay;
                        editValidDate.setText(finalDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        TextView textDate = dialogView.findViewById(R.id.textDate);
        textDate.setText(date);

        TextView textItem = dialogView.findViewById(R.id.item);
        textItem.setText(itemName);

        TextView textSpec = dialogView.findViewById(R.id.editSpec);
        textSpec.setText(spec);

        CheckBox packageCheck = dialogView.findViewById(R.id.packageCheck);
        packageCheck.setChecked("1".equals(packageConfirm));

        CheckBox odorCheckBox = dialogView.findViewById(R.id.odorCheck);
        odorCheckBox.setChecked("1".equals(odorCheck));

        CheckBox vectorsCheck = dialogView.findViewById(R.id.vectorsCheck);
        vectorsCheck.setChecked("1".equals(vector));

        TextView textDegreeDisplay = dialogView.findViewById(R.id.editDegree);
        textDegreeDisplay.setText(
                (degreeDisplay == null || degreeDisplay.toLowerCase().contains("null") || degreeDisplay.trim().isEmpty())
                        ? "" : degreeDisplay.trim()
        );

        CheckBox packageLabelCheck = dialogView.findViewById(R.id.packageLabelCheck);
        packageLabelCheck.setChecked("1".equals(packageLabel));

        TextView quantity = dialogView.findViewById(R.id.quantity);
        quantity.setText(amount);

        CheckBox palletCheck = dialogView.findViewById(R.id.palletCheck);
        palletCheck.setChecked("1".equals(pallet));

        CheckBox coaCheck = dialogView.findViewById(R.id.coaCheck);
        coaCheck.setChecked("1".equals(COA));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("驗收資料")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .setPositiveButton("確定", null)  // 設為 null，我們手動處理
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            // 取得 checkbox 狀態
            boolean isPackageChecked = packageCheck.isChecked();
            boolean isOdorChecked = odorCheckBox.isChecked();
            boolean isVectorChecked = vectorsCheck.isChecked();
            boolean isPackageLabelChecked = packageLabelCheck.isChecked();
            boolean isPalletChecked = palletCheck.isChecked();
            boolean isCoaChecked = coaCheck.isChecked();

            // 取得輸入值
            String specText = textSpec.getText().toString().trim();
            String degreeText = textDegreeDisplay.getText().toString().trim();
            String validDateText = editValidDate.getText().toString().trim();

            Spinner inspectorSpinner = dialogView.findViewById(R.id.inspector);
            Spinner confirmSpinner = dialogView.findViewById(R.id.confirmPerson);
            String inspectorValue = inspectorSpinner.getSelectedItem().toString();
            String confirmValue = confirmSpinner.getSelectedItem().toString();

            // 資料驗證（錯誤時不關閉 dialog）
            if (specText.isEmpty()) {
                Toast.makeText(InspectTable.this, "請輸入規格", Toast.LENGTH_SHORT).show();
                return;
            }
            if (type == "原料" && degreeText.isEmpty()) {
                Toast.makeText(InspectTable.this, "請輸入溫度", Toast.LENGTH_SHORT).show();
                return;
            }
            if (validDateText.isEmpty()) {
                Toast.makeText(InspectTable.this, "請選擇有效日期", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("請選擇".equals(inspectorValue)) {
                Toast.makeText(InspectTable.this, "請選擇驗收", Toast.LENGTH_SHORT).show();
                return;
            }
            if (confirm){
                if (confirmValue != null && !"".equals(confirmValue.trim()) && "請選擇".equals(confirmValue)) {
                    Toast.makeText(InspectTable.this, "請選擇確認人員", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // 呼叫資料庫更新
            ConnectDB.updateInspectRecord(
                    type,
                    importId,
                    specText,
                    validDateText,
                    isPackageChecked,
                    isOdorChecked,
                    isVectorChecked,
                    degreeText,
                    isPackageLabelChecked,
                    isPalletChecked,
                    isCoaChecked,
                    inspectorValue,
                    confirmValue,
                    success -> {
                        if (success) {
                            Toast.makeText(InspectTable.this, "更新成功", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();  // ✅ 成功才關閉 dialog
                            recreate();        // 或者用你自己的 refresh 方法
                        } else {
                            Toast.makeText(InspectTable.this, "更新失敗", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }
    private void showPasswordDialog(String type, int importId, String date, String itemName, String spec, String packageConfirm, String vector,
                                    String packageLabel, String amount, String validDate, String pallet, String COA, String inspector, String confirmed, String odorCheck, String degreeDisplay) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_password, null);
        EditText editPassword = dialogView.findViewById(R.id.editPassword);

        new AlertDialog.Builder(this)
                .setTitle("密碼驗證")
                .setView(dialogView)
                .setPositiveButton("確定", (dialog, which) -> {
                    String password = editPassword.getText().toString().trim();
                    if ((DataSource.getPasswords().contains(password))) {
                        showInspectDialog(type, importId, date, itemName, spec, packageConfirm, vector, packageLabel,
                                amount, validDate, pallet, COA, inspector, confirmed, odorCheck, degreeDisplay, true);
                    } else {
                        Toast.makeText(this, "密碼錯誤", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
