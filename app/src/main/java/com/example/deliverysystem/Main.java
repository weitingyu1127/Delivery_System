package com.example.deliverysystem;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

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
                Intent intent = new Intent(Main.this, SettingMain.class);
                startActivity(intent);
            }
        });
    }
}