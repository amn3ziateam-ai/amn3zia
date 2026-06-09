#!/usr/bin/env bash
# anti-delete.sh  <telegram-src-dir>
#
# AMN3ZIA Anti-Delete v3
# Patches MessagesStorage.markMessagesAsDeleted() — the REAL deletion path.
# When AmnPrivacySettings.isAntiDelete() is true, returns early without
# deleting anything from the local SQLite database. Messages stay visible.
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MSRC="$TG/TMessagesProj/src/main/java/org/telegram"
echo "  [AD] Injecting Anti-Delete v3..."

# ─────────────────────────────────────────────────────────────────────────────
# AmnAntiDelete.java — minimal helper (real work done by settings check)
# ─────────────────────────────────────────────────────────────────────────────
mkdir -p "$MSRC/messenger"
cat > "$MSRC/messenger/AmnAntiDelete.java" << 'JAVA'
package org.telegram.messenger;
/** AMN3ZIA Anti-Delete — see AmnPrivacySettings.isAntiDelete() */
public class AmnAntiDelete { }
JAVA

# ─────────────────────────────────────────────────────────────────────────────
# Patch MessagesStorage.markMessagesAsDeleted()
# The public 6-param overload is called from MessagesController for ALL
# message deletions (user-initiated AND server-pushed). When anti-delete
# is ON we return an empty list — messages stay in SQLite, stay visible.
#
# Signature confirmed from commit 9fea726:
#   public ArrayList<Long> markMessagesAsDeleted(long dialogId,
#       ArrayList<Integer> messages, boolean useQueue, boolean deleteFiles,
#       int mode, int topicId)
# ─────────────────────────────────────────────────────────────────────────────
MS="$MSRC/messenger/MessagesStorage.java"
if [ -f "$MS" ]; then
    if ! grep -q "AmnPrivacySettings.isAntiDelete" "$MS"; then
        python3 - "$MS" << 'PY'
import sys
path = sys.argv[1]
with open(path, 'r', encoding='utf-8') as f:
    src = f.read()

# Anchor: the public 6-param markMessagesAsDeleted overload
# Must match exactly, then inject after the isEmpty() guard.
old = (
    '    public ArrayList<Long> markMessagesAsDeleted(long dialogId, ArrayList<Integer> messages, '
    'boolean useQueue, boolean deleteFiles, int mode, int topicId) {\n'
    '        if (messages.isEmpty()) {\n'
    '            return null;\n'
    '        }'
)
new = (
    '    public ArrayList<Long> markMessagesAsDeleted(long dialogId, ArrayList<Integer> messages, '
    'boolean useQueue, boolean deleteFiles, int mode, int topicId) {\n'
    '        if (messages.isEmpty()) {\n'
    '            return null;\n'
    '        }\n'
    '        // AMN3ZIA Anti-Delete: keep messages in local DB when toggle is ON\n'
    '        if (AmnPrivacySettings.isAntiDelete(ApplicationLoader.applicationContext)) {\n'
    '            return new java.util.ArrayList<>();\n'
    '        }'
)

if old in src:
    src = src.replace(old, new, 1)
    print('    Patched MessagesStorage.markMessagesAsDeleted (anti-delete v3)')
    with open(path, 'w', encoding='utf-8') as f:
        f.write(src)
else:
    # Try to find it with flexible whitespace
    import re
    pattern = re.compile(
        r'(public\s+ArrayList<Long>\s+markMessagesAsDeleted\s*\(\s*long\s+dialogId\s*,\s*'
        r'ArrayList<Integer>\s+messages\s*,\s*boolean\s+useQueue\s*,\s*boolean\s+deleteFiles\s*,\s*'
        r'int\s+mode\s*,\s*int\s+topicId\s*\)\s*\{[^}]*?if\s*\(messages\.isEmpty\(\)\)\s*\{[^}]*?\})'
    )
    m = pattern.search(src)
    if m:
        orig = m.group(0)
        replacement = orig + (
            '\n        // AMN3ZIA Anti-Delete\n'
            '        if (AmnPrivacySettings.isAntiDelete(ApplicationLoader.applicationContext)) {\n'
            '            return new java.util.ArrayList<>();\n'
            '        }'
        )
        src = src.replace(orig, replacement, 1)
        print('    Patched MessagesStorage.markMessagesAsDeleted (regex fallback)')
        with open(path, 'w', encoding='utf-8') as f:
            f.write(src)
    else:
        print('    WARN: markMessagesAsDeleted anchor not found — anti-delete NOT applied')
PY
    else
        echo "    MessagesStorage already patched"
    fi
else
    echo "    WARN: MessagesStorage.java not found"
fi

echo "  [AD] Done."
