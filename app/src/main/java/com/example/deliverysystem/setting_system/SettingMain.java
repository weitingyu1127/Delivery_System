package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;

public class SettingMain extends BaseActivity {
    LinearLayout btnSupplier, btnEmployees, btnPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_main);

        btnSupplier = findViewById(R.id.supplier);
        btnEmployees = findViewById(R.id.employees);
        btnPassword = findViewById(R.id.password);

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
//
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

    }
}
