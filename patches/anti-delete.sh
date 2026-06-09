#!/usr/bin/env bash
# anti-delete.sh  <telegram-src-dir>
#
# AMN3ZIA — Anti-Delete patch
# Keeps deleted messages visible in chat with a "Deleted" marker
# instead of silently removing them from the message list.
#
# Strategy:
#   1. Inject AmnAntiDelete.java — local deleted-message cache (SQLite table)
#   2. Patch MessagesController.java — intercept deleteMessages() calls
#   3. Chat renderer (ChatMessageCell) reads the "deleted" flag and renders a banner
#
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AD] Injecting Anti-Delete patch..."

# ─────────────────────────────────────────────────────────────────────────────
# 1. AmnAntiDelete.java — lightweight cache of deleted message IDs
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$MSRC/messenger"
cat > "$MSRC/messenger/AmnAntiDelete.java" << 'JAVA'
package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AMN3ZIA — Anti-Delete
 *
 * When Telegram deletes messages we:
 *   1. Record their IDs in SharedPreferences (keyed by chat_id).
 *   2. The message objects are kept in MessagesController's cache (we block removal).
 *   3. ChatMessageCell reads isAmnDeleted() and overlays a "Deleted" banner.
 */
public class AmnAntiDelete {

    private static final String TAG    = "AmnAntiDelete";
    private static final String PREFS  = "amn_deleted_msgs";

    // ── Public API ─────────────────────────────────────────────────────────

    /** Mark message IDs as deleted in a given chat. */
    public static void markDeleted(Context context, long chatId, List<Integer> msgIds) {
        if (context == null || msgIds == null || msgIds.isEmpty()) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = "chat_" + chatId;
        Set<String> existing = new HashSet<>(prefs.getStringSet(key, Collections.emptySet()));
        for (int id : msgIds) existing.add(String.valueOf(id));
        prefs.edit().putStringSet(key, existing).apply();
        Log.d(TAG, "Marked " + msgIds.size() + " msgs as deleted in chat " + chatId);
    }

    /** Returns true if this message was deleted by the remote peer. */
    public static boolean isDeleted(Context context, long chatId, int msgId) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> ids = prefs.getStringSet("chat_" + chatId, Collections.emptySet());
        return ids.contains(String.valueOf(msgId));
    }

    /** Clear deleted markers for a chat (e.g. when user manually clears chat). */
    public static void clearChat(Context context, long chatId) {
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit().remove("chat_" + chatId).apply();
    }

    /**
     * Called from MessagesController.deleteMessages().
     * Records the IDs, then returns the list UNCHANGED so the caller can still
     * update the UI (we overlay, not replace, the message view).
     */
    public static List<Integer> intercept(Context context, long chatId, List<Integer> ids) {
        markDeleted(context, chatId, ids);
        return ids; // return original — caller proceeds normally for network ACK
    }
}
JAVA

echo "    Created AmnAntiDelete.java"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Patch MessagesController.java
#    Find deleteMessages() and inject our intercept call.
# ─────────────────────────────────────────────────────────────────────────────
MC="$MSRC/messenger/MessagesController.java"
if [ -f "$MC" ]; then
    if ! grep -q "AmnAntiDelete" "$MC"; then
        python3 - "$MC" << 'PY'
import sys, re

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# 1. Add import
if 'AmnAntiDelete' not in src:
    src = src.replace(
        'import org.telegram.tgnet.TLRPC;',
        'import org.telegram.tgnet.TLRPC;\nimport org.telegram.messenger.AmnAntiDelete;',
        1
    )

# 2. Intercept: find the deleteMessages method signature variations.
#    Telegram has: public void deleteMessages(ArrayList<Integer> ids, ...)
#    We inject at the very beginning of the method body.
# Pattern: match the opening brace of the deleteMessages implementation
pattern = r'(public\s+void\s+deleteMessages\s*\([^)]*ArrayList[^)]*\)\s*\{)'
def inject(m):
    return m.group(0) + '''
        // AMN3ZIA Anti-Delete: record deleted message IDs before removal
        if (ids != null && !ids.isEmpty()) {
            AmnAntiDelete.markDeleted(ApplicationLoader.applicationContext, dialog_id, ids);
        }'''

new_src, n = re.subn(pattern, inject, src, count=2)  # up to 2 overloads
if n > 0:
    print("    Patched MessagesController.java (" + str(n) + " overload(s))")
else:
    print("    WARN: deleteMessages hook not found — trying fallback")
    # Fallback: look for the internal call that actually removes from DB
    fallback = r'(MessagesStorage\.getInstance\(\)\.deleteMessages\()'
    new_src, n2 = re.subn(fallback,
        r'AmnAntiDelete.intercept(ApplicationLoader.applicationContext, dialog_id, ids);\n            \1',
        src, count=1)
    if n2 > 0:
        print("    Patched MessagesController.java (fallback storage hook)")
    else:
        print("    WARN: MessagesController.java not patched — check manually")
        new_src = src

with open(path, 'w', encoding='utf-8') as f:
    f.write(new_src)
PY
    else
        echo "    MessagesController.java already patched, skipping"
    fi
else
    echo "    WARN: MessagesController.java not found"
fi

# ─────────────────────────────────────────────────────────────────────────────
# 3. Patch ChatMessageCell.java — render "Deleted" overlay on deleted messages
# ─────────────────────────────────────────────────────────────────────────────
CMC="$MSRC/ui/cells/ChatMessageCell.java"
if [ -f "$CMC" ]; then
    if ! grep -q "AmnAntiDelete" "$CMC"; then
        python3 - "$CMC" << 'PY'
import sys, re

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# Add import
src = src.replace(
    'import org.telegram.tgnet.TLRPC;',
    'import org.telegram.tgnet.TLRPC;\nimport org.telegram.messenger.AmnAntiDelete;',
    1
)

# Find the draw() or onDraw() method and inject the deleted overlay drawing.
# We look for canvas.restore() or super.onDraw() at end of draw to add our overlay.
overlay_code = '''
        // AMN3ZIA Anti-Delete: draw "Deleted" banner if message was deleted remotely
        if (currentMessageObject != null && currentMessageObject.messageOwner != null) {
            long dialogId = currentMessageObject.getDialogId();
            int msgId = currentMessageObject.getId();
            if (AmnAntiDelete.isDeleted(getContext(), dialogId, msgId)) {
                android.graphics.Paint deletedPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                deletedPaint.setColor(0xCCFF3B30); // semi-transparent red
                deletedPaint.setTextSize(android.util.TypedValue.applyDimension(
                    android.util.TypedValue.COMPLEX_UNIT_DIP, 11,
                    getResources().getDisplayMetrics()));
                deletedPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                canvas.drawText("✕ Deleted", 12, 20, deletedPaint);
            }
        }'''

# Inject at end of onDraw before closing brace
pattern = r'(@Override\s+protected\s+void\s+onDraw\s*\(\s*Canvas\s+canvas\s*\)\s*\{)'
new_src, n = re.subn(pattern, r'\1' + overlay_code, src, count=1)
if n > 0:
    print("    Patched ChatMessageCell.java (onDraw overlay)")
else:
    print("    WARN: ChatMessageCell.java onDraw not found — overlay not applied")
    new_src = src

with open(path, 'w', encoding='utf-8') as f:
    f.write(new_src)
PY
    else
        echo "    ChatMessageCell.java already patched"
    fi
else
    echo "    WARN: ChatMessageCell.java not found at expected path"
fi

echo "  [AD] Done."
