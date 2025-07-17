package com.example.deliverysystem.import_system;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;
import com.google.android.flexbox.FlexboxLayout;

import java.text.SimpleDateFormat;
import java.util.*;

public class ImportItem extends BaseActivity {

    private LinearLayout selectedItemInputContainer;
    private Button btnSubmit;
    List<CheckBox> vendorCheckBoxes = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_item);

        FlexboxLayout vendorLayout = findViewById(R.id.vendorCheckboxContainer);
        FlexboxLayout productLayout = findViewById(R.id.productCheckboxContainer);
        selectedItemInputContainer = findViewById(R.id.selectedItemInputContainer);

        Map<String, VendorInfo> vendorProductMap = DataSource.getVendorProductMap();

        for (String vendor : vendorProductMap.keySet()) {
            CheckBox cb = new CheckBox(this);
            cb.setText(vendor);
            cb.setTextSize(18f);

            cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    for (CheckBox other : vendorCheckBoxes) {
                        if (other != cb) other.setChecked(false);
                    }

                    productLayout.removeAllViews();
                    selectedItemInputContainer.removeAllViews();
                    btnSubmit = null;

                    List<String> products = DataSource.getProductsByVendor(vendor);
                    for (String product : products) {
                        CheckBox prodCb = new CheckBox(this);
                        prodCb.setText(product);
                        prodCb.setTextSize(18f);

                        prodCb.setOnCheckedChangeListener((pView, pChecked) -> {
                            if (pChecked) {
                                addProductInputRow(product);
                                showSubmitButton();
                            } else {
                                removeProductInputRow(product);
                                hideSubmitButton();
                            }
                        });

                        productLayout.addView(prodCb);
                    }
                } else {
                    if (!anyVendorChecked(vendorCheckBoxes)) {
                        productLayout.removeAllViews();
                        selectedItemInputContainer.removeAllViews();
                        btnSubmit = null;
                    }
                }
            });

            vendorLayout.addView(cb);
            vendorCheckBoxes.add(cb);
        }
    }

    private void addProductInputRow(String product) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setTag(product);

        TextView tv = new TextView(this);
        tv.setText(product);
        tv.setTextSize(18f);
        tv.setPadding(8, 8, 8, 8);

        EditText et = new EditText(this);
        et.setHint("數量");
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setEms(4);

        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"個", "箱", "桶"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        row.addView(tv);
        row.addView(et);
        row.addView(spinner);
        selectedItemInputContainer.addView(row);
        // ✅ 將「新增」按鈕移到底部
        if (btnSubmit != null) {
            selectedItemInputContainer.removeView(btnSubmit);
            selectedItemInputContainer.addView(btnSubmit);
        }
    }

    private void removeProductInputRow(String product) {
        View toRemove = selectedItemInputContainer.findViewWithTag(product);
        if (toRemove != null) {
            selectedItemInputContainer.removeView(toRemove);
        }
    }

    private void showSubmitButton() {
        if (btnSubmit == null) {
            btnSubmit = new Button(this);
            btnSubmit.setText("新增");
            btnSubmit.setTextColor(Color.WHITE);
            btnSubmit.setBackgroundColor(Color.parseColor("#2196F3"));
            btnSubmit.setOnClickListener(v -> {
                // 取得目前所選廠商
                String selectedVendor = null;
                boolean hasEmptyQty = false;
                for (CheckBox cb : vendorCheckBoxes) {
                    if (cb.isChecked()) {
                        selectedVendor = cb.getText().toString();
                        break;
                    }
                }
                final String vendorFinal = selectedVendor;

                if (selectedVendor == null) {
                    Toast.makeText(this, "請選擇廠商", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 收集品項資料
                List<String> itemSummaries = new ArrayList<>();
                for (int i = 0; i < selectedItemInputContainer.getChildCount(); i++) {
                    View view = selectedItemInputContainer.getChildAt(i);
                    if (view instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) view;
                        if (row.getChildCount() >= 3) {
                            TextView nameView = (TextView) row.getChildAt(0);
                            EditText qtyView = (EditText) row.getChildAt(1);
                            Spinner unitSpinner = (Spinner) row.getChildAt(2);

                            String name = nameView.getText().toString();
                            String qty = qtyView.getText().toString().trim();
                            String unit = unitSpinner.getSelectedItem().toString();
                            //控制"未輸入數量-dialog不顯示"
                            if (qty.isEmpty()) {
                                hasEmptyQty = true;
                                break;
                            }
                            itemSummaries.add(name + " - " + qty + " " + unit);
                        }
                    }
                }
                //控制"未輸入數量-dialog不顯示"
                if (hasEmptyQty) {
                    Toast.makeText(ImportItem.this, "請填寫所有商品的數量", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 顯示 Dialog
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
                TextView textDate = dialogView.findViewById(R.id.textDate);
                TextView tvVendorName = dialogView.findViewById(R.id.tvVendorName);
                LinearLayout itemListContainer = dialogView.findViewById(R.id.itemListContainer);

                String currentDateTime = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                textDate.setText(currentDateTime);

                tvVendorName.setText(vendorFinal);

                for (String summary : itemSummaries) {
                    TextView itemTextView = new TextView(this);
                    itemTextView.setText(summary);
                    itemTextView.setTextSize(25f);
                    itemTextView.setTextColor(getResources().getColor(R.color.black));
                    itemListContainer.addView(itemTextView);
                }

                new android.app.AlertDialog.Builder(this)
                        .setTitle("確認資料")
                        .setView(dialogView)
                        .setPositiveButton("確定", (dialogInterface, which) -> {
                            // 準備送出的資料
                            String importDate = currentDateTime;

                            for (int i = 0; i < selectedItemInputContainer.getChildCount(); i++) {
                                View view = selectedItemInputContainer.getChildAt(i);
                                if (view instanceof LinearLayout) {
                                    LinearLayout row = (LinearLayout) view;
                                    if (row.getChildCount() >= 3) {
                                        TextView nameView = (TextView) row.getChildAt(0);
                                        EditText qtyView = (EditText) row.getChildAt(1);
                                        Spinner unitSpinner = (Spinner) row.getChildAt(2);

                                        String product = nameView.getText().toString();
                                        String quantity = qtyView.getText().toString().trim() + unitSpinner.getSelectedItem().toString();

                                        // 呼叫 ConnectDB 新增到資料庫
                                        ConnectDB.addImportRecord(importDate, vendorFinal, product, quantity, success -> {
                                            if (success) {
                                                runOnUiThread(() -> Toast.makeText(this, "新增成功", Toast.LENGTH_SHORT).show());
                                            } else {
                                                runOnUiThread(() -> Toast.makeText(this, "新增失敗", Toast.LENGTH_SHORT).show());
                                            }
                                        });
                                    }
                                }
                            }
                            // 全部送出後回上一頁
                            Toast.makeText(ImportItem.this, "新增成功", Toast.LENGTH_SHORT).show();
                            finish(); // 返回 ImportActivity.java
                        })

                        .setNegativeButton("取消", null)
                        .show();
            });

            selectedItemInputContainer.addView(btnSubmit);
        }
    }

    private void hideSubmitButton() {
        boolean hasRow = false;
        for (int i = 0; i < selectedItemInputContainer.getChildCount(); i++) {
            View view = selectedItemInputContainer.getChildAt(i);
            if (view instanceof LinearLayout) {
                hasRow = true;
                break;
            }
        }
        if (!hasRow && btnSubmit != null) {
            selectedItemInputContainer.removeView(btnSubmit);
            btnSubmit = null;
        }
    }

    private boolean anyVendorChecked(List<CheckBox> checkBoxes) {
        for (CheckBox cb : checkBoxes) {
            if (cb.isChecked()) return true;
        }
        return false;
    }
}
