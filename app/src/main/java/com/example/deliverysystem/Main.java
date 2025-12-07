package com.example.deliverysystem;

import android.os.Bundle;
import android.widget.LinearLayout;

import com.example.deliverysystem.import_system.ImportMain;
import com.example.deliverysystem.inspect_system.InspectMain;
import com.example.deliverysystem.setting_system.SettingMain;
import com.example.deliverysystem.storage.StorageMain;
import com.example.deliverysystem.utility.*;

public class Main extends BaseActivity {
    /*
    * btnImport: 進貨
    * btnInspect: 驗收
    * btnSetting: 設定
    * btnStorage: 庫存
    * **/
    LinearLayout btnImport, btnInspect, btnSetting, btnStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_page);

        btnImport = findViewById(R.id.import_page);
        btnInspect = findViewById(R.id.inspect_page);
        btnStorage = findViewById(R.id.storage_page);
        btnSetting = findViewById(R.id.setting_page);

        Tools.navigator(btnImport,  this, ImportMain.class, false, null);
        Tools.navigator(btnInspect,  this, InspectMain.class, false, null);
        Tools.navigator(btnStorage,  this, StorageMain.class, false, null);
        Tools.navigator(btnSetting,  this, SettingMain.class, true, null);
    }
}