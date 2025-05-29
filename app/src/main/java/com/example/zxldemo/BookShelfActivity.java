package com.example.zxldemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/**
 * 简易书架：用 ListView 展示已导入书籍。
 */
public class BookShelfActivity extends Activity {
    private List<Book> books = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookshelf);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) ListView listView = findViewById(R.id.listBooks);

        // 读取书架数据
        books = BookShelfManager.loadBooks(this);
        List<String> titles = new ArrayList<>();
        for (Book b : books) titles.add(b.title);

        // 简单 ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(adapter);

        // 点击打开阅读
        listView.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            Book book = books.get(position);
            Intent intent = new Intent(BookShelfActivity.this, ReaderActivity.class);
            // 判断是资产还是外部文件
            if (book.uriString.startsWith("asset_")) {
                intent.putExtra("isAsset", true);
                intent.putExtra("assetFileName", book.uriString.substring("asset_".length()));
            } else {
                intent.setData(Uri.parse(book.uriString));
            }
            startActivity(intent);
        });
        // 长按：删除该书
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("删除书籍")
                    .setMessage("确定要从书架中移除 \"" + books.get(position).title + "\" 吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        books.remove(position);
                        BookShelfManager.saveBooks(BookShelfActivity.this, books); // 保存
                        titles.remove(position);
                        adapter.notifyDataSetChanged(); // 刷新列表
                    })
                    .setNegativeButton("取消", null)
                    .show();
            return true;
        });
    }
}
