package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SettingSupplier extends BaseActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_supplier);

        renderVendors();

        FloatingActionButton addSupplier = findViewById(R.id.addSupplier);
        addSupplier.setOnClickListener(v -> showAddSupplierDialog());
    }
    private void renderVendors() {
        FlexboxLayout container = findViewById(R.id.supplier_container);
        container.removeAllViews();

        Map<String, VendorInfo> vendorMap = DataSource.getVendorProductMap();

        // 1. 分類：industry → List of vendor_name
        Map<String, List<String>> industryMap = new LinkedHashMap<>();
        for (Map.Entry<String, VendorInfo> entry : vendorMap.entrySet()) {
            String vendor = entry.getKey();
            String industry = entry.getValue().getIndustry();

            industryMap.putIfAbsent(industry, new ArrayList<>());
            industryMap.get(industry).add(vendor);
        }

        // 2. 建立 UI
        for (Map.Entry<String, List<String>> entry : industryMap.entrySet()) {
            String industry = entry.getKey();
            List<String> vendors = entry.getValue();

            // 標題
            TextView titleView = new TextView(this);
            titleView.setText(industry);
            titleView.setTextSize(20);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setTextColor(Color.BLACK);
            titleView.setPadding(0, 24, 0, 8);
            container.addView(titleView);

            // 對應的廠商群組
            FlexboxLayout flexbox = new FlexboxLayout(this);
            flexbox.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            flexbox.setFlexWrap(FlexWrap.WRAP);
            flexbox.setFlexDirection(FlexDirection.ROW);
            flexbox.setJustifyContent(JustifyContent.FLEX_START);

            for (String vendorName : vendors) {
                Button btn = new Button(this);
                btn.setText(vendorName);
                btn.setTextColor(Color.WHITE);
                btn.setTextSize(16);
                btn.setBackgroundColor(Color.parseColor("#4CAF50"));

                FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(16, 8, 16, 8);
                btn.setLayoutParams(params);

                btn.setOnClickListener(v -> {
                    VendorInfo vendorInfo = vendorMap.get(vendorName);
                    showVendorSettingDialog(
                            vendorName,
                            vendorInfo.getType(),
                            vendorInfo.getIndustry(),
                            vendorInfo.getProducts()
                    );
                });

                flexbox.addView(btn);
            }
            container.addView(flexbox);
        }
    }

    private void showVendorSettingDialog(String vendorName, String type, String industry, List<String> productList) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_setting_supplier, null);

        AlertDialog settingDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        settingDialog.show();

        TextView textVendorName = dialogView.findViewById(R.id.textVendorName);
        TextView textType = dialogView.findViewById(R.id.textType);
        TextView textIndustry = dialogView.findViewById(R.id.textIndustry);
        FlexboxLayout itemContainer = dialogView.findViewById(R.id.itemContainer);
        ImageButton btnAddItem = dialogView.findViewById(R.id.btnAddItem);
        LinearLayout newItemLayout = dialogView.findViewById(R.id.newItemLayout);
        EditText editNewItem = dialogView.findViewById(R.id.editNewItem);
        Button btnConfirmAdd = dialogView.findViewById(R.id.btnConfirmAdd);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        // 設定廠商名稱
        textVendorName.setText(vendorName);
        textType.setText(type);
        textIndustry.setText(industry);

        // 顯示品項
        for (String item : productList) {
            addItemRow(vendorName, item, itemContainer, productList);
        }

        // 新增品項按鈕
        btnAddItem.setOnClickListener(v -> newItemLayout.setVisibility(View.VISIBLE));

        btnConfirmAdd.setOnClickListener(v -> {
            String newItem = editNewItem.getText().toString().trim();
            if (newItem.isEmpty()) {
                Toast.makeText(this, "請輸入品項名稱", Toast.LENGTH_SHORT).show();
                return;
            }

            if (productList.contains(newItem)) {
                Toast.makeText(this, "此品項已存在", Toast.LENGTH_SHORT).show();
                return;
            }

            // 新增至資料庫
            ConnectDB.addProduct(vendorName, newItem, success -> {
                if (success) {
                    productList.add(newItem);
                    addItemRow(vendorName, newItem, itemContainer, productList);
                    editNewItem.setText("");
                    newItemLayout.setVisibility(View.GONE);
                    Toast.makeText(this, "新增成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "新增失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                }
            });
        });
        btnDelete.setOnClickListener(v -> {
            // 建立輸入密碼的 EditText 並加入 padding
            final EditText inputPassword = new EditText(this);
            inputPassword.setHint("請輸入密碼");

            // 設定左右 padding
            int padding = (int) getResources().getDisplayMetrics().density * 24;
            inputPassword.setPadding(padding, inputPassword.getPaddingTop(), padding, inputPassword.getPaddingBottom());

            // 或使用 FrameLayout 包住並加 padding，更精緻控制位置
            FrameLayout container = new FrameLayout(this);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.leftMargin = params.rightMargin = padding;
            inputPassword.setLayoutParams(params);
            container.addView(inputPassword);

            AlertDialog deleteDialog = new AlertDialog.Builder(this)
                    .setTitle("刪除 " + vendorName)
                    .setMessage("\n此操作將刪除廠商，請輸入密碼以繼續")
                    .setView(container)
                    .setPositiveButton("確認", null) // 先設為 null，稍後再綁定按鈕事件
                    .setNegativeButton("取消", null)
                    .create();

            deleteDialog.setOnShowListener(dialogInterface -> {
                Button positiveBtn = deleteDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveBtn.setOnClickListener(view -> {
                    String enteredPassword = inputPassword.getText().toString().trim();
                    if (enteredPassword.equals(DataSource.getPasswords().get(0))) {
                        ConnectDB.deleteVendor(this, vendorName, success -> {
                            if (success) {
                                deleteDialog.dismiss();
                                settingDialog.dismiss();
                                ConnectDB.getVendorProductData(vendorProductMap -> {
                                    DataSource.setVendorProductMap(vendorProductMap);
                                    renderVendors();
                                });
                                Toast.makeText(this, "刪除成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "密碼錯誤，無法刪除", Toast.LENGTH_SHORT).show();
                    }
                });
            });
            deleteDialog.show();
        });
    }
    private void addItemRow(String vendorName, String item,  ViewGroup container, List<String> itemList) {
        Context context = container.getContext();

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 8, 0, 8);

        TextView itemText = new TextView(context);
        itemText.setText(item);
        itemText.setTextSize(16);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        itemText.setLayoutParams(textParams);

        ImageButton deleteBtn = new ImageButton(context);
        deleteBtn.setImageResource(android.R.drawable.ic_delete);
        deleteBtn.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(20));
        btnParams.setMargins(10, 0, 5, 0);
        deleteBtn.setLayoutParams(btnParams);

        deleteBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("確認刪除")
                    .setMessage("確定要刪除「" + item + "」這個品項嗎？")
                    .setPositiveButton("刪除", (dialog, which) -> {
                        ConnectDB.deleteProduct(vendorName, item, success -> {
                            if (success) {
                                container.removeView(row);
                                itemList.remove(item);
                                Toast.makeText(context, "已刪除「" + item + "」", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "刪除失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
        row.addView(deleteBtn);
        row.addView(itemText);
        container.addView(row);
    }
    private void showAddSupplierDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_supplier, null);

        EditText editVendorName = dialogView.findViewById(R.id.editVendorName);
        Spinner spinnerType = dialogView.findViewById(R.id.spinnerType);
        Spinner spinnerIndustry = dialogView.findViewById(R.id.spinnerIndustry);
        EditText editNewProduct = dialogView.findViewById(R.id.editNewProduct);
        Button btnAddProduct = dialogView.findViewById(R.id.btnAddProduct);
        FlexboxLayout productListContainer = dialogView.findViewById(R.id.productListContainer);
        Button btnSaveVendor = dialogView.findViewById(R.id.btnSaveVendor);

        ConnectDB.getDistinctData("type", "vendors", data -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(adapter);
        });

        ConnectDB.getDistinctData("industry", "vendors", data -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, data);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerIndustry.setAdapter(adapter);
        });


        List<String> products = new ArrayList<>();
        btnAddProduct.setOnClickListener(v -> {
            String newProduct = editNewProduct.getText().toString().trim();
            if (!newProduct.isEmpty()) {
                products.add(newProduct);

                // 建立外層 LinearLayout (水平)
                LinearLayout itemLayout = new LinearLayout(this);
                itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                itemLayout.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                itemLayout.setPadding(8, 8, 8, 8); // 外距

                // 建立文字
                TextView tag = new TextView(this);
                tag.setText(newProduct);
                tag.setTextColor(Color.BLACK);
                tag.setTextSize(16);
                tag.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                // 建立刪除按鈕
                ImageButton deleteBtn = new ImageButton(this);
                deleteBtn.setImageResource(android.R.drawable.ic_delete);
                deleteBtn.setBackgroundColor(Color.TRANSPARENT); // 無背景
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                        dpToPx(20), dpToPx(20)); // 大小
                btnParams.setMargins(0, 0, dpToPx(8), 0); // 與文字間距
                deleteBtn.setLayoutParams(btnParams);

                // 點擊刪除這筆
                deleteBtn.setOnClickListener(del -> {
                    productListContainer.removeView(itemLayout);
                    products.remove(newProduct);
                });

                // 組合元件
                itemLayout.addView(deleteBtn);
                itemLayout.addView(tag);
                productListContainer.addView(itemLayout);

                // 清空輸入框
                editNewProduct.setText("");
            }
        });


        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("新增廠商")
                .setView(dialogView)
                .create();
        dialog.show();
        btnSaveVendor.setOnClickListener(v -> {
            String name = editVendorName.getText().toString().trim();
            String type = spinnerType.getSelectedItem().toString();
            String industry = spinnerIndustry.getSelectedItem().toString();

            if (name.isEmpty()) {
                Toast.makeText(this, "請輸入廠商名稱", Toast.LENGTH_SHORT).show();
                return;
            }
            if (products == null || products.isEmpty()) {
                Toast.makeText(this, "請至少輸入一項產品", Toast.LENGTH_SHORT).show();
                return;
            }
            Set<String> uniqueCheck = new HashSet<>(products);
            if (uniqueCheck.size() < products.size()) {
                Toast.makeText(this, "產品列表中有重複項目，請修改", Toast.LENGTH_SHORT).show();
                return;
            }
            ConnectDB.addVendorWithProducts(
                    this,
                    name,
                    type,
                    industry,
                    products,
                    success -> {
                        if (success) {
                            dialog.dismiss();
                            ConnectDB.getVendorProductData(vendorProductMap -> {
                                DataSource.setVendorProductMap(vendorProductMap);
                                renderVendors();
                            });
                            Toast.makeText(this, "新增成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "請檢查該廠商是否已存在", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}