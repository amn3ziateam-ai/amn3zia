package com.amn3zia.app.core.proxy

import com.amn3zia.app.core.tdlib.TdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import org.drinkless.tdlib.TdApi

sealed interface ProxyConfig {
    data class Socks5(val server: String, val port: Int, val username: String? = null, val password: String? = null) : ProxyConfig
    data class MtProto(val server: String, val port: Int, val secret: String) : ProxyConfig
    data object None : ProxyConfig
}

/**
 * Proxy Engine — per-account SOCKS5/MTProto proxy with rotation and kill-switch.
 *
 * Each [TdClient] gets its own proxy configuration applied via TdApi.AddProxy +
 * TdApi.EnableProxy, fully isolated from other accounts (TDLib proxies are
 * scoped to the Client instance that adds them).
 *
 * Kill-switch: when enabled, if the active proxy becomes unreachable the engine
 * disables the proxy AND pauses all network activity for that account
 * (TdApi.SetNetworkType(NetworkTypeNone)) rather than falling back to a direct
 * connection — preventing any traffic from leaking outside the proxy tunnel.
 */
class ProxyEngine(private val accountId: String, private val client: TdClient) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var killSwitchEnabled: Boolean = true
    @Volatile var rotationEnabled: Boolean = false
    @Volatile var rotationIntervalMinutes: Int = 30

    private var pool: List<ProxyConfig> = emptyList()
    private var poolIndex = 0
    private var activeTdProxyId: Int? = null
    private var rotationJob: kotlinx.coroutines.Job? = null

    fun setPool(configs: List<ProxyConfig>) {
        pool = configs
        poolIndex = 0
    }

    fun applyConfig(config: ProxyConfig) = scope.launch {
        runCatching {
            disableActiveProxy()
            when (config) {
                is ProxyConfig.None -> {
                    client.send(TdApi.DisableProxy())
                }
                is ProxyConfig.Socks5 -> {
                    val type = TdApi.ProxyTypeSocks5(config.username.orEmpty(), config.password.orEmpty())
                    val added = client.send(TdApi.AddProxy(TdApi.Proxy(config.server, config.port, type), true, ""))
                    activeTdProxyId = added.id
                }
                is ProxyConfig.MtProto -> {
                    val type = TdApi.ProxyTypeMtproto(config.secret)
                    val added = client.send(TdApi.AddProxy(TdApi.Proxy(config.server, config.port, type), true, ""))
                    activeTdProxyId = added.id
                }
            }
            verifyConnectivityOrTriggerKillSwitch()
        }
    }

    fun startRotation(configs: List<ProxyConfig>) {
        setPool(configs)
        rotationEnabled = true
        rotationJob?.cancel()
        rotationJob = scope.launch {
            while (rotationEnabled && pool.isNotEmpty()) {
                val next = pool[poolIndex % pool.size]
                poolIndex++
                applyConfig(next).join()
                kotlinx.coroutines.delay(rotationIntervalMinutes * 60_000L)
            }
        }
    }

    fun stopRotation() {
        rotationEnabled = false
        rotationJob?.cancel()
        rotationJob = null
    }

    private suspend fun disableActiveProxy() {
        activeTdProxyId?.let { id ->
            runCatching { client.send(TdApi.RemoveProxy(id)) }
            activeTdProxyId = null
        }
    }

    /**
     * Kill-switch: verify the proxy connection actually establishes. If it
     * doesn't within the timeout, cut all network for this account rather than
     * letting TDLib silently fall back to a direct (unproxied) connection.
     */
    private suspend fun verifyConnectivityOrTriggerKillSwitch() {
        if (!killSwitchEnabled) return
        // TDLib has no synchronous "get connection state" call — the state is pushed
        // via UpdateConnectionState, so wait for one that reports Ready.
        repeat(CONNECTIVITY_CHECK_ATTEMPTS) {
            val update = withTimeoutOrNull(CONNECTIVITY_CHECK_INTERVAL_MS) {
                client.updates.filterIsInstance<TdApi.UpdateConnectionState>().first()
            }
            if (update?.state is TdApi.ConnectionStateReady) return
        }
        engageKillSwitch()
    }

    private suspend fun engageKillSwitch() {
        runCatching {
            disableActiveProxy()
            client.send(TdApi.SetNetworkType(TdApi.NetworkTypeNone()))
        }
    }

    /** Manual re-enable after a kill-switch trip, once the user fixes the proxy. */
    suspend fun releaseKillSwitch() {
        runCatching { client.send(TdApi.SetNetworkType(TdApi.NetworkTypeOther())) }
    }

    companion object {
        private const val CONNECTIVITY_CHECK_ATTEMPTS = 5
        private const val CONNECTIVITY_CHECK_INTERVAL_MS = 2_000L
    }
}
