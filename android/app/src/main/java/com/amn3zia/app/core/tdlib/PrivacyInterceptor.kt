package com.amn3zia.app.core.tdlib

import org.drinkless.tdlib.TdApi

/**
 * The single chokepoint between the UI/business logic and raw TDLib traffic.
 *
 * Per the architecture requirement ("Privacy Layer MUST intercept and control
 * all sensitive behavior"), every [TdClient] routes both directions of traffic
 * through this interface:
 *
 *  - [beforeOutgoingRequest] — called before any TdApi.Function is sent. Can
 *    veto the call entirely (return false) or let it pass. Used for:
 *      Ghost Mode      -> drop/delay TdApi.ViewMessages, TdApi.SetChatDraftMessage
 *                         based typing pings, TdApi.SendChatAction
 *      Anti-Tracking   -> strip outgoing requests that would fetch link previews
 *                         for untrusted domains
 *
 *  - [onIncomingUpdate] — called for every update TDLib pushes. Used for:
 *      Auto-Clean      -> notice new messages in chats marked for timed deletion
 *      Ghost Mode      -> intercept UpdateChatReadInbox to apply delayed read
 *                         marking instead of TDLib's default behaviour
 *
 * Implementations must be fast and non-blocking — they run on the TDLib
 * update-dispatch thread.
 */
interface PrivacyInterceptor {
    fun beforeOutgoingRequest(accountId: String, function: TdApi.Function<*>): Boolean
    fun onIncomingUpdate(accountId: String, update: TdApi.Object)
}
