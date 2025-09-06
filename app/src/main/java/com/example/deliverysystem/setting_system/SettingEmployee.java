package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

        Button saveBtn = findViewById(R.id.save_btn);
        saveBtn.setOnClickListener(v -> {
            for (String id : tempDeleted) {
                ConnectDB.deleteEmployee(id, SettingEmployee.this,
                        (success, message, confirmList, inspectorList) -> {
                            if (success) {
                                DataSource.setConfirmPersons(confirmList);
                                DataSource.setInspectors(inspectorList);
                            }
                        });
            }
            tempDeleted.clear();

            for (Map.Entry<String, Boolean> entry : tempStatus.entrySet()) {
                String id = entry.getKey();
                boolean newChecked = entry.getValue();
                ConnectDB.updateAuthorityEmployee(id, !newChecked, SettingEmployee.this,
                        (success, message, confirmList, inspectorList) -> {
                            if (success) {
                                DataSource.setConfirmPersons(confirmList);
                                DataSource.setInspectors(inspectorList);
                                populateEmployeeList();
                            }
                        });
            }
            tempStatus.clear();
            Toast.makeText(SettingEmployee.this, "已儲存變更", Toast.LENGTH_SHORT).show();
        });
    }
    private void populateEmployeeList() {
        employeeListLayout.removeAllViews();

        List<Map<String,String>> confirmList = DataSource.getConfirmPerson();
        List<Map<String,String>> inspectorList = DataSource.getInspector();

        Set<String> addedIds = new HashSet<>();

        // 先加入打勾的
        for (Map<String,String> m : inspectorList) {
            String id = m.get("id");
            String name = m.get("name");
            if (id != null && addedIds.add(id)) {
                addEmployeeItem(employeeListLayout, id, name, true);
            }
        }
        // 再加入未打勾的
        for (Map<String,String> m : confirmList) {
            String id = m.get("id");
            String name = m.get("name");
            if (id != null && addedIds.add(id)) {
                addEmployeeItem(employeeListLayout, id, name, false);
            }
        }
    }

    private Map<String, Boolean> tempStatus = new HashMap<>();
    private Set<String> tempDeleted = new HashSet<>();

    private void addEmployeeItem(FlexboxLayout parentLayout, String id, String name, boolean isChecked) {
        View itemView = getLayoutInflater().inflate(R.layout.employee_item_pattern, parentLayout, false);

        TextView nameView = itemView.findViewById(R.id.nameText);
        ImageView toggleIcon = itemView.findViewById(R.id.toggleIcon);
        ImageView deleteIcon = itemView.findViewById(R.id.deleteIcon);

        nameView.setText(name);

        boolean currentChecked = tempStatus.containsKey(id) ? tempStatus.get(id) : isChecked;
        toggleIcon.setImageResource(currentChecked ? R.drawable.circle_checked : R.drawable.circle_unchecked_gray);

        toggleIcon.setOnClickListener(v -> {
            boolean newChecked = ! (tempStatus.containsKey(id) ? tempStatus.get(id) : isChecked);
            tempStatus.put(id, newChecked);
            populateEmployeeList();
        });

        deleteIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(SettingEmployee.this)
                    .setTitle("刪除 " + name + " 員工")
                    .setMessage("仍需存檔才會更新!!!!!")
                    .setPositiveButton("刪除", (dialog, which) -> {
                        tempDeleted.add(id);
                        parentLayout.removeView(itemView);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        parentLayout.addView(itemView);
    }

    private void showAddEmployeeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新增員工名稱");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("請輸入姓名");

        int padding = (int) getResources().getDisplayMetrics().density * 16;
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