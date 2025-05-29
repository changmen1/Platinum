package com.example.zxldemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;

import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
// ================== 本地 TXT 小说阅读器（含书架功能）==================
// 目录：
// - MainActivity.java            // 入口：选择TXT / 阅读内置 / 打开书架
// - ReaderActivity.java          // 阅读页（支持进度记忆、编码检测）
// - BookShelfActivity.java       // 书架页（列表展示）
// - BookShelfManager.java        // 书架数据存取（SharedPreferences + JSON）
// - Book.java                    // 书籍模型
// - res/layout/activity_main.xml // 主页布局
// - res/layout/activity_reader.xml
// - res/layout/activity_bookshelf.xml
// - res/layout/item_book.xml     // 书架单行
// =====================================================================

public class HomeActivity extends Activity {
    private static final int REQUEST_CODE = 1; // ⭐ 必须声明这个常量

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.home); //绑定home.xml
        // TODO 返回登录页
        Button homeButton = findViewById(R.id.btnHome);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //跳转到 HomeActivity
                Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        // TODO 打开文件管理器
        Button selectButton = findViewById(R.id.btnSelectFile);
        Button readAssetButton = findViewById(R.id.btnReadAsset);
        Button btnShelf = findViewById(R.id.btnShelf);
        selectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //  Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                //  intent.setType("text/plain");
                // 新写法：用 Storage Access Framework 的 ACTION_OPEN_DOCUMENT
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("text/plain");

                // 关键：一次性授予可持久化的读权限
                intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_CODE);
            }
        });

        // 直接阅读内置小说
        readAssetButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ReaderActivity.class);
            intent.putExtra("isAsset", true);  // 标记为读取 assets 文件
            intent.putExtra("assetFileName", "novel.txt"); // 你放在 assets 的文件名
            startActivity(intent);
        });

        // 打开书架
        btnShelf.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, BookShelfActivity.class)));

        Button btnDonate = findViewById(R.id.btnzz);
        btnDonate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDonateDialog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();

            if (uri == null) return;

            try {
                final int takeFlags = data.getFlags() &
                        (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                // 持久化失败没关系，至少当前会话还能读
                e.printStackTrace();
            }


            String title = queryFileName(uri);
            // 添加到书架
            BookShelfManager.addBook(this, new Book(title, uri.toString()));

            Intent intent = new Intent(this, ReaderActivity.class);
            intent.setData(uri);
            startActivity(intent);
        }
    }


    /**
     * 根据 Uri 查询显示名（文件名）
     */
    private String queryFileName(Uri uri) {
        String result = uri.getLastPathSegment();
        ContentResolver resolver = getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                result = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return result == null ? "未知文件" : result;
    }


    /**
     * 打开微信赞助
     */
    private void showDonateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_wx, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.show();

        Button btnSave = dialogView.findViewById(R.id.btnSave);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveImageToGallery(dialogView.findViewById(R.id.imgDonate));
                dialog.dismiss();
            }
        });
    }

    private void saveImageToGallery(ImageView imageView) {
        BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
        Bitmap bitmap = drawable.getBitmap();

        String fileName = "donation_qrcode_" + System.currentTimeMillis() + ".jpg";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (uri != null) {
                try {
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                        Toast.makeText(this, "图片已保存到相册", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            // 针对Android 9及以下版本的处理
            // 这里需要添加WRITE_EXTERNAL_STORAGE权限
            // 代码略，建议优先使用Android Q及以上的API
        }
    }
}
