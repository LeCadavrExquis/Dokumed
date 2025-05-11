package pl.fzar.dokumed.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.Charset
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val KEY_ALIAS = "DokumedPinKeyAlias"
private const val TRANSFORMATION = "AES/GCM/NoPadding"

// SharedPreferences keys for storing encrypted data and IV
private const val ENCRYPTED_PIN_PREF = "keystore_encrypted_pin_v1"
private const val IV_PREF = "keystore_iv_v1"

class KeystoreHelper(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("DokumedKeystorePrefs", Context.MODE_PRIVATE)
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun getSecretKey(): SecretKey? {
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun generateSecretKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    fun setPin(pin: String) {
        try {
            val secretKey = getSecretKey() ?: generateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(pin.toByteArray(Charset.defaultCharset()))

            sharedPreferences.edit()
                .putString(ENCRYPTED_PIN_PREF, Base64.encodeToString(encryptedData, Base64.DEFAULT))
                .putString(IV_PREF, Base64.encodeToString(iv, Base64.DEFAULT))
                .apply()
        } catch (e: Exception) {
            // Consider logging this error
            e.printStackTrace()
            // Optionally, rethrow as a custom exception to be handled by ViewModel
        }
    }

    fun checkPin(pinToCheck: String): Boolean {
        try {
            val secretKey = getSecretKey() ?: return false // PIN not set if key doesn't exist

            val encryptedDataString = sharedPreferences.getString(ENCRYPTED_PIN_PREF, null) ?: return false
            val ivString = sharedPreferences.getString(IV_PREF, null) ?: return false

            val encryptedData = Base64.decode(encryptedDataString, Base64.DEFAULT)
            val iv = Base64.decode(ivString, Base64.DEFAULT)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv) // 128 is tag length in bits for GCM
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val decryptedPinBytes = cipher.doFinal(encryptedData)
            val decryptedPin = String(decryptedPinBytes, Charset.defaultCharset())

            return decryptedPin == pinToCheck
        } catch (e: Exception) {
            // Consider logging this error
            e.printStackTrace()
            return false
        }
    }

    fun isPinSet(): Boolean {
        return sharedPreferences.contains(ENCRYPTED_PIN_PREF) && 
               sharedPreferences.contains(IV_PREF) && 
               getSecretKey() != null
    }

    fun removePin() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
            sharedPreferences.edit()
                .remove(ENCRYPTED_PIN_PREF)
                .remove(IV_PREF)
                .apply()
        } catch (e: Exception) {
            // Consider logging this error
            e.printStackTrace()
        }
    }
}
