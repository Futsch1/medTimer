package com.futsch1.medtimer

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor


class Biometrics(
    val activity: FragmentActivity,
    val successCallback: () -> Unit,
    val failureCallback: () -> Unit
) {
    fun hasBiometrics(): Boolean {
        val biometricManager =
            BiometricManager.from(activity)
        return biometricManager.canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS
    }

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    fun authenticate(
    ) {
        executor = ContextCompat.getMainExecutor(activity)
        biometricPrompt = getPrompt(activity, failureCallback, successCallback)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.login))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()

        @Suppress("kotlin:S6293") // No cryptography required inside app, only use authentication to protect app access
        biometricPrompt.authenticate(promptInfo)
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
                successCallback()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                authenticate()
            }
        })
}