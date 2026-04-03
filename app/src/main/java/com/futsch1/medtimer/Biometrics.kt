package com.futsch1.medtimer

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class Biometrics @AssistedInject constructor(
    @Assisted activity: FragmentActivity,
    @param:ApplicationContext private val context: Context,
    @Assisted("onSuccess") private val successCallback: () -> Unit,
    @Assisted("onFailure") private val failureCallback: () -> Unit
) {
    companion object {
        private const val KEY_ALIAS = "MedTimerBiometricKey"
    }

    @AssistedFactory
    interface Factory {
        fun create(
            activity: FragmentActivity,
            @Assisted("onSuccess") successCallback: () -> Unit,
            @Assisted("onFailure") failureCallback: () -> Unit
        ): Biometrics
    }

    private val biometricManager = BiometricManager.from(context)

    // API 28-29 only supports BIOMETRIC_STRONG (with a negative button); DEVICE_CREDENTIAL
    // as a standalone authenticator, and BIOMETRIC_STRONG | DEVICE_CREDENTIAL combined are
    // only valid on API 30+.
    private val biometricPromptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.login))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    } else {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.login))
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText(context.getString(android.R.string.cancel))
            .build()
    }

    private val deviceCredentialPromptInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.login))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()
    } else {
        null
    }

    private val biometricPrompt: BiometricPrompt = BiometricPrompt(activity, context.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            super.onAuthenticationError(errorCode, errString)
            failureCallback()
        }

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)

            if (result.cryptoObject == null) {
                Log.d(LogTags.BIOMETRICS, "Successfully authenticated with device credentials")
                successCallback()
                return
            }

            val resultCipher = result.cryptoObject?.cipher
            if (resultCipher == null) {
                Log.w(LogTags.BIOMETRICS, "Failed to authenticate with biometrics - no crypto object")
                failureCallback()
                return
            }

            try {
                val testData = "medtimer_auth".toByteArray(Charsets.UTF_8)
                resultCipher.doFinal(testData)
                Log.d(LogTags.BIOMETRICS, "Successfully authenticated with biometrics")
                successCallback()
            } catch (e: Exception) {
                Log.w(LogTags.BIOMETRICS, "Failed to authenticate with biometrics: $e")
                failureCallback()
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            authenticate()
        }
    })

    private fun generateSecretKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            Log.d(LogTags.BIOMETRICS, "Key already exists")
            return
        }
        Log.d(LogTags.BIOMETRICS, "Generating key")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
            .build()
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            "${KeyProperties.KEY_ALGORITHM_AES}/" +
                    "${KeyProperties.BLOCK_MODE_CBC}/" +
                    KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

    fun authenticate(
    ) {
        try {
            authenticateInternal()
        } catch (_: KeyPermanentlyInvalidatedException) {
            // When the biometrics are disabled, the key is invalidated. So delete it and create it again (safe since no user data is encrypted using it).
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.deleteEntry(KEY_ALIAS)
            authenticateInternal()
        }
    }

    private fun authenticateInternal() {
        val canUseStrong = biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS
        val cryptoObject = generateCryptoObject(canUseStrong)

        if (cryptoObject != null) {
            biometricPrompt.authenticate(biometricPromptInfo, cryptoObject)
        } else {
            authenticateWithDeviceCredentials()
        }
    }

    private fun generateCryptoObject(canUseStrong: Boolean): BiometricPrompt.CryptoObject? {
        return if (canUseStrong) {
            try {
                generateSecretKey()
                val cipher = getCipher()
                val secretKey = getSecretKey()
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                BiometricPrompt.CryptoObject(cipher)
            } catch (_: Exception) {
                Log.w(LogTags.BIOMETRICS, "Failed to generate crypto object")
                null
            }
        } else {
            Log.d(LogTags.BIOMETRICS, "Biometrics not supported")
            null
        }
    }

    @SuppressWarnings("kotlin:S6293")
    private fun authenticateWithDeviceCredentials() {
        if (deviceCredentialPromptInfo != null) {
            biometricPrompt.authenticate(deviceCredentialPromptInfo)
        } else {
            failureCallback()
        }
    }
}