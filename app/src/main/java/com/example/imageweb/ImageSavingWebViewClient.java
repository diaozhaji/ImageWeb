package com.example.imageweb;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSavingWebViewClient extends WebViewClient {
    private final Context context;

    public ImageSavingWebViewClient(Context context) {
        this.context = context;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        final String url = request.getUrl().toString();
        if (isImageRequest(request)) {
            new Thread(() -> downloadAndSaveImage(url)).start();
        }
        return super.shouldInterceptRequest(view, request);
    }

    // 判断是否为图片请求
    private boolean isImageRequest(WebResourceRequest request) {
        String mimeType = request.getRequestHeaders().get("Accept");
        String url = request.getUrl().toString();
        return (mimeType != null && mimeType.startsWith("image/")) ||
                url.matches(".*\\.(jpg|jpeg|png|webp|gif|bmp|svg)");
    }

    // 下载并保存图片
    private void downloadAndSaveImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());

            // 生成存储路径
            File saveDir = generateSaveDirectory(imageUrl);
            if (!saveDir.exists() && !saveDir.mkdirs()) {
                Log.e("ImageSave", "Failed to create directory: " + saveDir.getPath());
                return;
            }

            // 保存文件
            String fileName = generateFileName(imageUrl);
            File outputFile = new File(saveDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                bitmap.compress(getCompressFormat(imageUrl), 100, fos);
                Log.d("ImageSave", "Saved: " + outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e("ImageSave", "Error saving image: " + e.getMessage());
        }
    }

    // 生成存储目录（按域名和路径）
    private File generateSaveDirectory(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String domain = uri.getHost().replace("www.", "");
            String path = uri.getPath();

            // 移除文件名部分（保留目录结构）
            if (path != null && path.contains("/")) {
                path = path.substring(0, path.lastIndexOf('/'));
            }

            // 组合路径
            File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            return new File(baseDir, "WebImages/" + domain + (path != null ? path : ""));
        } catch (URISyntaxException e) {
            return new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), "WebImages/Unknown");
        }
    }

    // 生成唯一文件名
    private String generateFileName(String url) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String urlHash = Integer.toHexString(url.hashCode());
        return "IMG_" + timestamp + "_" + urlHash + getFileExtension(url);
    }

    // 获取文件扩展名
    private String getFileExtension(String url) {
        if (url.contains(".")) {
            return url.substring(url.lastIndexOf('.'));
        }
        return ".jpg";
    }

    // 根据URL选择压缩格式
    private Bitmap.CompressFormat getCompressFormat(String url) {
        if (url.endsWith(".png")) {
            return Bitmap.CompressFormat.PNG;
        } else if (url.endsWith(".webp")) {
            return Bitmap.CompressFormat.WEBP;
        }
        return Bitmap.CompressFormat.JPEG;
    }
}