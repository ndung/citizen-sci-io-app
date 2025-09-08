package io.sci.citizen.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public final class MultipartUtils {

    private MultipartUtils() {}

    /** Create a multipart part from a Uri without copying to disk. */
    public static MultipartBody.Part uriToPart(@NonNull Context ctx,
                                               @NonNull String section,
                                               @NonNull Uri uri,
                                               @NonNull String formFieldName) {
        final String fileName = section+"-"+chooseFileName(ctx, uri);
        final MediaType mediaType = MediaType.get(chooseMimeType(ctx, uri));

        RequestBody body = new RequestBody() {
            @Nullable @Override public MediaType contentType() { return mediaType; }

            @Override public long contentLength() throws IOException {
                long len = querySize(ctx, uri);
                return (len >= 0) ? len : -1; // -1 = unknown, OkHttp will use chunked transfer
            }

            @Override public void writeTo(@NonNull BufferedSink sink) throws IOException {
                try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new FileNotFoundException("Cannot open: " + uri);
                    try (Source source = Okio.source(in)) {
                        sink.writeAll(source); // streams efficiently
                    }
                }
            }
        };

        return MultipartBody.Part.createFormData(formFieldName, fileName, body);
    }

    /** Build a full multipart body with the Uri and optional extra fields. */
    public static MultipartBody buildBody(@NonNull Context ctx,
                                          @NonNull String fileName,
                                          @NonNull Uri uri,
                                          @NonNull String formFieldName,
                                          @Nullable java.util.Map<String, String> extraFields) {
        MultipartBody.Builder b = new MultipartBody.Builder().setType(MultipartBody.FORM);
        b.addPart(uriToPart(ctx, fileName, uri, formFieldName));
        if (extraFields != null) {
            for (java.util.Map.Entry<String,String> e : extraFields.entrySet()) {
                b.addFormDataPart(e.getKey(), e.getValue());
            }
        }
        return b.build();
    }

    // ---------- helpers ----------

    private static String chooseMimeType(Context ctx, Uri uri) {
        ContentResolver cr = ctx.getContentResolver();
        String mime = cr.getType(uri);
        if (mime != null) return mime;

        // Fallback from extension
        String ext = null;
        String name = queryDisplayName(ctx, uri);
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) ext = name.substring(dot + 1);
        }
        if (ext != null) {
            String guess = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            if (guess != null) return guess;
        }
        return "application/octet-stream";
    }

    private static String chooseFileName(Context ctx, Uri uri) {
        // Prefer display name from provider
        String name = queryDisplayName(ctx, uri);
        if (name != null && !name.trim().isEmpty()) return name;

        // Try to salvage from file://
        if ("file".equalsIgnoreCase(uri.getScheme())) {
            File f = new File(uri.getPath());
            if (f.getName() != null) return f.getName();
        }

        // Fallback
        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(chooseMimeType(ctx, uri));
        if (ext == null) ext = "bin";
        return "upload_" + System.currentTimeMillis() + "." + ext;
    }

    @Nullable
    private static String queryDisplayName(Context ctx, Uri uri) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri,
                    new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally { if (c != null) c.close(); }
        return null;
    }

    private static long querySize(Context ctx, Uri uri) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri,
                    new String[]{OpenableColumns.SIZE}, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(OpenableColumns.SIZE);
                if (idx >= 0) return c.getLong(idx);
            }
        } catch (Exception ignored) {
        } finally { if (c != null) c.close(); }
        return -1;
    }
}
