#!/usr/bin/env bash
# anti-view-once.sh  <telegram-src-dir>
#
# AMN3ZIA — View Once Bypass patch (v2)
#
# Saves TTL/view-once photos and videos to the gallery (Pictures/AMN3ZIA)
# using MediaStore API on Android 10+ for proper gallery visibility.
# Shows a Toast notification so the user knows the file was saved.
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AVO] Injecting View Once bypass v2..."

# ─────────────────────────────────────────────────────────────────────────────
# 1. AmnAntiViewOnce.java  — MediaStore-aware saver
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$MSRC/messenger"
cat > "$MSRC/messenger/AmnAntiViewOnce.java" << 'JAVA'
package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AMN3ZIA — Anti View Once (v2)
 * Saves TTL / view-once media to Gallery (Pictures/AMN3ZIA) before Telegram destroys it.
 * Uses MediaStore API on Android 10+ so files appear in the gallery immediately.
 */
public class AmnAntiViewOnce {

    private static final String TAG    = "AmnAntiViewOnce";
    private static final String ALBUM  = "AMN3ZIA";

    public static void onFileReady(Context context, File srcFile, TLRPC.MessageMedia media) {
        if (context == null || srcFile == null || !srcFile.exists() || media == null) return;

        // Check TTL (view-once / self-destruct timer)
        int ttl = 0;
        boolean isVideo = false;
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            ttl = ((TLRPC.TL_messageMediaPhoto) media).ttl_seconds;
        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
            ttl = ((TLRPC.TL_messageMediaDocument) media).ttl_seconds;
            TLRPC.TL_messageMediaDocument doc = (TLRPC.TL_messageMediaDocument) media;
            if (doc.document instanceof TLRPC.TL_document) {
                for (TLRPC.DocumentAttribute attr : ((TLRPC.TL_document) doc.document).attributes) {
                    if (attr instanceof TLRPC.TL_documentAttributeVideo) {
                        isVideo = true;
                        break;
                    }
                }
            }
        }
        if (ttl <= 0) return;  // not a view-once/TTL media

        final String ext  = extensionOf(srcFile.getName(), isVideo);
        final String mime = isVideo ? "video/mp4" : "image/jpeg";
        final String ts   = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        final String name = "amn_" + ts + ext;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ — insert directly into MediaStore (appears in gallery)
                Uri collection = isVideo
                    ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    : MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);

                ContentValues values = new ContentValues();
                values.put(isVideo
                    ? MediaStore.Video.Media.DISPLAY_NAME
                    : MediaStore.Images.Media.DISPLAY_NAME, name);
                values.put(isVideo
                    ? MediaStore.Video.Media.MIME_TYPE
                    : MediaStore.Images.Media.MIME_TYPE, mime);
                values.put(isVideo
                    ? MediaStore.Video.Media.RELATIVE_PATH
                    : MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + ALBUM);
                values.put(isVideo
                    ? MediaStore.Video.Media.IS_PENDING
                    : MediaStore.Images.Media.IS_PENDING, 1);

                Uri itemUri = context.getContentResolver().insert(collection, values);
                if (itemUri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(itemUri)) {
                        copyStream(srcFile, os);
                    }
                    values.clear();
                    values.put(isVideo
                        ? MediaStore.Video.Media.IS_PENDING
                        : MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(itemUri, values, null, null);
                    Log.i(TAG, "Saved via MediaStore: " + name);
                    showToast(context, "AMN3ZIA: сохранено в Галерею (" + name + ")");
                }
            } else {
                // Android 9 and below — write to public Pictures directory
                File outDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), ALBUM);
                if (!outDir.exists()) outDir.mkdirs();
                File dst = new File(outDir, name);
                copyFile(srcFile, dst);
                MediaScannerConnection.scanFile(context,
                    new String[]{dst.getAbsolutePath()},
                    new String[]{mime}, null);
                Log.i(TAG, "Saved to Pictures: " + dst.getAbsolutePath());
                showToast(context, "AMN3ZIA: сохранено в Галерею (" + name + ")");
            }
        } catch (Exception e) {
            Log.e(TAG, "Save failed", e);
        }
    }

    private static void showToast(final Context ctx, final String msg) {
        new Handler(Looper.getMainLooper()).post(() ->
            Toast.makeText(ctx.getApplicationContext(), msg, Toast.LENGTH_LONG).show());
    }

    private static String extensionOf(String name, boolean isVideo) {
        int dot = name.lastIndexOf('.');
        if (dot >= 0) return name.substring(dot);
        return isVideo ? ".mp4" : ".jpg";
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            copyStream(in, out);
        }
    }

    private static void copyStream(File src, OutputStream out) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            copyStream(in, out);
        }
    }

    private static void copyStream(FileInputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
    }
}
JAVA
echo "    Created AmnAntiViewOnce.java (MediaStore v2)"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Patch FileLoader.java
#    Inject BEFORE delegate.fileDidLoaded(...)
#    Variables in scope: finalFile (File), parentObject (Object)
# ─────────────────────────────────────────────────────────────────────────────
FL="$MSRC/messenger/FileLoader.java"
if [ -f "$FL" ]; then
    if ! grep -q "AmnAntiViewOnce" "$FL"; then
        python3 - "$FL" << 'PY'
