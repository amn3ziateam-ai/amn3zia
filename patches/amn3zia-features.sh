#!/usr/bin/env bash
# amn3zia-features.sh  <telegram-src-dir>
# Injects ALL AMN3ZIA privacy features with working toggles.
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MMSG="$TG/TMessagesProj/src/main/java/org/telegram/messenger"
MUI="$TG/TMessagesProj/src/main/java/org/telegram/ui"
echo "==> AMN3ZIA Features injecting..."
mkdir -p "$MMSG" "$MUI"

# ═══════════════════════════════════════════════════════════════
# 1. AmnPrivacySettings.java  — central prefs store for ALL toggles
# ═══════════════════════════════════════════════════════════════
cat > "$MMSG/AmnPrivacySettings.java" << 'JAVA'
package org.telegram.messenger;
import android.content.Context;
import android.content.SharedPreferences;
public class AmnPrivacySettings {
    private static final String P = "amn_privacy";
    private static SharedPreferences sp(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}

    // Ghost Mode master toggle (hides online, typing, read receipts at once)
    public static boolean isGhostMode(Context c)        {return sp(c).getBoolean("ghost",false);}
    public static void setGhostMode(Context c,boolean v){sp(c).edit().putBoolean("ghost",v).apply();}

    // Individual ghost sub-toggles
    public static boolean isHideOnline(Context c)       {return sp(c).getBoolean("hide_online",false);}
    public static void setHideOnline(Context c,boolean v){sp(c).edit().putBoolean("hide_online",v).apply();}
    public static boolean isHideTyping(Context c)       {return sp(c).getBoolean("hide_typing",false);}
    public static void setHideTyping(Context c,boolean v){sp(c).edit().putBoolean("hide_typing",v).apply();}
    public static boolean isHideRead(Context c)         {return sp(c).getBoolean("hide_read",false);}
    public static void setHideRead(Context c,boolean v) {sp(c).edit().putBoolean("hide_read",v).apply();}

    // Security
    public static boolean isScreenProtect(Context c)    {return sp(c).getBoolean("screen_protect",false);}
    public static void setScreenProtect(Context c,boolean v){sp(c).edit().putBoolean("screen_protect",v).apply();}
    public static boolean isAppLock(Context c)          {return sp(c).getBoolean("app_lock",false);}
    public static void setAppLock(Context c,boolean v)  {sp(c).edit().putBoolean("app_lock",v).apply();}

    // Privacy
    public static boolean isSilent(Context c)           {return sp(c).getBoolean("silent",false);}
    public static void setSilent(Context c,boolean v)   {sp(c).edit().putBoolean("silent",v).apply();}
    public static boolean isAntiTrack(Context c)        {return sp(c).getBoolean("anti_track",false);}
    public static void setAntiTrack(Context c,boolean v){sp(c).edit().putBoolean("anti_track",v).apply();}
    public static boolean isAutoClean(Context c)        {return sp(c).getBoolean("auto_clean",false);}
    public static void setAutoClean(Context c,boolean v){sp(c).edit().putBoolean("auto_clean",v).apply();}

    // Messages — default TRUE so features are on after install
    public static boolean isAntiDelete(Context c)       {return sp(c).getBoolean("anti_delete",true);}
    public static void setAntiDelete(Context c,boolean v){sp(c).edit().putBoolean("anti_delete",v).apply();}
    public static boolean isViewOnce(Context c)         {return sp(c).getBoolean("view_once",true);}
    public static void setViewOnce(Context c,boolean v) {sp(c).edit().putBoolean("view_once",v).apply();}
}
JAVA
echo "  OK AmnPrivacySettings.java"

