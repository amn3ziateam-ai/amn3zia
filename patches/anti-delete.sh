#!/usr/bin/env bash
# anti-delete.sh  <telegram-src-dir>
#
# AMN3ZIA — Anti-Delete patch (v2)
#
# Strategy: intercept deleteMessages() ONLY when cacheOnly=true, which means
# the deletion was pushed by the server (someone else deleted the message).
# We return early → message stays in local SQLite → stays visible in chat.
# User-initiated deletes (cacheOnly=false) still work normally.
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AD] Injecting Anti-Delete patch v2..."

# ─────────────────────────────────────────────────────────────────────────────
# 1. AmnAntiDelete.java  (minimal — just logging; actual prevention is via return)
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$MSRC/messenger"
cat > "$MSRC/messenger/AmnAntiDelete.java" << 'JAVA'
package org.telegram.messenger;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;

/**
 * AMN3ZIA — Anti-Delete
 * Messages deleted by others are kept in local DB by returning early in deleteMessages().
 */
public class AmnAntiDelete {
    private static final String TAG = "AmnAntiDelete";

    /** Called when we intercept a server-pushed deletion. Just logs. */
    public static void onIntercepted(long dialogId, ArrayList<Integer> messages) {
        if (messages == null) return;
        Log.i(TAG, "Anti-delete: kept " + messages.size() + " msg(s) in dialog " + dialogId);
    }
}
JAVA
echo "    Created AmnAntiDelete.java"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Patch MessagesController.java
#    Target ONLY the 12-param overload (has 'movedToScheduled' — unique identifier).
#    Inject at top: if cacheOnly==true → record + return (don't delete from DB).
# ─────────────────────────────────────────────────────────────────────────────
MC="$MSRC/messenger/MessagesController.java"
if [ -f "$MC" ]; then
    if ! grep -q "AmnAntiDelete" "$MC"; then
        python3 - "$MC" << 'PY'
import sys, re

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# Match ONLY the 12-param overload: identified by 'movedToScheduled' in signature.
# Regex captures: entire signature up to and including the opening brace.
pattern = re.compile(
    r'(public\s+void\s+deleteMessages\s*\([^)]*boolean\s+movedToScheduled[^)]*\)\s*\{)',
    re.DOTALL
)

inject_code = (
    '\n        // AMN3ZIA Anti-Delete: server-pushed delete (cacheOnly=true) → keep message\n'
    '        if (cacheOnly && messages != null && !messages.isEmpty()) {\n'
    '            AmnAntiDelete.onIntercepted(dialogId, messages);\n'
    '            return;  // Message stays in local DB and remains visible in chat\n'
    '        }\n'
)

count = 0
def replace_fn(m):
    global count
    count += 1
    return m.group(0) + inject_code

new_src = pattern.sub(replace_fn, src)
if count > 0:
    print(f'    Patched MessagesController.java ({count} overload(s) with anti-delete)')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(new_src)
else:
    print('    WARN: movedToScheduled overload not found — anti-delete NOT applied')
PY
    else
        echo "    MessagesController.java already patched"
    fi
else
    echo "    WARN: MessagesController.java not found"
fi

echo "  [AD] Done."
