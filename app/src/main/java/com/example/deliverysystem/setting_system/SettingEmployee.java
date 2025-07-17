package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.JustifyContent;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SettingEmployee extends BaseActivity {

    private FlexboxLayout employeeListLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_employee);
        FloatingActionButton addEmployeeBtn = findViewById(R.id.addEmployee);
        addEmployeeBtn.setOnClickListener(v -> showAddEmployeeDialog());

        employeeListLayout = findViewById(R.id.employeeListLayout);
        employeeListLayout.setFlexWrap(FlexWrap.WRAP);
        employeeListLayout.setFlexDirection(FlexDirection.ROW);
        employeeListLayout.setJustifyContent(JustifyContent.FLEX_START);

        populateEmployeeList();
    }

    private void populateEmployeeList() {
        employeeListLayout.removeAllViews();

        List<String> confirmList = DataSource.getConfirmPerson();
        List<String> inspectorList = DataSource.getInspector();

        Set<String> added = new HashSet<>();

        // 先加入打勾的員工
        for (String name : confirmList) {
            if (added.add(name)) {
                addEmployeeItem(employeeListLayout, name, true);
            }
        }

        // 再加入未打勾的員工
        for (String name : inspectorList) {
            if (added.add(name)) {
                addEmployeeItem(employeeListLayout, name, false);
            }
        }
    }

    private void addEmployeeItem(FlexboxLayout parentLayout, String name, boolean isChecked) {
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(16, 16, 16, 16);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        itemLayout.setLayoutParams(new FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        CheckBox checkBox = new CheckBox(this);
        // 建立 listener（不要馬上綁）
        CompoundButton.OnCheckedChangeListener listener = (buttonView, checked) -> {
            ConnectDB.updateAuthorityEmployee(name, checked, SettingEmployee.this, (success, message) -> {
                Toast.makeText(SettingEmployee.this, message, Toast.LENGTH_SHORT).show();
            });
        };

        // 避免 setChecked 時觸發 listener
        checkBox.setOnCheckedChangeListener(null);
        checkBox.setChecked(isChecked);
        checkBox.setOnCheckedChangeListener(listener);

        TextView nameView = new TextView(this);
        nameView.setText(name);
        nameView.setTextSize(18);
        nameView.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        itemLayout.addView(checkBox);
        itemLayout.addView(nameView);
        parentLayout.addView(itemLayout);
    }
    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新增員工名稱");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("請輸入姓名");

        int padding = (int) getResources().getDisplayMetrics().density * 16; // 16dp
        input.setPadding(padding, padding, padding, padding);

        // 設定寬度為 MATCH_PARENT
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(inputParams);

        // 包裝容器加上 padding
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, padding);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("確認", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                ConnectDB.addEmployee(name, SettingEmployee.this, (success, message, newConfirmList, newInspectorList) -> {
                    Toast.makeText(SettingEmployee.this, message, Toast.LENGTH_SHORT).show();
                    if (success) {
                        DataSource.setConfirmPersons(newConfirmList);
                        DataSource.setInspectors(newInspectorList);
                        populateEmployeeList();
                    }
                });
            } else {
                Toast.makeText(this, "姓名不能為空", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
        builder.show();
    }
}