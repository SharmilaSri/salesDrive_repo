package com.retailx.dreamdx.retailx.Syncronisation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationManagerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.StringRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.retailx.dreamdx.retailx.POJO.DataPart;
import com.retailx.dreamdx.retailx.POJO.PaymentMethod;
import com.retailx.dreamdx.retailx.POJO.Product;
import com.retailx.dreamdx.retailx.UtilityFunctions;
import com.retailx.dreamdx.retailx.apicalls.ApiCalls;
import com.retailx.dreamdx.retailx.apicalls.AppSingleton;
import com.retailx.dreamdx.retailx.apicalls.ResponseParser;
import com.retailx.dreamdx.retailx.apicalls.VolleyCalls;
import com.retailx.dreamdx.retailx.apicalls.VolleyMultipartRequest;
import com.retailx.dreamdx.retailx.database.DBHelper;
import com.retailx.dreamdx.retailx.sharedpreference.SharedPreference;
import com.retailx.dreamdx.retailx.utils.ConstantsUsed;
//import com.squareup.picasso.Downloader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class SyncFunctions {

    static public String userId="-1";
    static public Context context;
    static public DBHelper db;
    final static String CHANNEL_ID="1000";
    static Notification notification;
    static NotificationManagerCompat notificationManager;

    public static  void syncWithServer(Context ctx, String userid){
        userId=userid;
        context=ctx;
        db=new DBHelper(ctx);


        if(UtilityFunctions.isNetworkConnected(context) || UtilityFunctions.isInternetAvailable()) {

            try {

                if (userId != null && !userId.equalsIgnoreCase("-1")) {

                    syncWithServerCategory(context, userId);

                    syncWithServerProduct(context, userId);

                    syncWithServerCustomer(context, userId);

                    syncWithServerTransactions(context, userId);

                    //getAllServerError(context);

                    getAllCategoriesFromServer(context, userId);

                    getAllProductsFromServer(context, userId);


                    syncWithServerImages(context);

                    String is_admin=SharedPreference.getInstance().getValue(ctx,ConstantsUsed.IS_ADMIN);
                    if(is_admin!=null && is_admin.equalsIgnoreCase("0")){
                        getBussinessInfo(context,userId);
                    }else if(is_admin!=null && is_admin.equalsIgnoreCase("1")){
                        syncBussinessDetails(context, userId);
                    }


                }
            }catch(OutOfMemoryError e){
               // Toast.makeText(ctx,"out of memeory erroy occured",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                //Toast.makeText(ctx,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
            }
        }

    }

    public static  void syncImagesWithServer(Context ctx, String userid){
        userId=userid;
        context=ctx;
        db=new DBHelper(ctx);


        if(UtilityFunctions.isNetworkConnected(context) || UtilityFunctions.isInternetAvailable()) {

            try {

                if (userId != null && !userId.equalsIgnoreCase("-1")) {

                    syncWithServerImages(context);


                }
            }catch(OutOfMemoryError e){
                // Toast.makeText(ctx,"out of memeory erroy occured",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                //Toast.makeText(ctx,e.getMessage().toString(),Toast.LENGTH_SHORT).show();
            }
        }

    }

    public static final String TAG = "MyTag";
    static StringRequest stringRequest;
    static RequestQueue requestQueue;
    static JsonArrayRequest jobReq;
    static Cache cache ;


    private static void syncWithServerStringPost(final Context ctx,String url,final int type,final Map<String, String> params){

        cache = new DiskBasedCache(ctx.getCacheDir(), 1024 * 1024); // 1MB cap
        requestQueue = AppSingleton.getInstance(ctx).
                getRequestQueue();


        requestQueue.start();

        stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response != null && response.length() > 0) {
                            ResponseParser.PARSE_JSON(ctx, response, type, setUpGson());
                        }

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //Toast.makeText(ctx,"Error"+error.toString(),Toast.LENGTH_SHORT).show();

            }
        }) {
            @Override
            protected Map<String, String> getParams() {

                return params;
            }

        };

        stringRequest.setTag(TAG);


        AppSingleton.getInstance(ctx).addToRequestQueue(stringRequest);


    }



    private static void syncWithServerJsonArrayPostRequest(final Context ctx, String url, final int type, final JSONArray jsonArray){

        cache = new DiskBasedCache(ctx.getCacheDir(), 1024 * 1024); // 1MB cap
        requestQueue = AppSingleton.getInstance(ctx).
                getRequestQueue();


        requestQueue.start();

        jobReq = new JsonArrayRequest(Request.Method.POST, url, jsonArray,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {
                        if (jsonArray.toString() != null && jsonArray.toString().length() > 0) {
                            ResponseParser.PARSE_JSON(ctx, jsonArray.toString(), type, setUpGson());
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                       // Toast.makeText(ctx, "NETWORK ERROR"+volleyError.toString()
                             //   , Toast.LENGTH_LONG).show();
                    }
                });

        jobReq.setTag(TAG);

        AppSingleton.getInstance(ctx).addToRequestQueue(jobReq);

    }

    public static String connectTONetworkPostMultipart(
            final Context ctx, String url, final int type,
            final Map<String, String> params, final Bitmap bitmap) {

            cache = new DiskBasedCache(ctx.getCacheDir(), 1024 * 1024); // 1MB cap
            requestQueue = AppSingleton.getInstance(ctx).
                getRequestQueue();


            requestQueue.start();

                //our custom volley request
                VolleyMultipartRequest volleyMultipartRequest = new VolleyMultipartRequest(Request.Method.POST, ConstantsUsed.URL_POST_IMAGE,
                new Response.Listener<NetworkResponse>() {
                    @Override
                    public void onResponse(NetworkResponse response) {
                        ResponseParser.PARSE_JSON(ctx, new String(response.data), type,setUpGson());


                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Toast.makeText(ctx, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }) {

            /*
             * If you want to add more parameters with the image
             * you can do it here
             * here we have only one parameter with the image
             * which is tags
             * */
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {

                return params;
            }

            /*
             * Here we are passing image by renaming it with a unique name
             * */
            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                long imagename = System.currentTimeMillis();
                params.put("image", new DataPart(imagename + ".png", getFileDataFromDrawable(bitmap)));
                return params;
            }
        };
        volleyMultipartRequest.setTag(TAG);
        //adding the request to volley
        AppSingleton.getInstance(ctx).addToRequestQueue(volleyMultipartRequest);


        return "";

    }
    public static byte[] getFileDataFromDrawable(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }


    static private Gson gson;
    private static  Gson setUpGson() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setDateFormat("M/d/yy hh:mm a");
        gson = gsonBuilder.create();
        return gson;
    }

    public static void stopAll(){
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG);
        }

    }

    private static void getAllServerError(Context ctx){

        if(!userId.isEmpty() && !userId.equalsIgnoreCase("-1")) {

            Cursor cur = null;
            String brand="0";
            String device="0";
            String model="0";
            String phone_id="0";
            String phone_product="0";
            String sdk="0";
            String release="0";
            String incremental="0";
            String description="0";
            try{
                cur = db.getErrosDetailsNotUploaded();

                while (cur.moveToNext()) {
                    brand = cur.getString(cur.getColumnIndex((ConstantsUsed.BRAND)));
                    device = cur.getString(cur.getColumnIndex((ConstantsUsed.DEVICE)));
                    model = cur.getString(cur.getColumnIndex((ConstantsUsed.MODEL)));
                    phone_id = cur.getString(cur.getColumnIndex((ConstantsUsed.PHONE_ID)));
                    phone_product = cur.getString(cur.getColumnIndex((ConstantsUsed.PHONE_PRODUCT)));
                    sdk = cur.getString(cur.getColumnIndex((ConstantsUsed.SDK)));
                    release = cur.getString(cur.getColumnIndex((ConstantsUsed.RELEASE)));
                    incremental = cur.getString(cur.getColumnIndex((ConstantsUsed.INCREMENTAL)));
                    description = cur.getString(cur.getColumnIndex((ConstantsUsed.DESCRIPTION)));
                    if(description.contains("'"))
                        description=description.replaceAll("'", "");
                }


            }catch (Exception e){


            }finally{
                if(cur!=null){
                    cur.close();
                }

                ApiCalls apicalls=new VolleyCalls();
                Map<String,String> params = new HashMap<String, String>();
                params.put("user_id",userId);
                params.put("brand",brand);
                params.put("model",model);
                params.put("id",phone_id);
                params.put("product",phone_product);
                params.put("sdk",sdk);
                params.put("release",release);
                params.put("incremental",incremental);
                params.put("error_description",description);
                params.put("device",device);
                apicalls.connectToNetworkPost(ctx, ConstantsUsed.URL_ERROR_REPORT,ConstantsUsed.TYPE_ERROR,params);

            }
        }

    }

    private static void syncBussinessDetails(Context ctx, String clientId){
        if (!clientId.isEmpty() && !clientId.equalsIgnoreCase("-1")) {
            String name="Not available";
            String address="Not available";
            String number="Not available";
            Cursor cur = null;
            try{
                cur = db.getBussinessDetailsNotUploaded();

                while (cur.moveToNext()) {
                    name = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_BUSSINESS_NAME)));
                    address = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_BUSSINESS_ADDRESS)));
                    number = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_BUSSINESS_NUMBER)));
                }


            }catch (Exception e){


            }finally{
                if(cur!=null){
                    cur.close();
                }

                Map<String,String> params = new HashMap<String, String>();
                params.put("user_id",clientId);
                params.put("first_name","System");
                params.put("last_name","User");
                params.put("company_name",name);
                params.put("contact_number",number);
                params.put("address",address);
                syncWithServerStringPost(ctx, ConstantsUsed.URL_CREATE_BUSSINESS,ConstantsUsed.TYPE_BUSSINESS_INFO,params);

            }
        }
    }

    private static  void syncWithServerCustomer(Context ctx,String userId){

        if (!userId.isEmpty() && !userId.equalsIgnoreCase("-1")) {

            Cursor cur = null;
            JSONArray customer_array=null;
            customer_array = new JSONArray();

            try {
                cur = db.getCustomerDetailsNotUploaded();

                while (cur.moveToNext()) {
                    String cust_id = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CUSTOMER_ID)));
                    String cust_name = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CUSTOMER_NAME)));
                    String cust_number = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CUSTOMER_NUMBER)));
                    String cust_gender = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CUSTOMER_GENDER)));
                    String cust_email = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CUSTOMER_EMAIL)));
                    String cust_is_vat = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_IS_VAT)));

                    try {
                        JSONObject customerObje = new JSONObject();
                        customerObje.put("user_id", userId);
                        customerObje.put("customer_type", cust_gender);
                        customerObje.put("email_id", cust_email);
                        customerObje.put("customer_id", cust_id);
                        customerObje.put("customer_name", cust_name);//hardcoded
                        customerObje.put("mobile_number", cust_number);
                        customerObje.put("vat_enabled", cust_is_vat);

                        customer_array.put(customerObje);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                //Toast.makeText(ctx, "ERROR" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            } finally {
                if(cur!=null)
                    cur.close();


                if(customer_array.length()>0) {
                    syncWithServerJsonArrayPostRequest(ctx, ConstantsUsed.URL_CUSTOMER_UPLOAD, ConstantsUsed.TYPE_UPLOAD_CUSTOMER, customer_array);
                }
            }

        }
    }



    private static void getAllProductsFromServer(Context ctx, String clntId){

        if (!clntId.isEmpty() && !clntId.equalsIgnoreCase("-1")) {

            Map<String,String> params = new HashMap<String, String>();
            params.put("user_id",clntId);
            syncWithServerStringPost(ctx, ConstantsUsed.URL_GET_PRODUCTS,ConstantsUsed.TYPE_GET_PRODUCTS,params);

        }

    }

    private static void createNotificationChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager notificationManager= ctx.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(serviceChannel);
        }
    }


    private static  void getBussinessInfo(Context ctx, String userId){

        if (!userId.isEmpty() && !userId.equalsIgnoreCase("-1")) {
            Map<String,String> params = new HashMap<String, String>();
            params.put("user_id",userId);
            syncWithServerStringPost(ctx, ConstantsUsed.URL_GET_BUSSINESS_INFO,ConstantsUsed.TYPE_GET_BUSSINESS_INFO,params);

        }

    }

    private static  void getAllCategoriesFromServer(Context ctx, String clntId){

        if (!clntId.isEmpty() && !clntId.equalsIgnoreCase("-1")) {

            Map<String,String> params = new HashMap<String, String>();
            params.put("user_id",clntId);
            syncWithServerStringPost(ctx, ConstantsUsed.URL_GET_CATEGORY,ConstantsUsed.TYPE_GET_CATEGORY,params);

        }

    }

    public static void syncWithServerCategory(Context ctx, String userId){


        JSONArray array=new JSONArray();


        Cursor cur = null;
        try {
            cur = db.getCategoriesNotUploaded(userId);


            while (cur.moveToNext()) {
                String title = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CAT_TITLE)));
                String date = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CAT_DATE)));
                String id = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CAT_ID)));
                String desc = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CAT_DESCRIPTION)));

                JSONObject params=new JSONObject();
                try {
                    params.put(ConstantsUsed.COLUMN_MOBILE_ID,id);
                    params.put(ConstantsUsed.COLUMN_USER_ID, userId);
                    params.put(ConstantsUsed.COLUMN_CAT_TITLE,title);
                    params.put(ConstantsUsed.COLUMN_CAT_DESCRIPTION,desc);
                    params.put(ConstantsUsed.COLUMN_CAT_DATE,"2019-03-12 10:20:22");
                    array.put(params);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
           // Toast.makeText(ctx, "ERROR" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
        } finally {

            if(cur!=null)
                cur.close();

            if(array.length()>0) {
                syncWithServerJsonArrayPostRequest(ctx, ConstantsUsed.URL_CREATE_CATEGORY, ConstantsUsed.TYPE_CREATE_CATEGORY, array);
            }
        }

    }


    public static void apiCalBackCategory(String res,Context ctx){

        //update local db status
        db.updateCategoryTableDbStatus(0);


    }

    public static void apiCalBackCategoryGet(String res,Context ctx){

        //update local db status
        db.updateCategoryTableDbStatus(0);

    }

    public static void apiCalBackProduct(String res,Context ctx){

        //syncWithServerImages(ctx);
        db.updateProductTableDbStatus(0);



    }

    public static void apiCalBackProductImages(String productId,Context ctx){
        //update local db status
        db.updateProductTableImageDbStatus(productId,0);

    }

    public static void apiCalBackCustomer(){

        //update local db status
        db.updateCustomerTableDbStatus(0);

    }

    public static void apiCalBackError(){

        //update local db status
        db.updateCErrorTableDbStatus(0);

    }

    public static void apiCalBackTransaction(String res,Context ctx){

        //update local db status
        db.updateTransactionDetailsTableDbStatus(0,"");
        db.updateTransactionSummaryTableDbStatus(0,"");

    }


    private static void syncWithServerTransactions(Context ctx,String clientId) {


        if (!clientId.isEmpty() && !clientId.equalsIgnoreCase("-1")) {


            String paymentType="1";
            if(PaymentMethod.getInstance().getMethodName().equalsIgnoreCase("CASH")){
                paymentType="1";
            }else if(PaymentMethod.getInstance().getMethodName().equalsIgnoreCase("DEBIT")){
                paymentType="2";
            }else if(PaymentMethod.getInstance().getMethodName().equalsIgnoreCase("CREDIT")){
                paymentType="3";
            }else if(PaymentMethod.getInstance().getMethodName().equalsIgnoreCase("CHEQUE")){
                paymentType="4";
            }else if(PaymentMethod.getInstance().getMethodName().equalsIgnoreCase("QR")){
                paymentType="5";
            }else if(PaymentMethod.getInstance().getMethodName().equalsIgnoreCase("SPLIT")){
                paymentType="6";
            }

            Cursor cur = null;
            Cursor details = null;
            JSONArray transaction_array=null;
            transaction_array = new JSONArray();

            try {
                cur = db.getTransactionsSummaryNotUploaded();
                int i=0;

                while (cur.moveToNext()) {
                    String id = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_TRANSACTION_ID)));
                    Double itemTotalCount = cur.getDouble(cur.getColumnIndex((ConstantsUsed.COLUMN_TOTAL_ITEM_COUNT)));
                    Double itemTotal = cur.getDouble(cur.getColumnIndex((ConstantsUsed.COLUMN_TOTAL_AMOUNT)));
                    String date = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_TRANSACTION_DATE)));
                    String custId = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CUSTOMER_ID)));


                    try {

                        JSONObject transaction_summary = new JSONObject();

                        transaction_summary.put("invoice_number", id);
                        transaction_summary.put("user_id", clientId);
                        transaction_summary.put("customer_id", custId);
                        transaction_summary.put("total_item_count", String.valueOf(itemTotalCount));
                        transaction_summary.put("transaction_total_amount", String.valueOf(itemTotal-Product.getDiscount()));
                        transaction_summary.put("total_tax_amount", "00");//hardcoded
                        transaction_summary.put("item_total_amount", String.valueOf(itemTotal));
                        transaction_summary.put("payment_type", paymentType);
                        transaction_summary.put("discount", String.valueOf(Product.getDiscount()));
                        transaction_summary.put("order_date", date);
                        transaction_summary.put("invoice_type", "1");

                        details = db.getTransactionsDetailsNotUploaded(id);

                        JSONArray item_list = new JSONArray();

                        while (details.moveToNext()) {
                            String productId = details.getString(details.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_ID)));
                            Double itemCount = details.getDouble(details.getColumnIndex((ConstantsUsed.COLUMN_TOTAL_ITEM_UNITS)));
                            Double itemAmount = details.getDouble(details.getColumnIndex((ConstantsUsed.COLUMN_TOTAL_ITEM_AMOUNT)));

                            JSONObject item_list_obj = new JSONObject();
                            item_list_obj.put("item_id",  productId);
                            item_list_obj.put("total_units", String.valueOf(itemCount));
                            item_list_obj.put("total_amount", String.valueOf(itemAmount));
                            item_list_obj.put("total_tax", "90.00");//hardcoded
                            item_list_obj.put("item_discount",String.valueOf(Product.getDiscount()));
                            item_list.put(item_list_obj);


                        }
                        transaction_summary.put("item_list", item_list);
                        transaction_array.put(transaction_summary);


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
               // Toast.makeText(ctx, "ERROR" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            } finally {
                if(cur!=null)
                    cur.close();
                if(details!=null)
                    details.close();

                if(transaction_array.length()>0) {

                    syncWithServerJsonArrayPostRequest(ctx, ConstantsUsed.URL_TRANSACTION_SUMMARY, ConstantsUsed.TYPE_CREATE_INVOICE, transaction_array);
                }
            }

        }
    }


    private static void syncWithServerImages(Context ctx){
        String id="";
        String path="";
        if(!userId.isEmpty() && !userId.equalsIgnoreCase("-1")) {

            Cursor cur = null;
            try {
                cur = db.getImagesNotUploaded(userId);

                while (cur.moveToNext()) {
                    path = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_IMAGE_PATH)));
                    id = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_ID)));

                    if(!path.equalsIgnoreCase("NO_IMAGE") ){

                        UtilityFunctions.uploadBitmap(UtilityFunctions.getBitMapFromPath(path,ctx),ctx,userId,id);
                    }

                }
            }catch (Exception e){

            }
        }

    }


    private static void syncWithServerProduct(Context ctx,String userId){


        if(!userId.isEmpty() && !userId.equalsIgnoreCase("-1")){


            JSONArray array=new JSONArray();


            Cursor cur = null;
            try {
                cur = db.getProductsNotUploaded(userId);

                while (cur.moveToNext()) {
                    String title = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_TITLE)));
                    String date = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_CAT_DATE)));
                    String serial_code = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PROD_SERIAL_CODE)));
                    String unit_measurement = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_UNIT_OF_MEASURE)));
                    String id = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_ID)));
                    String catId = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PROD_CAT_ID)));
                    String buyingPrice = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PROD_BUYING_PRICE)));
                    String sellingPrice = cur.getString(cur.getColumnIndex((ConstantsUsed.COLUMN_PRODUCT_SELLING_PRICE)));

                    JSONObject params=new JSONObject();
                    try {
                        params.put(ConstantsUsed.COLUMN_USER_ID,userId);
                        params.put(ConstantsUsed.COLUMN_MOBILE_ID,id);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_CAT_ID,catId);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_SERIAL_CODE,serial_code);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_TITLE,title);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_DESCRIPTION,"test");
                        params.put(ConstantsUsed.COLUMN_PRODUCT_UNIT_OF_MEASURE,unit_measurement);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_BRAND_CODE,"C1BR1");
                        params.put(ConstantsUsed.COLUMN_PRODUCT_SUPPLIER_CODE,"C5SUP2");
                        params.put(ConstantsUsed.COLUMN_PRODUCT_BUYING_PRICE,buyingPrice);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_SELLING_PRICE,sellingPrice);
                        params.put(ConstantsUsed.COLUMN_PRODUCT_CREATED_DATE,"2019-03-12 10:20:22");
                        array.put(params);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                //Toast.makeText(ctx, "ERROR" + e.getMessage().toString(), Toast.LENGTH_LONG).show();
            } finally {
                if(cur!=null)
                    cur.close();

                if(array.length()>0) {
                    syncWithServerJsonArrayPostRequest(ctx, ConstantsUsed.URL_CREATE_PRODUCT, ConstantsUsed.TYPE_CREATE_PRODUCT, array);
                }
            }

        }
    }
}