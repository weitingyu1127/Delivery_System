package com.example.deliverysystem.setting_system;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.example.deliverysystem.R;
import com.google.android.flexbox.*;

import java.util.*;

public class SettingEmployee extends BaseActivity {

    private FlexboxLayout employeeListLayout;
    private final Map<String, Boolean> tempStatus = new HashMap<>();
    private final Set<String> tempDeleted = new HashSet<>();
    private final List<String> tempAdded = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_employee);

        employeeListLayout = findViewById(R.id.employeeListLayout);
        employeeListLayout.setFlexWrap(FlexWrap.WRAP);
        employeeListLayout.setFlexDirection(FlexDirection.ROW);

        findViewById(R.id.addEmployee).setOnClickListener(v -> showAddEmployeeDialog());
        findViewById(R.id.save_btn).setOnClickListener(v -> saveChanges());

        populateEmployeeList();
    }

    private void saveChanges() {
        tempDeleted.forEach(id -> ConnectDB.deleteEmployee(id, this, (s, m, c, i) -> {
            if (s) { DataSource.setConfirmPersons(c); DataSource.setInspectors(i); }
        }));
        tempDeleted.clear();

        tempStatus.forEach((id, checked) -> ConnectDB.updateAuthorityEmployee(id, !checked, this, (s, m, c, i) -> {
            if (s) { DataSource.setConfirmPersons(c); DataSource.setInspectors(i); populateEmployeeList(); }
        }));
        tempStatus.clear();

        tempAdded.forEach(name -> ConnectDB.addEmployee(name, this, (s, m, c, i) -> {
            Toast.makeText(this, m, Toast.LENGTH_SHORT).show();
            if (s) { DataSource.setConfirmPersons(c); DataSource.setInspectors(i); populateEmployeeList(); }
        }));
        tempAdded.clear();

        Toast.makeText(this, "已儲存變更", Toast.LENGTH_SHORT).show();
    }

    private void populateEmployeeList() {
        employeeListLayout.removeAllViews();
        Set<String> added = new HashSet<>();

        DataSource.getInspector().forEach(m -> addIfNew(m, true, added));
        DataSource.getConfirmPerson().forEach(m -> addIfNew(m, false, added));
        tempAdded.forEach(name -> addEmployeeItem("temp_" + name, name, false));
    }

    private void addIfNew(Map<String,String> m, boolean checked, Set<String> added) {
        String id = m.get("id"), name = m.get("name");
        if (id != null && added.add(id)) addEmployeeItem(id, name, checked);
    }

    private void addEmployeeItem(String id, String name, boolean isChecked) {
        View item = getLayoutInflater().inflate(R.layout.employee_item_pattern, employeeListLayout, false);
        TextView nameView = item.findViewById(R.id.nameText);
        ImageView toggle = item.findViewById(R.id.toggleIcon);
        ImageView delete = item.findViewById(R.id.deleteIcon);

        nameView.setText(name);
        boolean current = tempStatus.getOrDefault(id, isChecked);
        toggle.setImageResource(current ? R.drawable.circle_checked : R.drawable.circle_unchecked_gray);

        toggle.setOnClickListener(v -> { tempStatus.put(id, !current); populateEmployeeList(); });
        delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("刪除 " + name + " 員工").setMessage("仍需存檔才會更新!!!!!")
                .setPositiveButton("刪除", (d, w) -> {
                    if (id.startsWith("temp_")) tempAdded.remove(name); else tempDeleted.add(id);
                    employeeListLayout.removeView(item);
                }).setNegativeButton("取消", null).show());

        employeeListLayout.addView(item);
    }

    private void showAddEmployeeDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("請輸入姓名");
        input.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout container = new LinearLayout(this);
        int pad = (int)(getResources().getDisplayMetrics().density * 16);
        container.setPadding(pad, pad, pad, pad);
        container.addView(input);

        new AlertDialog.Builder(this).setTitle("新增員工名稱").setView(container)
                .setPositiveButton("確認", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) { tempAdded.add(name); populateEmployeeList(); }
                    else Toast.makeText(this, "姓名不能為空", Toast.LENGTH_SHORT).show();
                }).setNegativeButton("取消", null).show();
    }
}
