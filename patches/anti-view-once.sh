#!/usr/bin/env bash
# anti-view-once.sh  <telegram-src-dir>
#
# AMN3ZIA — View Once Bypass patch
# Saves TTL/view-once photos and videos to private storage before Telegram destroys them.
#
# Injection point: FileLoader.java → didFinishLoadingFile()
#   Variables available: finalFile (File), parentObject (Object), currentAccount (int)
#   parentObject can be cast to MessageObject to get .messageOwner.media (TLRPC.MessageMedia)
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
 * Silently saves any TTL / view-once media before Telegram self-destructs it.
 */
public class AmnAntiViewOnce {

    private static final String TAG     = "AmnAntiViewOnce";
    private static final String SUBDIR  = "AMN3ZIA/SavedMedia";

    public static void onFileReady(Context context, File srcFile, TLRPC.MessageMedia media) {
        if (context == null || srcFile == null || !srcFile.exists() || media == null) return;

        int ttl = 0;
        if (media instanceof TLRPC.TL_messageMediaPhoto) {
            ttl = ((TLRPC.TL_messageMediaPhoto) media).ttl_seconds;
        } else if (media instanceof TLRPC.TL_messageMediaDocument) {
            ttl = ((TLRPC.TL_messageMediaDocument) media).ttl_seconds;
        }
        if (ttl <= 0) return;

        try {
            File outDir = new File(context.getExternalFilesDir(null), SUBDIR);
            if (!outDir.exists() && !outDir.mkdirs()) return;

            String ext = extensionOf(srcFile.getName());
            String ts  = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
            File dst   = new File(outDir, "amn_" + ts + ext);
            copyFile(srcFile, dst);
            MediaScannerConnection.scanFile(context, new String[]{dst.getAbsolutePath()}, null, null);
            Log.i(TAG, "Saved view-once: " + dst.getName());
        } catch (Exception e) {
            Log.e(TAG, "Save failed", e);
        }
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
# 2. Patch FileLoader.java
#    Inject BEFORE delegate.fileDidLoaded(...) inside didFinishLoadingFile()
#    Variables in scope: finalFile (File), parentObject (Object), currentAccount (int)
# ─────────────────────────────────────────────────────────────────────────────
FL="$MSRC/messenger/FileLoader.java"
if [ -f "$FL" ]; then
    if ! grep -q "AmnAntiViewOnce" "$FL"; then
        python3 - "$FL" << 'PY'
import sys, re

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# NOTE: AmnAntiViewOnce is in the same package (org.telegram.messenger) — no import needed

# Inject right before `delegate.fileDidLoaded(` inside didFinishLoadingFile
# Anchor is unique enough: the if-delegate-not-null block that calls fileDidLoaded
old = 'if (delegate != null) {\n                        delegate.fileDidLoaded('
new = (
    '// AMN3ZIA: save view-once media before self-destruct fires\n'
    '                if (parentObject instanceof MessageObject) {\n'
    '                    MessageObject _amnMo = (MessageObject) parentObject;\n'
    '                    if (_amnMo.messageOwner != null && _amnMo.messageOwner.media != null) {\n'
    '                        AmnAntiViewOnce.onFileReady(ApplicationLoader.applicationContext,\n'
    '                            finalFile, _amnMo.messageOwner.media);\n'
    '                    }\n'
    '                }\n'
    '                if (delegate != null) {\n'
    '                        delegate.fileDidLoaded('
)
if old in src:
    src = src.replace(old, new, 1)
    print('    Patched FileLoader.java (injected before fileDidLoaded)')
else:
    print('    WARN: anchor not found in FileLoader.java — skipping AVO patch')

with open(path, 'w', encoding='utf-8') as f:
    f.write(src)
PY
    else
        echo "    FileLoader.java already patched"
    fi
else
    echo "    WARN: FileLoader.java not found"
fi

echo "  [AVO] Done."
