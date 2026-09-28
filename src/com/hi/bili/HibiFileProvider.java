package com.hi.bili;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 最小可用 FileProvider - 纯源码无混淆
 * 用于 Android 7.0+ 安装 APK 时分享文件 URI
 */
public class HibiFileProvider extends ContentProvider {

    private static final String AUTHORITY = "com.hi.bili.fileprovider";

    public boolean onCreate() {
        return true;
    }

    public static Uri getUriForFile(Context context, File file) {
        try {
            return Uri.parse("content://" + AUTHORITY + "/" + file.getAbsolutePath());
        } catch (Exception e) {
            return Uri.fromFile(file);
        }
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            String path = uri.getPath();
            if (path != null && path.startsWith("/")) {
                path = path.substring(1);
            }
            File file = new File(path);
            if (file.exists()) {
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Cannot open: " + uri);
        }
        throw new FileNotFoundException("No file for " + uri);
    }

    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        return null;
    }

    public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        return 0;
    }
}