import sys

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# Anchor: the if-delegate-not-null block calling fileDidLoaded
# Actual source (confirmed from commit 9fea726):
#     if (delegate != null) {
#         delegate.fileDidLoaded(fileName, finalFile, parentObject, finalType);
#     }
# We search without leading whitespace (substring match handles indentation)
old = 'if (delegate != null) {\n                        delegate.fileDidLoaded('
new = (
    '// AMN3ZIA: save view-once before self-destruct fires\n'
    '                if (parentObject instanceof MessageObject) {\n'
    '                    MessageObject _amnMo = (MessageObject) parentObject;\n'
    '                    if (_amnMo.messageOwner != null && _amnMo.messageOwner.media != null) {\n'
    '                        AmnAntiViewOnce.onFileReady(\n'
    '                            ApplicationLoader.applicationContext,\n'
    '                            finalFile, _amnMo.messageOwner.media);\n'
    '                    }\n'
    '                }\n'
    '                if (delegate != null) {\n'
    '                        delegate.fileDidLoaded('
)

if old in src:
    src = src.replace(old, new, 1)
    print('    Patched FileLoader.java (view-once saver injected)')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)
else:
    # Try alternative indentation (some builds use different spacing)
    old2 = 'if (delegate != null) {\n                    delegate.fileDidLoaded('
    new2 = (
        '// AMN3ZIA: save view-once before self-destruct fires\n'
        '            if (parentObject instanceof MessageObject) {\n'
        '                MessageObject _amnMo = (MessageObject) parentObject;\n'
        '                if (_amnMo.messageOwner != null && _amnMo.messageOwner.media != null) {\n'
        '                    AmnAntiViewOnce.onFileReady(\n'
        '                        ApplicationLoader.applicationContext,\n'
        '                        finalFile, _amnMo.messageOwner.media);\n'
        '                }\n'
        '            }\n'
        '            if (delegate != null) {\n'
        '                    delegate.fileDidLoaded('
    )
    if old2 in src:
        src = src.replace(old2, new2, 1)
        print('    Patched FileLoader.java (view-once, alt indentation)')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(src)
    else:
        print('    WARN: fileDidLoaded anchor not found — view-once NOT applied')
        print('    Trying to show what is around delegate.fileDidLoaded...')
        idx = src.find('delegate.fileDidLoaded(')
        if idx >= 0:
            print(repr(src[max(0,idx-200):idx+100]))
PY
    else
        echo "    FileLoader.java already patched"
    fi
else
    echo "    WARN: FileLoader.java not found"
fi

echo "  [AVO] Done."
