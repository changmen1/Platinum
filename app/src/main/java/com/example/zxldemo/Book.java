package com.example.zxldemo;

/**
 * 书籍模型：保存书籍标题（文件名）和 Uri 字符串。
 */
public class Book {
    public String title;      // 书名 / 文件名
    public String uriString;  // 文件 Uri（或 asset 前缀）

    public Book(String title, String uriString) {
        this.title = title;
        this.uriString = uriString;
    }
}
