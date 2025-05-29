package com.example.zxldemo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReaderActivity extends Activity {
    private TextView textView;
    private ProgressBar progressBar;
    private ScrollView scrollView;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String PREFS_NAME = "reader_prefs";
    private static final String KEY_SCROLL_Y_PREFIX = "scroll_y_";

    private String currentFileKey; // 用于保存和恢复滚动位置的key

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reader);

        textView = findViewById(R.id.txtContent);
        progressBar = findViewById(R.id.loadingBar);
        scrollView = findViewById(R.id.scrollView);

        boolean isAsset = getIntent().getBooleanExtra("isAsset", false);
        if (isAsset) {
            // 从assets读取文件
            String assetFileName = getIntent().getStringExtra("assetFileName");
            if (assetFileName == null) {
                Toast.makeText(this, "资产文件名为空", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            currentFileKey = "asset_" + assetFileName;
            loadTextFromAssetsAsync(assetFileName);
        } else {
            // 从外部uri读取
            Uri uri = getIntent().getData();
            if (uri != null) {
                currentFileKey = uri.toString();
                loadTextAsync(uri);
            } else {
                Toast.makeText(this, "未获取到文件", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // 从assets异步读取文本
    private void loadTextFromAssetsAsync(String assetFileName) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        executor.execute(() -> {
            try {
                InputStream is = getAssets().open(assetFileName);
                BufferedInputStream bis = new BufferedInputStream(is);
                bis.mark(4);
                byte[] bom = new byte[3];
                int read = bis.read(bom, 0, 3);
                Charset charset;
                if (read == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
                    charset = StandardCharsets.UTF_8;
                } else {
                    charset = Charset.forName("GBK");
                    bis.reset();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(bis, charset));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();

                String content = sb.toString();
                mainHandler.post(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    textView.setText(content);
                    scrollView.setVisibility(ScrollView.VISIBLE);
                    restoreScrollPosition();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(ReaderActivity.this, "读取资产文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }


    // 从外部uri异步读取文本，带编码检测
    private void loadTextAsync(Uri uri) {
        progressBar.setVisibility(ProgressBar.VISIBLE);
        executor.execute(() -> {
            try {
                String content = readText(uri);
                mainHandler.post(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    textView.setText(content);
                    scrollView.setVisibility(ScrollView.VISIBLE);
                    restoreScrollPosition();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    Toast.makeText(ReaderActivity.this, "读取失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private String readText(Uri uri) throws Exception {
        InputStream is = getContentResolver().openInputStream(uri);
        BufferedInputStream bis = new BufferedInputStream(is);
        bis.mark(4);
        byte[] bom = new byte[3];
        int read = bis.read(bom, 0, 3);
        Charset charset;
        if (read == 3 && bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            charset = StandardCharsets.UTF_8;
        } else {
            charset = Charset.forName("GBK");
            bis.reset();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(bis, charset));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    private void restoreScrollPosition() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int savedY = prefs.getInt(KEY_SCROLL_Y_PREFIX + currentFileKey, 0);
        scrollView.post(() -> scrollView.scrollTo(0, savedY));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 保存当前滚动位置
        int scrollY = scrollView.getScrollY();
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_SCROLL_Y_PREFIX + currentFileKey, scrollY).apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
