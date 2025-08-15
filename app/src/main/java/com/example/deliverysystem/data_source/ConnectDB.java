package com.example.deliverysystem.data_source;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.widget.Toast;

import com.example.deliverysystem.import_system.ImportRecord;
import com.example.deliverysystem.inspect_system.InspectRecord;
import com.example.deliverysystem.inspect_system.InspectTable;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;

public class ConnectDB {

    // === 員工資料：inspector / confirmPerson ===
    public static void getEmployees(String type, Runnable callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        boolean isConfirm = "confirmPerson".equals(type);

        db.collection("employees")
                .whereEqualTo("employee_authority", isConfirm)
                .get()
                .addOnSuccessListener(qs -> {
                    List<Map<String, String>> list = new ArrayList<>();
                    for (DocumentSnapshot doc : qs) {
                        String id   = doc.getId();
                        String name = doc.getString("employee_name");
                        if (name == null) continue;
                        name = name.trim();
                        if (name.isEmpty()) continue;

                        Map<String, String> m = new HashMap<>();
                        m.put("id", id);
                        m.put("name", name);
                        list.add(m);
                    }

                    // 依姓名排序（可省略）
                    list.sort(Comparator.comparing(m -> m.getOrDefault("name", "")));

                    if (isConfirm) {
                        DataSource.setConfirmPersons(list);   // List<Map<String,String>>
                    } else {
                        DataSource.setInspectors(list);       // List<Map<String,String>>
                    }

                    if (callback != null) callback.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "getEmployees 失敗：" + e.getMessage(), e);
                    if (callback != null) callback.run();
                });
    }

    // === 廠商與產品對應資料 ===
    public static void getVendorProductData(Consumer<Map<String, VendorInfo>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, VendorInfo> vendorMap = new LinkedHashMap<>();

        db.collection("vendors")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String vendor = doc.getId(); // Document ID 當作 vendor 名
                            String industry = doc.getString("industry");
                            String type = doc.getString("type");

                            List<String> productList = (List<String>) doc.get("products");
                            if (productList == null) productList = new ArrayList<>();

                            VendorInfo info = new VendorInfo(industry, type, productList);
                            vendorMap.put(vendor, info);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    callback.accept(vendorMap);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "讀取 vendors 失敗：" + e.getMessage(), e);
                    callback.accept(vendorMap); // 回傳空資料避免卡住
                });
    }

    public static void getPasswords(Consumer<List<String>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("authority_password")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> passwordList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String password = doc.getString("password");
                        if (password != null) {
                            passwordList.add(password);
                        }
                    }
                    callback.accept(passwordList);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(new ArrayList<>()); // 出錯時回傳空陣列避免卡住流程
                });
    }

    // === 匯入資料紀錄 ===
    public static void getInspectRecords(String type, String placeValue, Consumer<List<InspectRecord>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<InspectRecord> records = new ArrayList<>();

        db.collection("import_records")
                .whereEqualTo("type", type)
                .whereEqualTo("place", placeValue)
                .orderBy("import_date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        String importId        = doc.getString("import_id");
                        String importDate      = doc.getString("import_date");
                        String vendor          = doc.getString("vendor");
                        String product         = doc.getString("product");
                        String spec            = doc.getString("spec");
                        String packageComplete = doc.getString("package_complete");
                        String vectorComplete  = doc.getString("vector_complete");
                        String packageLabel    = doc.getString("package_label");
                        String quantity        = doc.getString("quantity");
                        String validDate       = doc.getString("validDate");
                        String palletComplete  = doc.getString("pallet_complete");
                        String coa             = doc.getString("coa");
                        String note            = doc.getString("note");
                        String place            = doc.getString("place");
//                        String picture         = doc.getString("image_name");
                        Object imgObj = doc.get("image_name");
                        List<String> imageNames = new ArrayList<>();
                        if (imgObj instanceof List<?>) {
                            for (Object o : (List<?>) imgObj) {
                                if (o != null) imageNames.add(String.valueOf(o));
                            }
                        } else if (imgObj instanceof String) {
                            String s = (String) imgObj;
                            if (!s.trim().isEmpty()) imageNames.add(s);
                        }
                        String inspectorStaff  = doc.getString("inspector_staff");
                        String confirmStaff    = doc.getString("confirm_staff");

                        InspectRecord record;
                        if ("原料".equals(type)) {
                            String odor   = doc.getString("odor");
                            String degree = doc.getString("degree");
                            record = new InspectRecord(
                                    importId, importDate, vendor, product, spec,
                                    packageComplete, vectorComplete, packageLabel,
                                    quantity, validDate, palletComplete, coa, note, place, imageNames,
                                    inspectorStaff, confirmStaff, odor, degree
                            );
                        } else {
                            record = new InspectRecord(
                                    importId, importDate, vendor, product, spec,
                                    packageComplete, vectorComplete, packageLabel,
                                    quantity, validDate, palletComplete, coa, note, place, imageNames,
                                    inspectorStaff, confirmStaff, "", ""
                            );
                        }
                        records.add(record);
                    }
                    callback.accept(records);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(new ArrayList<>());
                });
    }

    public static void getImportRecords(String vendorName, Consumer<List<ImportRecord>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<ImportRecord> records = new ArrayList<>();

        db.collection("import_records")
                .whereEqualTo("vendor", vendorName)
                .orderBy("import_date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            ImportRecord record = new ImportRecord(
                                    doc.getId(),
                                    doc.getString("import_date"),
                                    doc.getString("vendor"),
                                    doc.getString("product"),
                                    doc.getString("quantity"),
                                    doc.getString("place")
                            );
                            records.add(record);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    callback.accept(records);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(new ArrayList<>()); // 回傳空列表表示錯誤
                });
    }

    public static void deleteImportRecordById(String importId, Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("import_records").document(importId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
                });
    }

    public static void deleteProduct(String vendorName, String productName, Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("vendors").document(vendorName);

        docRef.update("products", FieldValue.arrayRemove(productName))
                .addOnSuccessListener(aVoid -> {
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
                });
    }

    public static void addImportRecord(String type, String date, String vendor, String product, String quantity, String place,
                                       Context context, Consumer<Boolean> callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String importId = db.collection("import_records").document().getId();

        Map<String, Object> importData = new HashMap<>();
        importData.put("import_id", importId);
        importData.put("type", type);
        importData.put("import_date", date);
        importData.put("vendor", vendor);
        importData.put("product", product);
        importData.put("quantity", quantity);
        importData.put("place", place);

        db.collection("import_records").document(importId)
                .set(importData)
                .addOnSuccessListener(unused -> {
                    // 導頁到 InspectTable，傳入 type（需要更多欄位就再加 putExtra）
                    Intent intent = new Intent(context, InspectTable.class);
                    intent.putExtra("type", type);
                    // 若 context 不是 Activity，補上 FLAG
                    if (!(context instanceof Activity)) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(intent);

                    // 通知呼叫端成功（如果你還想保留 callback）
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
                });
    }

    public static void updateInspectRecord(String type, String importId, String amountCombined, String spec, String validDate,
                                           boolean packageComplete, boolean odorCheck, boolean vector, String degree,
                                           boolean packageLabel, boolean pallet, boolean coa,
                                           String note, List<String> imageFileName, String inspector, String confirmer,
                                           Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 要更新的欄位內容
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("quantity", amountCombined);
        updateData.put("spec", spec);
        updateData.put("validDate", validDate);
        updateData.put("package_complete", packageComplete ? "1" : "0");
        updateData.put("odor", odorCheck ? "1" : "0");
        updateData.put("vector_complete", vector ? "1" : "0");
        updateData.put("degree", degree);
        updateData.put("package_label", packageLabel ? "1" : "0");
        updateData.put("pallet_complete", pallet ? "1" : "0");
        updateData.put("coa", coa ? "1" : "0");
        updateData.put("note", note);
        updateData.put("image_name", imageFileName);
        updateData.put("inspector_staff", inspector);

        if (confirmer != null && !confirmer.trim().isEmpty() && !"確認人員".equals(confirmer)) {
            updateData.put("confirm_staff", confirmer);
        }

        // 根據 import_id 查找 document 並更新
        db.collection("import_records")
                .whereEqualTo("import_id", importId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // 假設 import_id 是唯一的，只更新第一筆找到的文件
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        doc.getReference().update(updateData)
                                .addOnSuccessListener(unused -> callback.accept(true))
                                .addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    callback.accept(false);
                                });
                    } else {
                        // 找不到該筆資料
                        callback.accept(false);
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(false);
                });
    }

    public static void getFilteredInspectRecords(
            String type, String vendor, String product,
            String inspector, String confirmer, String date, String place,
            Consumer<List<InspectRecord>> callback) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference ref = db.collection("import_records");

        Query query = ref;

        if (!type.isEmpty()) query = query.whereEqualTo("type", type);
        if (!vendor.isEmpty()) query = query.whereEqualTo("vendor", vendor);
        if (!product.isEmpty()) query = query.whereEqualTo("product", product);
        if (!place.isEmpty()) query = query.whereEqualTo("place", place);
        if (!inspector.isEmpty()) query = query.whereEqualTo("inspector_staff", inspector);
        if (!confirmer.isEmpty()) query = query.whereEqualTo("confirm_staff", confirmer);
        if (!date.isEmpty()) {
            query = query.whereEqualTo("import_date", date);
        }

        query.get().addOnSuccessListener(querySnapshot -> {
            List<InspectRecord> records = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                try {
                    // --- 兼容 image_name = List 或 String ---
                    Object imgObj = doc.get("image_name");
                    List<String> imageNames = new ArrayList<>();
                    if (imgObj instanceof List) {
                        //noinspection unchecked
                        imageNames.addAll((List<String>) imgObj);
                    } else if (imgObj instanceof String) {
                        String s = (String) imgObj;
                        if (s != null && !s.trim().isEmpty()) imageNames.add(s);
                    }

                    // 原料才有異味/溫度；物料給空字串即可
                    String odor   = "原料".equals(type) ? doc.getString("odor")   : "";
                    String degree = "原料".equals(type) ? doc.getString("degree") : "";

                    InspectRecord record = new InspectRecord(
                            doc.getString("import_id"),
                            doc.getString("import_date"),
                            doc.getString("vendor"),
                            doc.getString("product"),
                            doc.getString("spec"),
                            doc.getString("package_complete"),
                            doc.getString("vector_complete"),
                            doc.getString("package_label"),
                            doc.getString("quantity"),
                            doc.getString("validDate"),
                            doc.getString("pallet_complete"),
                            doc.getString("coa"),
                            doc.getString("note"),
                            doc.getString("place"),
//                            doc.getString("image_name"),
                            imageNames, // <-- 這裡改成 List<String>
                            doc.getString("inspector_staff"),
                            doc.getString("confirm_staff"),
                            odor,
                            degree
                    );

                    records.add(record);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            callback.accept(records);
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            callback.accept(new ArrayList<>());
        });
//            callback.accept(records);
//        }).addOnFailureListener(e -> {
//            e.printStackTrace();
//            callback.accept(new ArrayList<>());
//        });
    }

    // 發出 HTTP GET 請求到 PHP API，取得指定欄位的 distinct 值
    public static void getDistinctData(String collectionName, String fieldName, Consumer<String[]> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(collectionName)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Set<String> uniqueValues = new HashSet<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String value = doc.getString(fieldName);
                        if (value != null) {
                            uniqueValues.add(value);
                        }
                    }

                    String[] result = uniqueValues.toArray(new String[0]);
                    Log.d("Firestore_DATA", Arrays.toString(result));

                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(result));
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> callback.accept(new String[]{}));
                });
    }

    public static void addProduct(Context context, String vendor, List<String> products, Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("vendors").document(vendor);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            boolean[] hasSuccess = {false};

            // 保留原有欄位
            Map<String, Object> updates = new HashMap<>();
            List<String> existingProducts = new ArrayList<>();

            if (documentSnapshot.exists()) {
                // 已有廠商資料
                existingProducts = (List<String>) documentSnapshot.get("products");
                if (existingProducts == null) existingProducts = new ArrayList<>();

                for (String product : products) {
                    if (!existingProducts.contains(product)) {
                        existingProducts.add(product);
                        hasSuccess[0] = true;
                    }
                }

            } else {
                existingProducts.addAll(products);
                hasSuccess[0] = true;
            }
            updates.put("products", existingProducts);
            docRef.set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!hasSuccess[0]) {
                                Toast.makeText(context, "所有品項皆已存在，未新增任何項目", Toast.LENGTH_SHORT).show();
                            }
                            callback.accept(true);
                        });
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(context, "品項新增失敗", Toast.LENGTH_SHORT).show();
                            callback.accept(false);
                        });
                    });

        }).addOnFailureListener(e -> {
            e.printStackTrace();
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "讀取廠商資料失敗", Toast.LENGTH_SHORT).show();
                callback.accept(false);
            });
        });
    }


    public static void addVendorWithProducts(Context context, String name, String type, String industry, List<String> products, final Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference vendorRef = db.collection("vendors").document(name);

        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("industry", industry);
        data.put("products", products);

        vendorRef.set(data)
                .addOnSuccessListener(unused -> callback.onResult(true))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResult(false);
                });
    }

    public interface Callback {
        void onResult(boolean success);
    }
    public static void deleteVendor(Context context, String vendorName, final Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 該 vendor 的 document ID 就是 vendorName（如你的資料結構如此）
        db.collection("vendors").document(vendorName)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "已刪除廠商：" + vendorName, Toast.LENGTH_SHORT).show();
                    callback.onResult(true);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(context, "刪除失敗：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    callback.onResult(false);
                });
    }

    public interface UpdatePasswordCallback {
        void onResult(boolean success, String message);
    }

    public static void updatePassword(Context context, String newPassword, UpdatePasswordCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference colRef = db.collection("authority_password");

        colRef.get().addOnSuccessListener(querySnapshot -> {
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0); // 取第一筆文件
                DocumentReference docRef = doc.getReference();

                Map<String, Object> updateData = new HashMap<>();
                updateData.put("password", newPassword);

                docRef.set(updateData, SetOptions.merge())
                        .addOnSuccessListener(unused -> {
                            ((Activity) context).runOnUiThread(() -> callback.onResult(true, "密碼更新成功"));
                        })
                        .addOnFailureListener(e -> {
                            e.printStackTrace();
                            ((Activity) context).runOnUiThread(() -> callback.onResult(false, "密碼更新失敗: " + e.getMessage()));
                        });
            } else {
                callback.onResult(false, "未找到密碼文件");
            }
        });

    }
    public static void updateAuthorityEmployee(String id, boolean isChecked, Context context,  ConnectDB.EmployeeUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("employees").document(id)
                .update("employee_authority", isChecked)
                .addOnSuccessListener(unused -> fetchUpdatedLists(db, context, callback))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() ->
                            callback.onResult(false, "更新失敗：" + e.getMessage(), new ArrayList<>(), new ArrayList<>()));
                });
    }

    public static void deleteEmployee(String id, Context context,  ConnectDB.EmployeeUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("employees").document(id)
                .delete()
                .addOnSuccessListener(unused -> fetchUpdatedLists(db, context, callback))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() ->
                            callback.onResult(false, "刪除失敗：" + e.getMessage(), new ArrayList<>(), new ArrayList<>()));
                });
    }

    // 🔁 更新後重新抓取 confirmList / inspectorList
    private static void fetchUpdatedLists(FirebaseFirestore db,
                                          Context context,
                                          EmployeeUpdateCallback callback) {
        db.collection("employees").get()
                .addOnSuccessListener(allDocs -> {
                    List<Map<String, String>> confirmList = new ArrayList<>();
                    List<Map<String, String>> inspectorList = new ArrayList<>();

                    for (DocumentSnapshot doc : allDocs) {
                        String id = doc.getId();
                        String name = doc.getString("employee_name");
                        Boolean isConfirm = doc.getBoolean("employee_authority");
                        if (name == null || isConfirm == null) continue;
                        name = name.trim();
                        if (name.isEmpty()) continue;

                        Map<String, String> emp = new HashMap<>();
                        emp.put("id", id);
                        emp.put("name", name);

                        if (isConfirm) confirmList.add(emp);
                        else           inspectorList.add(emp);
                    }

                    Comparator<Map<String,String>> byName = Comparator.comparing(m -> m.getOrDefault("name",""));
                    confirmList.sort(byName);
                    inspectorList.sort(byName);

                    ((Activity) context).runOnUiThread(() ->
                            callback.onResult(true, "更新成功", confirmList, inspectorList)
                    );
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    ((Activity) context).runOnUiThread(() ->
                            callback.onResult(false, "取得名單失敗：" + e.getMessage(),
                                    new ArrayList<>(), new ArrayList<>())
                    );
                });
    }

    public static void addEmployee(String name, Context context, EmployeeUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference employeeRef = db.collection("employees");

        // 建立要新增的資料
        Map<String, Object> data = new HashMap<>();
        data.put("employee_name", name);
        data.put("employee_authority", false);
        // 新增文件（自動產生 ID）
        employeeRef.add(data)
                .addOnSuccessListener(documentReference -> {
                    // 成功新增員工後，讀取 confirm 與 inspector 清單
                    fetchEmployeeLists(db, callback, true, "新增成功");
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResult(false, "新增失敗：" + e.getMessage(), new ArrayList<>(), new ArrayList<>());
                });
    }
    private static void fetchEmployeeLists(FirebaseFirestore db,
                                           EmployeeUpdateCallback callback,
                                           boolean success,
                                           String message) {

        db.collection("employees").get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, String>> confirmList = new ArrayList<>();
                    List<Map<String, String>> inspectorList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String empId = doc.getId();
                        String empName = doc.getString("employee_name");
                        Boolean empAuthority = doc.getBoolean("employee_authority");
                        if (empName == null || empAuthority == null) continue;
                        empName = empName.trim();
                        if (empName.isEmpty()) continue;

                        Map<String, String> emp = new HashMap<>();
                        emp.put("id", empId);
                        emp.put("name", empName);

                        if (empAuthority) confirmList.add(emp);
                        else              inspectorList.add(emp);
                    }

                    // 可選：排序（依姓名）
                    Comparator<Map<String,String>> byName = Comparator.comparing(m -> m.getOrDefault("name",""));
                    confirmList.sort(byName);
                    inspectorList.sort(byName);

                    callback.onResult(true, message, confirmList, inspectorList);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResult(false, "讀取清單失敗：" + e.getMessage(),
                            new ArrayList<>(), new ArrayList<>());
                });
    }

    public interface EmployeeUpdateCallback {
        void onResult(boolean success, String message,
                      List<Map<String, String>> confirmList,
                      List<Map<String, String>> inspectorList);
    }
    public static void uploadImageToFirebase(Context context, Uri imageUri, String filename, Consumer<String> callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference().child("inspect_images/" + filename);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> callback.accept(uri.toString()))
                )
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(null);
                });
    }
    public static void getTodayImageFilename(Consumer<String> callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference ref = storage.getReference().child("inspect_images/");

        String todayPrefix = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        ref.listAll().addOnSuccessListener(listResult -> {
            int count = 0;
            for (StorageReference item : listResult.getItems()) {
                if (item.getName().startsWith(todayPrefix)) {
                    count++;
                }
            }
            String filename = todayPrefix + "_" + String.format("%02d", count + 1) + ".jpg";
            callback.accept(filename);
        }).addOnFailureListener(e -> {
            e.printStackTrace();
            callback.accept(null);
        });
    }

    public static void imageDelete(List<String> toBeDeletedImages) {
        FirebaseStorage storage = FirebaseStorage.getInstance();

        for (String url : toBeDeletedImages) {
            storage.getReferenceFromUrl(url)
                    .delete()
                    .addOnSuccessListener(aVoid ->
                            Log.d("FirebaseStorage", "已刪除：" + url)
                    )
                    .addOnFailureListener(e ->
                            Log.e("FirebaseStorage", "刪除失敗：" + url + "，原因：" + e.getMessage())
                    );
        }
        toBeDeletedImages.clear();
    }

    public interface ExportCallback {
        void onResult(boolean success, String message);
    }

    public static String changeFormat(Map<String, Object> record, String key) {
        if (!record.containsKey(key) || record.get(key) == null) {
            return "缺";
        }
        String value = String.valueOf(record.get(key)).trim();
        if ("0".equals(value)) return "無";
        if ("1".equals(value)) return "有";
        return value;
    }

    public static void exportDataToExcel(Context context, String startDate, String endDate, ExportCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Date start, end;
        try {
            start = sdf.parse(startDate);
            end = sdf.parse(endDate);
        } catch (ParseException e) {
            callback.onResult(false, "日期格式錯誤");
            return;
        }

        db.collection("import_records")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Map<String, Object>> filteredList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String dateStr = doc.getString("import_date");
                        try {
                            Date docDate = sdf.parse(dateStr);
                            if (docDate != null && !docDate.before(start) && !docDate.after(end)) {
                                filteredList.add(doc.getData());
                            }
                        } catch (ParseException ignored) {}
                    }

                    if (filteredList.isEmpty()) {
                        callback.onResult(false, "沒有符合日期的資料");
                        return;
                    }

                    try {
                        Workbook workbook = new XSSFWorkbook();
                        Sheet sheet = workbook.createSheet("資料");

                        String[] columns = {"型態", "進貨日期", "廠商", "品項", "進貨數量", "規格", "外包裝完整", "無異味", "無病媒", "溫度°C", "包材標示", "有效日期批號", "棧板", "COA", "備註", "進貨地點", "驗收人員", "確認人員"};

                        // 標題列
                        Row header = sheet.createRow(0);
                        for (int i = 0; i < columns.length; i++) {
                            header.createCell(i).setCellValue(columns[i]);
                        }

                        // 資料列
                        int rowNum = 1;
                        for (Map<String, Object> record : filteredList) {
                            Row row = sheet.createRow(rowNum++);
                            row.createCell(0).setCellValue(String.valueOf(record.getOrDefault("type", "")));
                            row.createCell(1).setCellValue(String.valueOf(record.getOrDefault("import_date", "")));
                            row.createCell(2).setCellValue(String.valueOf(record.getOrDefault("vendor", "")));
                            row.createCell(3).setCellValue(String.valueOf(record.getOrDefault("product", "")));
                            row.createCell(4).setCellValue(String.valueOf(record.getOrDefault("quantity", "")));
                            row.createCell(5).setCellValue(String.valueOf(record.getOrDefault("spec", "")));
                            row.createCell(6).setCellValue(changeFormat(record, "package_complete"));
                            row.createCell(7).setCellValue(changeFormat(record, "odor"));
                            row.createCell(8).setCellValue(changeFormat(record, "vector_complete"));
                            row.createCell(9).setCellValue(String.valueOf(record.getOrDefault("degree", "")));
                            row.createCell(10).setCellValue(changeFormat(record, "package_label"));
                            row.createCell(11).setCellValue(String.valueOf(record.getOrDefault("validDate", "")));
                            row.createCell(12).setCellValue(changeFormat(record, "pallet_complete"));
                            row.createCell(13).setCellValue(changeFormat(record, "coa"));
                            row.createCell(14).setCellValue(String.valueOf(record.getOrDefault("note", "")));
                            row.createCell(15).setCellValue(String.valueOf(record.getOrDefault("place", "")));
                            row.createCell(16).setCellValue(String.valueOf(record.getOrDefault("inspector_staff", "")));
                            row.createCell(17).setCellValue(String.valueOf(record.getOrDefault("confirm_staff", "")));
                        }

                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs();
                        }

                        String fileName = startDate + " ~ " + endDate + ".xlsx";
                        File file = new File(downloadsDir, fileName);
                        FileOutputStream fos = new FileOutputStream(file);
                        workbook.write(fos);
                        fos.close();
                        workbook.close();

                        callback.onResult(true, "匯出成功，檔案已儲存：" + file.getAbsolutePath());

                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onResult(false, "匯出失敗：" + e.getMessage());
                    }
                })
                .addOnFailureListener(e -> callback.onResult(false, "查詢失敗：" + e.getMessage()));
    }

}
