package io.sci.citizen.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class UriUtils {
    private UriUtils() {
    }

    /**
     * Copy a Uri's content into app cache and return the File. For file:// just returns the File.
     */
    @Nullable
    public static File copyToCache(@NonNull Context ctx, @NonNull Uri uri) throws IOException {
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme)) {
            return new File(uri.getPath());
        }

        if (!"content".equalsIgnoreCase(scheme)) {
            // Unknown scheme: still try to stream-copy into cache
            return copyStream(ctx, uri, buildCacheFile(ctx, "bin", "bin"));
        }

        // 1) Determine display name & MIME
        String displayName = queryDisplayName(ctx, uri);
        String mime = safeMime(ctx, uri);
        String ext = chooseExtension(displayName, mime);

        // 2) Build a unique cache File
        String base = stripExtension(displayName);
        if (base == null || base.trim().isEmpty()) base = "picked_" + System.currentTimeMillis();
        File out = buildCacheFile(ctx, base, ext);

        // 3) Copy bytes
        return copyStream(ctx, uri, out);
    }

    @Nullable
    private static String queryDisplayName(Context ctx, Uri uri) {
        Cursor c = null;
        try {
            c = ctx.getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return c.getString(idx);
            }
        } catch (Exception ignored) {
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    private static String safeMime(Context ctx, Uri uri) {
        String t = ctx.getContentResolver().getType(uri);
        return t != null ? t : "application/octet-stream";
    }

    private static String chooseExtension(@Nullable String displayName, @NonNull String mime) {
        // Prefer extension from display name if present
        if (displayName != null) {
            int dot = displayName.lastIndexOf('.');
            if (dot > 0 && dot < displayName.length() - 1) {
                return displayName.substring(dot + 1);
            }
        }
        // Fallback from MIME
        String ext = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
        if (ext == null) {
            // Heuristic for common image types
            if (mime.equals("image/heic") || mime.equals("image/heif")) return "heic";
            if (mime.equals("image/webp")) return "webp";
            if (mime.startsWith("image/")) return "jpg"; // generic fallback
            return "bin";
        }
        return ext;
    }

    @Nullable
    private static String stripExtension(@Nullable String name) {
        if (name == null) return null;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static File buildCacheFile(Context ctx, String base, String ext) {
        String fname = base + "." + ext;
        File out = new File(ctx.getCacheDir(), fname);
        // Ensure uniqueness
        int i = 1;
        while (out.exists()) {
            out = new File(ctx.getCacheDir(), base + "_" + (i++) + "." + ext);
        }
        return out;
    }

    @Nullable
    private static File copyStream(Context ctx, Uri uri, File out) throws IOException {
        try (InputStream in = ctx.getContentResolver().openInputStream(uri);
             OutputStream os = new FileOutputStream(out)) {
            if (in == null) throw new FileNotFoundException("Cannot open: " + uri);
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = in.read(buf)) != -1) os.write(buf, 0, n);
            os.flush();
            return out;
        }
    }
}
