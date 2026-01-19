package com.futsch1.medtimer

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey


class Biometrics(
    val activity: FragmentActivity,
    val successCallback: () -> Unit,
    val failureCallback: () -> Unit
) {
    private val keyAlias = "MedTimerBiometricKey"

    fun hasBiometrics(): Boolean {
        val biometricManager =
            BiometricManager.from(activity)
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS
    }

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private fun generateSecretKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
        if (keyStore.containsAlias(keyAlias)) {
            Log.d(LogTags.BIOMETRICS, "Key already exists")
            return
        }
        Log.d(LogTags.BIOMETRICS, "Generating key")
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
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
        return keyStore.getKey(keyAlias, null) as SecretKey
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
            keyStore.deleteEntry(keyAlias)
            authenticateInternal()
        }
    }

    private fun authenticateInternal() {
        executor = ContextCompat.getMainExecutor(activity)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.login))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        val biometricManager = BiometricManager.from(activity)
        val canUseStrong = biometricManager.canAuthenticate(BIOMETRIC_STRONG) == BIOMETRIC_SUCCESS

        val cryptoObject = generateCryptoObject(canUseStrong)

        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                failureCallback()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)

                if (cryptoObject != null) {
                    val resultCipher = result.cryptoObject?.cipher
                    if (resultCipher != null) {
                        try {
                            val testData = "medtimer_auth".toByteArray(Charsets.UTF_8)
                            resultCipher.doFinal(testData)
                            Log.d(LogTags.BIOMETRICS, "Successfully authenticated with biometrics")
                            successCallback()
                        } catch (e: Exception) {
                            Log.w(LogTags.BIOMETRICS, "Failed to authenticate with biometrics: $e")
                            failureCallback()
                        }
                    } else {
                        Log.w(LogTags.BIOMETRICS, "Failed to authenticate with biometrics - no crypto object")
                        failureCallback()
                    }
                } else {
                    Log.d(LogTags.BIOMETRICS, "Successfully authenticated with device credentials")
                    successCallback()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                authenticate()
            }
        }

        biometricPrompt = BiometricPrompt(activity, executor, authenticationCallback)

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
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
        biometricPrompt.authenticate(promptInfo)
    }
}