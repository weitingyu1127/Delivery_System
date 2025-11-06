package com.example.deliverysystem.import_system;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.VendorInfo;
import com.example.deliverysystem.inspect_system.InspectDetail;
import com.example.deliverysystem.inspect_system.InspectMain;
import com.example.deliverysystem.inspect_system.InspectTable;
import com.google.android.flexbox.AlignSelf;
import com.google.android.flexbox.FlexboxLayout;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

public class ImportItem extends AppCompatActivity {

    private FlexboxLayout selectedItemInputContainer;
    private Button btnSubmit;
    private Map<String, VendorInfo> vendorProductMap;
    private String vendorName;
    private String vendorType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_item);

        Spinner importPlace = findViewById(R.id.import_place);
        List<String> placeList = Arrays.asList("進貨地點","本廠", "倉庫", "線西");
        ArrayAdapter<String> placeAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                placeList
        );
        placeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        importPlace.setAdapter(placeAdapter);
        importPlace.setSelection(0, false);

        vendorName = getIntent().getStringExtra("vendor_name");

        TextView title = findViewById(R.id.import_vendor_title);
        title.setText(vendorName);

        List<String> products = DataSource.getProductsByVendor(vendorName);
        vendorType = DataSource.getTypeByVendor(vendorName);

        selectedItemInputContainer = findViewById(R.id.selectedItemInputContainer);

        vendorProductMap = DataSource.getVendorProductMap();

        if (!products.isEmpty()) {
            for (String product : products) {
                addProductInputRow(product);
            }
            showSubmitButton(vendorType, vendorName);
        }
        ImageView backIcon = findViewById(R.id.back_icon);
        backIcon.setOnClickListener(v -> {
            finish();
        });
    }

    private void addProductInputRow(String product) {
        float dp = getResources().getDisplayMetrics().density;
        int gap = (int) (12 * dp);

        FlexboxLayout.LayoutParams rowParams = new FlexboxLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(gap, gap, gap, gap);
        rowParams.setFlexBasisPercent(0.47f);


        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setTag(product);
        row.setPadding((int)(20*dp), (int)(20*dp), (int)(20*dp), (int)(20*dp));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.product_add_pattern);
        row.setLayoutParams(rowParams);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_product);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams((int)(50*dp), (int)(50*dp));
        iconParams.setMargins(0, 0, (int)(20*dp), 0);
        row.addView(icon, iconParams);

        TextView tv = new TextView(this);
        tv.setText(product);
        tv.setTextSize(20f);
        tv.setTextColor(Color.BLACK);
        LinearLayout.LayoutParams tvParams =
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tv.setLayoutParams(tvParams);
        row.addView(tv);

        ImageButton btnMinus = new ImageButton(this);
        btnMinus.setImageResource(R.drawable.ic_minus);
        btnMinus.setBackgroundColor(Color.TRANSPARENT);
        btnMinus.setLayoutParams(new LinearLayout.LayoutParams((int)(30*dp), (int)(30*dp)));
        row.addView(btnMinus);

        EditText qtyInput = new EditText(this);
        qtyInput.setText("0");
        qtyInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        qtyInput.setGravity(Gravity.CENTER);
        qtyInput.setWidth((int)(120*dp));
        qtyInput.setHeight((int)(40*dp));
        qtyInput.setBackgroundColor(Color.parseColor("#DDDDDD"));
        qtyInput.setPadding((int)(10*dp), 0, (int)(10*dp), 0);
        row.addView(qtyInput);

        ImageButton btnPlus = new ImageButton(this);
        btnPlus.setImageResource(R.drawable.ic_plus);
        btnPlus.setBackgroundColor(Color.TRANSPARENT);
        btnPlus.setLayoutParams(new LinearLayout.LayoutParams((int)(30*dp), (int)(30*dp)));
        row.addView(btnPlus);

        Spinner unitSpinner = new Spinner(this);
        DataSource.setupUnitSpinner(this, unitSpinner, null);
        row.addView(unitSpinner);

        btnMinus.setOnClickListener(v -> {
            int qty = Integer.parseInt(qtyInput.getText().toString());
            if (qty > 0) qtyInput.setText(String.valueOf(qty - 1));
        });
        btnPlus.setOnClickListener(v -> {
            int qty = Integer.parseInt(qtyInput.getText().toString());
            qtyInput.setText(String.valueOf(qty + 1));
        });

        selectedItemInputContainer.addView(row);

        if (btnSubmit != null) {
            selectedItemInputContainer.removeView(btnSubmit);
            selectedItemInputContainer.addView(btnSubmit);
        }
    }

    private void showSubmitButton(String type, String selectedVendor) {
        FrameLayout btnContainer = findViewById(R.id.btn_container);

        if (btnSubmit != null && btnSubmit.getParent() != null) {
            ((ViewGroup) btnSubmit.getParent()).removeView(btnSubmit);
        }

        if (btnSubmit == null) {
            btnSubmit = new Button(this);
            btnSubmit.setText("新增");
            btnSubmit.setTextSize(32f);
            btnSubmit.setTypeface(null, Typeface.BOLD);
            btnSubmit.setTextColor(Color.WHITE);

            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(Color.parseColor("#F8B272"));
            drawable.setCornerRadius(50);
            btnSubmit.setBackground(drawable);

            FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(267, 80);
            btnParams.gravity = Gravity.END;
            btnParams.topMargin = 50;
            btnSubmit.setLayoutParams(btnParams);
            btnSubmit.setOnClickListener(v -> {
                Spinner importPlace = findViewById(R.id.import_place);
                String place = importPlace.getSelectedItem() != null
                        ? importPlace.getSelectedItem().toString()
                        : "";

                if ("進貨地點".equals(place) || place.isEmpty()) {
                    Toast.makeText(this, "請選擇進貨地點", Toast.LENGTH_SHORT).show();
                    return;
                }

                boolean hasValidItem = false;
                String importDate = LocalDate.now().toString();

                // 收集所有要加入的產品
                List<Map<String, Object>> productsToAdd = new ArrayList<>();

                for (int i = 0; i < selectedItemInputContainer.getChildCount(); i++) {
                    View view = selectedItemInputContainer.getChildAt(i);
                    if (view instanceof LinearLayout) {
                        LinearLayout row = (LinearLayout) view;
                        if (row.getChildCount() >= 6) {
                            TextView name = (TextView) row.getChildAt(1);
                            EditText qty = (EditText) row.getChildAt(3);
                            Spinner unit = (Spinner) row.getChildAt(5);

                            String product = name.getText().toString();
                            String amountStr = qty.getText().toString().trim();
                            int amount = amountStr.isEmpty() ? 0 : Integer.parseInt(amountStr);

                            if (amount > 0) {
                                hasValidItem = true;
                                String unitStr = unit.getSelectedItem().toString();
                                String amountWithUnit = amount + unitStr;

                                Map<String, Object> item = new HashMap<>();
                                item.put("product", product);
                                item.put("quantity", amountWithUnit);
                                item.put("place", place);
                                productsToAdd.add(item);
                            }
                        }
                    }
                }

                if (!hasValidItem) {
                    Toast.makeText(this, "請至少填寫一項商品的數量", Toast.LENGTH_SHORT).show();
                    return;
                }

                ConnectDB.addImportRecord(vendorType, importDate, vendorName, productsToAdd,
                this, success -> {
                    if (success) {
                        ConnectDB.addStorage(place, vendorType, vendorName, productsToAdd, successStorage -> {
                            if (successStorage) {
                                Toast.makeText(this, "新增完成", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(this, InspectMain.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(this, "庫存更新失敗", Toast.LENGTH_SHORT).show();
                            }
                        });
                        finish();
                    } else {
                        Toast.makeText(this, "新增失敗", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        }
        btnContainer.removeAllViews();
        btnContainer.addView(btnSubmit);
        btnContainer.setVisibility(View.VISIBLE);
    }
}
