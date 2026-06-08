package com.amn3zia.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.amn3zia.app.ui.screens.auth.AuthScreen
import com.amn3zia.app.ui.screens.chat.ChatScreen
import com.amn3zia.app.ui.screens.chats.ChatListScreen
import com.amn3zia.app.ui.screens.panic.PanicButtonScreen
import com.amn3zia.app.ui.screens.settings.PrivacyDashboardScreen

object Routes {
    const val AUTH = "auth"
    const val CHAT_LIST = "chats"
    const val CHAT = "chat/{chatId}"
    const val SETTINGS = "settings"
    const val PANIC = "panic"

    fun chat(chatId: Long) = "chat/$chatId"
}

@Composable
fun AmnNavGraph(startDestination: String = Routes.AUTH) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.AUTH) {
            AuthScreen(onAuthenticated = {
                navController.navigate(Routes.CHAT_LIST) {
                    popUpTo(Routes.AUTH) { inclusive = true }
                }
            })
        }
        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                onOpenChat = { chatId -> navController.navigate(Routes.chat(chatId)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPanic = { navController.navigate(Routes.PANIC) },
            )
        }
        composable(Routes.CHAT) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")?.toLongOrNull() ?: return@composable
            ChatScreen(chatId = chatId, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            PrivacyDashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenPanic = { navController.navigate(Routes.PANIC) },
            )
        }
        composable(Routes.PANIC) {
            PanicButtonScreen(onWipeComplete = {
                navController.navigate(Routes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            })
        }
    }
}
