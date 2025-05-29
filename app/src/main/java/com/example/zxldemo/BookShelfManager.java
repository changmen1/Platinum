package com.example.zxldemo;
// - BookShelfManager.java        // 书架数据存取（SharedPreferences + JSON）

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 书架数据的增删改查，使用 SharedPreferences + JSON 存储，便于初学者理解。
 */
public class BookShelfManager {
    private static final String PREF_NAME = "bookshelf_prefs";
    private static final String KEY_BOOKS = "books_json";

    /**
     * 保存书籍列表到本地
     */
    public static void saveBooks(Context context, List<Book> books) {
        try {
            JSONArray array = new JSONArray();
            for (Book b : books) {
                JSONObject obj = new JSONObject();
                obj.put("title", b.title);
                obj.put("uri", b.uriString);
                array.put(obj);
            }
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            sp.edit().putString(KEY_BOOKS, array.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    /**
     * 读取书籍列表
     */
    public static List<Book> loadBooks(Context context) {
        List<Book> list = new ArrayList<>();
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_BOOKS, "");
        if (json.isEmpty()) return list;
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new Book(obj.getString("title"), obj.getString("uri")));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    /**
     * 向书架追加书，如果已存在则忽略
     */
    public static void addBook(Context ctx, Book book) {
        List<Book> books = loadBooks(ctx);
        for (Book b : books) {
            if (b.uriString.equals(book.uriString)) return; // 已存在
        }
        books.add(book);
        saveBooks(ctx, books);
    }
}
