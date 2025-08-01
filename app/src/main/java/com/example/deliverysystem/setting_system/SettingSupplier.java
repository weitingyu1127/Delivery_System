package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

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
    private FlexboxLayout vendorContainer;
    private Map<String, List<String>> industryMap;
    private String currentFilter = "All";
    private ActivityResultLauncher<Intent> supplierLauncher;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_supplier);

        FlexboxLayout mainContainer = findViewById(R.id.supplier_container);

        Map<String, VendorInfo> vendorMap = DataSource.getVendorProductMap();

        industryMap = new LinkedHashMap<>();
        for (Map.Entry<String, VendorInfo> entry : vendorMap.entrySet()) {
            String vendor = entry.getKey();
            String industry = entry.getValue().getIndustry();
            industryMap.putIfAbsent(industry, new ArrayList<>());
            industryMap.get(industry).add(vendor);
        }

        for (Map.Entry<String, List<String>> entry : industryMap.entrySet()) {
            String industry = entry.getKey();
            List<String> vendors = entry.getValue();
            Log.d("IndustryMapLog1", "產業: " + industry + "，廠商數: " + vendors.size() + "，廠商清單: " + vendors);
        }

        // 建立分類按鈕列
        LinearLayout categoryRow = new LinearLayout(this);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        categoryRow.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        addCategoryButton(categoryRow, "All");
        for (String industry : industryMap.keySet()) {
            addCategoryButton(categoryRow, industry);
        }
        mainContainer.addView(categoryRow);

        // 廠商容器
        vendorContainer = new FlexboxLayout(this);
        vendorContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        vendorContainer.setFlexWrap(FlexWrap.WRAP);
        vendorContainer.setJustifyContent(JustifyContent.FLEX_START);
        vendorContainer.setPadding(16, 16, 16, 16);

        mainContainer.addView(vendorContainer);

        renderVendors("All");
        updateCategoryColors(categoryRow);

        supplierLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        ConnectDB.getVendorProductData(vendorProductMap -> {
                            DataSource.setVendorProductMap(vendorProductMap);
                            runOnUiThread(() -> renderVendors(currentFilter));
                        });
                    }
                }
        );

        FloatingActionButton addSupplier = findViewById(R.id.addSupplier);
        addSupplier.setOnClickListener(v -> {
            Intent intent = new Intent(SettingSupplier.this, AddSupplier.class);
            supplierLauncher.launch(intent);
        });
    }
    @Override
    protected void onResume() {
        super.onResume();
        ConnectDB.getVendorProductData(vendorProductMap -> {
            DataSource.setVendorProductMap(vendorProductMap);
            runOnUiThread(() -> renderVendors(currentFilter));

            industryMap.clear();
            for (Map.Entry<String, VendorInfo> entry : vendorProductMap.entrySet()) {
                String vendor = entry.getKey();
                String industry = entry.getValue().getIndustry();
                industryMap.putIfAbsent(industry, new ArrayList<>());
                industryMap.get(industry).add(vendor);
            }
            for (Map.Entry<String, List<String>> entry : industryMap.entrySet()) {
                String industry = entry.getKey();
                List<String> vendors = entry.getValue();
            }
            for (Map.Entry<String, VendorInfo> entry : vendorProductMap.entrySet()) {
                String vendor = entry.getKey();
                VendorInfo info = entry.getValue();
                String industry = info.getIndustry();
                String type = info.getType();
                List<String> products = info.getProducts();
            }
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void addCategoryButton(LinearLayout categoryRow, String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(18);
        btn.setTextColor(Color.BLACK);
        btn.setBackgroundResource(R.drawable.btn_category_white);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                dpToPx(108), dpToPx(42));
        params.setMargins(8, 8, 8, 8);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            currentFilter = label;
            renderVendors(label);
            updateCategoryColors(categoryRow);
        });

        categoryRow.addView(btn);
    }
    private void renderVendors(String filter) {
        vendorContainer.removeAllViews();

        List<String> filtered = new ArrayList<>();
        if (filter.equals("All")) {
            for (List<String> group : industryMap.values()) {
                filtered.addAll(group);
            }
        } else {
            filtered = industryMap.getOrDefault(filter, new ArrayList<>());
        }

        for (String vendor : filtered) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setBackgroundResource(R.drawable.btn_vendor_pattern);
            itemLayout.setPadding(24, 16, 24, 16);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            int widthInDp = 484;
            int heightInDp = 93;
            float scale = getResources().getDisplayMetrics().density;
            int widthPx = (int) (widthInDp * scale + 0.5f);
            int heightPx = (int) (heightInDp * scale + 0.5f);

            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(widthPx, heightPx);
            layoutParams.setMargins(16, 8, 16, 8);
            itemLayout.setLayoutParams(layoutParams);

            // 左 icon
            FrameLayout iconFrame = new FrameLayout(this);
            iconFrame.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
            iconFrame.setBackgroundResource(R.drawable.circle_gray_pattern);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_person);
            icon.setLayoutParams(new FrameLayout.LayoutParams(32, 32, Gravity.CENTER));
            iconFrame.addView(icon);

            // 中文字
            TextView nameText = new TextView(this);
            nameText.setText(vendor);
            nameText.setTextSize(18);
            nameText.setTextColor(Color.BLACK);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            nameText.setPadding(24, 0, 24, 0);

            // 右箭頭
            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.ic_arrow);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(48, 48);
            arrow.setLayoutParams(arrowParams);

            itemLayout.addView(iconFrame);
            itemLayout.addView(nameText);
            itemLayout.addView(arrow);

            itemLayout.setOnClickListener(v -> {
                VendorInfo info = DataSource.getVendorProductMap().get(vendor);
                if (info != null) {
                    Intent intent = new Intent(SettingSupplier.this, AddSupplier.class);
                    intent.putExtra("isEditMode", true);
                    intent.putExtra("vendor", vendor);
                    intent.putExtra("type", info.getType());
                    intent.putExtra("industry", info.getIndustry());
                    intent.putStringArrayListExtra("products", new ArrayList<>(info.getProducts()));
                    supplierLauncher.launch(intent);
                }
            });
            vendorContainer.addView(itemLayout);
        }
    }
    private void updateCategoryColors(ViewGroup categoryRow) {
        for (int i = 0; i < categoryRow.getChildCount(); i++) {
            Button btn = (Button) categoryRow.getChildAt(i);
            if (btn.getText().toString().equals(currentFilter)) {
                btn.setBackgroundResource(R.drawable.btn_category_orange);
            } else {
                btn.setBackgroundResource(R.drawable.btn_category_white); // ✅ 白底黑框還原
            }
        }
    }
}