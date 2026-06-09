#!/usr/bin/env bash
# apply-branding.sh  <telegram-src-dir>  <api_id>  <api_hash>
#
# Transforms an official Telegram Android checkout into AMN3ZIA by:
#   1. Renaming app strings ("Telegram" → "AMN3ZIA")
#   2. Setting the API credentials
#   3. Replacing the primary accent colour with AMN3ZIA blue (#4F7CFF)
#   4. Bundling the custom AMN3ZIA splash / launcher icons
#
# All changes are pure text patches — no Telegram logic is altered.
set -euo pipefail

TG="$1"
API_ID="${2:-}"
API_HASH="${3:-}"

if [ ! -d "$TG" ]; then
  echo "ERROR: Telegram source directory not found: $TG"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
echo "==> AMN3ZIA branding patch starting (tg-src: $TG)"

# ─────────────────────────────────────────────────────────────────────────────
# 1. APP CREDENTIALS
# ─────────────────────────────────────────────────────────────────────────────
if [ -n "$API_ID" ] && [ -n "$API_HASH" ]; then
  echo "  [1/5] Writing API credentials..."
  # Telegram reads APP_ID/APP_HASH from jni/config.h in some versions,
  # or from BuildConfig fields. We cover both.

  # jni config (older style)
  CONFIG_H="$TG/TMessagesProj/jni/config.h"
  if [ -f "$CONFIG_H" ]; then
    sed -i "s/define APP_ID [0-9]*/define APP_ID $API_ID/" "$CONFIG_H"
    sed -i "s/define APP_HASH \"[^\"]*\"/define APP_HASH \"$API_HASH\"/" "$CONFIG_H"
    echo "    Patched $CONFIG_H"
  fi

  # Gradle properties style (newer builds expose via BuildConfig)
  LOCAL_PROPS="$TG/local.properties"
  {
    echo "APP_ID=$API_ID"
    echo "APP_HASH=$API_HASH"
  } >> "$LOCAL_PROPS"
  echo "    Wrote $LOCAL_PROPS"

  # ApplicationLoader / BuildVars style fallback
  BUILDVARS="$TG/TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
  if [ -f "$BUILDVARS" ]; then
    sed -i "s/APP_ID = [0-9]*/APP_ID = $API_ID/" "$BUILDVARS"
    sed -i "s/APP_HASH = \"[^\"]*\"/APP_HASH = \"$API_HASH\"/" "$BUILDVARS"
    echo "    Patched BuildVars.java"
  fi
else
  echo "  [1/5] No API credentials provided — skipping"
fi

# ─────────────────────────────────────────────────────────────────────────────
# 2. APP NAME  (Telegram → AMN3ZIA)
# ─────────────────────────────────────────────────────────────────────────────
echo "  [2/5] Renaming app strings..."

# All localised strings.xml files
find "$TG" -path "*/res/values*/strings.xml" | while read -r f; do
  sed -i \
    -e 's|<string name="app_name">Telegram</string>|<string name="app_name">AMN3ZIA</string>|g' \
    -e 's|<string name="app_name">Telegram X</string>|<string name="app_name">AMN3ZIA</string>|g' \
    -e 's|>Telegram Desktop<|>AMN3ZIA Desktop<|g' \
    -e 's|Telegram for Android|AMN3ZIA Messenger|g' \
    "$f"
done

# AndroidManifest app label
find "$TG" -name "AndroidManifest.xml" | while read -r f; do
  sed -i \
    -e 's|android:label="Telegram"|android:label="AMN3ZIA"|g' \
    -e 's|android:label="@string/app_name"|android:label="AMN3ZIA"|g' \
    "$f"
done

# Notification channel names that say "Telegram"
find "$TG" -name "*.java" -o -name "*.kt" | xargs grep -l '"Telegram"' 2>/dev/null | while read -r f; do
  sed -i 's|"Telegram"|"AMN3ZIA"|g' "$f"
done

echo "    Done."

# ─────────────────────────────────────────────────────────────────────────────
# 3. ACCENT COLOUR  (#2ca5e0 / #1da1f2 / #2AABEE → #4F7CFF)
# ─────────────────────────────────────────────────────────────────────────────
echo "  [3/5] Replacing accent colour..."

# Telegram blue variants used across Theme.java, XML colours, etc.
TG_BLUES=(
  "2ca5e0" "2CA5E0"
  "1da1f2" "1DA1F2"
  "2aabee" "2AABEE"
  "179cde" "179CDE"
  "1c93d4" "1C93D4"
  "31a7db" "31A7DB"
  "2f9fe6" "2F9FE6"
  "40a7e3" "40A7E3"
)

AMN_BLUE="4F7CFF"

for BLUE in "${TG_BLUES[@]}"; do
  # 0xffXXXXXX integer literals (Theme.java)
  find "$TG" -name "*.java" -o -name "*.kt" | \
    xargs -r sed -i "s/0xff${BLUE}/0xff${AMN_BLUE}/gI" 2>/dev/null || true
  find "$TG" -name "*.java" -o -name "*.kt" | \
    xargs -r sed -i "s/0xFF${BLUE}/0xFF${AMN_BLUE}/gI" 2>/dev/null || true

  # XML colour values
  find "$TG" -name "*.xml" | \
    xargs -r sed -i "s/#${BLUE}/#${AMN_BLUE}/gI" 2>/dev/null || true
  find "$TG" -name "*.xml" | \
    xargs -r sed -i "s/FF${BLUE}/FF${AMN_BLUE}/gI" 2>/dev/null || true
done

echo "    Done."

# ─────────────────────────────────────────────────────────────────────────────
# 4. PACKAGE NAME  (optional — keeps org.telegram.messenger as deep-link base
#    but registers AMN3ZIA as display name)
# ─────────────────────────────────────────────────────────────────────────────
echo "  [4/5] Package name — keeping org.telegram.messenger (safe default)."
# To change the package name, uncomment and adjust:
# find "$TG" -name "*.java" -o -name "*.kt" -o -name "*.xml" | \
#   xargs -r sed -i 's|org\.telegram\.messenger|com.amn3zia.messenger|g'

# ─────────────────────────────────────────────────────────────────────────────
# 5. INJECT AMN3ZIA SPLASH / THEME
# ─────────────────────────────────────────────────────────────────────────────
echo "  [5/5] Injecting AMN3ZIA theme asset..."

ASSETS_DIR="$TG/TMessagesProj/src/main/assets"
mkdir -p "$ASSETS_DIR"

# Copy our pre-built theme if it exists
if [ -f "$SCRIPT_DIR/amn3zia.attheme" ]; then
  cp "$SCRIPT_DIR/amn3zia.attheme" "$ASSETS_DIR/amn3zia.attheme"
  echo "    Copied amn3zia.attheme → assets/"
fi

echo ""
echo "==> AMN3ZIA branding patch complete."
echo "    App name : AMN3ZIA"
echo "    Accent   : #4F7CFF"
echo "    API ID   : ${API_ID:-(not set)}"
