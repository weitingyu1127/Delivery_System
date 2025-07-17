package com.example.deliverysystem.import_system;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_main);

        FlexboxLayout container = findViewById(R.id.main_container);
        Map<String, VendorInfo> vendorMap = DataSource.getVendorProductMap();

        // industry → vendors
        Map<String, List<String>> industryMap = new LinkedHashMap<>();
        for (Map.Entry<String, VendorInfo> entry : vendorMap.entrySet()) {
            String vendor = entry.getKey();
            String industry = entry.getValue().getIndustry();

            industryMap.putIfAbsent(industry, new ArrayList<>());
            industryMap.get(industry).add(vendor);
        }

        for (Map.Entry<String, List<String>> entry : industryMap.entrySet()) {
            String industry = entry.getKey();
            List<String> vendors = entry.getValue();

            // 每組分類包成一個區塊
            LinearLayout industryBlock = new LinearLayout(this);
            industryBlock.setOrientation(LinearLayout.VERTICAL);
            industryBlock.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            // Title
            TextView titleView = new TextView(this);
            titleView.setText(industry);
            titleView.setTextSize(20);
            titleView.setTypeface(null, Typeface.BOLD);
            titleView.setTextColor(Color.BLACK);
            titleView.setPadding(0, 32, 0, 16);
            industryBlock.addView(titleView);

            // Vendor Button Row
            FlexboxLayout buttonRow = new FlexboxLayout(this);
            buttonRow.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            buttonRow.setFlexWrap(FlexWrap.WRAP);
            buttonRow.setJustifyContent(JustifyContent.FLEX_START);
            buttonRow.setAlignItems(AlignItems.CENTER);


            for (String vendorName : vendors) {
                Button vendorButton = new Button(this);
                vendorButton.setText(vendorName);
                vendorButton.setTextColor(Color.WHITE);
                vendorButton.setTextSize(16);
                vendorButton.setBackgroundColor(Color.parseColor("#4CAF50"));

                FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(16, 8, 16, 8);
                vendorButton.setLayoutParams(params);

                vendorButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ImportTable.class);
                    intent.putExtra("vendor_name", vendorName);
                    startActivity(intent);
                });

                buttonRow.addView(vendorButton);
            }

            industryBlock.addView(buttonRow);
            container.addView(industryBlock);
        }

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(ImportMain.this, ImportItem.class);
            startActivity(intent);
        });
    }

}

