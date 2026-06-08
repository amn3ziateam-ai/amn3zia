package com.amn3zia.app.core.privacy

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.amn3zia.app.AmnApplication

/** Executes a single TIMER auto-clean pass; scheduled periodically by [AutoCleanManager]. */
class AutoCleanWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val accountId = inputData.getString(KEY_ACCOUNT_ID) ?: return Result.failure()
        val chatId = inputData.getLong(KEY_CHAT_ID, -1L).takeIf { it != -1L }
        val olderThanHours = inputData.getInt(KEY_OLDER_THAN_HOURS, 24)
        val mediaOnly = inputData.getBoolean(KEY_MEDIA_ONLY, false)

        val app = AmnApplication.from(applicationContext)
        app.autoClean.runCleanForRule(
            AutoCleanRule(
                accountId = accountId,
                chatId = chatId,
                trigger = AutoCleanTrigger.TIMER,
                olderThanHours = olderThanHours,
                wipeMediaOnly = mediaOnly,
            )
        )
        return Result.success()
    }

    companion object {
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_CHAT_ID = "chat_id"
        const val KEY_OLDER_THAN_HOURS = "older_than_hours"
        const val KEY_MEDIA_ONLY = "media_only"
    }
}
