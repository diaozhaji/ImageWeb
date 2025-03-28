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
import java.util.Locale;

public class ImageSavingWebViewClient extends WebViewClient {
    private final Context context;
    private String currentPageUrl = "";

    public ImageSavingWebViewClient(Context context) {
        this.context = context;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        currentPageUrl = url; // 记录当前页面URL
        Log.d("ImageSave", "url=" + currentPageUrl);
        super.onPageStarted(view, url, favicon);

    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        final String imageUrl = request.getUrl().toString();
        if (isImageRequest(request)) {
            new Thread(() -> downloadAndSaveImage(imageUrl)).start();
        }
        return super.shouldInterceptRequest(view, request);
    }

    // 判断是否为图片请求
    private boolean isImageRequest(WebResourceRequest request) {
        String mimeType = request.getRequestHeaders().get("Accept");
        String url = request.getUrl().toString();
        return (mimeType != null && mimeType.startsWith("image/")) ||
//                url.matches(".*\\.(jpg|jpeg|png|webp|gif|bmp|svg)(\\?.*)?");
                url.matches(".*\\.(jpg|jpeg|png|webp|bmp)(\\?.*)?");//无需gif和svg
    }

    // 下载并保存图片
    private void downloadAndSaveImage(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());

            // 生成存储路径（基于当前页面URL）
            File saveDir = generateSaveDirectory();
            if (!saveDir.exists() && !saveDir.mkdirs()) {
                Log.e("ImageSave", "目录创建失败: " + saveDir.getPath());
                return;
            }

            // 生成文件名（基于图片URL）
            String fileName = generateFileName(imageUrl);
            File outputFile = new File(saveDir, fileName);

            // 处理重复文件名
            int counter = 1;
            while (outputFile.exists()) {
                Log.d("ImageSave", "图片已存在，无需下载: " + outputFile.getAbsolutePath());
                return;
//                String newName = appendFileCounter(fileName, counter++);
//                outputFile = new File(saveDir, newName);

            }

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                bitmap.compress(getCompressFormat(imageUrl), 100, fos);
                Log.d("ImageSave", "图片已保存: " + outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e("ImageSave", "保存失败: " + e.getMessage());
        }
    }

    // 生成存储目录（基于当前页面URL）
    private File generateSaveDirectory() {
        try {
            if (currentPageUrl.isEmpty()) return getDefaultDir();

            URI uri = new URI(currentPageUrl);
            String domain = uri.getHost().replace("www.", "");
            String fullPath = uri.getPath(); // 获取完整路径

            // 清理路径中的结尾文件名（如果有）
            if (fullPath != null) {
                // 保留所有路径层级（如 /comics-read/1001339/YPleBmwuqgChJkJBytZu）
                // 仅移除最后一个片段（如果以文件扩展名结尾）
                if (fullPath.contains(".")) {
                    fullPath = fullPath.substring(0, fullPath.lastIndexOf('/'));
                }
            }

            // 组合路径：域名 + 完整路径
            File baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            return new File(baseDir,
                    "WebCache/" + domain + (fullPath != null ? fullPath : "")
            );

        } catch (URISyntaxException e) {
            return getDefaultDir();
        }
    }

    // 默认存储目录
    private File getDefaultDir() {
        return new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "WebCache/Unknown");
    }

    // 生成文件名（基于图片URL）
    private String generateFileName(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();

            // 提取原始文件名
            String fileName = new File(path).getName();
            if (fileName.isEmpty()) fileName = "image";

            // 移除URL查询参数
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf('?'));
            }

            // 添加扩展名
            if (!fileName.contains(".")) {
                String ext = getFileExtension(imageUrl);
                fileName += ext;
            }

            return fileName;
        } catch (URISyntaxException e) {
            return "image_" + Integer.toHexString(imageUrl.hashCode()) + getFileExtension(imageUrl);
        }
    }

    // 处理重复文件名
    private String appendFileCounter(String fileName, int counter) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) return fileName + "_" + counter;
        return fileName.substring(0, dotIndex) + "_" + counter + fileName.substring(dotIndex);
    }

    // 获取文件扩展名
    private String getFileExtension(String url) {
        if (url.contains(".")) {
            String ext = url.substring(url.lastIndexOf('.'));
            if (ext.contains("?")) ext = ext.substring(0, ext.indexOf('?'));
            if (ext.length() <= 5) return ext; // 扩展名最长4字符（如 .jpeg）
        }
        return ".jpg";
    }

    // 选择压缩格式
    private Bitmap.CompressFormat getCompressFormat(String url) {
        String ext = getFileExtension(url).toLowerCase(Locale.ROOT);
        switch (ext) {
            case ".png":
                return Bitmap.CompressFormat.PNG;
            case ".webp":
                return Bitmap.CompressFormat.WEBP;
            default:
                return Bitmap.CompressFormat.JPEG;
        }
    }
}