package com.example.deliverysystem;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.LinearLayout;

import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.import_system.ImportMain;
import com.example.deliverysystem.inspect_system.InspectMain;
import com.example.deliverysystem.setting_system.SettingMain;
import com.example.deliverysystem.storage.StorageMain;

public class Main extends BaseActivity {

    LinearLayout btnImport, btnInspect, btnSetting, btnStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);

        btnImport = findViewById(R.id.import_page);
        btnInspect = findViewById(R.id.inspect_page);
        btnStorage = findViewById(R.id.storage_page);
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

        btnStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Main.this, StorageMain.class);
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