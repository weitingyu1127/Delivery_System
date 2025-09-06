package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class SettingMain extends BaseActivity {
    LinearLayout btnSupplier, btnEmployees, btnPassword, btnExport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_main);

        btnSupplier = findViewById(R.id.supplier);
        btnEmployees = findViewById(R.id.employees);
        btnPassword = findViewById(R.id.password);
        btnExport = findViewById(R.id.export);
        btnSupplier.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingMain.this, SettingSupplier.class);
                startActivity(intent);
            }
        });

        btnEmployees.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SettingMain.this, SettingEmployee.class);
                startActivity(intent);
            }
        });

        btnPassword.setOnClickListener(view -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_update_password, null);
            EditText oldPassword = dialogView.findViewById(R.id.oldPassword);
            EditText newPassword = dialogView.findViewById(R.id.newPassword);
            EditText confirmPassword = dialogView.findViewById(R.id.confirmPassword);

            AlertDialog.Builder builder = new AlertDialog.Builder(this); // 🔧 這一行是關鍵
            builder.setTitle("更新密碼")
                    .setView(dialogView)
                    .setPositiveButton("確認", null) // 設為 null 才能自訂點擊邏輯
                    .setNegativeButton("取消", null);

            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dialogInterface -> {
                Button confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                confirm.setOnClickListener(v -> {
                    String oldPwd = oldPassword.getText().toString().trim();
                    String newPwd = newPassword.getText().toString().trim();
                    String confirmPwd = confirmPassword.getText().toString().trim();
                    String currentPassword = DataSource.getPasswords().get(0); // 假設密碼保存在這裡

                    if (!oldPwd.equals(currentPassword)) {
                        Toast.makeText(this, "舊密碼錯誤", Toast.LENGTH_SHORT).show();
                    } else if (newPwd.equals(oldPwd)) {
                        Toast.makeText(this, "新密碼不得與舊密碼相同", Toast.LENGTH_SHORT).show();
                    } else if (!newPwd.equals(confirmPwd)) {
                        Toast.makeText(this, "新密碼與確認密碼不一致", Toast.LENGTH_SHORT).show();
                    } else {
                        ConnectDB.updatePassword(this, newPwd, (success, message) -> {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            if (success) {
                                dialog.dismiss(); // 成功後關閉對話框
                            }
                        });
                    }
                });
            });

            dialog.show();
        });

        btnExport.setOnClickListener(v -> {
            View dateDialogView = getLayoutInflater().inflate(R.layout.dialog_date_range_export, null);
            EditText startDateInput = dateDialogView.findViewById(R.id.startDate);
            EditText endDateInput = dateDialogView.findViewById(R.id.endDate);
            Spinner vendorSpinner = dateDialogView.findViewById(R.id.vendorSpinner);

            // 日期選擇器
            View.OnClickListener dateClickListener = view -> {
                final EditText target = (EditText) view;
                Calendar calendar = Calendar.getInstance();
                DatePickerDialog datePicker = new DatePickerDialog(
                        SettingMain.this,
                        (view1, year, month, dayOfMonth) -> {
                            String dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                            target.setText(dateStr);
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                );
                datePicker.show();
            };

            startDateInput.setOnClickListener(dateClickListener);
            endDateInput.setOnClickListener(dateClickListener);

            // 🔹 產生廠商清單
            List<String> vendorList = new ArrayList<>();
            vendorList.add("全部廠商");
            vendorList.addAll(DataSource.getVendorProductMap().keySet()); // 假設 VendorProductMap 的 key 是廠商名稱
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, vendorList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            vendorSpinner.setAdapter(adapter);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("選擇日期區間")
                    .setView(dateDialogView)
                    .setPositiveButton("匯出", null)
                    .setNegativeButton("取消", null);

            AlertDialog dialog = builder.create();

            dialog.setOnShowListener(dialogInterface -> {
                Button exportBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                exportBtn.setOnClickListener(view -> {
                    String startDate = startDateInput.getText().toString().trim();
                    String endDate = endDateInput.getText().toString().trim();
                    String selectedVendor = vendorSpinner.getSelectedItem().toString();

                    if (startDate.isEmpty() || endDate.isEmpty()) {
                        Toast.makeText(this, "請選擇開始與結束日期", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 🔹 呼叫匯出功能 (區分是否選擇廠商)
                    if ("全部廠商".equals(selectedVendor)) {
                        ConnectDB.exportDataToExcel(this, startDate, endDate, "", (success, message) -> {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            if (success) dialog.dismiss();
                        });
                    } else {
                        ConnectDB.exportDataToExcel(this, startDate, endDate, selectedVendor, (success, message) -> {
                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            if (success) dialog.dismiss();
                        });
                    }
                });
            });

            dialog.show();
        });

    }
}
