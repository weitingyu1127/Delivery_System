package com.example.deliverysystem.inspect_system;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.R;
import com.example.deliverysystem.utility.Tools;

public class InspectMain extends BaseActivity {

    private LinearLayout ingredientButton;
    private LinearLayout materialButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inspect_main);

        ingredientButton = findViewById(R.id.ingredient);
        materialButton = findViewById(R.id.material);

        /** 原料 */
        Bundle ingredientParam = new Bundle();
        ingredientParam.putString("type", "原料");
        Tools.navigator(ingredientButton, this, InspectTable.class, false, ingredientParam);

        /** 物料 */
        Bundle materialParam = new Bundle();
        materialParam.putString("type", "物料");
        Tools.navigator(materialButton, this, InspectTable.class, false, materialParam);
    }
}