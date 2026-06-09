#!/usr/bin/env bash
# apply-icon.sh  <telegram-src-dir>
#
# Replaces Telegram app icons with AMN3ZIA logo at all required Android densities.
# Uses Python3 + Pillow (pre-installed on ubuntu-latest GitHub Actions runner).
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/ic_launcher_source.png"

if [ ! -f "$SRC" ]; then
  echo "  [ICON] SKIP: ic_launcher_source.png not found"
  exit 0
fi

echo "  [ICON] Installing AMN3ZIA launcher icon..."

# Ensure Pillow is available
python3 -c "from PIL import Image" 2>/dev/null || pip3 install Pillow -q

python3 - "$TG" "$SRC" << 'PY'
import sys, os
from PIL import Image

tg  = sys.argv[1]
src = sys.argv[2]

img = Image.open(src).convert("RGBA")

# Android mipmap density → icon size (px)
DENSITIES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

# Telegram keeps icons in TMessagesProj/src/main/res/
RES = os.path.join(tg, "TMessagesProj", "src", "main", "res")

placed = 0
for density, size in DENSITIES.items():
    folder = os.path.join(RES, density)
    if not os.path.isdir(folder):
        os.makedirs(folder, exist_ok=True)

    resized = img.resize((size, size), Image.LANCZOS)

    for name in ["ic_launcher.png", "ic_launcher_round.png",
                 "ic_launcher_foreground.png"]:
        out = os.path.join(folder, name)
        resized.save(out, "PNG", optimize=True)
        placed += 1

print(f"  Placed {placed} icon files across {len(DENSITIES)} densities")

# Also replace the notification / status-bar icon (white silhouette isn't needed
# but replace with a smaller version so it compiles without errors)
for density, size in DENSITIES.items():
    folder = os.path.join(RES, density)
    small = img.resize((size, size), Image.LANCZOS)
    for name in ["ic_notification.png", "ic_notification2.png"]:
        out = os.path.join(folder, name)
        if os.path.exists(out):
            small.save(out, "PNG", optimize=True)

print("  [ICON] Done.")
PY
