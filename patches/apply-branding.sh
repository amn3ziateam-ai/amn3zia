#!/usr/bin/env bash
# apply-branding.sh  <telegram-src-dir>  [api_id]  [api_hash]
#
# Patches an official Telegram Android checkout with AMN3ZIA branding:
#   - App name: "Telegram" → "AMN3ZIA"
#   - Accent colour: Telegram blues → #4F7CFF
#   - API credentials written to build config
set -euo pipefail

TG="${1:-}"
API_ID="${2:-}"
API_HASH="${3:-}"

if [ -z "$TG" ] || [ ! -d "$TG" ]; then
  echo "Usage: $0 <telegram-src-dir> [api_id] [api_hash]"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "==> AMN3ZIA branding: patching $TG"

# ─────────────────────────────────────────────────────────────────────────────
# Helper: in-place sed (portable, handles both GNU and macOS)
# ─────────────────────────────────────────────────────────────────────────────
inplace() {
  # $1 = pattern, $2 = file
  sed -i "s|${1}|${2}|g" "${3}" 2>/dev/null || true
}

# ─────────────────────────────────────────────────────────────────────────────
# 1. API CREDENTIALS
# ─────────────────────────────────────────────────────────────────────────────
echo "  [1/4] API credentials..."
if [ -n "$API_ID" ] && [ -n "$API_HASH" ]; then
  # Newer Telegram builds read from gradle local.properties
  {
    echo ""
    echo "APP_ID=$API_ID"
    echo "APP_HASH=$API_HASH"
  } >> "$TG/local.properties" 2>/dev/null || true

  # BuildVars.java style
  # IMPORTANT: match "public static int APP_ID" exactly — do NOT match HUAWEI_APP_ID etc.
  BV="$TG/TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
  if [ -f "$BV" ]; then
    sed -i "s/public static int APP_ID = [0-9]*/public static int APP_ID = $API_ID/" "$BV" 2>/dev/null || true
    sed -i "s/public static String APP_HASH = \"[^\"]*\"/public static String APP_HASH = \"$API_HASH\"/" "$BV" 2>/dev/null || true
    echo "    Patched BuildVars.java"
  fi

  # config.h style (JNI builds)
  CONFIG="$TG/TMessagesProj/jni/config.h"
  if [ -f "$CONFIG" ]; then
    sed -i "s/define APP_ID [0-9]*/define APP_ID $API_ID/" "$CONFIG" 2>/dev/null || true
    sed -i "s/define APP_HASH \"[^\"]*\"/define APP_HASH \"$API_HASH\"/" "$CONFIG" 2>/dev/null || true
    echo "    Patched config.h"
  fi
else
  echo "    (skipped — no credentials provided)"
fi

# ─────────────────────────────────────────────────────────────────────────────
# 2. APP NAME  Telegram → AMN3ZIA
# ─────────────────────────────────────────────────────────────────────────────
echo "  [2/4] App name..."

# strings.xml files (all locales)
# NOTE: Telegram debug build uses @string/AppNameBeta (= "Telegram Beta").
# We replace ALL variants: AppName, AppNameBeta, app_name.
while IFS= read -r -d '' f; do
  sed -i 's|<string name="AppName">Telegram</string>|<string name="AppName">AMN3ZIA</string>|g'                  "$f" 2>/dev/null || true
  sed -i 's|<string name="AppNameBeta">Telegram Beta</string>|<string name="AppNameBeta">AMN3ZIA</string>|g'     "$f" 2>/dev/null || true
  sed -i 's|<string name="app_name">Telegram</string>|<string name="app_name">AMN3ZIA</string>|g'                "$f" 2>/dev/null || true
  sed -i 's|<string name="app_name">Telegram X</string>|<string name="app_name">AMN3ZIA</string>|g'              "$f" 2>/dev/null || true
done < <(find "$TG" -path "*/res/values*/strings.xml" -print0 2>/dev/null)

# AndroidManifest — replace any hardcoded label AND string-ref labels
while IFS= read -r -d '' f; do
  sed -i 's|android:label="Telegram"|android:label="AMN3ZIA"|g'             "$f" 2>/dev/null || true
  sed -i 's|android:label="@string/AppName"|android:label="AMN3ZIA"|g'      "$f" 2>/dev/null || true
  sed -i 's|android:label="@string/AppNameBeta"|android:label="AMN3ZIA"|g'  "$f" 2>/dev/null || true
  sed -i 's|android:label="@string/app_name"|android:label="AMN3ZIA"|g'     "$f" 2>/dev/null || true
