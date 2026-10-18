package com.gizlirotalar.app.utils;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.gizlirotalar.app.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImgBBUploader {

    // API anahtarı local.properties içindeki IMGBB_API_KEY değerinden derleme sırasında gelir.
    // (local.properties GitHub'a gönderilmez — bkz. local.properties.example)
    private static final String API_KEY = BuildConfig.IMGBB_API_KEY;

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    /**
     * Bitmap'i imgBB'ye yükler. Sonuç callback ile döner (main thread).
     */
    public static void upload(Bitmap bitmap, UploadCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // Bitmap'i JPEG formatında base64'e dönüştür (kalite: 80%)
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

                // imgBB API endpoint
                String urlString = "https://api.imgbb.com/1/upload";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                // POST body
                String postData = "key=" + API_KEY + "&image=" + URLEncoder.encode(base64Image, "UTF-8");
                DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(postData);
                dos.flush();
                dos.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(responseCode == 200 ? conn.getInputStream() : conn.getErrorStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                if (json.getBoolean("success")) {
                    String imageUrl = json.getJSONObject("data").getString("url");
                    mainHandler.post(() -> callback.onSuccess(imageUrl));
                } else {
                    mainHandler.post(() -> callback.onFailure("Yükleme başarısız"));
                }

            } catch (Exception e) {
                Log.e("ImgBBUploader", "Upload error: " + e.getMessage());
                mainHandler.post(() -> callback.onFailure("Hata: " + e.getMessage()));
            }
        });
    }
}
