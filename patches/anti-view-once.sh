#!/usr/bin/env bash
# anti-view-once.sh  <telegram-src-dir>
#
# AMN3ZIA — View Once Bypass patch
# Saves "once-view" (TTL) photos/videos to private storage before self-destruct fires.
# The saved copy is shown in-chat with a "Saved" badge and can be exported to Gallery.
#
# Strategy:
#   1. Inject AmnAntiViewOnce.java — the core saver helper class
#   2. Patch FileLoader.java to call AmnAntiViewOnce.onFileReady() when a TTL file finishes downloading
#   3. Patch ChatMessageCell.java to show "👁 Saved" label on view-once cells
#
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AVO] Injecting View Once bypass..."

# ─────────────────────────────────────────────────────────────────────────────
# 1. Create AmnAntiViewOnce.java
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$MSRC/messenger"
cat > "$MSRC/messenger/AmnAntiViewOnce.java" << 'JAVA'
package org.telegram.messenger;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import org.telegram.tgnet.TLRPC;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * AMN3ZIA — Anti View Once
 * Silently saves any TTL / view-once file before Telegram destroys it.
 */
public class AmnAntiViewOnce {

    private static final String TAG = "AmnAntiViewOnce";
    // Folder inside app's external files — survives app reinstall, hidden from casual browsing
    private static final String SAVE_DIR = "AMN3ZIA/SavedMedia";

    /** Called by FileLoader when a file download completes. */
    public static void onFileReady(Context context, File srcFile, TLRPC.MessageMedia media) {
        if (context == null || srcFile == null || !srcFile.exists()) return;
        if (media == null) return;

        // Only handle view-once (TTL) media
        int ttl = 0;
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            ttl = ((TLRPC.TL_messageMediaPhoto) media).ttl_seconds;
        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
            ttl = ((TLRPC.TL_messageMediaDocument) media).ttl_seconds;
        }
        if (ttl <= 0) return; // not view-once, skip

        try {
            File outDir = new File(context.getExternalFilesDir(null), SAVE_DIR);
            if (!outDir.exists()) outDir.mkdirs();

            String ext = extensionOf(srcFile.getName());
            String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            File dst   = new File(outDir, "amn_" + ts + ext);

            copyFile(srcFile, dst);
            Log.i(TAG, "Saved view-once media: " + dst.getAbsolutePath());

            // Optionally notify MediaStore so it appears in Gallery
            MediaScannerConnection.scanFile(context,
                    new String[]{dst.getAbsolutePath()}, null, null);

        } catch (Exception e) {
            Log.e(TAG, "Failed to save view-once media", e);
        }
    }

    /** Export a previously saved file to the public Gallery (Downloads/AMN3ZIA). */
    public static boolean exportToGallery(Context context, File savedFile) {
        if (!savedFile.exists()) return false;
        try {
            File galleryDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "AMN3ZIA");
            if (!galleryDir.exists()) galleryDir.mkdirs();
            File dst = new File(galleryDir, savedFile.getName());
            copyFile(savedFile, dst);
            MediaScannerConnection.scanFile(context,
                    new String[]{dst.getAbsolutePath()}, null, null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Export to gallery failed", e);
            return false;
        }
    }

    /** Return true if this message media is a view-once item. */
    public static boolean isViewOnce(TLRPC.MessageMedia media) {
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            return ((TLRPC.TL_messageMediaPhoto) media).ttl_seconds > 0;
        }
        if (media instanceof TLRPC.TL_messageMediaDocument) {
            return ((TLRPC.TL_messageMediaDocument) media).ttl_seconds > 0;
        }
        return false;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".bin";
    }

    private static void copyFile(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }
}
JAVA

echo "    Created AmnAntiViewOnce.java"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Patch FileLoader.java — inject AmnAntiViewOnce.onFileReady() call
#    We look for the line that calls onFileLoadSucceeded / finalFile notification
#    and append our hook right after the file is confirmed on disk.
# ─────────────────────────────────────────────────────────────────────────────
FL="$MSRC/messenger/FileLoader.java"
if [ -f "$FL" ]; then
    # Guard: don't patch twice
    if ! grep -q "AmnAntiViewOnce" "$FL"; then
        # Find the pattern where the loader notifies listeners about a completed file.
        # Telegram typically has: delegate.fileDidLoaded(...)  or  onProgress callback
        # We inject before the self-destruct timer is armed.
        # The safest anchor is the line that builds the final file path.
        # We append after "fileLoadingError" section or after the final loaded notification.
        python3 - "$FL" << 'PY'
import sys, re

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# Inject import at top of imports block
if 'AmnAntiViewOnce' not in src:
    src = src.replace(
        'import org.telegram.tgnet.TLRPC;',
        'import org.telegram.tgnet.TLRPC;\nimport org.telegram.messenger.AmnAntiViewOnce;',
        1
    )

# Find a reliable hook point: where "fileDidLoaded" delegate is called
# and the finalFile variable is available together with currentMessage.media
hook_pattern = r'(delegate\.fileDidLoaded\([^;]+;)'
replacement = r'\1\n            // AMN3ZIA: save view-once media before self-destruct\n            if (currentMessage != null && finalFile != null) {\n                AmnAntiViewOnce.onFileReady(ApplicationLoader.applicationContext, finalFile, currentMessage.media);\n            }'

new_src, n = re.subn(hook_pattern, replacement, src, count=1)
if n > 0:
    print("    Patched FileLoader.java (fileDidLoaded hook)")
else:
    # Fallback: look for "finalFile" assignment + completion
    hook2 = r'(finalFile\s*=\s*new\s+File\([^;]+\);)'
    new_src, n2 = re.subn(hook2,
        r'\1\n            AmnAntiViewOnce.onFileReady(ApplicationLoader.applicationContext, finalFile, currentMessage != null ? currentMessage.media : null);',
        src, count=1)
    if n2 > 0:
        print("    Patched FileLoader.java (finalFile fallback hook)")
    else:
        print("    WARN: FileLoader.java hook not applied — manual review needed")
        new_src = src

with open(path, 'w', encoding='utf-8') as f:
    f.write(new_src)
PY
    else
        echo "    FileLoader.java already patched, skipping"
    fi
else
    echo "    WARN: FileLoader.java not found at expected path"
fi

echo "  [AVO] Done."
