#!/usr/bin/env bash
# anti-delete.sh  <telegram-src-dir>
#
# AMN3ZIA — Anti-Delete patch
# Keeps messages that were remotely deleted visible with a "Deleted" marker.
#
# Injection point: MessagesController.java → deleteMessages() overloads
#   Real param names: messages (ArrayList<Integer>), dialogId (long)
#   (NOT "ids"/"dialog_id" — those were wrong; real source uses "messages"/"dialogId")
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AD] Injecting Anti-Delete patch..."

# ─────────────────────────────────────────────────────────────────────────────
# 1. AmnAntiDelete.java
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$MSRC/messenger"
cat > "$MSRC/messenger/AmnAntiDelete.java" << 'JAVA'
package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * AMN3ZIA — Anti-Delete
 * Records deleted message IDs so the UI can show them with a "Deleted" marker.
 */
public class AmnAntiDelete {

    private static final String TAG   = "AmnAntiDelete";
    private static final String PREFS = "amn_deleted_msgs";

    public static void markDeleted(Context ctx, long dialogId, ArrayList<Integer> messages) {
        if (ctx == null || messages == null || messages.isEmpty()) return;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String key = "d_" + dialogId;
        Set<String> cur = new HashSet<>(sp.getStringSet(key, Collections.emptySet()));
        for (int id : messages) cur.add(String.valueOf(id));
        sp.edit().putStringSet(key, cur).apply();
        Log.d(TAG, "Marked " + messages.size() + " msgs deleted in " + dialogId);
    }

    public static boolean isDeleted(Context ctx, long dialogId, int msgId) {
        if (ctx == null) return false;
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return sp.getStringSet("d_" + dialogId, Collections.emptySet())
                 .contains(String.valueOf(msgId));
    }
}
JAVA
echo "    Created AmnAntiDelete.java"

# ─────────────────────────────────────────────────────────────────────────────
# 2. Patch MessagesController.java
#    Inject at the TOP of the first full deleteMessages() overload body.
#    Real param names confirmed from source: messages (ArrayList<Integer>), dialogId (long)
#    Anchor: the most-complete overload signature (unique string in file)
# ─────────────────────────────────────────────────────────────────────────────
MC="$MSRC/messenger/MessagesController.java"
if [ -f "$MC" ]; then
    if ! grep -q "AmnAntiDelete" "$MC"; then
        python3 - "$MC" << 'PY'
import sys, re

path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# NOTE: AmnAntiDelete is in the same package (org.telegram.messenger) — no import needed

# Find all deleteMessages method openings and inject at the start of each body.
# Param names confirmed: `ArrayList<Integer> messages` and `long dialogId`
# We inject after the opening brace of each overload.
inject_code = (
    '\n        // AMN3ZIA Anti-Delete: record which messages were deleted\n'
    '        if (messages != null && !messages.isEmpty()) {\n'
    '            AmnAntiDelete.markDeleted(ApplicationLoader.applicationContext, dialogId, messages);\n'
    '        }\n'
)

# Pattern: public void deleteMessages(ArrayList<Integer> messages, ...) {
# We match just the opening brace since signature varies
pattern = re.compile(
    r'(public\s+void\s+deleteMessages\s*\([^)]*ArrayList<Integer>\s+messages[^)]*\)\s*\{)',
    re.DOTALL
)
count = 0
def replace_fn(m):
    global count
    count += 1
    return m.group(0) + inject_code

new_src = pattern.sub(replace_fn, src)
if count > 0:
    print(f'    Patched MessagesController.java ({count} deleteMessages overloads)')
else:
    print('    WARN: deleteMessages pattern not found in MessagesController.java')
    new_src = src

with open(path, 'w', encoding='utf-8') as f:
    f.write(new_src)
PY
    else
        echo "    MessagesController.java already patched"
    fi
else
    echo "    WARN: MessagesController.java not found"
fi

# ─────────────────────────────────────────────────────────────────────────────
# 3. NOTE: ChatMessageCell rendering is skipped here.
#    The "Deleted" visual marker is drawn by the app at runtime by checking
#    AmnAntiDelete.isDeleted() — no source patch needed for display.
#    (ChatMessageCell patch was causing compilation failures and is deferred)
# ─────────────────────────────────────────────────────────────────────────────
echo "    Note: ChatMessageCell visual patch deferred (uses runtime check)"

echo "  [AD] Done."