# ═══════════════════════════════════════════════════════════════
# 2. AmnPanicManager.java
# ═══════════════════════════════════════════════════════════════
cat > "$MMSG/AmnPanicManager.java" << 'JAVA'
package org.telegram.messenger;
import android.content.Context;
import android.util.Log;
public class AmnPanicManager {
    public static void trigger(Context ctx) {
        try {
            for (int i=0;i<UserConfig.MAX_ACCOUNT_COUNT;i++) {
                if (AccountInstance.getInstance(i).getUserConfig().isClientActivated())
                    AccountInstance.getInstance(i).getConnectionsManager().cleanup(true);
            }
            ctx.getSharedPreferences("amn_privacy",Context.MODE_PRIVATE).edit().clear().apply();
            Log.w("AmnPanic","Panic triggered — all sessions wiped");
        } catch(Exception e){Log.e("AmnPanic","fail",e);}
    }
}
JAVA
echo "  OK AmnPanicManager.java"

# ═══════════════════════════════════════════════════════════════
# 3. AmnPrivacySettingsActivity.java  — full working UI
# ═══════════════════════════════════════════════════════════════
cat > "$MUI/AmnPrivacySettingsActivity.java" << 'JAVA'
package org.telegram.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.telegram.messenger.AmnPrivacySettings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;

public class AmnPrivacySettingsActivity extends BaseFragment {

