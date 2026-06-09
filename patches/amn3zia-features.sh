#!/usr/bin/env bash
# amn3zia-features.sh  <telegram-src-dir>
# Injects AMN3ZIA privacy features into Telegram fork.
set -euo pipefail

TG="${1:-}"
[ -z "$TG" ] || [ ! -d "$TG" ] && { echo "Usage: $0 <telegram-src-dir>"; exit 1; }

MMSG="$TG/TMessagesProj/src/main/java/org/telegram/messenger"
MUI="$TG/TMessagesProj/src/main/java/org/telegram/ui"
echo "==> AMN3ZIA Features injecting..."

mkdir -p "$MMSG" "$MUI"

# ═══════════════════════════════════════════════════════════════
# 1. AmnPrivacySettings.java  (central prefs store)
# ═══════════════════════════════════════════════════════════════
cat > "$MMSG/AmnPrivacySettings.java" << 'JAVA'
package org.telegram.messenger;
import android.content.Context;
import android.content.SharedPreferences;
public class AmnPrivacySettings {
    private static final String P = "amn_privacy";
    private static SharedPreferences sp(Context c){return c.getSharedPreferences(P,Context.MODE_PRIVATE);}
    public static boolean isGhostMode(Context c)       {return sp(c).getBoolean("ghost",false);}
    public static void    setGhostMode(Context c,boolean v){sp(c).edit().putBoolean("ghost",v).apply();}
    public static boolean isHideOnline(Context c)      {return sp(c).getBoolean("hide_online",false);}
    public static void    setHideOnline(Context c,boolean v){sp(c).edit().putBoolean("hide_online",v).apply();}
    public static boolean isHideTyping(Context c)      {return sp(c).getBoolean("hide_typing",false);}
    public static void    setHideTyping(Context c,boolean v){sp(c).edit().putBoolean("hide_typing",v).apply();}
    public static boolean isHideRead(Context c)        {return sp(c).getBoolean("hide_read",false);}
    public static void    setHideRead(Context c,boolean v){sp(c).edit().putBoolean("hide_read",v).apply();}
    public static boolean isScreenProtect(Context c)   {return sp(c).getBoolean("screen_protect",false);}
    public static void    setScreenProtect(Context c,boolean v){sp(c).edit().putBoolean("screen_protect",v).apply();}
    public static boolean isAppLock(Context c)         {return sp(c).getBoolean("app_lock",false);}
    public static void    setAppLock(Context c,boolean v){sp(c).edit().putBoolean("app_lock",v).apply();}
    public static boolean isPanic(Context c)           {return sp(c).getBoolean("panic",false);}
    public static void    setPanic(Context c,boolean v){sp(c).edit().putBoolean("panic",v).apply();}
    public static boolean isSilent(Context c)          {return sp(c).getBoolean("silent",false);}
    public static void    setSilent(Context c,boolean v){sp(c).edit().putBoolean("silent",v).apply();}
    public static boolean isAntiTrack(Context c)       {return sp(c).getBoolean("anti_track",false);}
    public static void    setAntiTrack(Context c,boolean v){sp(c).edit().putBoolean("anti_track",v).apply();}
    public static boolean isAutoClean(Context c)       {return sp(c).getBoolean("auto_clean",false);}
    public static void    setAutoClean(Context c,boolean v){sp(c).edit().putBoolean("auto_clean",v).apply();}
    public static int     getSendDelay(Context c)      {return sp(c).getInt("send_delay",0);}
    public static void    setSendDelay(Context c,int v){sp(c).edit().putInt("send_delay",v).apply();}
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
            Log.w("AmnPanic","Panic triggered");
        } catch(Exception e){Log.e("AmnPanic","fail",e);}
    }
}
JAVA
echo "  OK AmnPanicManager.java"

