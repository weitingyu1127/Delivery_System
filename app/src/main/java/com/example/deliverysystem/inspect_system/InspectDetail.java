package com.example.deliverysystem.inspect_system;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.deliverysystem.R;
import com.example.deliverysystem.data_source.ConnectDB;
import com.example.deliverysystem.data_source.DataSource;
import com.google.android.flexbox.FlexboxLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.github.chrisbanes.photoview.PhotoView;

public class InspectDetail extends AppCompatActivity {
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private Uri photoUri;

    /** 下拉選單: 溫度 */
    private Spinner spinnerDegree;
    private String degreeType, degreeValue;
    EditText degreeInput;

    private LinearLayout imagesContainer;
    private final List<String> dbImages = new ArrayList<>();

    private final List<Uri> stagedAdds = new ArrayList<>();

    private final List<String> stagedDeletes = new ArrayList<>();
    private int originalQty = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inspect_edit);

        imagesContainer = findViewById(R.id.imagesContainer);

        ImageView backIcon = findViewById(R.id.back_icon);
        backIcon.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        String type = intent.getStringExtra("type");
        String staff = intent.getStringExtra("staff");
        String mode = intent.getStringExtra("mode");
        if ("view".equalsIgnoreCase(mode)) {
            setFieldsEditable(false);
        }
        LinearLayout degreeContainer = findViewById(R.id.degree_container);
        if ("原料".equals(type)) {
            degreeContainer.setVisibility(View.VISIBLE);
        } else {
            degreeContainer.setVisibility(View.GONE);
        }
        LinearLayout confirm_container = findViewById(R.id.confirm_container);
        if ("confirm".equals(staff)){
            confirm_container.setVisibility(View.VISIBLE);
        }else {
            confirm_container.setVisibility(View.GONE);
        }
        String importId = intent.getStringExtra("importId");
        String date = intent.getStringExtra("date");
        String vendor = intent.getStringExtra("vendor");
        String itemName = intent.getStringExtra("itemName");
        String amount = intent.getStringExtra("amount");
        String spec = intent.getStringExtra("spec");
        String packageConfirm = intent.getStringExtra("packageConfirm");
        String vector = intent.getStringExtra("vector");
        String packageLabel = intent.getStringExtra("packageLabel");
        String validDate = intent.getStringExtra("validDate");
        String pallet = intent.getStringExtra("pallet");
        String COA = intent.getStringExtra("COA");
        String inspector = intent.getStringExtra("inspector");
        String confirmed = intent.getStringExtra("confirmed");
        String note = intent.getStringExtra("note");
        String place = intent.getStringExtra("place");
        if (importId != null) {
            ConnectDB.getImage(importId, imageNames -> {
                if (imageNames == null) imageNames = new ArrayList<>();
                if (!imageNames.isEmpty()) {
                    dbImages.addAll(imageNames);
                    for (String url : imageNames) {
                        addThumb(url);
                    }
                }
            });
        }
        String odor = intent.getStringExtra("odor");
        String degree = intent.getStringExtra("degree");
        if (degree != null && !degree.isEmpty()) {
            String[] degreeParts = degree.split(" ");
            degreeType = degreeParts[0];
            degreeValue = degreeParts.length > 1 ? degreeParts[1].replace("℃", "") : "";
        } else {
            degreeType = "";
            degreeValue = "";
        }
        ((TextView) findViewById(R.id.vendorName_text)).setText(vendor);
        ((TextView) findViewById(R.id.date_text)).setText(date);
        ((TextView) findViewById(R.id.product_text)).setText(itemName);

        Spinner spinnerUnit = findViewById(R.id.spinnerUnit);
        DataSource.setupUnitSpinner(this, spinnerUnit, null);
        String raw = amount == null ? "" : amount.trim();
        String number = "";
        String unit = "";

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^(\\d+(?:\\.\\d+)?)(.+)$")
                .matcher(raw);
        if (m.find()) {
            number = m.group(1);
            unit = m.group(2).trim();
        }
        // 解析舊數量數字
        if (amount != null) {
            try {
                originalQty = Integer.parseInt(number);
            } catch (Exception e) {
                originalQty = 0;
            }
        }
        ((TextView) findViewById(R.id.quantity_input)).setText(number);

        if (!unit.isEmpty()) {
            int unitIndex = DataSource.getUnits().indexOf(unit);
            if (unitIndex >= 0) {
                spinnerUnit.setSelection(unitIndex);
            }
        }

        ((TextView) findViewById(R.id.place_text)).setText(place);
        if (validDate != null && !validDate.trim().isEmpty() && !validDate.equalsIgnoreCase("null")) {
            ((TextView) findViewById(R.id.date_select)).setText(validDate);
        }
        ((TextView) findViewById(R.id.spec_input)).setText(spec);
        ((TextView) findViewById(R.id.note_input)).setText(note);

        // 套用下拉選單
        setupSpinnerData(R.id.spinnerInspector, DataSource.getInspector(), inspector, "inspect");
        setupSpinnerData(R.id.spinnerConfirmPerson, DataSource.getConfirmPerson(), confirmed, "confirm");

        // 依據勾選狀態初始化 Checkboxes
        FlexboxLayout checkOptions = findViewById(R.id.check_options);
        List<Boolean> checkStates = new ArrayList<>();
        checkStates.add("1".equals(packageConfirm)); // 外包裝完整
        if("原料".equals(type)){
           checkStates.add("1".equals(odor));        //異味(原料)
        }
        checkStates.add("1".equals(vector));         // 病媒
        checkStates.add("1".equals(packageLabel));   // 包裝標示
        checkStates.add("1".equals(pallet));         // 棧板完整度
        checkStates.add("1".equals(COA));            // COA
        populateChecks(this, checkOptions, checkStates, type, !"view".equalsIgnoreCase(mode));

        TextView dateSelect = findViewById(R.id.date_select);
        ConstraintLayout dateSelectContainer = findViewById(R.id.date_select_container);
        dateSelectContainer.setOnClickListener(v -> showDatePickerDialog(dateSelect));

        findViewById(R.id.save_btn).setOnClickListener(v -> {
            String qtyNumberStr = ((TextView) findViewById(R.id.quantity_input)).getText().toString().trim();
            int newQty = 0;
            try {
                newQty = Integer.parseInt(qtyNumberStr);
            } catch (Exception e) {
                newQty = 0;
            }
            int diff = newQty - originalQty;
            if (diff != 0) {
                ConnectDB.adjustQuantity(place, type, vendor, itemName, diff, success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            Toast.makeText(this, "✅ 庫存更新成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "❌ 庫存不足，無法扣除", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }

            String specText = ((TextView) findViewById(R.id.spec_input)).getText().toString().trim();
            String validDateText = ((TextView) findViewById(R.id.date_select)).getText().toString().trim();
            String noteText = ((TextView) findViewById(R.id.note_input)).getText().toString().trim();

            if (validDateText.isEmpty()) {
                Toast.makeText(this, "請選擇有效日期", Toast.LENGTH_SHORT).show();
                return;
            }
            if (specText.isEmpty()) {
                Toast.makeText(this, "請填寫規格", Toast.LENGTH_SHORT).show();
                return;
            }

            String qtyNumber = ((TextView) findViewById(R.id.quantity_input)).getText().toString().trim();
            Spinner unitSpinner = findViewById(R.id.spinnerUnit);
            String updateUnit = unitSpinner.getSelectedItem() != null ? unitSpinner.getSelectedItem().toString() : "";

            if (qtyNumber.isEmpty() || !qtyNumber.matches("\\d+")) {
                Toast.makeText(this, "請填寫正確的數量（整數）", Toast.LENGTH_SHORT).show();
                return;
            }
            if (updateUnit.isEmpty()) {
                Toast.makeText(this, "請選擇單位", Toast.LENGTH_SHORT).show();
                return;
            }
            String amountCombined = qtyNumber + updateUnit;
            final String degreeTextFinal;
            if ("原料".equals(type)) {
                String selected = spinnerDegree.getSelectedItem().toString();
                String num = degreeInput.getText().toString().trim()
                        .replace("℃","").replace("°","");

                if ("常溫".equals(selected)) {
                    degreeTextFinal = "常溫";
                } else {
                    if (num.isEmpty()) {
                        Toast.makeText(this, "請填寫溫度數字", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    degreeTextFinal = selected + " " + num + "°C";
                }
            } else {
                degreeTextFinal = "";
            }
            // 取得 Spinner 值
            Spinner inspectorSpinner = findViewById(R.id.spinnerInspector);
            Spinner confirmSpinner = findViewById(R.id.spinnerConfirmPerson);
            String inspectorValue = inspectorSpinner.getSelectedItem().toString();
            String confirmValue = confirmSpinner.getSelectedItem().toString();
            if ("驗收人員".equals(inspectorValue)) {
                Toast.makeText(this, "請選擇驗收人員", Toast.LENGTH_SHORT).show();
                return;
            }
            if("confirm".equals(staff)){
                if ("確認人員".equals(confirmValue)) {
                    Toast.makeText(this, "請選擇確認人員", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            // 勾選項目（依照 type 判斷數量）
            List<Boolean> checkedStates = new ArrayList<>();
            for (int i = 0; i < checkOptions.getChildCount(); i++) {
                View itemView = checkOptions.getChildAt(i);
                ImageView toggle = itemView.findViewById(R.id.optionToggle);
                boolean checked = toggle.getTag() != null && (boolean) toggle.getTag();
                checkedStates.add(checked);
            }

            // 拆解勾選清單
            boolean isPackageChecked = checkedStates.size() > 0 && checkedStates.get(0);
            boolean isOdorChecked = checkedStates.size() > 1 && checkedStates.get(1);
            boolean isVectorChecked = checkedStates.size() > 2 && checkedStates.get(2);
            boolean isPackageLabelChecked = checkedStates.size() > 3 && checkedStates.get(3);
            boolean isPalletChecked = checkedStates.size() > 4 && checkedStates.get(4);
            boolean isCoaChecked = checkedStates.size() > 5 && checkedStates.get(5);

            int n = stagedAdds.size();
            ConnectDB.getImageSeq(importId, n, startSeq -> {
                if (startSeq <= 0) {
                    runOnUiThread(() -> Toast.makeText(this, "取得圖片序號失敗", Toast.LENGTH_SHORT).show());
                    return;
                }

                List<String> names = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    int seq = startSeq + i;
                    String name = importId + "_" + seq + ".jpg";
                    names.add(name);
                }

                uploadUris(stagedAdds, names, 5, uploadedUrls -> {
                    List<String> finalImages = new ArrayList<>(dbImages);
                    finalImages.removeAll(stagedDeletes);
                    for (String url : uploadedUrls) {
                        if (!finalImages.contains(url)) finalImages.add(url);
                    }

                    ConnectDB.updateInspectRecord(
                            importId,
                            amountCombined,
                            specText,
                            validDateText,
                            isPackageChecked,
                            isOdorChecked,
                            isVectorChecked,
                            degreeTextFinal,
                            isPackageLabelChecked,
                            isPalletChecked,
                            isCoaChecked,
                            noteText,
                            finalImages,
                            inspectorValue,
                            confirmValue,
                            success -> {
                                if (success) {
                                    if (!stagedDeletes.isEmpty()) ConnectDB.imageDelete(stagedDeletes);
                                    stagedAdds.clear();
                                    stagedDeletes.clear();
                                    Toast.makeText(this, "更新成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, "更新失敗", Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                });
            });
        });
        findViewById(R.id.cancel_btn).setOnClickListener(v -> {
            stagedAdds.clear();
            stagedDeletes.clear();
            Toast.makeText(this, "已取消變更", Toast.LENGTH_SHORT).show();
            finish();
        });
        CardView uploadContainer = findViewById(R.id.upload_container);
        if (!"view".equalsIgnoreCase(mode)) {
            uploadContainer.setOnClickListener(v -> {
                String[] options = {"相簿選擇", "相機拍照"};
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle("選擇圖片來源")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                                pickIntent.setType("image/*");
                                pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 多選
                                startActivityForResult(Intent.createChooser(pickIntent, "選擇圖片"), REQUEST_IMAGE_PICK);
                            } else {
                                // 相機
                                takePhoto();
                            }
                        })
                        .show();
            });
        } else {
            uploadContainer.setEnabled(false);
        }

        spinnerDegree = findViewById(R.id.spinnerDegree);

        List<String> degreeOptions = new ArrayList<>();
        degreeOptions.add("常溫");
        degreeOptions.add("冷凍");
        degreeOptions.add("冷藏");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, degreeOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDegree.setAdapter(adapter);

        int selectedDegreePosition = degreeOptions.indexOf(degreeType);
        if (selectedDegreePosition != -1) {
            spinnerDegree.setSelection(selectedDegreePosition);
        }

        degreeInput = findViewById(R.id.degree_input);
        if("常溫".equals(degreeType) || degreeType == null || degreeType.isEmpty()){
            degreeInput.setVisibility(View.GONE);
        }
        if (!degreeValue.isEmpty()) {
            degreeInput.setText(degreeValue);
        }
        // 设置 Spinner 选择监听器
        spinnerDegree.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedDegree = parentView.getItemAtPosition(position).toString();
                if ("冷凍".equals(selectedDegree) || "冷藏".equals(selectedDegree)) {
                    degreeInput.setVisibility(View.VISIBLE);
                } else {
                    degreeInput.setVisibility(View.GONE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                degreeInput.setVisibility(View.GONE);
            }
        });
    }
    private void showDatePickerDialog(TextView targetView) {
        Calendar calendar = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = year + "-" + String.format("%02d", month + 1) + "-" + String.format("%02d", dayOfMonth);
                    targetView.setText(selectedDate);
                    targetView.setTextColor(getResources().getColor(android.R.color.black));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private static final String[] Material_CHECK_ITEMS  = {
            "外包裝完整",  "無病媒", "包材標示", "棧板完整度", "COA"
    };

    private static final String[] Ingredient_CHECK_ITEMS = {
            "外包裝完整", "無異味", "無病媒", "過敏原標示確認", "棧板完整度", "COA"
    };

    public static void populateChecks(Context context, FlexboxLayout container, List<Boolean> checkedStates, String type, boolean editable) {
        container.removeAllViews();
        String[] check_list;
        check_list = "原料".equals(type) ? Ingredient_CHECK_ITEMS : Material_CHECK_ITEMS;

        for (int i = 0; i < check_list.length; i++) {
            String label = check_list[i];
            boolean isChecked = i < checkedStates.size() && checkedStates.get(i);

            View itemView = View.inflate(context, R.layout.check_option_item, null);
            TextView optionText = itemView.findViewById(R.id.optionText);
            ImageView optionToggle = itemView.findViewById(R.id.optionToggle);

            optionText.setText(label);
            optionToggle.setImageResource(isChecked ? R.drawable.circle_checked : R.drawable.circle_unchecked_white);
            optionToggle.setTag(isChecked);

            if (editable) {
                optionToggle.setOnClickListener(v -> {
                    boolean nowChecked = optionToggle.getTag() != null && (boolean) optionToggle.getTag();
                    boolean newChecked = !nowChecked;
                    optionToggle.setImageResource(newChecked ? R.drawable.circle_checked : R.drawable.circle_unchecked_white);
                    optionToggle.setTag(newChecked);
                });
            } else {
                optionToggle.setEnabled(false);
            }

            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 8, 8, 8);
            itemView.setLayoutParams(params);

            container.addView(itemView);
        }
    }
    private void setupSpinnerData(int spinnerId, List<Map<String, String>> data, String selectedIdOrName, String type) {
        Spinner spinner = findViewById(spinnerId);
        setupSpinnerAdapter(spinner, data, selectedIdOrName, type);
    }

    private void setupSpinnerAdapter(Spinner spinner, List<Map<String, String>> data, String selectedIdOrName, String type) {
        List<String> display = new ArrayList<>();
        display.add("inspect".equals(type) ? "驗收人員" : "確認人員");

        List<String> ids = new ArrayList<>();
        ids.add(null);

        for (Map<String, String> emp : data) {
            display.add(emp.get("name"));
            ids.add(emp.get("id"));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, display);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setTag(R.id.tag_spinner_ids, ids);

        int pos = findPositionByIdOrName(data, selectedIdOrName);
        if (pos >= 0) spinner.setSelection(pos + 1);
    }

    private int findPositionByIdOrName(List<Map<String, String>> data, String target) {
        if (target == null) return -1;
        String t = target.trim();
        if (t.isEmpty()) return -1;

        // 先比 id
        for (int i = 0; i < data.size(); i++) {
            if (t.equals(data.get(i).get("id"))) return i;
        }
        // 再比 name
        for (int i = 0; i < data.size(); i++) {
            if (t.equals(data.get(i).get("name"))) return i;
        }
        return -1;
    }

    private void setFieldsEditable(boolean editable) {
        // 欄位元件
        findViewById(R.id.spinnerDegree).setEnabled(editable);
        findViewById(R.id.spec_input).setEnabled(editable);
        findViewById(R.id.degree_input).setEnabled(editable);
        findViewById(R.id.note_input).setEnabled(editable);
        findViewById(R.id.quantity_input).setEnabled(editable);
        findViewById(R.id.spinnerUnit).setEnabled(editable);

        // 日期欄位禁用點擊事件
        ConstraintLayout dateSelectContainer = findViewById(R.id.date_select_container);
        if (!editable) {
            dateSelectContainer.setOnClickListener(null);
        }

        // 下拉選單
        findViewById(R.id.spinnerInspector).setEnabled(editable);
        findViewById(R.id.spinnerConfirmPerson).setEnabled(editable);

        // Checkbox toggle 不可點
        FlexboxLayout checkOptions = findViewById(R.id.check_options);
        for (int i = 0; i < checkOptions.getChildCount(); i++) {
            View itemView = checkOptions.getChildAt(i);
            ImageView toggle = itemView.findViewById(R.id.optionToggle);
            toggle.setEnabled(editable);
        }

        // 隱藏按鈕（儲存 / 取消）
        if (!editable) {
            findViewById(R.id.save_btn).setVisibility(View.GONE);
            findViewById(R.id.cancel_btn).setVisibility(View.GONE);
        }
    }

    private void takePhoto() {
        String fileName = System.currentTimeMillis() + ".jpg";
        File photoFile = new File(getExternalFilesDir(null), fileName);

        photoUri = androidx.core.content.FileProvider.getUriForFile(
                this, "com.example.deliverysystem.fileprovider", photoFile
        );

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQUEST_IMAGE_PICK && data != null) {
            if (data.getClipData() != null) {
                ClipData clip = data.getClipData();
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri uri = clip.getItemAt(i).getUri();
                    stagedAdds.add(uri);       // 只加入暫存
                    addThumb(uri);             // 顯示縮圖（source = Uri）
                }
            } else if (data.getData() != null) {
                Uri uri = data.getData();
                stagedAdds.add(uri);
                addThumb(uri);
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && photoUri != null) {
            stagedAdds.add(photoUri);
            addThumb(photoUri);
        }
    }

    private ImageView addThumb(Object source) {
        if (imagesContainer == null) return null;

        ImageView iv = new ImageView(this);
        int size = (int) (getResources().getDisplayMetrics().density * 80);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.setMargins(8,8,8,8);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Glide.with(this).load(source).into(iv);
        iv.setOnClickListener(v -> previewImage(source));

        String mode = getIntent().getStringExtra("mode");
        if (!"view".equalsIgnoreCase(mode)) {
            iv.setOnLongClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setMessage("移除此圖片？")
                        .setPositiveButton("移除", (d, w) -> {
                            imagesContainer.removeView(iv);
                            if (source instanceof String) {
                                String url = (String) source;
                                dbImages.remove(url);
                                if (!stagedDeletes.contains(url)) stagedDeletes.add(url);
                            } else if (source instanceof Uri) {
                                stagedAdds.remove((Uri) source);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                return true;
            });
        }

        imagesContainer.addView(iv);
        imagesContainer.setVisibility(View.VISIBLE);
        return iv;
    }
    private interface OnUrlsUploaded {
        void onComplete(List<String> urls);
    }

    private void uploadUris(
            List<Uri> uris,
            List<String> names,
            int maxConcurrent,
            OnUrlsUploaded cb
    ) {
        if (uris == null || uris.isEmpty()) {
            cb.onComplete(new ArrayList<>());
            return;
        }
        if (names == null || names.size() != uris.size()) {
            Toast.makeText(this, "圖片命名數量不一致", Toast.LENGTH_SHORT).show();
            cb.onComplete(new ArrayList<>());
            return;
        }

        final int n = uris.size();
        final int limit = Math.min(maxConcurrent, n);

        final String[] paths = new String[n];

        final java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(limit);

        final java.util.concurrent.atomic.AtomicInteger nextIndex = new java.util.concurrent.atomic.AtomicInteger(0);
        final java.util.concurrent.atomic.AtomicInteger finished = new java.util.concurrent.atomic.AtomicInteger(0);

        Runnable worker = new Runnable() {
            @Override
            public void run() {
                int i = nextIndex.getAndIncrement();
                if (i >= n) return;

                Uri uri = uris.get(i);
                String name = names.get(i);

                Uri cacheUri;
                try {
                    cacheUri = compressToCache(uri, 1600, 80); // maxEdge / quality 你可調
                } catch (Exception e) {
                    e.printStackTrace();
                    cacheUri = uri;
                }

                ConnectDB.uploadImageToFirebase(InspectDetail.this, cacheUri, name, storagePath -> {
                    paths[i] = storagePath;

                    int done = finished.incrementAndGet();
                    if (done >= n) {
                        pool.shutdown();

                        List<String> result = new ArrayList<>();
                        for (String p : paths) {
                            if (p != null) result.add(p);
                        }
                        cb.onComplete(result);
                    } else {
                        // 同一 worker 繼續拿下一張
                        pool.execute(this);
                    }
                });
            }
        };

        for (int k = 0; k < limit; k++) {
            pool.execute(worker);
        }
    }

    private void previewImage(Object source) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        PhotoView photoView = new PhotoView(this);
        photoView.setBackgroundColor(Color.BLACK);
        photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        Glide.with(this).load(source).into(photoView);
        photoView.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(photoView);
        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.show();
    }

    private Uri compressToCache(Uri srcUri, int maxEdge, int jpegQuality) throws IOException {
        // 1) 讀取原圖尺寸
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        try (InputStream is = getContentResolver().openInputStream(srcUri)) {
            BitmapFactory.decodeStream(is, null, opts);
        }

        int srcW = opts.outWidth;
        int srcH = opts.outHeight;

        // 2) 計算縮放倍率（inSampleSize）
        int inSample = 1;
        while ((srcW / inSample) > maxEdge || (srcH / inSample) > maxEdge) {
            inSample *= 2;
        }

        // 3) 真正解碼
        BitmapFactory.Options opts2 = new BitmapFactory.Options();
        opts2.inSampleSize = inSample;
        opts2.inPreferredConfig = Bitmap.Config.RGB_565; // 省記憶體
        Bitmap bmp;
        try (InputStream is2 = getContentResolver().openInputStream(srcUri)) {
            bmp = BitmapFactory.decodeStream(is2, null, opts2);
        }
        if (bmp == null) throw new IOException("decode bitmap failed");

        // 4) 再做精準縮放（讓長邊 <= maxEdge）
        int w = bmp.getWidth(), h = bmp.getHeight();
        float scale = Math.min(1f, maxEdge / (float) Math.max(w, h));
        if (scale < 1f) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            Bitmap scaled = Bitmap.createBitmap(bmp, 0, 0, w, h, m, true);
            bmp.recycle();
            bmp = scaled;
        }

        // 5) 寫到 cache 檔
        File outFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            bmp.compress(Bitmap.CompressFormat.JPEG, jpegQuality, fos);
        }
        bmp.recycle();

        // 6) 用 FileProvider 轉成 content Uri
        return androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.example.deliverysystem.fileprovider",
                outFile
        );
    }
}