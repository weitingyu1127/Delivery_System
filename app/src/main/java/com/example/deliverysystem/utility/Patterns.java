package com.example.deliverysystem.utility;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.deliverysystem.R;
import com.google.android.flexbox.FlexboxLayout;

public class Patterns {
    /** 分隔線 */
    public static View divider(Context context) {
        View divider = new View(context);
        int height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1,
                context.getResources().getDisplayMetrics()
        );
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        height
                );
        params.setMargins(0, 16, 0, 16);
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#999999"));
        return divider;
    }

    /** 廠商button樣式 */
    public static View createVendorBtn(Context context, String vendorName, View.OnClickListener listener) {

        float scale = context.getResources().getDisplayMetrics().density;

        // -------------- 外層 Layout --------------
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setBackgroundResource(R.drawable.btn_vendor_pattern);
        itemLayout.setPadding(24, 16, 24, 16);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);

        int widthInPx = (int)(400 * scale + 0.5f);
        int heightInPx = (int)(93 * scale + 0.5f);

        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(widthInPx, heightInPx);
        params.setMargins(16, 8, 16, 8);
        itemLayout.setLayoutParams(params);

        // -------------- Icon Circle --------------
        FrameLayout iconContainer = new FrameLayout(context);
        iconContainer.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        iconContainer.setBackgroundResource(R.drawable.circle_gray_pattern);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.ic_person);
        icon.setLayoutParams(new FrameLayout.LayoutParams(32, 32, Gravity.CENTER));
        iconContainer.addView(icon);

        // -------------- Vendor Name Text --------------
        TextView nameText = new TextView(context);
        nameText.setText(vendorName);
        nameText.setTextSize(18);
        nameText.setTextColor(Color.BLACK);
        nameText.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ));
        nameText.setPadding(24, 0, 24, 0);

        // -------------- Arrow Icon --------------
        ImageView arrow = new ImageView(context);
        arrow.setImageResource(R.drawable.ic_arrow);
        arrow.setLayoutParams(new LinearLayout.LayoutParams(48, 48));

        // -------------- Add Views --------------
        itemLayout.addView(iconContainer);
        itemLayout.addView(nameText);
        itemLayout.addView(arrow);

        // -------------- Click --------------
        itemLayout.setOnClickListener(listener);

        return itemLayout;
    }

    /** 更新類別顏色 */
    public static void updateCategoryColors(String filterLabel, ViewGroup... categoryRows) {
        for (ViewGroup row : categoryRows) {
            for (int i = 0; i < row.getChildCount(); i++) {
                View child = row.getChildAt(i);
                if (child instanceof Button) {
                    Button btn = (Button) child;
                    if (btn.getText().toString().equals(filterLabel)) {
                        btn.setBackgroundResource(R.drawable.btn_category_orange);
                    } else {
                        btn.setBackgroundResource(R.drawable.btn_category_white);
                    }
                }
            }
        }
    }
}
