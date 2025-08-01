package com.example.deliverysystem.inspect_system;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import com.example.deliverysystem.BaseActivity;
import com.example.deliverysystem.R;

public class InspectMain extends BaseActivity {

    private LinearLayout ingredientButton;
    private LinearLayout materialButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inspect_main);

        ingredientButton = findViewById(R.id.ingredient);
        materialButton = findViewById(R.id.material);

        ingredientButton.setOnClickListener(v -> {
            Intent intent = new Intent(InspectMain.this, InspectTable.class);
            intent.putExtra("type", "原料");
            startActivity(intent);
        });

        materialButton.setOnClickListener(v -> {
            Intent intent = new Intent(InspectMain.this, InspectTable.class);
            intent.putExtra("type", "物料");
            startActivity(intent);
        });
    }
}