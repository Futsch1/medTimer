package com.futsch1.medtimer

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
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
            return
        }
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

    private fun authenticateInternal(
    ) {
        executor = ContextCompat.getMainExecutor(activity)
        biometricPrompt = getPrompt(activity, failureCallback, successCallback)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.login))
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        generateSecretKey()
        val cipher = getCipher()
        val secretKey = getSecretKey()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipher),
        )
    }

    private fun getPrompt(
        activity: FragmentActivity,
        failureCallback: () -> Unit,
        successCallback: () -> Unit
    ) = BiometricPrompt(
        activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                failureCallback()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
                super.onAuthenticationSucceeded(result)
                val cryptoObject = result.cryptoObject
                val cipher = cryptoObject?.cipher
                if (cipher != null) {
                    try {
                        // Perform a cryptographic operation that is required for access.
                        // Here we encrypt fixed data as a proof of successful authentication.
                        val testData = "medtimer_auth".toByteArray(Charsets.UTF_8)
                        cipher.doFinal(testData)
                        successCallback()
                    } catch (_: Exception) {
                        failureCallback()
                    }
                } else {
                    failureCallback()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                authenticate()
            }
        })
}