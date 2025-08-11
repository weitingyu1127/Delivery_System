package com.example.deliverysystem;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.import_system.ImportMain;
import com.example.deliverysystem.import_system.ImportTable;
import com.example.deliverysystem.inspect_system.InspectMain;
import com.example.deliverysystem.inspect_system.InspectTable;
import com.example.deliverysystem.setting_system.SettingMain;
import com.google.android.material.navigation.NavigationView;

public class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_base);

        getAllData(); // 集中載入資料並通知子頁面

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        Toolbar toolbar = findViewById(R.id.toolbar);

        // 設定 toolbar 為 action bar
        setSupportActionBar(toolbar);

        // 加入menu icon 按鈕
        toolbar.setNavigationIcon(R.drawable.ic_menu);

        // 點擊圖示開啟 drawer
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        // 選單項目點擊
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.nav_import_records) {
                if (!(this instanceof ImportTable)) {
                    startActivity(new Intent(this, ImportMain.class));
                }
            }
            if (id == R.id.nav_inspect_records) {
                if (!(this instanceof InspectTable)) {
                    startActivity(new Intent(this, InspectMain.class));
                }
            }
            if (id == R.id.nav_setting) {
                LayoutInflater inflater = LayoutInflater.from(BaseActivity.this); // 🔧 修正 this
                View dialogView = inflater.inflate(R.layout.dialog_password, null);
                EditText editPassword = dialogView.findViewById(R.id.editPassword);

                new AlertDialog.Builder(BaseActivity.this) // 🔧 修正 this
                        .setTitle("密碼驗證")
                        .setView(dialogView)
                        .setPositiveButton("確定", (dialog, which) -> {
                            String password = editPassword.getText().toString().trim();
                            if (DataSource.getPasswords().contains(password)) {
                                Intent intent = new Intent(BaseActivity.this, SettingMain.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(BaseActivity.this, "密碼錯誤", Toast.LENGTH_SHORT).show(); // 🔧 修正 this
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public void setContentView(int layoutResID) {
        FrameLayout contentFrame = findViewById(R.id.content_frame);
        if (contentFrame != null) {
            getLayoutInflater().inflate(layoutResID, contentFrame, true);
        }
    }

    public void getAllData() {
        ConnectDB.getEmployees("inspector", () -> {
            ConnectDB.getEmployees("confirmPerson", () -> {
                ConnectDB.getVendorProductData(vendorProductMap -> {
                    DataSource.setVendorProductMap(vendorProductMap);
                    ConnectDB.getPasswords(passwordList -> {
                        DataSource.setPasswords(passwordList);
                    });
                });
            });
        });
    }
}
