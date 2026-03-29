package com.futsch1.medtimer.helpers

import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import com.google.android.material.color.MaterialColors

fun Context.getMaterialColor(@AttrRes attrId: Int, errorMessageComponent: String?): Int =
    MaterialColors.getColor(this, attrId, errorMessageComponent)

fun Context.getMaterialColor(@AttrRes attrId: Int, @ColorInt defaultValue: Int): Int =
    MaterialColors.getColor(this, attrId, defaultValue)

fun Context.hasBiometrics(): Boolean {
    val biometricManager = BiometricManager.from(this)
    return biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) == BIOMETRIC_SUCCESS
}
