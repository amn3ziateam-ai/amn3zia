package com.amn3zia.app.core.privacy

import org.drinkless.tdlib.TdApi

/**
 * Anti-Tracking — client-side controls over metadata-leaking behaviors:
 *  - disableLinkPreviews: strip link-preview generation from outgoing messages
 *    (TDLib generates previews server-side from the URL the moment you type it,
 *    which can leak your IP/activity to the link's domain via Telegram's preview
 *    fetcher in some configurations) and suppress preview rendering for incoming ones.
 *  - blockExternalMedia: prevent TDLib from auto-downloading photos/videos/web
 *    pages from chats, which avoids passive metadata generation (read times,
 *    network fingerprints) for content you didn't explicitly open.
 *  - reduceMetadata: strips/avoids sending optional fields (e.g. precise typing
 *    timestamps via SendChatAction repetition, online status pings) that aren't
 *    required for basic protocol function.
 */
class AntiTrackingPolicy {

    @Volatile var disableLinkPreviews: Boolean = true
    @Volatile var blockExternalMedia: Boolean = true
    @Volatile var reduceMetadata: Boolean = true

    /** Called from PrivacyInterceptor.beforeOutgoingRequest. Returns a possibly-rewritten function, or null to drop it. */
    fun <R : TdApi.Object> filterOutgoing(function: TdApi.Function<R>): TdApi.Function<R>? {
        return when {
            disableLinkPreviews && function is TdApi.GetLinkPreview -> null
            disableLinkPreviews && function is TdApi.GetWebPageInstantView -> null
            blockExternalMedia && function is TdApi.DownloadFile && !function.synchronous -> null
            else -> function
        }
    }

    /** Builds the autodownload settings TDLib should use, applied at session start and on toggle. */
    fun autoDownloadSettings(): TdApi.AutoDownloadSettings = TdApi.AutoDownloadSettings(
        !blockExternalMedia,   // isAutoDownloadEnabled
        0, 0, 0,               // max photo / video / other file sizes (bytes) — 0 = none when blocked
        0,                     // videoUploadBitrate
        false, false, false, false, // preload* / use* flags — all disabled for anti-tracking
    )
}
