package com.example.deliverysystem.storage;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.utility.Tools;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StorageMain extends BaseActivity {

    private final List<Map<String, Object>> ingredientList = new ArrayList<>(); // 原料
    private final List<Map<String, Object>> materialList = new ArrayList<>();    // 物料
    private String currentPlace = "本廠"; // 預設本廠
    private Spinner ingredientSpinner;
    private Spinner materialSpinner;
    Map<String, Button> placeMap = new HashMap<>();
    private final Map<String, Map<String, Map<String, Integer>>> tempQuantity = new HashMap<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_page);

        // 儲存btn
        Button saveBtn = findViewById(R.id.btn_save_storage);
        saveBtn.setOnClickListener(v -> saveStorage());

        // 取的庫存資訊 
        getStorageData();

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
        
        // 下拉選單
        ingredientSpinner = findViewById(R.id.ingredient_vendor);
        materialSpinner = findViewById(R.id.material_vendor);
    }
    /** 更換地點 */
    private void changePlace(String place){
        Tools.selectPlace(this, place, placeMap);
        getStorageData();
    }
    /** 取的庫存資訊 */
    private void getStorageData() {
        // 原料
        ConnectDB.getStorage(currentPlace, "原料", storageList -> {
            ingredientList.clear();
            ingredientList.addAll(storageList);
            runOnUiThread(() -> {
                setupSpinner(ingredientSpinner, ingredientList, "原料");
                displayStorageData(ingredientList, findViewById(R.id.storageIngredientTable));
            });
        });

        // 物料
        ConnectDB.getStorage(currentPlace, "物料", storageList -> {
            materialList.clear();
            materialList.addAll(storageList);
            runOnUiThread(() -> {
                setupSpinner(materialSpinner, materialList, "物料");
                displayStorageData(materialList, findViewById(R.id.storageMaterialTable));
            });
        });
    }

    /** 顯示庫存 */
    private void displayStorageData(List<Map<String, Object>> dataList, LinearLayout tableLayout) {
        Tools.clearTable(tableLayout);
        // 資料為空
        if (dataList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("目前沒有資料");
            emptyView.setTextSize(18);
            emptyView.setTextColor(Color.GRAY);
            emptyView.setGravity(Gravity.CENTER);
            emptyView.setPadding(0, 50, 0, 0);
            tableLayout.addView(emptyView);
            return;
        }

        Map<String, List<Map<String, Object>>> groupedData = new LinkedHashMap<>();
        for (Map<String, Object> item : dataList) {
            String vendorName = (String) item.get("vendorName");
            groupedData.computeIfAbsent(vendorName, k -> new ArrayList<>()).add(item);
        }

        for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
            String vendorName = entry.getKey();
            List<Map<String, Object>> products = entry.getValue();

            // 廠商名稱
            TextView vendorTextView = new TextView(this);
            vendorTextView.setText(vendorName);
            vendorTextView.setTextSize(24);
            vendorTextView.setTextColor(Color.BLACK);
            vendorTextView.setTypeface(null, Typeface.BOLD);
            vendorTextView.setPadding(16, 24, 16, 8);
            tableLayout.addView(vendorTextView);

            FlexboxLayout productContainer = new FlexboxLayout(this);
            productContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            productContainer.setFlexWrap(FlexWrap.WRAP);
            productContainer.setJustifyContent(JustifyContent.FLEX_START);
            tableLayout.addView(productContainer);

            for (Map<String, Object> productData : products) {
                String id = (String) productData.get("id");
                String type = (String) productData.get("type");
                String productName = (String) productData.get("product");
                int amount = (productData.get("amount") instanceof Number) ? ((Number) productData.get("amount")).intValue() : 0;
                addProductInputRow(productContainer, id, type, vendorName, productName, amount);
            }

            View divider = new View(this);
            LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) (0.5f * getResources().getDisplayMetrics().density)
            );
            dividerParams.setMargins(
                    (int) (12 * getResources().getDisplayMetrics().density), 0,
                    (int) (12 * getResources().getDisplayMetrics().density), 0
            );
            divider.setLayoutParams(dividerParams);
            divider.setBackgroundColor(Color.parseColor("#999999"));
            tableLayout.addView(divider);
        }
    }

    /** 輸入樣式 */
    private void addProductInputRow(FlexboxLayout parentLayout, String id, String type, String vendorName, String product, int amount) {
        float dp = getResources().getDisplayMetrics().density;
        int gap = (int) (12 * dp);

        FlexboxLayout.LayoutParams rowParams = new FlexboxLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(gap, gap, gap, gap);
        rowParams.setFlexBasisPercent(0.3f);

        // 白色底
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setTag(product);
        row.setPadding((int) (20 * dp), (int) (20 * dp), (int) (20 * dp), (int) (20 * dp));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.product_add_pattern);
        row.setLayoutParams(rowParams);

        // 箱子icon
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_storage);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams((int) (50 * dp), (int) (50 * dp));
        iconParams.setMargins(0, 0, (int) (20 * dp), 0);
        row.addView(icon, iconParams);

        // 產品名稱
        TextView tv = new TextView(this);
        tv.setText(product);
        tv.setTextSize(20f);
        tv.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams tvParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        row.addView(tv);

        // 輸入框
        EditText qtyInput = new EditText(this);
        qtyInput.setText(String.valueOf(amount));
        qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        qtyInput.setGravity(Gravity.CENTER);
        qtyInput.setWidth((int) (80 * dp));
        qtyInput.setHeight((int) (40 * dp));
        qtyInput.setBackgroundColor(Color.parseColor("#DDDDDD"));
        qtyInput.setPadding((int) (10 * dp), 0, (int) (10 * dp), 0);

        // 減號
        ImageButton btnMinus = new ImageButton(this);
        btnMinus.setImageResource(R.drawable.ic_minus);
        btnMinus.setBackgroundColor(Color.TRANSPARENT);
        btnMinus.setLayoutParams(new LinearLayout.LayoutParams((int) (30 * dp), (int) (30 * dp)));
        row.addView(btnMinus);
        btnMinus.setOnClickListener(v -> {
            int qty = Integer.parseInt(qtyInput.getText().toString());
            if (qty > 0) {
                qty--;
                qtyInput.setText(String.valueOf(qty));

                tempQuantity
                    .computeIfAbsent(type, t -> new HashMap<>())
                    .computeIfAbsent(vendorName, vn -> new HashMap<>())
                    .put(product, qty);
            }
        });

        // 輸入框
        qtyInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String text = qtyInput.getText().toString().trim();
                int newAmount = text.isEmpty() ? 0 : Integer.parseInt(text);

                tempQuantity
                    .computeIfAbsent(type, t -> new HashMap<>())
                    .computeIfAbsent(vendorName, vn -> new HashMap<>())
                    .put(product, newAmount);
            }
        });
        row.addView(qtyInput);

        // 加號
        ImageButton btnPlus = new ImageButton(this);
        btnPlus.setImageResource(R.drawable.ic_plus);
        btnPlus.setBackgroundColor(Color.TRANSPARENT);
        btnPlus.setLayoutParams(new LinearLayout.LayoutParams((int) (30 * dp), (int) (30 * dp)));
        row.addView(btnPlus);
        btnPlus.setOnClickListener(v -> {
            int qty = Integer.parseInt(qtyInput.getText().toString());
            qty++;
            qtyInput.setText(String.valueOf(qty));

            tempQuantity
                .computeIfAbsent(type, t -> new HashMap<>())
                .computeIfAbsent(vendorName, vn -> new HashMap<>())
                .put(product, qty);
        });
        parentLayout.addView(row);
    }

    // 下拉選單: 廠商
    private void setupSpinner(Spinner spinner, List<Map<String, Object>> dataList, String type) {
        List<String> vendors = new ArrayList<>();
        vendors.add("選擇廠商");
        for (Map<String, Object> item : dataList) {
            String vendorName = (String) item.get("vendorName");
            if (vendorName != null && !vendors.contains(vendorName)) {
                vendors.add(vendorName);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, vendors);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    displayStorageData(
                        type.equals("原料") ? ingredientList : materialList,
                        type.equals("原料") ? findViewById(R.id.storageIngredientTable)
                                : findViewById(R.id.storageMaterialTable)
                    );
                } else {
                    String selectedVendor = vendors.get(position);
                    List<Map<String, Object>> filtered = new ArrayList<>();
                    for (Map<String, Object> item : dataList) {
                        if (selectedVendor.equals(item.get("vendorName"))) {
                            filtered.add(item);
                        }
                    }
                    displayStorageData(filtered,
                        type.equals("原料") ? findViewById(R.id.storageIngredientTable) : findViewById(R.id.storageMaterialTable)
                    );
                }
            }

            // 預設: 全部
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                displayStorageData(
                    type.equals("原料") ? ingredientList : materialList,
                    type.equals("原料") ? findViewById(R.id.storageIngredientTable) : findViewById(R.id.storageMaterialTable)
                );
            }
        });
    }
    
    /** 更新庫存 */
    private void saveStorage() {
        if (tempQuantity.isEmpty()) {return;}
        ConnectDB.updateQuantity(currentPlace, tempQuantity, success -> {
            runOnUiThread(() -> {
                if (success) {
                    Tools.showToast(this, "更新成功");
                    getStorageData();
                } else {
                    Tools.showToast(this, "更新失敗");
                }
                tempQuantity.clear();
            });
        });
    }
}