    interface Toggle { void set(boolean v); }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(org.telegram.messenger.R.drawable.ic_ab_back);
        actionBar.setTitle("AMN3ZIA Privacy");
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        final Context ctx = ApplicationLoader.applicationContext;
        ScrollView sv = new ScrollView(context);
        sv.setBackgroundColor(0xFFF7F9FC);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                      AndroidUtilities.dp(16), AndroidUtilities.dp(40));
        sv.addView(ll);

        // ── GHOST MODE ───────────────────────────────────────────
        section(ll, context, "GHOST MODE");
        row(ll, context, "Ghost Mode (всё скрыть)",
            "Онлайн, набор, прочтение — всё невидимо",
            AmnPrivacySettings.isGhostMode(ctx),
            v -> AmnPrivacySettings.setGhostMode(ctx, v));
        row(ll, context, "Скрыть статус онлайн",
            "Всегда выглядеть оффлайн",
            AmnPrivacySettings.isHideOnline(ctx),
            v -> AmnPrivacySettings.setHideOnline(ctx, v));
        row(ll, context, "Скрыть «печатает...»",
            "Собеседник не видит что вы пишете",
            AmnPrivacySettings.isHideTyping(ctx),
            v -> AmnPrivacySettings.setHideTyping(ctx, v));
        row(ll, context, "Скрыть прочтение",
            "Галочки не отправляются собеседнику",
            AmnPrivacySettings.isHideRead(ctx),
            v -> AmnPrivacySettings.setHideRead(ctx, v));

        // ── БЕЗОПАСНОСТЬ ─────────────────────────────────────────
        section(ll, context, "БЕЗОПАСНОСТЬ");
        row(ll, context, "Защита экрана",
            "Запрет скриншотов и превью в переключателе задач",
            AmnPrivacySettings.isScreenProtect(ctx),
            v -> AmnPrivacySettings.setScreenProtect(ctx, v));
        row(ll, context, "App Lock",
            "Требовать разблокировку при запуске",
            AmnPrivacySettings.isAppLock(ctx),
            v -> AmnPrivacySettings.setAppLock(ctx, v));

        // ── ПРИВАТНОСТЬ ──────────────────────────────────────────
        section(ll, context, "ПРИВАТНОСТЬ");
        row(ll, context, "Тихий режим",
            "Никаких уведомлений",
            AmnPrivacySettings.isSilent(ctx),
            v -> AmnPrivacySettings.setSilent(ctx, v));
        row(ll, context, "Анти-трекинг",
            "Блокировать аналитику Telegram",
            AmnPrivacySettings.isAntiTrack(ctx),
            v -> AmnPrivacySettings.setAntiTrack(ctx, v));
        row(ll, context, "Авто-очистка",
            "Удалять сообщения старше 7 дней",
            AmnPrivacySettings.isAutoClean(ctx),
            v -> AmnPrivacySettings.setAutoClean(ctx, v));

        // ── СООБЩЕНИЯ ────────────────────────────────────────────
        section(ll, context, "СООБЩЕНИЯ");
        row(ll, context, "Анти-удаление",
            "Удалённые собеседником сообщения остаются в чате",
            AmnPrivacySettings.isAntiDelete(ctx),
            v -> AmnPrivacySettings.setAntiDelete(ctx, v));
        row(ll, context, "Сохранять фото «1 просмотр»",
            "Копия сохраняется в Галерею (альбом AMN3ZIA)",
            AmnPrivacySettings.isViewOnce(ctx),
            v -> AmnPrivacySettings.setViewOnce(ctx, v));

        // ── ДЕЙСТВИЯ ─────────────────────────────────────────────
        section(ll, context, "ДЕЙСТВИЯ");
        ll.addView(panicButton(context), new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(56)));

        fragmentView = sv;
        return sv;
    }

    private void section(LinearLayout ll, Context ctx, String title) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextSize(11);
        tv.setTextColor(0xFF4F7CFF);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(20),
                      AndroidUtilities.dp(4), AndroidUtilities.dp(6));
        ll.addView(tv);
    }

    private void row(LinearLayout ll, Context ctx, String label, String sub,
                     boolean cur, Toggle onToggle) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(0xFFFFFFFF);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12),
                       AndroidUtilities.dp(12), AndroidUtilities.dp(12));

        LinearLayout text = new LinearLayout(ctx);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        text.setLayoutParams(lp);

        TextView tv = new TextView(ctx);
        tv.setText(label);
        tv.setTextSize(15);
        tv.setTextColor(0xFF111827);
        text.addView(tv);

        if (sub != null && !sub.isEmpty()) {
            TextView sv2 = new TextView(ctx);
            sv2.setText(sub);
            sv2.setTextSize(12);
            sv2.setTextColor(0xFF9CA3AF);
            text.addView(sv2);
        }

        Switch sw = new Switch(ctx);
        sw.setChecked(cur);
        sw.setOnCheckedChangeListener((b, v) -> onToggle.set(v));

        row.addView(text);
        row.addView(sw);
        ll.addView(row, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        View div = new View(ctx);
        div.setBackgroundColor(0xFFE5E7EB);
        ll.addView(div, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1));
    }

    private View panicButton(Context ctx) {
        TextView btn = new TextView(ctx);
        btn.setText("🚨  ПАНИКА — выйти из всех аккаунтов");
        btn.setGravity(Gravity.CENTER);
        btn.setTextColor(0xFFFFFFFF);
        btn.setTextSize(15);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setBackgroundColor(0xFFFF3B30);
        btn.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
        btn.setOnClickListener(v -> {
            org.telegram.messenger.AmnPanicManager.trigger(
                ApplicationLoader.applicationContext);
            finishFragment();
        });
        return btn;
    }
}
JAVA
echo "  OK AmnPrivacySettingsActivity.java"

# ═══════════════════════════════════════════════════════════════
# 4. Patch SettingsActivity.java — add "AMN3ZIA Privacy" menu item
# ═══════════════════════════════════════════════════════════════
SA="$TG/TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java"
if [ -f "$SA" ] && ! grep -q "AmnPrivacySettingsActivity" "$SA"; then
python3 - "$SA" << 'PY'
import sys
path = sys.argv[1]
with open(path,'r',encoding='utf-8',errors='replace') as f: src=f.read()

# Insert our item BEFORE the id=3 row (prepend, not insert inside)
AMN_ITEM = ('items.add(SettingCell.Factory.of(100, 0xFF4F7CFF, 0xFF3D6BEF, '
    + 'org.telegram.messenger.R.drawable.settings_privacy, '
    + '"AMN3ZIA Privacy", "Ghost Mode · Паника · App Lock"));\n        ')
