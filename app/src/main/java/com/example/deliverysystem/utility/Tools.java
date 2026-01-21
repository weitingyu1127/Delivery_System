package com.example.deliverysystem.utility;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.AsyncListUtil;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.inspect_system.InspectRecord;

import java.util.List;
import java.util.Map;

public class Tools extends BaseActivity {
    /** Toast訊息 */
    public static void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    /** 按鈕跳轉 */
    public static void navigator(View view, Context context, Class<? extends Activity> targetActivity, boolean needPassword, @Nullable Bundle params) {
        view.setOnClickListener(v -> {
            Runnable go = () -> {
                Intent intent = new Intent(context, targetActivity);
                if (params != null) {
                    intent.putExtras(params);
                }
                context.startActivity(intent);
            };
            if (!needPassword) {
                go.run();
                return;
            }
            LayoutInflater inflater = LayoutInflater.from(context);
            View dialogView = inflater.inflate(R.layout.dialog_password, null);
            EditText editPassword = dialogView.findViewById(R.id.editPassword);

            new AlertDialog.Builder(context)
                .setTitle("密碼驗證")
                .setView(dialogView)
                .setPositiveButton("確定", (dialog, which) -> {
                    String input = editPassword.getText().toString().trim();
                    if (DataSource.getPasswords().contains(input)) {
                        go.run();
                    } else {
                        showToast(context, "密碼錯誤");
                    }
                })
                .setNegativeButton("取消", null)
                .show();
        });
    }
    /** 清除 Table */
    public static void clearTable(ViewGroup layout) {
        if (layout != null) {
            layout.removeAllViews();
        }
    }
    /** 建立 Table Row */
    public static void addTableRow(
            Context context,
            ViewGroup tableLayout,
            List<String> columns,
            Runnable onDeleteClick
    ) {
        LinearLayout rowLayout = new LinearLayout(context);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setPadding(12, 15, 12, 15);
        rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        int textSize = 16;
        int padding = 8;

        for (String text : columns) {
            rowLayout.addView(createCell(context, text, 0.5f, textSize, padding));
        }

        // ------------- 刪除按鈕區域 -------------
        LinearLayout btnContainer = new LinearLayout(context);
        btnContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.5f));
        btnContainer.setGravity(Gravity.CENTER);

        AppCompatButton btnDelete = new AppCompatButton(context);
        btnDelete.setText("刪除");
        btnDelete.setTextSize(14f);
        btnDelete.setTextColor(Color.WHITE);
        btnDelete.setBackgroundResource(R.drawable.btn_orange);

        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 84, context.getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, context.getResources().getDisplayMetrics());
        btnDelete.setLayoutParams(new LinearLayout.LayoutParams(width, height));

        btnDelete.setOnClickListener(v -> {
            if (onDeleteClick != null) onDeleteClick.run();
        });

        btnContainer.addView(btnDelete);
        rowLayout.addView(btnContainer);

        // ------------- Divider -------------
        View divider = Patterns.divider(context);

        tableLayout.addView(rowLayout);
        tableLayout.addView(divider);
    }

    /** 建立 Table Cell */
    public static TextView createCell(Context context, String text, float weight, int textSize, int padding) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextSize(textSize);
        textView.setTextColor(Color.BLACK);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(padding, padding, padding, padding);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, weight
        ));
        return textView;
    }

    /** 地點選擇 */
    public static void selectPlace(Activity activity, String place, Map<String, Button> placeMap) {
        Button selected = placeMap.get(place);
        for (Button btn : placeMap.values()) {
            if (btn == selected) {
                btn.setBackgroundResource(R.drawable.btn_orange);
                btn.setTextColor(activity.getColor(android.R.color.white));
            } else {
                btn.setBackgroundResource(R.drawable.btn_white);
                btn.setTextColor(activity.getColor(android.R.color.black));
            }
        }
    }
}
