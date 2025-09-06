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
    private String currentFilter = "All";

    // 兩排分類 Row（為了在點擊時同時更新選中樣式）
    private FlexboxLayout rowRaw;   // 第一排：All + 原料
    private FlexboxLayout rowStuff; // 第二排：物料
    private EditText searchInput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_main);

        FlexboxLayout mainContainer = findViewById(R.id.main_container);
        searchInput = findViewById(R.id.search_input);
        Map<String, VendorInfo> vendorMap = DataSource.getVendorProductMap();

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
        Set<String> stuffIndustries = new LinkedHashSet<>();
        for (VendorInfo info : vendorMap.values()) {
            String industry = info.getIndustry();
            String type = info.getType(); // "原料" or "物料"
            if ("原料".equals(type)) {
                rawIndustries.add(industry);
            } else if ("物料".equals(type)) {
                stuffIndustries.add(industry);
            }
        }

        // 在 onCreate 裡
        // 第 1 排：原料
        LinearLayout rawRowContainer = makeRowWithLabel("原料:");
        rowRaw = (FlexboxLayout) rawRowContainer.getChildAt(1); // 第二個子元件是 FlexboxLayout
        addCategoryButton(rowRaw, "All");
        for (String industry : rawIndustries) {
            addCategoryButton(rowRaw, industry);
        }
        mainContainer.addView(rawRowContainer);

        View divider = new View(this);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (getResources().getDisplayMetrics().density * 1) // 1dp 高
        );
        dividerParams.setMargins(0, 16, 0, 16); // 上下留空隙
        divider.setLayoutParams(dividerParams);
        divider.setBackgroundColor(Color.parseColor("#999999")); // 灰色
        mainContainer.addView(divider);

        // 第 2 排：物料
        LinearLayout stuffRowContainer = makeRowWithLabel("物料:");
        rowStuff = (FlexboxLayout) stuffRowContainer.getChildAt(1);
        for (String industry : stuffIndustries) {
            addCategoryButton(rowStuff, industry);
        }
        mainContainer.addView(stuffRowContainer);


        // 👉 廠商按鈕容器（依選擇分類刷新）
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
        renderVendors("All", "");
        // 同步更新兩排的選中樣式
        updateCategoryColors(rowRaw);
        updateCategoryColors(rowStuff);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderVendors(currentFilter, s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    /** 建立一排：左邊是標籤，右邊是可換行的 FlexboxLayout */
    private LinearLayout makeRowWithLabel(String labelText) {
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
        label.setPadding(0, 0, 16, 0); // 標籤與按鈕間距
        container.addView(label);

        FlexboxLayout flexRow = makeFlexRow();
        flexRow.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        container.addView(flexRow);

        return container;
    }

    /** 建一排可換行的 Flexbox row（保持你既有風格） */
    private FlexboxLayout makeFlexRow() {
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

    private void addCategoryButton(FlexboxLayout targetRow, String label) {
        Button btn = new Button(this);
        btn.setText(label);
        btn.setTextSize(18);
        btn.setTextColor(Color.BLACK);
        btn.setBackgroundResource(R.drawable.btn_category_white);
        // 給左右空間（px）；如要 dp→px 可再換算
        btn.setPadding(32, 8, 32, 8);

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(8, 12, 8, 12); // 按鈕間距：左右 8、上下 12
        btn.setLayoutParams(lp);

        btn.setOnClickListener(v -> {
            currentFilter = label;
            renderVendors(label, searchInput.getText().toString());
            updateCategoryColors(rowRaw);
            updateCategoryColors(rowStuff);
        });

        targetRow.addView(btn);
    }

    /** 保留你原本的寫法：逐一檢查該 row 裡的 Button */
    private void updateCategoryColors(ViewGroup categoryRow) {
        for (int i = 0; i < categoryRow.getChildCount(); i++) {
            if (categoryRow.getChildAt(i) instanceof Button) {
                Button btn = (Button) categoryRow.getChildAt(i);
                if (btn.getText().toString().equals(currentFilter)) {
                    btn.setBackgroundResource(R.drawable.btn_category_orange);
                } else {
                    btn.setBackgroundResource(R.drawable.btn_category_white);
                }
            }
        }
    }

    private void renderVendors(String filter, String searchQuery) {
        vendorContainer.removeAllViews();

        List<String> filtered = new ArrayList<>();
        if (filter.equals("All")) {
            for (List<String> group : industryMap.values()) {
                filtered.addAll(group);
            }
        } else {
            filtered = industryMap.getOrDefault(filter, new ArrayList<>());
        }

        // 🔍 文字過濾（忽略大小寫）
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

        for (String vendor : filtered) {
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setBackgroundResource(R.drawable.btn_vendor_pattern);
            itemLayout.setPadding(24, 16, 24, 16);
            itemLayout.setGravity(Gravity.CENTER_VERTICAL);

            int widthInDp = 400;
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

            FrameLayout iconContainer = new FrameLayout(this);
            iconContainer.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
            iconContainer.setBackgroundResource(R.drawable.circle_gray_pattern);

            ImageView icon = new ImageView(this);
            icon.setImageResource(R.drawable.ic_person);
            icon.setLayoutParams(new FrameLayout.LayoutParams(32, 32, Gravity.CENTER));
            iconContainer.addView(icon);

            TextView nameText = new TextView(this);
            nameText.setText(vendor);
            nameText.setTextSize(18);
            nameText.setTextColor(Color.BLACK);
            nameText.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            nameText.setPadding(24, 0, 24, 0);

            ImageView arrow = new ImageView(this);
            arrow.setImageResource(R.drawable.ic_arrow);
            LinearLayout.LayoutParams arrowParams = new LinearLayout.LayoutParams(48, 48);
            arrow.setLayoutParams(arrowParams);

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
