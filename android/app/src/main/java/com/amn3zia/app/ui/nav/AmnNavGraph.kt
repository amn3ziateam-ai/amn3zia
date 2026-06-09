package com.amn3zia.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amn3zia.app.ui.screens.auth.AuthScreen
import com.amn3zia.app.ui.screens.chat.ChatScreen
import com.amn3zia.app.ui.screens.chats.ChatListScreen
import com.amn3zia.app.ui.screens.fakeui.CalculatorScreen
import com.amn3zia.app.ui.screens.panic.PanicScreen
import com.amn3zia.app.ui.screens.settings.AppLockScreen
import com.amn3zia.app.ui.screens.settings.PrivacyDashboardScreen

object Routes {
    const val AUTH        = "auth"
    const val CHAT_LIST   = "chats"
    const val CHAT        = "chat/{chatId}"
    const val SETTINGS    = "settings"
    const val PANIC       = "panic"
    const val APP_LOCK    = "app_lock"
    const val CALCULATOR  = "calculator"

    fun chat(chatId: Long) = "chat/$chatId"
}

@Composable
fun AmnNavGraph(startDestination: String = Routes.AUTH) {
    val nav: NavHostController = rememberNavController()

    NavHost(navController = nav, startDestination = startDestination) {

        composable(Routes.AUTH) {
            AuthScreen(onAuthenticated = {
                nav.navigate(Routes.CHAT_LIST) {
                    popUpTo(Routes.AUTH) { inclusive = true }
                }
            })
        }

        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onOpenChat     = { chatId -> nav.navigate(Routes.chat(chatId)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                onOpenPanic    = { nav.navigate(Routes.PANIC) },
                onOpenHidden   = { /* TODO: HiddenChatsScreen */ },
            )
        }

        composable(Routes.CHAT) { back ->
            val chatId = back.arguments?.getString("chatId")?.toLongOrNull() ?: return@composable
            ChatScreen(chatId = chatId, onBack = { nav.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            PrivacyDashboardScreen(
                onBack        = { nav.popBackStack() },
                onOpenPanic   = { nav.navigate(Routes.PANIC) },
                onOpenAppLock = { nav.navigate(Routes.APP_LOCK) },
                onOpenFakeUi  = { nav.navigate(Routes.CALCULATOR) },
                onOpenProxy   = { /* TODO */ },
                onOpenAutoClean = { /* TODO */ },
                onOpenHidden  = { /* TODO */ },
            )
        }

        composable(Routes.PANIC) {
            PanicScreen(
                onBack  = { nav.popBackStack() },
                onWipe  = {
                    nav.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.APP_LOCK) {
            AppLockScreen(onBack = { nav.popBackStack() })
        }

        composable(Routes.CALCULATOR) {
            CalculatorScreen(onUnlock = {
                nav.navigate(Routes.CHAT_LIST) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
    }
}
