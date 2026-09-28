package com.hi.bili;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

public class HibiFileProvider extends ContentProvider {
    private static final String AUTHORITY = "com.hi.bili.fileprovider";
    private UriMatcher uriMatcher;

    public boolean onCreate() {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "*", 1);
        return true;
    }

    public static Uri getUriForFile(Context ctx, File file) {
        try {
            String path = file.getCanonicalPath();
            String base = ctx.getCacheDir().getCanonicalPath();
            if (path.startsWith(base)) {
                return Uri.parse("content://" + AUTHORITY + "/cache/" + path.substring(base.length() + 1));
            }
            base = ctx.getFilesDir().getCanonicalPath();
            if (path.startsWith(base)) {
                return Uri.parse("content://" + AUTHORITY + "/files/" + path.substring(base.length() + 1));
            }
            File ext = ctx.getExternalFilesDir(null);
            if (ext != null) {
                base = ext.getCanonicalPath();
                if (path.startsWith(base)) {
                    return Uri.parse("content://" + AUTHORITY + "/ext/" + path.substring(base.length() + 1));
                }
            }
        } catch (Exception e) {
        }
        return Uri.fromFile(file);
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        try {
            File file = resolveFile(uri);
            if (file != null && file.exists()) {
                return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
            }
        } catch (Exception e) {
        }
        throw new FileNotFoundException("No file for " + uri);
    }

    private File resolveFile(Uri uri) {
        try {
            String path = uri.getPath();
            if (path == null) return null;
            Context ctx = getContext();
            if (ctx == null) return null;
            if (path.startsWith("/cache/")) {
                return new File(ctx.getCacheDir(), path.substring(7));
            } else if (path.startsWith("/files/")) {
                return new File(ctx.getFilesDir(), path.substring(7));
            } else if (path.startsWith("/ext/")) {
                File ext = ctx.getExternalFilesDir(null);
                if (ext != null) return new File(ext, path.substring(5));
            }
        } catch (Exception e) {
        }
        return null;
    }

    public Cursor query(Uri uri, String[] proj, String sel, String[] args, String sort) { return null; }
    public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
    public Uri insert(Uri uri, ContentValues values) { return null; }
    public int delete(Uri uri, String sel, String[] args) { return 0; }
    public int update(Uri uri, ContentValues values, String sel, String[] args) { return 0; }
}
