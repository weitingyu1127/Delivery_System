package com.example.deliverysystem.import_system;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImportMain extends BaseActivity {

    private FlexboxLayout vendorContainer;
    private Map<String, List<String>> industryMap;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_main);

        FlexboxLayout mainContainer = findViewById(R.id.main_container);
        Map<String, VendorInfo> vendorMap = DataSource.getVendorProductMap();

        // 整理 industryMap
        industryMap = new LinkedHashMap<>();
        for (Map.Entry<String, VendorInfo> entry : vendorMap.entrySet()) {
            String vendor = entry.getKey();
            String industry = entry.getValue().getIndustry();
            industryMap.putIfAbsent(industry, new ArrayList<>());
            industryMap.get(industry).add(vendor);
        }

        // 👉 分類按鈕列
        LinearLayout categoryRow = new LinearLayout(this);
        categoryRow.setOrientation(LinearLayout.HORIZONTAL);
        categoryRow.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // "All" 按鈕
        addCategoryButton(categoryRow, "All");

        // 其他分類
        for (String industry : industryMap.keySet()) {
            addCategoryButton(categoryRow, industry);
        }

        mainContainer.addView(categoryRow);

        // 👉 廠商按鈕容器（會根據選擇的分類清空再重新加入）
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

        // 預設顯示 All
        renderVendors("All");
        updateCategoryColors(categoryRow);

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ImportMain.this, ImportItem.class);
            startActivity(intent);
        });
    }

    private void addCategoryButton(LinearLayout categoryRow, String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(18);
        btn.setTextColor(Color.BLACK);
        btn.setBackgroundResource(R.drawable.btn_category_white); // ✅ 使用 drawable

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) getResources().getDisplayMetrics().density * 108,
                (int) getResources().getDisplayMetrics().density * 42
        );
        params.setMargins(8, 8, 8, 8);
        btn.setLayoutParams(params);

        btn.setOnClickListener(v -> {
            currentFilter = label;
            renderVendors(label);
            updateCategoryColors((ViewGroup) categoryRow);
        });

        categoryRow.addView(btn);
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
            itemLayout.setBackgroundResource(R.drawable.btn_vendor_pattern); // 你已設計的背景
            itemLayout.setPadding(24, 16, 24, 16);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            int widthInDp = 484;
            int heightInDp = 93;
            float scale = getResources().getDisplayMetrics().density;

            int widthInPx = (int) (widthInDp * scale + 0.5f);
            int heightInPx = (int) (heightInDp * scale + 0.5f);

            FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                    widthInPx,
                    heightInPx
            );
            layoutParams.setMargins(16, 8, 16, 8);
            itemLayout.setLayoutParams(layoutParams);

            // 左邊圓形icon
            FrameLayout iconContainer = new FrameLayout(this);
            FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(64, 64);
            iconContainer.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
            iconContainer.setBackgroundResource(R.drawable.circle_gray_pattern);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_person); // 用你自己的 user icon
            icon.setLayoutParams(new FrameLayout.LayoutParams(32, 32, Gravity.CENTER));
            iconContainer.addView(icon);

            // 名稱 TextView
            TextView nameText = new TextView(this);
            nameText.setText(vendor);
            nameText.setTextSize(18);
            nameText.setTextColor(Color.BLACK);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)); // 占滿中間空間
            nameText.setPadding(24, 0, 24, 0);

            // 右側箭頭
            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.ic_arrow); // 右側箭頭圖
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(48, 48);
            arrow.setLayoutParams(arrowParams);

            // 加入所有元件
            itemLayout.addView(iconContainer);
            itemLayout.addView(nameText);
            itemLayout.addView(arrow);

            itemLayout.setOnClickListener(v -> {
                Intent intent = new Intent(this, ImportTable.class);
                intent.putExtra("vendor_name", vendor);
                startActivity(intent);
            });

            vendorContainer.addView(itemLayout);
        }

    }
}
