#!/usr/bin/env bash
# anti-view-once.sh  <telegram-src-dir>
#
# AMN3ZIA — View Once Bypass v3
# Only saves TTL media when AmnPrivacySettings.isViewOnce() is true.
# Uses MediaStore API on Android 10+ for immediate gallery visibility.
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AVO] Injecting View Once bypass v3..."

# ─────────────────────────────────────────────────────────────────────────────
# AmnAntiViewOnce.java — MediaStore-aware saver
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
 * AMN3ZIA Anti View Once v3 — respects isViewOnce() toggle.
 */
public class AmnAntiViewOnce {

    private static final String TAG   = "AmnAntiViewOnce";
    private static final String ALBUM = "AMN3ZIA";

    public static void onFileReady(Context context, File srcFile, TLRPC.MessageMedia media) {
        // Check toggle first
        if (!AmnPrivacySettings.isViewOnce(context)) return;
        if (context == null || srcFile == null || !srcFile.exists() || media == null) return;

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
        // ttl != 0 covers both small values AND 0x7FFFFFFF (single-view)
        if (ttl == 0) return;

        final String ext  = extensionOf(srcFile.getName(), isVideo);
        final String mime = isVideo ? "video/mp4" : "image/jpeg";
        final String ts   = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        final String name = "amn_" + ts + ext;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri col = isVideo
                    ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                    : MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
                ContentValues cv = new ContentValues();
                cv.put(isVideo ? MediaStore.Video.Media.DISPLAY_NAME
                               : MediaStore.Images.Media.DISPLAY_NAME, name);
                cv.put(isVideo ? MediaStore.Video.Media.MIME_TYPE
                               : MediaStore.Images.Media.MIME_TYPE, mime);
                cv.put(isVideo ? MediaStore.Video.Media.RELATIVE_PATH
                               : MediaStore.Images.Media.RELATIVE_PATH,
                       Environment.DIRECTORY_PICTURES + "/" + ALBUM);
                cv.put(isVideo ? MediaStore.Video.Media.IS_PENDING
                               : MediaStore.Images.Media.IS_PENDING, 1);
                Uri uri = context.getContentResolver().insert(col, cv);
                if (uri != null) {
                    try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
                        copyStream(srcFile, os);
                    }
                    cv.clear();
                    cv.put(isVideo ? MediaStore.Video.Media.IS_PENDING
                                   : MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(uri, cv, null, null);
                    showToast(context, "AMN3ZIA: сохранено в Галерею → " + name);
                    Log.i(TAG, "Saved via MediaStore: " + name);
                }
            } else {
                File dir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), ALBUM);
                if (!dir.exists()) dir.mkdirs();
                File dst = new File(dir, name);
                try (FileInputStream in = new FileInputStream(srcFile);
                     FileOutputStream out = new FileOutputStream(dst)) {
                    copyStream(srcFile, out);
                }
                MediaScannerConnection.scanFile(context,
                    new String[]{dst.getAbsolutePath()}, new String[]{mime}, null);
                showToast(context, "AMN3ZIA: сохранено → " + name);
                Log.i(TAG, "Saved to Pictures: " + dst.getAbsolutePath());
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

    private static void copyStream(File src, OutputStream out) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    private static void copyStream(File src, FileOutputStream out) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            byte[] buf = new byte[65536]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }
}
JAVA
echo "    Created AmnAntiViewOnce.java v3"

# ─────────────────────────────────────────────────────────────────────────────
# Patch FileLoader.java — inject before delegate.fileDidLoaded()
# ─────────────────────────────────────────────────────────────────────────────
FL="$MSRC/messenger/FileLoader.java"
if [ -f "$FL" ]; then
    if ! grep -q "AmnAntiViewOnce" "$FL"; then
        python3 - "$FL" << 'PY'
import sys
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# Confirmed anchor from commit 9fea726 FileLoader.java:
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
    print('    Patched FileLoader.java (view-once injected)')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)
else:
    # Try alternate indentation
    old2 = 'if (delegate != null) {\n                    delegate.fileDidLoaded('
    new2 = (
        '// AMN3ZIA view-once\n'
        '            if (parentObject instanceof MessageObject) {\n'
        '                MessageObject _amnMo = (MessageObject) parentObject;\n'
        '                if (_amnMo.messageOwner != null && _amnMo.messageOwner.media != null)\n'
        '                    AmnAntiViewOnce.onFileReady(ApplicationLoader.applicationContext,\n'
        '                        finalFile, _amnMo.messageOwner.media);\n'
        '            }\n'
        '            if (delegate != null) {\n'
        '                    delegate.fileDidLoaded('
    )
    if old2 in src:
        src = src.replace(old2, new2, 1)
        print('    Patched FileLoader.java (alt indent)')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(src)
    else:
        idx = src.find('delegate.fileDidLoaded(')
        if idx > 0:
            print(f'    WARN: anchor not matched. Context: {repr(src[max(0,idx-120):idx+50])}')
        else:
            print('    WARN: fileDidLoaded not found at all')
PY
    else
        echo "    FileLoader already patched"
    fi
else
    echo "    WARN: FileLoader.java not found"
fi

echo "  [AVO] Done."
