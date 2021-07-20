package com.example.annotamobile.ui.library;

import android.graphics.Bitmap;

public class SearchResult {
    private Bitmap icon;
    private String id;
    private String name;
    private String date_time;
    private String content;
    private String comments;
    private String cat1;
    private String cat2;
    private String cat3;

    public SearchResult(String id, String name, String date_time, String content, String comments, String cat1, String cat2, String cat3, Bitmap icon) {
        this.id = id;
        this.name = name;
        this.date_time = date_time;
        this.content = content;
        this.comments = comments;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.icon = icon;
    }

    public Bitmap getIcon() {
        return icon;
    }

    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDate_time() {
        return date_time;
    }

    public void setDate_time(String date_time) {
        this.date_time = date_time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getCat1() {
        return cat1;
    }

    public void setCat1(String cat1) {
        this.cat1 = cat1;
    }

    public String getCat2() {
        return cat2;
    }

    public void setCat2(String cat2) {
        this.cat2 = cat2;
    }

    public String getCat3() {
        return cat3;
    }

    public void setCat3(String cat3) {
        this.cat3 = cat3;
    }
}
