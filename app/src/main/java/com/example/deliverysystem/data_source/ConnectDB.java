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
import com.example.deliverysystem.setting_system.SettingEmployee;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
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
    private static ListenerRegistration inspectListener;
    private static DocumentSnapshot lastDoc;

    // ===================== 進貨 =====================
    /** 進貨紀錄 */
    public static void getImportRecords(String vendorName, Consumer<List<ImportRecord>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<ImportRecord> records = new ArrayList<>();

        db.collection("import_records")
                .whereEqualTo("vendor", vendorName)
                .orderBy("import_date", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        try {
                            String docId = doc.getId();
                            String importId = doc.getString("import_id");
                            String importDate = doc.getString("import_date");
                            String vendor = doc.getString("vendor");
                            String product = doc.getString("product");
                            String quantity = doc.getString("quantity");
                            String place = doc.getString("place");
                            String type = doc.getString("type");

                            ImportRecord record = new ImportRecord(
                                    docId,
                                    importId,
                                    importDate,
                                    vendor,
                                    product,
                                    quantity,
                                    place,
                                    type
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
                    callback.accept(new ArrayList<>());
                });
    }

    /** 新增進貨 */
    public static void addImportRecord(String type, String date, String vendor,
                                       List<Map<String, Object>> products,
                                       Context context, Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String today = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        DocumentReference counterRef = db.collection("import_counters").document(today);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(counterRef);
            int lastSeq = 0;
            if (snapshot.exists()) {
                Long lastSeqLong = snapshot.getLong("lastSeq");
                if (lastSeqLong != null) {
                    lastSeq = lastSeqLong.intValue();
                }
            }
            for (int i = 0; i < products.size(); i++) {
                lastSeq++;
                String importId = today + String.format("%02d", lastSeq);
                Map<String, Object> productData = products.get(i);
                Map<String, Object> importData = new HashMap<>();
                importData.put("import_id", importId);
                importData.put("type", type);
                importData.put("import_date", date);
                importData.put("vendor", vendor);
                importData.put("product", productData.get("product"));
                importData.put("quantity", productData.get("quantity"));
                importData.put("place", productData.get("place"));
                DocumentReference recordRef = db.collection("import_records").document(importId);
                transaction.set(recordRef, importData);
            }
            transaction.set(counterRef, Collections.singletonMap("lastSeq", lastSeq));
            return null;
        }).addOnSuccessListener(aVoid -> {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
        }).addOnFailureListener(e -> {
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
        });
    }

    /** 刪除進貨 */
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

    // ===================== 驗收 =====================
    /** 更新驗收 */
    public static void updateInspectRecord(String importId, String amountCombined, String spec, String validDate,
                                           boolean packageComplete, boolean odorCheck, boolean vector, String degree,
                                           boolean packageLabel, boolean pallet, boolean coa,
                                           String note, List<String> imageFileName, String inspector, String confirmer,
                                           Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
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
        db.collection("import_records")
                .whereEqualTo("import_id", importId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
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

    /** 篩選驗收紀錄 */
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
        }else {
            query = query.orderBy("import_date", Query.Direction.DESCENDING);
        }
        query = query.limit(100);
        query.get().addOnSuccessListener(querySnapshot -> {
            List<InspectRecord> records = new ArrayList<>();
            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                try {
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
    }

    /** 上傳照片 */
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

    /** 取得照片序號 */
    public interface OnSeqRangeReserved {
        void onComplete(int startSeq);
    }
    public static void getImageSeq(String importId, int count, OnSeqRangeReserved cb) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference ref = db.collection("inspect").document(importId)
                .collection("meta").document("counter");

        db.runTransaction(transaction -> {
                    DocumentSnapshot snap = transaction.get(ref);
                    long next = 1;
                    if (snap.exists() && snap.contains("nextImageSeq")) {
                        Long v = snap.getLong("nextImageSeq");
                        if (v != null) next = v;
                    }
                    // 這次保留 [next, next+count-1]
                    long finalNext = next;
                    transaction.set(ref, new HashMap<String, Object>() {{
                        put("nextImageSeq", finalNext + count);
                    }}, SetOptions.merge());
                    return (int) next;
                }).addOnSuccessListener(startSeq -> cb.onComplete(startSeq))
                .addOnFailureListener(e -> cb.onComplete(-1));
    }

    /** 刪除照片 */
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

    /** 轉換文字 */
    public static String changeFormat(Map<String, Object> record, String key) {
        if (!record.containsKey(key) || record.get(key) == null) {
            return "缺";
        }
        Map<String, String> mapping = new HashMap<>();
        mapping.put("0", "無");
        mapping.put("1", "有");
        return mapping.getOrDefault(String.valueOf(record.get(key)).trim(), "缺");
    }

    /** 取得照片 */
    public static void getImage(String importId, Consumer<List<String>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("import_records")
                .whereEqualTo("import_id", importId)
                .limit(1) // 只會有一筆
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> imageNames = new ArrayList<>();

                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Object imgObj = doc.get("image_name");

                        if (imgObj instanceof List<?>) {
                            for (Object o : (List<?>) imgObj) {
                                if (o != null) {
                                    String val = String.valueOf(o).trim();
                                    if (!val.isEmpty()) imageNames.add(val);
                                }
                            }
                        } else if (imgObj instanceof String) {
                            String s = ((String) imgObj).trim();
                            if (!s.isEmpty()) imageNames.add(s);
                        }
                    }

                    callback.accept(imageNames != null ? imageNames : new ArrayList<>());
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(new ArrayList<>());
                });
    }

    /** 取得驗收紀錄 */
    public static void loadInspectRecords(
            String type,
            String placeValue,
            boolean listenMode,
            Consumer<List<InspectRecord>> callback
    ) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (listenMode) {

            // 清除舊監聽避免重複
            if (inspectListener != null) inspectListener.remove();

            lastDoc = null;

            inspectListener = db.collection("import_records")
                    .whereEqualTo("type", type)
                    .whereEqualTo("place", placeValue)
                    .orderBy("import_date", Query.Direction.DESCENDING)
                    .limit(20)
                    .addSnapshotListener((snapshots, e) -> {
                        if (e != null || snapshots == null) {
                            e.printStackTrace();
                            callback.accept(new ArrayList<>());
                            return;
                        }

                        List<InspectRecord> records = parseDocs(snapshots);

                        if (!snapshots.isEmpty()) {
                            lastDoc = snapshots.getDocuments()
                                    .get(snapshots.size() - 1);
                        }

                        callback.accept(records);
                    });

            return;
        }

        // listenMode = false → Load More（使用一次性 get()）
        if (lastDoc == null) {
            callback.accept(new ArrayList<>());
            return;
        }

        db.collection("import_records")
                .whereEqualTo("type", type)
                .whereEqualTo("place", placeValue)
                .orderBy("import_date", Query.Direction.DESCENDING)
                .startAfter(lastDoc)
                .limit(20)
                .get()
                .addOnSuccessListener(q -> {
                    List<InspectRecord> records = parseDocs(q);
                    if (!q.isEmpty()) {
                        lastDoc = q.getDocuments().get(q.size() - 1);
                    }
                    callback.accept(records);
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(new ArrayList<>());
                });
    }

    /**
     * 將 DocumentSnapshot -> InspectRecord
     */
    private static List<InspectRecord> parseDocs(QuerySnapshot querySnapshot) {
        List<InspectRecord> records = new ArrayList<>();
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
            String place           = doc.getString("place");
            String inspectorStaff  = doc.getString("inspector_staff");
            String confirmStaff    = doc.getString("confirm_staff");

            InspectRecord record;
            if ("原料".equals(doc.getString("type"))) {
                String odor   = doc.getString("odor");
                String degree = doc.getString("degree");
                record = new InspectRecord(
                        importId, importDate, vendor, product, spec,
                        packageComplete, vectorComplete, packageLabel,
                        quantity, validDate, palletComplete, coa, note, place,
                        inspectorStaff, confirmStaff, odor, degree
                );
            } else {
                record = new InspectRecord(
                        importId, importDate, vendor, product, spec,
                        packageComplete, vectorComplete, packageLabel,
                        quantity, validDate, palletComplete, coa, note, place,
                        inspectorStaff, confirmStaff, "", ""
                );
            }
            records.add(record);
        }
        return records;
    }

    // ===================== 庫存 =====================
    /** 新增庫存 */
    public static void addStorage(String place, String type, String vendorName,
                                  List<Map<String, Object>> products,
                                  Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        // 🔑 先確保 vendor document 存在
        DocumentReference vendorRef = db.collection("storage")
                .document(place)
                .collection(type)
                .document(vendorName);

        batch.set(vendorRef, new HashMap<String, Object>() {{
            put("vendorName", vendorName);  // 只是一個 marker，避免成為空文件
            put("updatedAt", System.currentTimeMillis());
        }}, SetOptions.merge());

        // 🔁 寫入各個產品數量
        for (Map<String, Object> productData : products) {
            String product = (String) productData.get("product");
            int amount = 0;
            try {
                String quantity = (String) productData.get("quantity");
                amount = Integer.parseInt(quantity.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                Log.e("Firestore", "數量解析失敗: " + productData.get("quantity"), e);
                continue;
            }

            DocumentReference docRef = vendorRef
                    .collection("products")
                    .document(product);

            int finalAmount = amount;
            batch.set(docRef, new HashMap<String, Object>() {{
                put("amount", FieldValue.increment(finalAmount));  // ✅ 數量加總
            }}, SetOptions.merge());

            Log.d("Firestore", "📌 正在寫入: " + docRef.getPath() + " , amount=" + finalAmount);
        }

        batch.commit()
                .addOnSuccessListener(a -> {
                    Log.d("Firestore", "✅ addStorage 完成 place=" + place + ", type=" + type + ", vendor=" + vendorName);
                    callback.accept(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "❌ addStorage 失敗", e);
                    callback.accept(false);
                });
    }

    /** 取得庫存 */
    public static void getStorage(String place, String type, Consumer<List<Map<String, Object>>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("storage")
                .document(place)
                .collection(type)
                .get()
                .addOnSuccessListener(vendorSnapshots -> {
                    List<Map<String, Object>> resultList = new ArrayList<>();

                    if (vendorSnapshots.isEmpty()) {
                        callback.accept(resultList);
                        return;
                    }

                    int totalVendors = vendorSnapshots.size();
                    final int[] completed = {0};

                    for (DocumentSnapshot vendorDoc : vendorSnapshots) {
                        String vendorName = vendorDoc.getId();

                        vendorDoc.getReference().collection("products")
                                .get()
                                .addOnSuccessListener(productSnapshots -> {
                                    for (DocumentSnapshot productDoc : productSnapshots) {
                                        String productName = productDoc.getId();
                                        int amount = productDoc.contains("amount") && productDoc.get("amount") instanceof Number
                                                ? ((Number) productDoc.get("amount")).intValue()
                                                : 0;

                                        Map<String, Object> data = new HashMap<>();
                                        data.put("id", productDoc.getId());
                                        data.put("place", place);
                                        data.put("type", type);
                                        data.put("vendorName", vendorName);
                                        data.put("product", productName);
                                        data.put("amount", amount);
                                        resultList.add(data);
                                    }

                                    if (++completed[0] == totalVendors) {
                                        callback.accept(resultList);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (++completed[0] == totalVendors) {
                                        callback.accept(resultList);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    callback.accept(new ArrayList<>());
                });
    }

    /** 更新庫存 */
    public static void updateQuantity(String place,
                                      Map<String, Map<String, Map<String, Integer>>> changes,
                                      Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        for (Map.Entry<String, Map<String, Map<String, Integer>>> typeEntry : changes.entrySet()) {
            String type = typeEntry.getKey();
            for (Map.Entry<String, Map<String, Integer>> vendorEntry : typeEntry.getValue().entrySet()) {
                String vendorName = vendorEntry.getKey();
                for (Map.Entry<String, Integer> productEntry : vendorEntry.getValue().entrySet()) {
                    String productName = productEntry.getKey();
                    int qty = productEntry.getValue();

                    DocumentReference docRef = db.collection("storage")
                            .document(place)
                            .collection(type)
                            .document(vendorName)
                            .collection("products")
                            .document(productName);

                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("amount", qty);

                    batch.set(docRef, updateData, SetOptions.merge());

                    Log.d("Firestore", "📌 更新: " + place + "/" + type + "/" + vendorName + "/" + productName + " → " + qty);
                }
            }
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "✅ 批次更新完成 place=" + place);
                    callback.accept(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "❌ 批次更新失敗 place=" + place, e);
                    callback.accept(false);
                });
    }

    /** 調整庫存 */ //TODO
    public static void adjustQuantity(String place, String type, String vendorName, String productName, int diff, Consumer<Boolean> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("storage")
                .document(place)
                .collection(type)
                .document(vendorName)
                .collection("products")
                .document(productName);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            if (!snapshot.exists()) {
                throw new FirebaseFirestoreException(
                        "找不到此紀錄: " + productName,
                        FirebaseFirestoreException.Code.NOT_FOUND
                );
            }

            Long current = snapshot.getLong("amount");
            int currentAmount = (current != null) ? current.intValue() : 0;
            int newAmount = currentAmount + diff;

            if (newAmount < 0) {
                throw new FirebaseFirestoreException(
                        "數量不足，無法扣除",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            transaction.update(docRef, "amount", newAmount);
            return newAmount;
        }).addOnSuccessListener(newAmount -> {
            callback.accept(true);
        }).addOnFailureListener(e -> {
            callback.accept(false);
        });
    }

    // ===================== 設定 =====================
    /** 員工資料 */
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
                        String name = doc.getString("name");
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
                        DataSource.setConfirmPersons(list);
                    } else {
                        DataSource.setInspectors(list);
                    }

                    if (callback != null) callback.run();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "getEmployees 失敗：" + e.getMessage(), e);
                    if (callback != null) callback.run();
                });
    }

    /** 單位 */
    public static void getUnit(Consumer<List<String>> callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("unit")
                .document("unit") // 這裡是你的文件名稱（左邊看到 unit → unit）
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 取得所有欄位的值
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            List<String> units = new ArrayList<>();
                            for (Object value : data.values()) {
                                units.add(value.toString());
                            }
                            callback.accept(units); // 回傳結果
                        } else {
                            callback.accept(new ArrayList<>());
                        }
                    } else {
                        callback.accept(new ArrayList<>());
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.accept(new ArrayList<>());
                });
    }

    /** 廠商/產品 */
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
                    callback.accept(vendorMap);
                });
    }

    /** 密碼 */
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
                callback.accept(new ArrayList<>());
            });
    }

    /** 刪除產品 */
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

    /** 新增產品 */
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

    /** 新增廠商 */
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

    /** 刪除廠商 */
    public static void deleteVendor(Context context, String vendorName, final Callback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
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

    /** 更新密碼 */
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

    /** 更新員工權限 */
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

    /** 刪除員工 */
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

    /** 員工資料 */
    private static void fetchUpdatedLists(FirebaseFirestore db,
                                          Context context,
                                          EmployeeUpdateCallback callback) {
        db.collection("employees").get()
            .addOnSuccessListener(allDocs -> {
                List<Map<String, String>> confirmList = new ArrayList<>();
                List<Map<String, String>> inspectorList = new ArrayList<>();

                for (DocumentSnapshot doc : allDocs) {
                    String id = doc.getId();
                    String name = doc.getString("name");
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

    /** 新增員工 */
    public static void addEmployee(String name, Context context, EmployeeUpdateCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference employeeRef = db.collection("employees");

        // 建立要新增的資料
        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("employee_authority", false);
        // 新增文件（自動產生 ID）
        employeeRef.add(data)
                .addOnSuccessListener(documentReference -> {
                    fetchEmployeeLists(db, callback, true, "新增成功");
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    callback.onResult(false, "新增失敗：" + e.getMessage(), new ArrayList<>(), new ArrayList<>());
                });
    }
    /** 員工資料 */
    private static void fetchEmployeeLists(FirebaseFirestore db, EmployeeUpdateCallback callback, boolean success, String message) {

        db.collection("employees").get()
            .addOnSuccessListener(querySnapshot -> {
                List<Map<String, String>> confirmList = new ArrayList<>();
                List<Map<String, String>> inspectorList = new ArrayList<>();

                for (QueryDocumentSnapshot doc : querySnapshot) {
                    String empId = doc.getId();
                    String empName = doc.getString("name");
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

    public interface ExportCallback {
        void onResult(boolean success, String message);
    }
    /** 匯出 */
    public static void exportDataToExcel(Context context, String startDate, String endDate, String vendor, String place, ExportCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date start, end;

        try {
            start = sdf.parse(startDate);
            end = sdf.parse(endDate);
            if (start != null && end != null && start.after(end)) {
                callback.onResult(false, "開始日期不能晚於結束日期");
                return;
            }
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
                    String docVendor = doc.getString("vendor");
                    String docPlace = doc.getString("place");
                    try {
                        Date docDate = sdf.parse(dateStr);
                        if (docDate != null && !docDate.before(start) && !docDate.after(end)) {
                            boolean vendorOk = (vendor == null || vendor.isEmpty() || vendor.equals(docVendor));
                            boolean placeOk  = (place == null || place.isEmpty() || place.equals(docPlace));

                            if (vendorOk && placeOk) {
                                filteredList.add(doc.getData());
                            }
                        }
                    } catch (ParseException ignored) {}
                }

                try {
                    Workbook workbook = new XSSFWorkbook();
                    Sheet sheet = workbook.createSheet("資料");

                    String[] columns = {"型態", "進貨日期", "進貨地點","廠商", "品項", "進貨數量", "規格", "外包裝完整", "無異味", "無病媒", "溫度°C", "包材標示", "有效日期批號", "棧板", "COA", "備註", "進貨地點", "驗收人員", "確認人員"};

                    Row header = sheet.createRow(0);
                    for (int i = 0; i < columns.length; i++) {
                        header.createCell(i).setCellValue(columns[i]);
                    }

                    int rowNum = 1;
                    for (Map<String, Object> record : filteredList) {
                        Row row = sheet.createRow(rowNum++);
                        row.createCell(0).setCellValue(String.valueOf(record.getOrDefault("type", "")));
                        row.createCell(1).setCellValue(String.valueOf(record.getOrDefault("import_date", "")));
                        row.createCell(2).setCellValue(String.valueOf(record.getOrDefault("place", "")));
                        row.createCell(3).setCellValue(String.valueOf(record.getOrDefault("vendor", "")));
                        row.createCell(4).setCellValue(String.valueOf(record.getOrDefault("product", "")));
                        row.createCell(5).setCellValue(String.valueOf(record.getOrDefault("quantity", "")));
                        row.createCell(6).setCellValue(String.valueOf(record.getOrDefault("spec", "")));
                        row.createCell(7).setCellValue(changeFormat(record, "package_complete"));
                        row.createCell(8).setCellValue(changeFormat(record, "odor"));
                        row.createCell(9).setCellValue(changeFormat(record, "vector_complete"));
                        row.createCell(10).setCellValue(String.valueOf(record.getOrDefault("degree", "")));
                        row.createCell(11).setCellValue(changeFormat(record, "package_label"));
                        row.createCell(12).setCellValue(String.valueOf(record.getOrDefault("validDate", "")));
                        row.createCell(13).setCellValue(changeFormat(record, "pallet_complete"));
                        row.createCell(14).setCellValue(changeFormat(record, "coa"));
                        row.createCell(15).setCellValue(String.valueOf(record.getOrDefault("note", "")));
                        row.createCell(16).setCellValue(String.valueOf(record.getOrDefault("place", "")));
                        row.createCell(17).setCellValue(String.valueOf(record.getOrDefault("inspector_staff", "")));
                        row.createCell(18).setCellValue(String.valueOf(record.getOrDefault("confirm_staff", "")));
                    }

                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs();
                    }

                    String fileName;
                    if (vendor == null || vendor.isEmpty()) {
                        fileName = startDate + " ~ " + endDate + ".xlsx";
                    } else {
                        fileName = vendor + "_" + startDate + " ~ " + endDate + ".xlsx";
                    }
                    if (place != null && !place.isEmpty()) {
                        fileName = place + "_" + fileName; // 前面加地點
                    }

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