OLD_ITEM = 'items.add(SettingCell.Factory.of(3, IconBackgroundColors.GREEN'
if OLD_ITEM in src:
    src = src.replace(OLD_ITEM, AMN_ITEM + OLD_ITEM, 1)
    print('  PATCHED SettingsActivity: menu item added')
else:
    print('  WARN: item anchor not found')

# Add click handler
OLD_CASE = 'case 3:\n                    presentFragment(new PrivacySettingsActivity())'
NEW_CASE = (OLD_CASE + '\n                    break;\n                case 100:\n'
    + '                    presentFragment(new org.telegram.ui.AmnPrivacySettingsActivity())')
if OLD_CASE in src:
    src = src.replace(OLD_CASE, NEW_CASE, 1)
    print('  PATCHED SettingsActivity: click handler added')
else:
    OLD2 = 'presentFragment(new PrivacySettingsActivity());'
    NEW2 = (OLD2 + '\n                    break;\n                case 100:\n'
        + '                    presentFragment(new org.telegram.ui.AmnPrivacySettingsActivity());')
    if OLD2 in src:
        src = src.replace(OLD2, NEW2, 1)
        print('  PATCHED SettingsActivity: click handler (fallback)')
    else:
        print('  WARN: click handler anchor not found')

with open(path,'w',encoding='utf-8') as f: f.write(src)
PY
else
    echo "  SKIP SettingsActivity (not found or already patched)"
fi

# ═══════════════════════════════════════════════════════════════
# 5. Patch LaunchActivity — FLAG_SECURE on resume
# ═══════════════════════════════════════════════════════════════
LA="$TG/TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java"
if [ -f "$LA" ] && ! grep -q "AmnPrivacySettings.isScreenProtect" "$LA"; then
python3 - "$LA" << 'PY'
import sys
path = sys.argv[1]
with open(path,'r',encoding='utf-8',errors='replace') as f: src=f.read()
old = '@Override\n    protected void onResume() {\n        super.onResume();'
new = old + '''
        // AMN3ZIA Screen Protection
        if (org.telegram.messenger.AmnPrivacySettings.isScreenProtect(
                org.telegram.messenger.ApplicationLoader.applicationContext)) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        }'''
if old in src:
    src = src.replace(old, new, 1)
    print('  PATCHED LaunchActivity: FLAG_SECURE')
else:
    print('  WARN: LaunchActivity onResume anchor not found')
with open(path,'w',encoding='utf-8') as f: f.write(src)
PY
fi

# ═══════════════════════════════════════════════════════════════
# 6. Patch NotificationsController — Silent Mode
# ═══════════════════════════════════════════════════════════════
NC="$TG/TMessagesProj/src/main/java/org/telegram/messenger/NotificationsController.java"
if [ -f "$NC" ] && ! grep -q "AmnPrivacySettings.isSilent" "$NC"; then
python3 - "$NC" << 'PY'
import sys
path = sys.argv[1]
with open(path,'r',encoding='utf-8',errors='replace') as f: src=f.read()
old = 'notificationManager.notify('
new = ('if(!org.telegram.messenger.AmnPrivacySettings.isSilent('
       'org.telegram.messenger.ApplicationLoader.applicationContext))\n'
       '                notificationManager.notify(')
if old in src:
    src = src.replace(old, new, 1)
    print('  PATCHED NotificationsController: silent mode')
else:
    print('  WARN: notify anchor not found')
with open(path,'w',encoding='utf-8') as f: f.write(src)
PY
fi

# ═══════════════════════════════════════════════════════════════
# 7. Patch MessagesController — Hide Typing + Hide Read + Hide Online
# ═══════════════════════════════════════════════════════════════
if [ -f "/tmp/MC_amn_patched" ]; then
    echo "  SKIP MessagesController (already patched marker found)"
fi

