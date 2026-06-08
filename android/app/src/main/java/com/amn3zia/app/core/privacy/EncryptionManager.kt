package com.amn3zia.app.core.privacy

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val MASTER_KEY_ALIAS = "amn3zia_master_key"
private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12

/**
 * Local Encryption (per requirements):
 *  - All sensitive local data is encrypted at rest.
 *  - Each account gets its own randomly generated 256-bit database key.
 *  - Account keys are themselves wrapped (encrypted) by a master key that
 *    lives in the Android Keystore / hardware-backed keymaster (Secure
 *    Enclave equivalent on Android) and never leaves it in plaintext.
 *
 * The wrapped per-account keys are stored in an [EncryptedSharedPreferences]
 * file (itself backed by a Keystore master key), so a compromise of app
 * storage alone does not expose any key material.
 */
class EncryptionManager(private val context: Context) {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    private val secureRandom = SecureRandom()

    private val vault by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "amn3zia_key_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    init {
        ensureMasterKeyExists()
    }

    private fun ensureMasterKeyExists() {
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) return
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Hardware-backed when the device supports StrongBox (Secure Enclave equivalent)
                .setIsStrongBoxBacked(deviceSupportsStrongBox())
                .setUserAuthenticationRequired(false)
                .build()
        )
        keyGen.generateKey()
    }

    private fun deviceSupportsStrongBox(): Boolean =
        context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)

    /**
     * Returns the raw 256-bit database encryption key for [accountId],
     * generating and persisting (wrapped) one on first use.
     *
     * This key is passed straight into TDLib's `SetTdlibParameters.databaseEncryptionKey`
     * and used as the SQLCipher key for any auxiliary local databases.
     */
    fun databaseKeyFor(accountId: String): ByteArray {
        val prefKey = "dbkey_$accountId"
        vault.getString(prefKey, null)?.let { stored ->
            return android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
        }
        val newKey = ByteArray(32).also(secureRandom::nextBytes)
        vault.edit().putString(prefKey, android.util.Base64.encodeToString(newKey, android.util.Base64.NO_WRAP)).apply()
        return newKey
    }

    /** Irreversibly forgets [accountId]'s key — used by the panic button wipe. */
    fun destroyKeyFor(accountId: String) {
        vault.edit().remove("dbkey_$accountId").apply()
    }

    /** Wipes every stored key — used by self-destruct (full app wipe). */
    fun destroyAllKeys() {
        vault.edit().clear().apply()
        runCatching { keyStore.deleteEntry(MASTER_KEY_ALIAS) }
    }

    /** Generic encrypt/decrypt for small blobs (e.g. cached settings) using the Keystore master key. */
    fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return iv + ciphertext
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > GCM_IV_BYTES) { "Payload too short" }
        val iv = payload.copyOfRange(0, GCM_IV_BYTES)
        val ciphertext = payload.copyOfRange(GCM_IV_BYTES, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterSecretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun masterSecretKey(): SecretKey =
        keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
}
