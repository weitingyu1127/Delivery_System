package com.example.deliverysystem.import_system;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;
import com.example.deliverysystem.utility.Patterns;
import com.example.deliverysystem.utility.Tools;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ImportMain extends BaseActivity {
    private FlexboxLayout vendorContainer;
    private Map<String, List<String>> industryMap;
    /** 預設(全) */
    private String filterLabel = "All";
    /** 原料 */
    private FlexboxLayout rowIngredient;
    /** 物料 */
    private FlexboxLayout rowMaterial;
    private EditText searchInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_main);

        FlexboxLayout mainContainer = findViewById(R.id.main_container);
        searchInput = findViewById(R.id.search_input);
        Map<String, VendorInfo> vendorMap = DataSource.getVendorProductMap();

        // TODO 可以考慮整理進去DATASOURCE
        // 整理 industryMap（保留原邏輯，用 industry 當 key，收集 vendor 名單）
        industryMap = new LinkedHashMap<>();
        for (Map.Entry<String, VendorInfo> entry : vendorMap.entrySet()) {
            String vendor = entry.getKey();
            String industry = entry.getValue().getIndustry();
            industryMap.putIfAbsent(industry, new ArrayList<>());
            industryMap.get(industry).add(vendor);
        }

        // 依 type 自動蒐集 industry：原料 / 物料（用 LinkedHashSet 保序又去重）
        Set<String> rawIndustries = new LinkedHashSet<>();
        Set<String> materialIndustries = new LinkedHashSet<>();
        for (VendorInfo info : vendorMap.values()) {
            String industry = info.getIndustry();
            String type = info.getType();
            if ("原料".equals(type)) {
                rawIndustries.add(industry);
            } else if ("物料".equals(type)) {
                materialIndustries.add(industry);
            }
        }

        // 第 1 排：原料
        LinearLayout rawRowContainer = createRow("原料");
        rowIngredient = (FlexboxLayout) rawRowContainer.getChildAt(1);
        addCategoryButton(rowIngredient, "All");
        rawIndustries.forEach(industry -> addCategoryButton(rowIngredient, industry));
        mainContainer.addView(rawRowContainer);
        
        // 分隔線
        mainContainer.addView(Patterns.divider(this));

        // 第 2 排：物料
        LinearLayout materialRowContainer = createRow("物料");
        rowMaterial = (FlexboxLayout) materialRowContainer.getChildAt(1);
        materialIndustries.forEach(industry -> addCategoryButton(rowMaterial, industry));
        mainContainer.addView(materialRowContainer);
        
        // 廠商Button
        vendorContainer = new FlexboxLayout(this);
        vendorContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        vendorContainer.setFlexWrap(FlexWrap.WRAP);
        vendorContainer.setJustifyContent(JustifyContent.FLEX_START);
        vendorContainer.setAlignItems(AlignItems.CENTER);
        vendorContainer.setPadding(0, 24, 0, 24);

        mainContainer.addView(vendorContainer);

        // 預設 All
        renderVendors("All", "");

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderVendors(filterLabel, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    /** 建立Row */
    private LinearLayout createRow(String labelText) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        container.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText(labelText);
        label.setTextSize(20);
        label.setTextColor(Color.BLACK);
        label.setPadding(0, 0, 16, 0);
        container.addView(label);

        FlexboxLayout flexRow = createFlexRow();
        flexRow.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        container.addView(flexRow);

        return container;
    }

    /** 自動換行 */
    private FlexboxLayout createFlexRow() {
        FlexboxLayout row = new FlexboxLayout(this);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        row.setFlexWrap(FlexWrap.WRAP);
        row.setJustifyContent(JustifyContent.FLEX_START);
        row.setAlignItems(AlignItems.CENTER);
        row.setPadding(0, 0, 0, 0);
        return row;
    }

    /** 類別按鈕 */
    private void addCategoryButton(FlexboxLayout targetRow, String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(18);
        btn.setTextColor(Color.BLACK);
        btn.setBackgroundResource(R.drawable.btn_category_white);
        btn.setPadding(32, 8, 32, 8);
        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(8, 12, 8, 12);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> {
            filterLabel = label;
            renderVendors(label, searchInput.getText().toString());
        });
        targetRow.addView(btn);
    }

    /** 動態更新廠商button */
    private void renderVendors(String filter, String searchQuery) {
        // 清除廠商
        vendorContainer.removeAllViews();

        // 選項搜尋
        List<String> filtered = new ArrayList<>();
        if (filter.equals("All")) {
            for (List<String> group : industryMap.values()) {
                filtered.addAll(group);
            }
        } else {
            filtered = industryMap.getOrDefault(filter, new ArrayList<>());
        }

        // 文字搜尋
        String query = searchQuery.trim().toLowerCase();
        if (!query.isEmpty()) {
            List<String> temp = new ArrayList<>();
            for (String vendor : filtered) {
                if (vendor.toLowerCase().contains(query)) {
                    temp.add(vendor);
                }
            }
            filtered = temp;
        }

        // 建立廠商button
        for (String vendor : filtered) {
            View item = Patterns.createVendorBtn(this, vendor, v -> {
                Intent intent = new Intent(this, ImportTable.class);
                intent.putExtra("vendor_name", vendor);
                startActivity(intent);
            });

            vendorContainer.addView(item);
        }

        // 更新顏色
        Patterns.updateCategoryColors(filterLabel, rowIngredient, rowMaterial);
    }
}
