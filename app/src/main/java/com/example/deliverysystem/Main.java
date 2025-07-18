package com.example.deliverysystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.import_system.ImportMain;
import com.example.deliverysystem.inspect_system.InspectMain;
import com.example.deliverysystem.setting_system.SettingMain;

public class Main extends BaseActivity {

    Button btnImport, btnInspect, btnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page); // 替換成你的 layout 名稱

        btnImport = findViewById(R.id.import_page);
        btnInspect = findViewById(R.id.inspect_page);
        btnSetting = findViewById(R.id.setting_page);

        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, ImportMain.class);
                startActivity(intent);
            }
        });

        btnInspect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, InspectMain.class);
                startActivity(intent);
            }
        });

        btnSetting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LayoutInflater inflater = LayoutInflater.from(Main.this); // 🔧 修正 this
                View dialogView = inflater.inflate(R.layout.dialog_password, null);
                EditText editPassword = dialogView.findViewById(R.id.editPassword);

                new AlertDialog.Builder(Main.this) // 🔧 修正 this
                        .setTitle("密碼驗證")
                        .setView(dialogView)
                        .setPositiveButton("確定", (dialog, which) -> {
                            String password = editPassword.getText().toString().trim();
                            if (DataSource.getPasswords().contains(password)) {
                                Intent intent = new Intent(Main.this, SettingMain.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(Main.this, "密碼錯誤", Toast.LENGTH_SHORT).show(); // 🔧 修正 this
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

    }
}