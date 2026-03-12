package com.linkcapture.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class LinkContentProvider extends ContentProvider {
    private SQLiteOpenHelper dbHelper;

    @Override
    public boolean onCreate() {
        dbHelper = new SQLiteOpenHelper(getContext(), "links.db", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL("CREATE TABLE links (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT, source TEXT, timestamp INTEGER)");
            }
            @Override
            public void onUpgrade(SQLiteDatabase db, int o, int n) {
                db.execSQL("DROP TABLE IF EXISTS links");
                onCreate(db);
            }
        };
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        long id = dbHelper.getWritableDatabase().insert("links", null, values);
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.withAppendedPath(uri, String.valueOf(id));
    }

    @Override
    public Cursor query(Uri uri, String[] proj, String sel, String[] selArgs, String sort) {
        return dbHelper.getReadableDatabase().query("links", proj, sel, selArgs, null, null, sort != null ? sort : "timestamp DESC");
    }

    @Override
    public int delete(Uri uri, String sel, String[] selArgs) {
        int count = dbHelper.getWritableDatabase().delete("links", sel, selArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String sel, String[] selArgs) { return 0; }

    @Override
    public String getType(Uri uri) { return null; }
}