done < <(find "$TG" -name "AndroidManifest.xml" -print0 2>/dev/null)

echo "    Done."

# ─────────────────────────────────────────────────────────────────────────────
# 3. ACCENT COLOUR
# Replace known Telegram blue hex values with AMN3ZIA blue #4F7CFF
# We cover the most common integer literals found in Theme.java
# ─────────────────────────────────────────────────────────────────────────────
echo "  [3/4] Accent colour..."

# Build a list of (old, new) sed substitutions
do_color_replace() {
  local OLD="$1"
  local NEW="4f7cff"
  # Java/Kotlin int literals: 0xff2ca5e0 → 0xff4f7cff
  while IFS= read -r -d '' f; do
    sed -i "s/0xff${OLD}/0xff${NEW}/g" "$f" 2>/dev/null || true
    sed -i "s/0xFF${OLD}/0xFF${NEW}/g" "$f" 2>/dev/null || true
  done < <(find "$TG" \( -name "*.java" -o -name "*.kt" \) -print0 2>/dev/null)
  # XML colour strings
  while IFS= read -r -d '' f; do
    sed -i "s/#${OLD}/#4F7CFF/g" "$f" 2>/dev/null || true
    sed -i "s/FF${OLD}/FF4F7CFF/g" "$f" 2>/dev/null || true
  done < <(find "$TG" -name "*.xml" -print0 2>/dev/null)
}

do_color_replace "2ca5e0"
do_color_replace "2CA5E0"
do_color_replace "1da1f2"
do_color_replace "1DA1F2"
do_color_replace "2aabee"
do_color_replace "2AABEE"
do_color_replace "179cde"
do_color_replace "179CDE"

echo "    Done."

# ─────────────────────────────────────────────────────────────────────────────
# 4. COPY ASSETS
# ─────────────────────────────────────────────────────────────────────────────
echo "  [4/4] Assets..."
ASSETS="$TG/TMessagesProj/src/main/assets"
mkdir -p "$ASSETS"
if [ -f "$SCRIPT_DIR/amn3zia.attheme" ]; then
  cp "$SCRIPT_DIR/amn3zia.attheme" "$ASSETS/amn3zia.attheme"
  echo "    Copied amn3zia.attheme"
fi

# ─────────────────────────────────────────────────────────────────────────────
# 5. AMN3ZIA FEATURE PATCHES
# ─────────────────────────────────────────────────────────────────────────────
echo "  [5/6] AMN3ZIA launcher icon..."
if [ -f "$SCRIPT_DIR/apply-icon.sh" ]; then
  bash "$SCRIPT_DIR/apply-icon.sh" "$TG" || echo "    WARN: apply-icon.sh failed (non-fatal)"
else
  echo "    WARN: apply-icon.sh not found"
fi

echo "  [6/6] AMN3ZIA feature patches..."

# Main features patch (Ghost Mode, Panic, App Lock, Screen Protect, etc.)
if [ -f "$SCRIPT_DIR/amn3zia-features.sh" ]; then
  bash "$SCRIPT_DIR/amn3zia-features.sh" "$TG" || echo "    WARN: amn3zia-features.sh failed (non-fatal)"
else
  echo "    WARN: amn3zia-features.sh not found"
fi

# View Once bypass (save TTL photos/videos before self-destruct)
if [ -f "$SCRIPT_DIR/anti-view-once.sh" ]; then
  bash "$SCRIPT_DIR/anti-view-once.sh" "$TG" || echo "    WARN: anti-view-once.sh failed (non-fatal)"
else
  echo "    WARN: anti-view-once.sh not found"
fi

# Anti-delete (keep deleted messages visible with "Deleted" marker)
if [ -f "$SCRIPT_DIR/anti-delete.sh" ]; then
  bash "$SCRIPT_DIR/anti-delete.sh" "$TG" || echo "    WARN: anti-delete.sh failed (non-fatal)"
else
  echo "    WARN: anti-delete.sh not found"
fi

echo ""
echo "==> Done. App: AMN3ZIA | Accent: #4F7CFF | API ID: ${API_ID:-(not set)}"
# retrigger 20260609183834
