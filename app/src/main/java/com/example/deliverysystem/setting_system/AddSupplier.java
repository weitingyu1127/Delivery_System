package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.inspect_system.InspectDetail;
import com.example.deliverysystem.inspect_system.InspectTable;
import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.List;

public class AddSupplier extends AppCompatActivity {
    EditText productInput, supplierNameInput, supplierIndustryInput;
    Button addButton, confirmBtn, cancelBtn;
    FlexboxLayout productListContainer;
    Spinner spinnerType;
    boolean isEditMode = false;
    String vendorName = null;
    List<String> originalProducts = new ArrayList<>();
    List<String> deleteTempList = new ArrayList<>();
    ImageView deleteSupplier;
    String selectedType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_supplier);

        productInput = findViewById(R.id.product_input);
        addButton = findViewById(R.id.add_button);
        productListContainer = findViewById(R.id.product_list_container);
        supplierNameInput = findViewById(R.id.supplier_name);
        supplierIndustryInput = findViewById(R.id.supplier_industry);
        confirmBtn = findViewById(R.id.confirm_btn);
        cancelBtn = findViewById(R.id.cancel_btn);
        deleteSupplier = findViewById(R.id.ic_delete);

        spinnerType = findViewById(R.id.spinnerType);
        List<String> spinnerOptions = new ArrayList<>();
        spinnerOptions.add("原料");
        spinnerOptions.add("物料");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerOptions) {
            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (!spinnerType.isEnabled()) {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (!spinnerType.isEnabled()) {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                } else {
                    textView.setTextColor(getResources().getColor(android.R.color.black));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(adapter);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                selectedType = spinnerOptions.get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        Intent intent = getIntent();
        isEditMode = intent.getBooleanExtra("isEditMode", false);

        if (isEditMode) {
            vendorName = intent.getStringExtra("vendor");
            String type = getIntent().getStringExtra("type");
            String industry = getIntent().getStringExtra("industry");

            ArrayList<String> products = intent.getStringArrayListExtra("products");

            supplierNameInput.setText(vendorName);
            supplierNameInput.setEnabled(false);

            supplierIndustryInput.setText(industry);
            supplierIndustryInput.setEnabled(false);

            for (String p : products) {
                originalProducts.add(p);
                addProductItem(p);
            }
            // 设置 Spinner 的选中项
            int selectedPosition = spinnerOptions.indexOf(type); // 获取当前类型在 spinnerOptions 中的位置
            if (selectedPosition != -1) {
                spinnerType.setSelection(selectedPosition); // 设置 Spinner 的选中项
            }

            spinnerType.setEnabled(false); // 禁用 Spinner 编辑
        }else {
            deleteSupplier.setVisibility(View.GONE);
        }

        addButton.setOnClickListener(v -> {
            String productName = productInput.getText().toString().trim();
            if (!productName.isEmpty()) {
                addProductItem(productName);
                productInput.setText("");
            }
        });

        confirmBtn.setOnClickListener(v -> {
            String name = supplierNameInput.getText().toString().trim();
            String industry = supplierIndustryInput.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "請輸入廠商名稱", Toast.LENGTH_SHORT).show();
                return;
            }
            if (industry.isEmpty()) {
                Toast.makeText(this, "請輸入分類", Toast.LENGTH_SHORT).show();
                return;
            }
            if (productListContainer.getChildCount() == 0) {
                Toast.makeText(this, "請新增至少一個產品", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> currentProducts = new ArrayList<>();
            for (int i = 0; i < productListContainer.getChildCount(); i++) {
                View item = productListContainer.getChildAt(i);
                TextView textView = item.findViewById(R.id.product_name);
                if (textView != null) {
                    currentProducts.add(textView.getText().toString());
                }
            }

            if (isEditMode) {
                deleteNextProduct(0, () -> {
                    List<String> toAdd = new ArrayList<>();
                    for (String p : currentProducts) {
                        if (!originalProducts.contains(p)) toAdd.add(p);
                    }
                    if (toAdd.isEmpty()) {
                        Toast.makeText(this, "修改成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                        return;
                    }
                    ConnectDB.addProduct(this, vendorName, toAdd, success -> {
                        if (success) {
                            Toast.makeText(this, "產品新增成功", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, "新增失敗，請重試", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            } else {
                ConnectDB.addVendorWithProducts(this, name, selectedType, industry, currentProducts, success -> {
                    if (success) {
                        Toast.makeText(this, "新增成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "請檢查該廠商是否已存在", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        cancelBtn.setOnClickListener(v -> finish());

        ImageView backIcon = findViewById(R.id.back_icon);
        backIcon.setOnClickListener(v -> finish());

        deleteSupplier.setOnClickListener(v -> {
            LayoutInflater inflater = LayoutInflater.from(AddSupplier.this);
            View dialogView = inflater.inflate(R.layout.dialog_password, null);
            EditText editPassword = dialogView.findViewById(R.id.editPassword);

            new AlertDialog.Builder(AddSupplier.this)
                    .setTitle("密碼驗證")
                    .setView(dialogView)
                    .setPositiveButton("確定", (dialog, which) -> {
                        String password = editPassword.getText().toString().trim();

                        if (DataSource.getPasswords().contains(password)) {
                            String vendorName = supplierNameInput.getText().toString().trim();

                            ConnectDB.deleteVendor(this, vendorName, success -> {
                                if (success) {
                                    Toast.makeText(AddSupplier.this, "廠商刪除成功", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(AddSupplier.this, "刪除失敗，請稍後再試", Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            Toast.makeText(AddSupplier.this, "密碼錯誤", Toast.LENGTH_SHORT).show();
                        }

                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

    }

    private void deleteNextProduct(int index, Runnable onComplete) {
        if (index >= deleteTempList.size()) {
            onComplete.run();
            return;
        }
        String product = deleteTempList.get(index);
        ConnectDB.deleteProduct(vendorName, product, success -> {
            if (success) originalProducts.remove(product);
            deleteNextProduct(index + 1, onComplete);
        });
    }

    private void addProductItem(String name) {
        for (int i = 0; i < productListContainer.getChildCount(); i++) {
            View item = productListContainer.getChildAt(i);
            TextView textView = item.findViewById(R.id.product_name);
            if (textView != null && textView.getText().toString().equals(name)) {
                Toast.makeText(this, "該產品已經存在", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        View itemView = getLayoutInflater().inflate(R.layout.item_product, null);
        TextView nameText = itemView.findViewById(R.id.product_name);
        ImageView deleteIcon = itemView.findViewById(R.id.delete_icon);

        nameText.setText(name);
        deleteIcon.setOnClickListener(v -> {
            if (productListContainer.getChildCount() == 1) {
                Toast.makeText(this, "請至少保留一項產品", Toast.LENGTH_SHORT).show();
                return;
            }
            productListContainer.removeView(itemView);
            if (isEditMode && originalProducts.contains(name)) {
                deleteTempList.add(name);
            }
        });

        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(24, 24, 24, 24);
        itemView.setLayoutParams(params);

        productListContainer.addView(itemView);
    }
}