# ═══════════════════════════════════════════════════════════════
# 3. AmnPrivacySettingsActivity.java  (Privacy Dashboard UI)
# ═══════════════════════════════════════════════════════════════
cat > "$MUI/AmnPrivacySettingsActivity.java" << 'JAVA'
package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import org.telegram.messenger.AmnPrivacySettings;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;

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

        Context ctx = ApplicationLoader.applicationContext;
        ScrollView sv = new ScrollView(context);
        sv.setBackgroundColor(0xFFF7F9FC);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8),
                      AndroidUtilities.dp(16), AndroidUtilities.dp(40));
        sv.addView(ll);

        // ── Ghost Mode section ──────────────────────────────────────
        section(ll, context, "GHOST MODE");
        row(ll, context, "Ghost Mode (всё скрыть)",
            "Онлайн, набор, прочтение — всё невидимо",
            AmnPrivacySettings.isGhostMode(ctx), v -> AmnPrivacySettings.setGhostMode(ctx, v));
        row(ll, context, "Скрыть статус онлайн", null,
            AmnPrivacySettings.isHideOnline(ctx), v -> AmnPrivacySettings.setHideOnline(ctx, v));
        row(ll, context, "Скрыть «печатает...»", null,
            AmnPrivacySettings.isHideTyping(ctx), v -> AmnPrivacySettings.setHideTyping(ctx, v));
        row(ll, context, "Скрыть прочтение", null,
            AmnPrivacySettings.isHideRead(ctx), v -> AmnPrivacySettings.setHideRead(ctx, v));

        // ── Security section ─────────────────────────────────────────
        section(ll, context, "БЕЗОПАСНОСТЬ");
        row(ll, context, "Защита экрана",
            "Запрет скриншотов и показа в переключателе задач",
            AmnPrivacySettings.isScreenProtect(ctx), v -> AmnPrivacySettings.setScreenProtect(ctx, v));
        row(ll, context, "App Lock (PIN)",
            "Блокировать приложение при сворачивании",
            AmnPrivacySettings.isAppLock(ctx), v -> AmnPrivacySettings.setAppLock(ctx, v));
        row(ll, context, "Кнопка паники",
            "Красная кнопка → мгновенный выход из всех аккаунтов",
            AmnPrivacySettings.isPanic(ctx), v -> AmnPrivacySettings.setPanic(ctx, v));

        // ── Privacy section ──────────────────────────────────────────
        section(ll, context, "ПРИВАТНОСТЬ");
        row(ll, context, "Тихий режим",
            "Никаких уведомлений в режиме невидимки",
            AmnPrivacySettings.isSilent(ctx), v -> AmnPrivacySettings.setSilent(ctx, v));
        row(ll, context, "Анти-трекинг",
            "Блокировать отправку аналитики Telegram",
            AmnPrivacySettings.isAntiTrack(ctx), v -> AmnPrivacySettings.setAntiTrack(ctx, v));
        row(ll, context, "Авто-очистка",
            "Удалять сообщения старше 7 дней",
            AmnPrivacySettings.isAutoClean(ctx), v -> AmnPrivacySettings.setAutoClean(ctx, v));

        // ── Messages section ─────────────────────────────────────────
        section(ll, context, "СООБЩЕНИЯ");
        row(ll, context, "Анти-удаление",
            "Удалённые собеседником сообщения остаются с меткой ✕",
            false, v -> {});  // enabled by anti-delete.sh patch always
        row(ll, context, "Сохранять фото «1 просмотр»",
            "Копия сохраняется до самоудаления",
            false, v -> {});  // enabled by anti-view-once.sh patch always

        // ── Panic button ─────────────────────────────────────────────
        section(ll, context, "ДЕЙСТВИЯ");
        View panicBtn = panicButton(context);
        ll.addView(panicBtn, new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(52)));

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
        row.setGravity(Gravity.CENTER_VERTICAL);
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

        // divider
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

# 4a. Add menu item after the Privacy & Security row (id=3)
OLD_ITEM = 'items.add(SettingCell.Factory.of(3, IconBackgroundColors.GREEN'
NEW_ITEM = (OLD_ITEM + '\n        '
    + 'items.add(SettingCell.Factory.of(100, 0xFF4F7CFF, 0xFF3D6BEF, '
    + 'org.telegram.messenger.R.drawable.settings_privacy, '
    + '"AMN3ZIA Privacy", "Ghost Mode · Паника · App Lock"));')
if OLD_ITEM in src:
    src = src.replace(OLD_ITEM, NEW_ITEM, 1)
    print('  PATCHED SettingsActivity: menu item added')
else:
    print('  WARN: settings item anchor not found')

# 4b. Add click handler case 100
OLD_CASE = 'case 3:\n                    presentFragment(new PrivacySettingsActivity())'
NEW_CASE = (OLD_CASE + '\n                    break;\n                case 100:\n'
    + '                    presentFragment(new org.telegram.ui.AmnPrivacySettingsActivity())')
if OLD_CASE in src:
    src = src.replace(OLD_CASE, NEW_CASE, 1)
    print('  PATCHED SettingsActivity: click handler added')
else:
    # fallback: try without break
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
       'org.telegram.messenger.ApplicationLoader.applicationContext))'
       '\n                notificationManager.notify(')
if old in src:
    src = src.replace(old, new, 1)
    print('  PATCHED NotificationsController: silent mode')
else:
    print('  WARN: notify anchor not found')
with open(path,'w',encoding='utf-8') as f: f.write(src)
PY
fi

echo ""
echo "==> AMN3ZIA Features done."
echo "    Entry: Telegram → Settings → AMN3ZIA Privacy"
