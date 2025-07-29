package com.example.deliverysystem.data_source;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.deliverysystem.import_system.ImportRecord;
import com.example.deliverysystem.inspect_system.InspectRecord;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Consumer;

public class ConnectDB {

    private static final String BASE_URL = "http://192.168.0.149/mei_hua_siang/";

    // === 公用工具：取得 JSON Array ===
    private static void fetchJsonArrayFromUrl(String urlStr, Consumer<JSONArray> callback) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONArray jsonArray = new JSONArray(result.toString());

                new Handler(Looper.getMainLooper()).post(() -> callback.accept(jsonArray));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // === 公用工具：取得 JSON Object ===
    private static void fetchJsonObjectFromUrl(String urlStr, Consumer<JSONObject> callback) {
        new Thread(() -> {
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                JSONObject jsonObject = new JSONObject(result.toString());
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(jsonObject));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // === 員工資料：inspector / confirmPerson ===
    public static void getEmployees(String type, Runnable callback) {
        String url = BASE_URL + "getEmployees.php?type=" + type;
        fetchJsonArrayFromUrl(url, jsonArray -> {
            List<String> nameList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    if (obj.has("employee_name")) {
                        nameList.add(obj.getString("employee_name"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if ("inspector".equals(type)) {
                DataSource.setInspectors(nameList);
            } else if ("confirmPerson".equals(type)) {
                DataSource.setConfirmPersons(nameList);
            }

            if (callback != null) callback.run();
        });
    }

    // === 廠商與產品對應資料 ===
    public static void getVendorProductData(Consumer<Map<String, VendorInfo>> callback) {
        String url = BASE_URL + "getVendorProductMap.php";

        fetchJsonObjectFromUrl(url, jsonObject -> {
            Map<String, VendorInfo> vendorMap = new LinkedHashMap<>();

            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String vendor = keys.next();
                try {
                    JSONObject vendorObj = jsonObject.getJSONObject(vendor);

                    String industry = vendorObj.getString("industry");
                    String type = vendorObj.getString("type");

                    JSONArray productsArray = vendorObj.getJSONArray("products");
                    List<String> productList = new ArrayList<>();
                    for (int i = 0; i < productsArray.length(); i++) {
                        productList.add(productsArray.getString(i));
                    }
                    VendorInfo info = new VendorInfo(industry, type, productList);

                    vendorMap.put(vendor, info);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            callback.accept(vendorMap);
        });
    }

    public static void getPasswords(Consumer<List<String>> callback) {
        String url = BASE_URL + "getPassword.php";
        fetchJsonArrayFromUrl(url, jsonArray -> {
            List<String> passwordList = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    passwordList.add(jsonArray.getString(i));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            callback.accept(passwordList);
        });
    }

    // === 匯入資料紀錄 ===
    public static void getInspectRecords(String type, Consumer<List<InspectRecord>> callback) {
        String url = BASE_URL + "getInspectRecords.php?type=" + Uri.encode(type);
        fetchJsonArrayFromUrl(url, jsonArray -> {
            List<InspectRecord> records = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    // 取基本欄位
                    int importId = obj.getInt("import_id");
                    String importDate = obj.getString("import_date");
                    String vendor = obj.getString("vendor");
                    String product = obj.getString("product");
                    String spec = obj.getString("spec");
                    String packageComplete = obj.getString("package_complete");
                    String vectorComplete = obj.getString("vector_complete");
                    String packageLabel = obj.getString("package_label");
                    String quantity = obj.getString("quantity");
                    String validDate = obj.getString("validDate");
                    String palletComplete = obj.getString("pallet_complete");
                    String coa = obj.getString("coa");
                    String inspectorStaff = obj.optString("inspector_staff", "");
                    String confirmStaff = obj.optString("confirm_staff", "");

                    if ("原料".equals(type)) {
                        String odor = obj.getString("odor");
                        String degree = obj.getString("degree");

                        InspectRecord record = new InspectRecord(
                                importId, importDate, vendor, product, spec,
                                packageComplete, vectorComplete, packageLabel,
                                quantity, validDate, palletComplete, coa,
                                inspectorStaff, confirmStaff, odor, degree
                        );
                        records.add(record);
                    } else {
                        // 非原料
                        InspectRecord record = new InspectRecord(
                                importId, importDate, vendor, product, spec,
                                packageComplete, vectorComplete, packageLabel,
                                quantity, validDate, palletComplete, coa,
                                inspectorStaff, confirmStaff, "", ""
                        );
                        records.add(record);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            callback.accept(records);
        });
    }

    public static void getImportRecords(String vendorName, Consumer<List<ImportRecord>> callback) {
        String url = BASE_URL + "getImportRecords.php?vendor=" + Uri.encode(vendorName);

        fetchJsonArrayFromUrl(url, jsonArray -> {
            List<ImportRecord> records = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    ImportRecord record = new ImportRecord(
                            obj.getInt("import_id"),
                            obj.getString("import_date"),
                            obj.getString("vendor"),
                            obj.getString("product"),
                            obj.getString("quantity")
                    );
                    records.add(record);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            callback.accept(records);
        });
    }
    public static void deleteImportRecordById(String importId, Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + "deleteImportRecord.php?import_id=" + URLEncoder.encode(importId, "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);

                int responseCode = conn.getResponseCode();
                boolean isSuccess = (responseCode == 200); // 200 表示成功

                new Handler(Looper.getMainLooper()).post(() -> callback.accept(isSuccess));
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        }).start();
    }
    public static void deleteProduct(String vendorName, String productName, Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + "deleteProduct.php"
                        + "?vendor=" + URLEncoder.encode(vendorName, "UTF-8")
                        + "&product=" + URLEncoder.encode(productName, "UTF-8");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                boolean success = (responseCode == 200);

                new Handler(Looper.getMainLooper()).post(() -> callback.accept(success));
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        }).start();
    }

    public static void addImportRecord(String date, String vendor, String product, String quantity, Consumer<Boolean> callback) {
        new Thread(() -> {
            boolean success = false;
            try {
                String urlStr = BASE_URL + "addImportRecord.php"
                        + "?import_date=" + URLEncoder.encode(date, "UTF-8")
                        + "&vendor=" + URLEncoder.encode(vendor, "UTF-8")
                        + "&product=" + URLEncoder.encode(product, "UTF-8")
                        + "&quantity=" + URLEncoder.encode(quantity, "UTF-8");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String result = reader.readLine();

                success = "success".equals(result);

                reader.close();
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean finalSuccess = success;
            new Handler(Looper.getMainLooper()).post(() -> callback.accept(finalSuccess));
        }).start();
    }
    public static void updateInspectRecord(String type, int id, String spec, String validDate,
                                               boolean packageComplete, boolean odorCheck, boolean vector, String degree,
                                               boolean packageLabel, boolean pallet,
                                               boolean coa, String inspector, String confirmer,
                                               Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                StringBuilder urlBuilder = new StringBuilder(BASE_URL + "updateInspectRecord.php");
                urlBuilder.append("?import_id=").append(id);
                urlBuilder.append("&type=").append(type);
                urlBuilder.append("&spec=").append(URLEncoder.encode(spec, "UTF-8"));
                urlBuilder.append("&validDate=").append(URLEncoder.encode(validDate, "UTF-8"));
                urlBuilder.append("&package_complete=").append(packageComplete ? "1" : "0");
                urlBuilder.append("&odorCheck=").append(odorCheck ? "1" : "0");
                urlBuilder.append("&vector=").append(vector ? "1" : "0");
                urlBuilder.append("&degree=").append(URLEncoder.encode(degree, "UTF-8"));
                urlBuilder.append("&package_label=").append(packageLabel ? "1" : "0");
                urlBuilder.append("&pallet_complete=").append(pallet ? "1" : "0");
                urlBuilder.append("&coa=").append(coa ? "1" : "0");
                urlBuilder.append("&inspector_staff=").append(URLEncoder.encode(inspector, "UTF-8"));

                if (confirmer != null && !confirmer.trim().isEmpty() && !"請選擇".equals(confirmer)) {
                    urlBuilder.append("&confirm_staff=").append(URLEncoder.encode(confirmer, "UTF-8"));
                }

                String urlStr = urlBuilder.toString();
                Log.d("url", urlStr);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String result = reader.readLine();
                reader.close();

                boolean success = "success".equalsIgnoreCase(result);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(success));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        }).start();
    }
    public static void getFilteredInspectRecords(String type, String vendor, String product, String inspector, String confimer, String date, Consumer<List<InspectRecord>> callback) {
        String url = BASE_URL + "getFilteredInspectRecords.php"
                + "?type=" + Uri.encode(type)
                + "&vendor=" + Uri.encode(vendor)
                + "&product=" + Uri.encode(product)
                + "&inspector=" + Uri.encode(inspector)
                + "&confirmer=" + Uri.encode(confimer)
                + "&date=" + Uri.encode(date);
        fetchJsonArrayFromUrl(url, jsonArray -> {
            List<InspectRecord> records = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject obj = jsonArray.getJSONObject(i);

                    InspectRecord record = new InspectRecord(
                            obj.getInt("import_id"),
                            obj.getString("import_date"),
                            obj.getString("vendor"),
                            obj.getString("product"),
                            obj.getString("spec"),
                            obj.getString("package_complete"),
                            obj.getString("vector_complete"),
                            obj.getString("package_label"),
                            obj.getString("quantity"),
                            obj.getString("validDate"),
                            obj.getString("pallet_complete"),
                            obj.getString("coa"),
                            obj.optString("inspector_staff", ""),
                            obj.optString("confirm_staff", ""),
                            obj.getString("odor"),
                            obj.getString("degree")
                    );
                    records.add(record);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            callback.accept(records);
        });
    }
    // 發出 HTTP GET 請求到 PHP API，取得指定欄位的 distinct 值
    public static void getDistinctData(String column, String table, Consumer<String[]> callback) {
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + "getDistinctData.php"
                        + "?column=" + URLEncoder.encode(column, "UTF-8")
                        + "&table=" + URLEncoder.encode(table, "UTF-8");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream in = new BufferedInputStream(conn.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                JSONArray jsonArray = new JSONArray(result.toString());
                String[] data = new String[jsonArray.length()];
                for (int i = 0; i < jsonArray.length(); i++) {
                    data[i] = jsonArray.getString(i);
                }
                Log.d("DB_DATA", Arrays.toString(data));
                // 回傳到主執行緒
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(data));

            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(new String[]{}));
            }
        }).start();
    }
    public static void addProduct(String vendorName, String productName, Consumer<Boolean> callback) {
        new Thread(() -> {
            try {
                String urlStr = BASE_URL + "addProduct.php"
                        + "?vendor=" + URLEncoder.encode(vendorName, "UTF-8")
                        + "&product=" + URLEncoder.encode(productName, "UTF-8");

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                int responseCode = conn.getResponseCode();
                boolean success = (responseCode == 200);

                new Handler(Looper.getMainLooper()).post(() -> callback.accept(success));
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        }).start();
    }
    public static void addVendorWithProducts(Context context, String name, String type, String industry, List<String> products, final Callback callback) {
        try {
            String url = BASE_URL + "addNewSupplier.php";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("vendor_name", name);
            jsonBody.put("type", type);
            jsonBody.put("industry", industry);
            jsonBody.put("products", new JSONArray(products));
            Log.d("DEBUG_JSON", jsonBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        boolean success = response.optBoolean("success", false);
                        callback.onResult(success);
                    },
                    error -> {
                        error.printStackTrace();
                        callback.onResult(false);
                    }
            );

            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(request);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onResult(false);
        }
    }

    public interface Callback {
        void onResult(boolean success);
    }
    public static void deleteVendor(Context context, String vendorName, final Callback callback) {
        try {
            String url = BASE_URL + "deleteSupplier.php";

            JSONObject jsonBody = new JSONObject();
            jsonBody.put("vendor_name", vendorName);

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    response -> {
                        boolean success = response.optBoolean("success", false);
                        callback.onResult(success);
                    },
                    error -> {
                        error.printStackTrace();
                        callback.onResult(false);
                    }
            );

            RequestQueue queue = Volley.newRequestQueue(context);
            queue.add(request);

        } catch (Exception e) {
            e.printStackTrace();
            callback.onResult(false);
        }
    }
    public interface UpdatePasswordCallback {
        void onResult(boolean success, String message);
    }

    public static void updatePassword(Context context, String newPassword, UpdatePasswordCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "update_password.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "&newPassword=" + URLEncoder.encode(newPassword, "UTF-8");

                OutputStream outputStream = conn.getOutputStream();
                outputStream.write(postData.getBytes());
                outputStream.flush();
                outputStream.close();

                int responseCode = conn.getResponseCode();
                InputStream inputStream = (responseCode == 200) ? conn.getInputStream() : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                String response = responseBuilder.toString();
                JSONObject json = new JSONObject(response);
                boolean success = json.getBoolean("success");
                String message = json.getString("message");

                ((Activity) context).runOnUiThread(() -> callback.onResult(success, message));

            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() -> callback.onResult(false, "連線錯誤: " + e.getMessage()));
            }
        }).start();
    }
    public static void updateAuthorityEmployee(String name, boolean isChecked, Context context, UpdatePasswordCallback callback) {
        Log.d("function", name + isChecked);
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "update_authority_employee.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "name=" + URLEncoder.encode(name, "UTF-8") +
                        "&status=" + (isChecked ? "1" : "0");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();

                JSONObject json = new JSONObject(result.toString());
                boolean success = json.getBoolean("success");
                String message = json.getString("message");

                ((Activity) context).runOnUiThread(() -> callback.onResult(success, message));

            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() ->
                        callback.onResult(false, "錯誤：" + e.getMessage()));
            }
        }).start();
    }
    public static void addEmployee(String name, Context context, EmployeeUpdateCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(BASE_URL + "add_employee.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "name=" + URLEncoder.encode(name, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) result.append(line);
                reader.close();

                JSONObject json = new JSONObject(result.toString());
                boolean success = json.getBoolean("success");
                String message = json.getString("message");

                List<String> confirmList = new ArrayList<>();
                List<String> inspectorList = new ArrayList<>();

                if (success) {
                    JSONArray confirmArray = json.getJSONArray("confirmList");
                    JSONArray inspectorArray = json.getJSONArray("inspectorList");

                    for (int i = 0; i < confirmArray.length(); i++) {
                        confirmList.add(confirmArray.getString(i));
                    }
                    for (int i = 0; i < inspectorArray.length(); i++) {
                        inspectorList.add(inspectorArray.getString(i));
                    }
                }

                ((Activity) context).runOnUiThread(() ->
                        callback.onResult(success, message, confirmList, inspectorList));

            } catch (Exception e) {
                e.printStackTrace();
                ((Activity) context).runOnUiThread(() ->
                        callback.onResult(false, "錯誤：" + e.getMessage(), new ArrayList<>(), new ArrayList<>()));
            }
        }).start();
    }

    public interface EmployeeUpdateCallback {
        void onResult(boolean success, String message, List<String> confirmList, List<String> inspectorList);
    }
}