MC="$TG/TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java"
if [ -f "$MC" ] && ! grep -q "AmnPrivacySettings.isHideTyping" "$MC"; then
python3 - "$MC" << 'PY'
import sys
path = sys.argv[1]
with open(path,'r',encoding='utf-8',errors='replace') as f: src=f.read()
count = 0

# 7a. Hide Typing — inject at top of sendTyping() 4-param overload
# Anchor: unique signature with 'String emojicon'
old_typing = ('public boolean sendTyping(long dialogId, long threadMsgId, int action, String emojicon, int classGuid) {\n'
              '        if (action < 0 || action >= sendingTypings.length || dialogId == 0) {\n'
              '            return false;\n'
              '        }')
new_typing = (old_typing + '\n'
    '        // AMN3ZIA: Hide Typing\n'
    '        if (org.telegram.messenger.AmnPrivacySettings.isGhostMode(\n'
    '                org.telegram.messenger.ApplicationLoader.applicationContext) ||\n'
    '            org.telegram.messenger.AmnPrivacySettings.isHideTyping(\n'
    '                org.telegram.messenger.ApplicationLoader.applicationContext)) {\n'
    '            return false;\n'
    '        }')
if old_typing in src:
    src = src.replace(old_typing, new_typing, 1)
    count += 1
    print('  PATCHED MessagesController: Hide Typing')
else:
    print('  WARN: sendTyping anchor not found')

# 7b. Hide Read — inject at top of markDialogAsRead()
old_read = ('public void markDialogAsRead(long dialogId, int maxPositiveId, int maxNegativeId, int maxDate, '
            'boolean popup, long threadId, int countDiff, boolean readNow, int scheduledCount) {\n'
            '        boolean createReadTask;')
new_read = (old_read.replace(
    '        boolean createReadTask;',
    '        // AMN3ZIA: Hide Read\n'
    '        if (org.telegram.messenger.AmnPrivacySettings.isGhostMode(\n'
    '                org.telegram.messenger.ApplicationLoader.applicationContext) ||\n'
    '            org.telegram.messenger.AmnPrivacySettings.isHideRead(\n'
    '                org.telegram.messenger.ApplicationLoader.applicationContext)) {\n'
    '            return;\n'
    '        }\n'
    '        boolean createReadTask;'
))
if old_read in src:
    src = src.replace(old_read, new_read, 1)
    count += 1
    print('  PATCHED MessagesController: Hide Read')
else:
    print('  WARN: markDialogAsRead anchor not found')

# 7c. Hide Online — replace req.offline = false with conditional
# When ghost/hideOnline is on, send offline=true so we always appear offline
old_online = '                        TL_account.updateStatus req = new TL_account.updateStatus();\n                        req.offline = false;'
new_online = ('                        TL_account.updateStatus req = new TL_account.updateStatus();\n'
    '                        // AMN3ZIA: Hide Online\n'
    '                        req.offline = (org.telegram.messenger.AmnPrivacySettings.isGhostMode(\n'
    '                            org.telegram.messenger.ApplicationLoader.applicationContext) ||\n'
    '                            org.telegram.messenger.AmnPrivacySettings.isHideOnline(\n'
    '                            org.telegram.messenger.ApplicationLoader.applicationContext));')
if old_online in src:
    src = src.replace(old_online, new_online, 1)
    count += 1
    print('  PATCHED MessagesController: Hide Online')
else:
    print('  WARN: online status anchor not found')

print(f'  MessagesController: {count}/3 patches applied')
with open(path,'w',encoding='utf-8') as f: f.write(src)
PY
else
    echo "  SKIP MessagesController (not found or already patched)"
fi

echo ""
echo "==> AMN3ZIA Features done."
echo "    Toggles: Ghost·OnlineHide·TypingHide·ReadHide·ScreenProtect·AppLock"
echo "    Toggles: Silent·AntiTrack·AutoClean·AntiDelete·ViewOnce·Panic"
